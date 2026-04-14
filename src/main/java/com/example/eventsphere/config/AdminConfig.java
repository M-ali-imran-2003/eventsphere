package com.example.eventsphere.config;

import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.UserRole;
import com.example.eventsphere.enums.UserStatus;
import com.example.eventsphere.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
@Slf4j
@Configuration
public class AdminConfig {

    @Value("${admin.default.username}")
    private String adminUsername;

    @Value("${admin.default.email}")
    private String adminEmail;

    @Value("${admin.default.password}")
    private String adminPassword;

    @Value("${admin.default.cnic}")
    private String adminCnic;

    @Value("${admin.default.phone}")
    private String adminPhone;

    @Value("${admin.default.pic}")
    private String adminPic;

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            try {
                // THE UPGRADE: We now safely rely on the immutable username
                if (!userRepository.existsByUsername(adminUsername)) {

                    User admin = new User();
                    admin.setName("System Admin");
                    admin.setUsername(adminUsername); // Locked forever
                    admin.setEmail(adminEmail);       // Changeable via profile update
                    admin.setPassword(encoder.encode(adminPassword));
                    admin.setRole(UserRole.ADMIN);
                    admin.setStatus(UserStatus.ACTIVE);
                    admin.setProfilePic(adminPic);
                    admin.setCnic(adminCnic);
                    admin.setPhoneNo(adminPhone);
                    admin.setCreatedAt(LocalDateTime.now());
                    admin.setModifiedAt(LocalDateTime.now());

                    userRepository.save(admin);
                    log.info("Default Admin created successfully with username: {}", adminUsername);
                } else {
                    log.info("Default Admin already exists. Skipping seeding.");
                }
            } catch (Exception e) {
                log.error("Admin seeding failed: ", e);
            }
        };
    }
}