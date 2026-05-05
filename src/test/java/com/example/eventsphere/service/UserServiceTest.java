package com.example.eventsphere.service;

import com.example.eventsphere.dto.*;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.UserRole;
import com.example.eventsphere.enums.UserStatus;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.UserRepository;
import com.example.eventsphere.utils.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private GenericMapper mapper;
    @Mock private SecurityUtil securityUtil;
    @Mock private FileService fileService;

    @InjectMocks
    private UserService userService;

    private User mockAdmin;

    @BeforeEach
    void setUp() {
        mockAdmin = new User();
        mockAdmin.setId(UUID.randomUUID());
    }

    // ==========================================
    // GET USERS TESTS
    // ==========================================
    @Test
    void findById_ShouldThrowException_WhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.findById(id);
        });

        assertEquals("User Not Found", exception.getMessage());
    }

    // ==========================================
    // ADD ADMIN TESTS (With File Upload)
    // ==========================================
    @Test
    void addAdmin_ShouldHashPasswordAndSave_WhenValid() throws IOException {
        NewUserDTO newAdminDTO = new NewUserDTO();
        newAdminDTO.setUsername("admin123");
        newAdminDTO.setPassword("rawPassword");
        MockMultipartFile fakeImage = new MockMultipartFile("profilePic", "test.jpg", "image/jpeg", "image data".getBytes());
        newAdminDTO.setProfilePic(fakeImage);

        User mappedUser = new User();

        when(userRepository.findConflicts(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(mapper.map(any(NewUserDTO.class), eq(User.class))).thenReturn(mappedUser);
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword");
        when(fileService.saveFile(any(), any())).thenReturn("https://s3.aws.com/test.jpg");

        // NEW WAY: Static Mocking Block
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUser).thenReturn(mockAdmin);

            // WHEN
            userService.addAdmin(newAdminDTO);

            // THEN
            assertEquals("hashedPassword", mappedUser.getPassword());
            assertEquals(UserRole.ADMIN, mappedUser.getRole());
            assertEquals(UserStatus.ACTIVE, mappedUser.getStatus());
            assertEquals("https://s3.aws.com/test.jpg", mappedUser.getProfilePic());
            verify(userRepository, times(1)).save(mappedUser);
        }
    }

    // ==========================================
    // UPDATE STATUS TESTS
    // ==========================================
    @Test
    void updateUserStatus_ShouldThrowException_WhenAdminDisablesThemselves() {
        UUID adminId = mockAdmin.getId();
        User adminUser = new User();
        adminUser.setId(adminId);

        ChangeUserStatusDTO statusDTO = new ChangeUserStatusDTO();
        statusDTO.setStatus(UserStatus.INACTIVE);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        // NEW WAY: Static Mocking Block
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUser).thenReturn(mockAdmin);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                userService.updateUserStatus(statusDTO, adminId);
            });

            assertEquals("Cannot Change the status of Current User", exception.getMessage());
        }
    }
}