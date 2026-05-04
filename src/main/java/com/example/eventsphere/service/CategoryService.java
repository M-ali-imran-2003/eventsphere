package com.example.eventsphere.service;

import com.example.eventsphere.dto.CategoryDTO;
import com.example.eventsphere.dto.UpdateCategoryDTO;
import com.example.eventsphere.entity.Category;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.CategoryRepository;
import com.example.eventsphere.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final GenericMapper mapper;

    @Autowired
    CategoryService(CategoryRepository categoryRepository, GenericMapper mapper) {

        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    public void addCategory(String name) {
        // 1. Check if it already exists
        if (categoryRepository.existsByName(name)) {
            log.warn("Category Already Exists With Name: {}", name);
            throw new RuntimeException("Category already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setStatus(AppStatus.ACTIVE);
        category.setCode(name.toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .trim()
                .replaceAll("\\s+", "-"));
        category.setCreatedAt(LocalDateTime.now());
        category.setCreatedBy(SecurityUtil.getCurrentUser().getId());
        category.setModifiedAt(LocalDateTime.now());
        category.setModifiedBy(SecurityUtil.getCurrentUser().getId());

        categoryRepository.save(category);
    }

    public void updateCategory(UUID id, UpdateCategoryDTO updateCategory) {
        // 1. Check if it already exists
        Category category = categoryRepository.findById(id).orElseThrow(()-> new RuntimeException("Category Not Found with the ID"));


        if(updateCategory.getName() !=null && !updateCategory.getName().isBlank()) {

            if (!categoryRepository.findCategoryConflicts(updateCategory.getName(),id).isEmpty()) {
                log.warn("Category Already Exists With Name: {}", updateCategory.getName());
                throw new RuntimeException("Category already exists with this name");
            }

            category.setName(updateCategory.getName());
            category.setCode(updateCategory.getName().toLowerCase()
                    .replaceAll("[^a-z0-9 ]", "")
                    .trim()
                    .replaceAll("\\s+", "-"));
        }

        if(updateCategory.getStatus() !=null && !updateCategory.getStatus().name().isBlank()) {
            category.setStatus(updateCategory.getStatus());
        }

        category.setModifiedAt(LocalDateTime.now());
        category.setModifiedBy(SecurityUtil.getCurrentUser().getId());

        categoryRepository.save(category);
    }

    public List<CategoryDTO> getAllCategories() {

        return mapper.mapList(categoryRepository.findAll(), CategoryDTO.class);
    }

    public Category findCategory(UUID id) {

        return categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category Not Found"));
    }

//    public List<CategoryDTO> getAllActiveCategories() {
//        return categoryRepository.findAll();
//    }
}
