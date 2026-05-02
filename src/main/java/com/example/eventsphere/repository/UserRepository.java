package com.example.eventsphere.repository;

import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE (" +
            "(:username IS NULL OR u.username = :username) OR " +
            "(:email IS NULL OR u.email = :email) OR " +
            "(:phone IS NULL OR u.phoneNo = :phone) OR " +
            "(:cnic IS NULL OR u.cnic = :cnic)" +
            ") AND (:id IS NULL OR u.id <> :id)")
    List<User> findConflicts(String username, String email, String phone, String cnic, UUID id);

    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    long countByRole(UserRole role);
}
