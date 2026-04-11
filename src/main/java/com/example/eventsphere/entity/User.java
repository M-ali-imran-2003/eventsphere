package com.example.eventsphere.entity;

import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.*; // Added for Java validation
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    @NotBlank(message = "Name is required")
    @Size(max = 30, message = "Name must be under 30 characters")
    @Column(name = "name")
    private String name;


    @NotBlank(message = "Username is required")
    @Size(max = 30, message = "Username must be under 30 characters")
    @Column(name = "username")
    private String username;

    @NotBlank(message = "CNIC is required")
    @Size(max = 13, message = "CNIC must be under 13 characters")
    @Column(name = "cnic")
    private String cnic;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    @Size(max = 30, message = "Email must be under 30 characters")
    @Column(name = "email")
    private String email;

    @NotBlank(message = "Password is required")
    @Column(name = "password")
    private String password;

    @Column(name = "profile_pic")
    private String profilePic;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    @Column(name = "phone_no")
    private String phoneNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AppStatus Status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by")
    private UUID modifiedBy;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + getRole().name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Return true to indicate the account is valid
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Return true to indicate the account is not locked
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Return true to indicate passwords don't expire
    }

    @Override
    public boolean isEnabled() {
        return true; // Return true to indicate the user is active
    }
}
