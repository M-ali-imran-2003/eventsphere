package com.example.eventsphere.service;

import com.example.eventsphere.dto.CompleteProfileRequest;
import com.example.eventsphere.dto.LoginResponse;
import com.example.eventsphere.dto.SignupRequest;
import com.example.eventsphere.dto.VerifyOtpRequest;
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

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlackList tokenBlackList;
    private final OtpService otpService;
    private final EmailService emailService;
    private final GenericMapper mapper;


    @Autowired
    public AuthService(UserRepository userRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder, TokenBlackList tokenBlackList, OtpService otpService, EmailService emailService, GenericMapper mapper) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.tokenBlackList = tokenBlackList;
        this.otpService = otpService;
        this.emailService = emailService;
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

        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        log.info("Successful login for user: {}", user.getUsername());
        return new LoginResponse(
                token,
                user.getRole().name()
        );
    }

    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            tokenBlackList.add(jwt);
            log.info("User successfully logged out and token blacklisted.");
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
    public LoginResponse completeSignup(CompleteProfileRequest request, String registrationToken) {

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
        String accessToken = jwtUtil.generateToken(savedUser.getId(), savedUser.getRole().name());

        return new LoginResponse(accessToken, savedUser.getRole().name());
    }


}
