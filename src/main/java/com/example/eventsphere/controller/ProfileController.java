package com.example.eventsphere.controller;

import com.example.eventsphere.dto.ProfileDTO;
import com.example.eventsphere.dto.UpdateProfileDTO;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@Slf4j
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/getProfile")
    public ResponseEntity<ProfileDTO> getProfile(@AuthenticationPrincipal User user) {
        ProfileDTO profile = userService.getProfile(user.getId());
        return ResponseEntity.ok(profile);
    }

    @PatchMapping(value = "/updateProfile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> updateProfile(
            @AuthenticationPrincipal User user,
            @ModelAttribute UpdateProfileDTO profile) throws IOException { // Throws signature required for S3

        userService.updateProfile(profile, user.getId());

        return ResponseEntity.ok(Map.of(
                "message", "Profile updated successfully",
                "status", "Success"
        ));
    }
    }
