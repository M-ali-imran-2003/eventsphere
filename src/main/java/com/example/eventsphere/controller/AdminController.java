package com.example.eventsphere.controller;


import com.example.eventsphere.service.AdminService;
import com.example.eventsphere.service.CategoryService;
import com.example.eventsphere.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Slf4j
public class AdminController {
    private final UserService userService;
    private final AdminService adminService;
    private final CategoryService categoryService;


    public AdminController(UserService userService, AdminService adminService, CategoryService categoryService) {

        this.userService = userService;
        this.adminService = adminService;

        this.categoryService = categoryService;
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
