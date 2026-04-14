package com.example.eventsphere.controller;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.UserRole;
import com.example.eventsphere.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getAllUsers")
    public ResponseEntity<List<UsersDTO>> getAllUsers() {
        List<UsersDTO> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getUserById/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        UserDTO user = userService.findById(id); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/updateUser/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody UpdateUserDTO userDTO) {
        userService.updateUser(userDTO,id); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/addAdmin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDTO> addAdmin(@Valid @ModelAttribute NewUserDTO userDTO) {
        userService.addAdmin(userDTO); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok().build();
    }
}
