package com.example.eventsphere.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Slf4j
@Service
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final int otpExpirationMinutes;

    @Autowired
    public OtpService(StringRedisTemplate redisTemplate, @Value("${otp.expiration.minutes}") int otpExpirationMinutes) {
        this.redisTemplate = redisTemplate;
        this.otpExpirationMinutes = otpExpirationMinutes;
    }

    // 1. Generate a 6-digit code and save it to Redis
    public String generateAndSaveOtp(String email) {
        // Generate a random 6-digit number (e.g., 045921)
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Save to Redis with a strict 10-minute Time-To-Live (TTL)
        redisTemplate.opsForValue().set(email, otp, Duration.ofMinutes(otpExpirationMinutes));

        log.info("Generated new OTP for {}, expires in {} minutes", email, otpExpirationMinutes);
        return otp;
    }

    // 2. Verify the code the user typed in
    public boolean verifyOtp(String email, String inputOtp) {
        // Look up the email in our Redis notepad
        String savedOtp = redisTemplate.opsForValue().get(email);

        // If the code exists AND matches what the user typed...
        if (savedOtp != null && savedOtp.equals(inputOtp)) {
            // Success! Delete the note immediately so it can't be reused
            redisTemplate.delete(email);
            log.info("OTP successfully verified and deleted for {}", email);
            return true;
        }

        // If it doesn't match, or if it expired
        log.warn("Invalid or expired OTP attempt for {}", email);
        return false;
    }
}
