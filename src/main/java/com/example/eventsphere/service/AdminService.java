package com.example.eventsphere.service;

import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.UserRepository;
import com.example.eventsphere.utils.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final GenericMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;

    @Autowired
    public AdminService(UserRepository userRepository,GenericMapper mapper,SecurityUtil securityUtil,PasswordEncoder passwordEncoder){
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.securityUtil = securityUtil;
    }

//    @Transactional
//    public void addAdmin(UserDTO user){
//
//        if(userRepository.findByUsername(user.getUsername()).isPresent()) throw new RuntimeException("username already exists");
//       // if(userRepository.findByEmail(user.getEmail()).isPresent()) throw new RuntimeException("Email already exists");
//        //if(userRepository.findByCnic(user.getCnic()).isPresent()) throw new RuntimeException("CNIC is already registered");
//
//        User dbuser = mapper.map(user, User.class);
////        dbuser.setPassword(passwordEncoder.encode(user.getPassword()));
//        dbuser.setRole(UserRole.ADMIN);
//        dbuser.setStatus(AppStatus.ACTIVE);
//        dbuser.setCreatedAt(LocalDateTime.now());
//        dbuser.setModifiedAt(LocalDateTime.now());
//        dbuser.setCreatedBy(securityUtil.getCurrentUser().getId());
//        dbuser.setModifiedBy(securityUtil.getCurrentUser().getId());
//
//        userRepository.save(dbuser);
//
//    }
}
