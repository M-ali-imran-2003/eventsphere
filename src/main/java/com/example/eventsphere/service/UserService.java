package com.example.eventsphere.service;

import com.example.eventsphere.dto.ProfileDTO;
import com.example.eventsphere.dto.UpdateProfileDTO;
import com.example.eventsphere.dto.UserDTO;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.enums.FileType;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

 private final UserRepository userRepository;
 private final PasswordEncoder passwordEncoder;
 private final GenericMapper mapper;
 private final FileService fileService;

 @Autowired
 public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, GenericMapper mapper, FileService fileService){
  this.userRepository = userRepository;
     this.passwordEncoder = passwordEncoder;
     this.mapper = mapper;
     this.fileService = fileService;
 }

 public List<UserDTO> findAllUsers(){

  return mapper.mapList(userRepository.findAll(),UserDTO.class);
 }

 public void saveUser(User user){
  userRepository.save(user);
 }

 public UserDTO findById(UUID id)
 {
  User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User Not Found"));
  return mapper.map(user,UserDTO.class);
 }

 public ProfileDTO getProfile(UUID id)
 {
  User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User Not Found"));
  if(!user.getStatus().equals(AppStatus.ACTIVE)) throw new RuntimeException("User is Not Active");

  return mapper.map(user,ProfileDTO.class);
 }

 @Transactional
 public void updateProfile(UpdateProfileDTO profileDTO, UUID id) throws IOException {
  User existingUser = userRepository.findById(id).orElseThrow(() -> {
   log.warn("Attempted to update profile for non-existent user ID: {}", id);
   return new RuntimeException("User Not Found");
  });

  List<User> conflicts = userRepository.findConflicts(
          null, profileDTO.getEmail(), profileDTO.getPhoneNo(), null, id);

  if (!conflicts.isEmpty()) {
   for (User c : conflicts) {
    if (profileDTO.getEmail() != null && profileDTO.getEmail().equals(c.getEmail())) {
     throw new RuntimeException("Email Already Exists");
    }
    if (profileDTO.getPhoneNo() != null && profileDTO.getPhoneNo().equals(c.getPhoneNo())) {
     throw new RuntimeException("Phone Number Already Exists");
    }
   }
  }
  if (profileDTO.getName() != null && !profileDTO.getName().isBlank()) {
   existingUser.setName(profileDTO.getName());
  }


  if (profileDTO.getEmail() != null && !profileDTO.getEmail().isBlank()) {
   existingUser.setEmail(profileDTO.getEmail());
  }

  if (profileDTO.getPhoneNo() != null && !profileDTO.getPhoneNo().isBlank()) {
   existingUser.setPhoneNo(profileDTO.getPhoneNo());
  }

  // --- PASSWORD CHECKER ---
  // Only update if password is provided and not just an empty string
  if (profileDTO.getPassword() != null && !profileDTO.getPassword().isBlank()) {
   existingUser.setPassword(passwordEncoder.encode(profileDTO.getPassword()));
  }

  // --- FILE/IMAGE CHECKER ---
  if (profileDTO.getProfilePic() != null && !profileDTO.getProfilePic().isEmpty()) {
   // 1. Keep track of the old file path
   String oldFilePath = existingUser.getProfilePic();

   // 2. Save the NEW file first
   String imageUrl = fileService.saveFile(profileDTO.getProfilePic(), FileType.IMAGE);

   // 3. Delete the OLD file only if it actually exists
   if (oldFilePath != null && !oldFilePath.isBlank()) {
    try {
     fileService.deleteFile(oldFilePath);
    } catch (Exception e) {
     // Log the error but don't stop the update.
     // Better to have a "ghost" file on disk than a crashed profile update.
     log.error("Could not delete old profile pic for user {}. File path: {}", id, oldFilePath, e);    }
   }

   // 2. Set the new URL to the user entity
   existingUser.setProfilePic(imageUrl);
  }

  existingUser.setModifiedAt(LocalDateTime.now());
  existingUser.setModifiedBy(id);

  log.debug("User before save: {}", existingUser);  // Save the updated entity back to the database
  userRepository.saveAndFlush(existingUser);
  log.info("Successfully updated profile for user ID: {}", id);
 }

}
