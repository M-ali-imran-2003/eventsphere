package com.example.eventsphere.repository;

import com.example.eventsphere.entity.Category;
import com.example.eventsphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByCode(String code);

    boolean existsByName(String name);

    @Query("SELECT c FROM Category c WHERE (" +
            "(:name IS NULL OR c.name = :name)" +
            ") AND (:id IS NULL OR c.id <> :id)")
    List<Category> findCategoryConflicts(String name, UUID id);

}
