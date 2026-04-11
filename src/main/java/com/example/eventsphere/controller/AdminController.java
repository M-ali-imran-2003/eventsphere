package com.example.eventsphere.controller;


import com.example.eventsphere.dto.UserDTO;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.service.AdminService;
import com.example.eventsphere.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {
    private final UserService userService;
    private final AdminService adminService;

    public AdminController(UserService userService,AdminService adminService) {

        this.userService = userService;
        this.adminService = adminService;

    }

    @GetMapping("/getAllUsers")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/getUserById/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        UserDTO user = userService.findById(id); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(user);
    }

//    @PostMapping("/addAdmin")
//    public ResponseEntity<?> addAdmin(@RequestBody UserDTO user) {
//        try{
//            adminService.addAdmin(user);
//            return new ResponseEntity<>(HttpStatus.OK);
//        }
//        catch (RuntimeException e){
//            return ResponseEntity.badRequest().body(Map.of(
//                    "Error", e.getMessage(),
//                    "Status", "Failed"));
//        }
//
//        }
}
