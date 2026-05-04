package com.example.eventsphere.service;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.Token;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.UserRole;
import com.example.eventsphere.enums.UserStatus;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.UserRepository;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.security.TokenBlackList;
import com.example.eventsphere.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlackList tokenBlackList;
    private final OtpService otpService;
    private final EmailService emailService;
    private final TokenService tokenService;
    private final GenericMapper mapper;


    @Autowired
    public AuthService(UserRepository userRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder, TokenBlackList tokenBlackList, OtpService otpService, EmailService emailService, TokenService tokenService, GenericMapper mapper) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.tokenBlackList = tokenBlackList;
        this.otpService = otpService;
        this.emailService = emailService;
        this.tokenService = tokenService;
        this.mapper = mapper;
    }

    public LoginResponse loginUser(String identifier, String password) {
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Invalid Credentials"));

        if (user.getRole().equals(UserRole.ADMIN)) {
            log.warn("Failed login attempt: Admin tried to login via standard user portal. Identifier: {}", identifier);
            throw new RuntimeException("Access Denied: Admin Not Allowed.");
        }
        return verifyAndGenerateResponse(user, password);
    }

    public LoginResponse loginAdmin(String identifier, String password) {
        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("Invalid Credentials"));

        // THE GATEKEEPER: Block token generation if not an admin
        if (!user.getRole().equals(UserRole.ADMIN)) {
            log.warn("Failed login attempt: Non-admin tried to login via admin portal. Identifier: {}", identifier);
            throw new RuntimeException("Access Denied: Admin privileges required.");
        }

        return verifyAndGenerateResponse(user, password);
    }

    private LoginResponse verifyAndGenerateResponse(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            throw new RuntimeException("Account is inactive or suspended.");
        }

        String accessToken = jwtUtil.generateToken(user.getId(), user.getRole().name(),user.getUsername());
        String refreshToken = tokenService.createRefreshToken(user);

        log.info("Successful login for user: {}", user.getUsername());
        return new LoginResponse(
                accessToken,
                refreshToken,
                user.getRole().name()
        );
    }

    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            // 1. Blacklist the short-lived JWT
            tokenBlackList.add(jwt);

            // 2. Extract the User ID and kill their Refresh Token in the database
            String userId = jwtUtil.extractUserId(jwt);
            tokenService.revokeAllUserTokens(UUID.fromString(userId));

            log.info("User successfully logged out and all tokens revoked.");
            return;
        }
        throw new RuntimeException("No valid token provided for logout."); // Triggers Global Handler!
    }

    public void initiateSignup(SignupRequest request) {
        // Check if user already exists in the database
        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent() && existingUser.get().getStatus() == UserStatus.ACTIVE) {
            throw new RuntimeException("Email is already registered.");
        }

        // Manager tells Workers what to do
        String otp = otpService.generateAndSaveOtp(request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), otp);
        log.info("Signup initiated for email: {}", request.getEmail());
    }

    public String verifyOtp(VerifyOtpRequest request) {
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP.");
        }

        // If valid, print the Golden Ticket (Registration Token)
        return jwtUtil.generateRegistrationToken(request.getEmail());
    }

    // ACT 3: Complete Profile
    public void completeSignup(CompleteProfileRequest request, String registrationToken) {

        if(tokenBlackList.isBlacklisted(registrationToken)){
            throw new RuntimeException("Token is invalidated");
        }

        String email;
        try {
            // 1. Validate and extract. (JJWT throws exceptions here if anything is wrong)
            jwtUtil.validateToken(registrationToken);
            email = jwtUtil.extractEmail(registrationToken);

        } catch (ExpiredJwtException ex) {
            // EXACT MATCH: The 15 minutes are up.
            log.info("Registration token expired. User took too long to complete profile.");
            throw new RuntimeException("Session expired. Please enter your email to get a new code.");

        } catch (SignatureException | MalformedJwtException ex) {
            // TAMPERING MATCH: Someone tried to fake the token.
            log.warn("Invalid JWT signature/format intercepted during signup");
            throw new RuntimeException("Invalid security token. Please restart the signup process.");

        } catch (Exception ex) {
            // FALLBACK: Anything else goes wrong.
            log.error("Unexpected error parsing registration token", ex);
            throw new RuntimeException("An error occurred during verification. Please try again.");
        }

        // 2. Double-check they didn't sign up in another tab while waiting
        List<User> conflicts = userRepository.findConflicts(
                request.getUsername(), email, request.getPhoneNo(), request.getCnic(), null);
        if (!conflicts.isEmpty()) {
            for (User existingUser : conflicts) {
                // Check Email (Usually the most common conflict)
                if (existingUser.getEmail().equalsIgnoreCase(email)) {
                    throw new RuntimeException("This email is already registered. Please log in.");
                }
                // Check Username
                if (request.getUsername() != null && request.getUsername().equalsIgnoreCase(existingUser.getUsername())) {
                    throw new RuntimeException("This username is already taken. Please choose another.");
                }
                // Check CNIC
                if (request.getCnic() != null && request.getCnic().equals(existingUser.getCnic())) {
                    throw new RuntimeException("This CNIC is already registered to another account.");
                }
                // Check Phone Number
                if (request.getPhoneNo() != null && request.getPhoneNo().equals(existingUser.getPhoneNo())) {
                    throw new RuntimeException("This phone number is already registered.");
                }
            }

            // Fallback just in case something else matched
            throw new RuntimeException("An account with these details already exists.");
        }

        if (request.getRole() == UserRole.ADMIN) {
            log.warn("SECURITY ALERT: User {} attempted to register as an ADMIN!", email);
            throw new RuntimeException("You do not have permission to register as an Admin.");
        }

        // 3. Create and save the new User
        User newUser = new User();
        newUser = mapper.map(request,User.class);
        newUser.setEmail(email);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setPassword(passwordEncoder.encode(request.getPassword())); // Always hash!
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setModifiedAt(LocalDateTime.now());
        User savedUser = userRepository.save(newUser);
        log.info("New user successfully registered: {}", savedUser.getEmail());

        // 4. Generate the final Access Token so they are immediately logged in
        tokenBlackList.add(registrationToken);
    }

    public void initiateForgotPassword(ForgotPasswordInitiateRequest request) {
        // Find the user. If they don't exist, we throw a generic error to prevent email enumeration.
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email not found."));

        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            throw new RuntimeException("Account is inactive or suspended.");
        }

        // Generate OTP via your OtpService and send via your EmailService
        String otp = otpService.generateAndSaveOtp(user.getEmail());
        emailService.sendPasswordResetEmail(user.getEmail(), otp);
        log.info("Forgot password initiated for email: {}", user.getEmail());
    }

    // STEP 2: VERIFY OTP AND ISSUE TEMPORARY TOKEN
    public String verifyForgotPasswordOtp(ForgotPasswordVerifyRequest request) {
        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!isValid) {
            throw new RuntimeException("Invalid or expired OTP.");
        }

        // Reuse your Registration Token logic! It's a perfect 15-minute temporary token.
        log.info("OTP verified for forgot password: {}", request.getEmail());
        return jwtUtil.generateRegistrationToken(request.getEmail());
    }

    // STEP 3: RESET THE PASSWORD
    public void resetPassword(ForgotPasswordResetRequest request, String resetToken) {

        if(tokenBlackList.isBlacklisted(resetToken)){
            throw new RuntimeException("Token is invalidated");
        }

        // ADD THIS NEW CHECK
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match.");
        }

        String email;
        try {
            // Validate and extract using your existing logic
            jwtUtil.validateToken(resetToken);
            email = jwtUtil.extractEmail(resetToken);
        } catch (ExpiredJwtException ex) {
            throw new RuntimeException("Reset token expired. Please request a new OTP.");
        } catch (Exception ex) {
            throw new RuntimeException("Invalid security token. Please restart the reset process.");
        }

        // Find the user and update the password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setModifiedAt(LocalDateTime.now());
        user.setModifiedBy(user.getId());

        userRepository.save(user);
        tokenBlackList.add(resetToken);
        log.info("Password successfully reset for user: {}", email);
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // 1. Validate the raw token against the database hash
        Token validToken = tokenService.verifyRefreshToken(request.getRefreshToken());

        // 2. Find the user
        User user = userRepository.findById(validToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. Generate a brand new Access Token
        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getRole().name(), user.getUsername());

        // 4. Token Rotation (Highly Secure): Issue a new Refresh Token and invalidate the old one
        String newRefreshToken = tokenService.createRefreshToken(user);

        log.info("Successfully refreshed tokens for user: {}", user.getUsername());
        return new LoginResponse(newAccessToken, newRefreshToken, user.getRole().name());
    }


}
