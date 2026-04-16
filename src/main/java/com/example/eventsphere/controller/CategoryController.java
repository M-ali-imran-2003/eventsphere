package com.example.eventsphere.controller;

import com.example.eventsphere.dto.CategoryDTO;
import com.example.eventsphere.dto.NewCategoryRequest;
import com.example.eventsphere.dto.UpdateCategoryDTO;
import com.example.eventsphere.entity.Category;
import com.example.eventsphere.service.CategoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-categories")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> categories = categoryService.getAllCategories(); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(categories);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-category-by-id/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable UUID id) {
        Category category = categoryService.findCategory(id); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok(category);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add-category")
    public ResponseEntity<?> addCategory(@RequestBody NewCategoryRequest categoryRequest) {
        categoryService.addCategory(categoryRequest.getName()); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/update-category/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable UUID id,@Valid @RequestBody UpdateCategoryDTO updateCategory) {
        categoryService.updateCategory(id,updateCategory); // If not found, throws RuntimeException -> GlobalHandler
        return ResponseEntity.ok().build();
    }
}
