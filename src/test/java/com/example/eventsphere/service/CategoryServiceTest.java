package com.example.eventsphere.service;

import com.example.eventsphere.dto.CategoryDTO;
import com.example.eventsphere.dto.UpdateCategoryDTO;
import com.example.eventsphere.entity.Category;
import com.example.eventsphere.entity.User;
import com.example.eventsphere.enums.AppStatus;
import com.example.eventsphere.mapper.GenericMapper;
import com.example.eventsphere.repository.CategoryRepository;
import com.example.eventsphere.utils.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.mockStatic;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private SecurityUtil securityUtil;
    @Mock private GenericMapper mapper;

    @InjectMocks
    private CategoryService categoryService;

    private User mockAdmin;

    @BeforeEach
    void setUp() {
        // We set up a fake admin user because your service checks securityUtil.getCurrentUser().getId()
        mockAdmin = new User();
        mockAdmin.setId(UUID.randomUUID());
    }

    // ==========================================
    // ADD CATEGORY TESTS
    // ==========================================
    @Test
    void addCategory_ShouldSave_WhenNameIsUnique() {
        String newCategoryName = "Tech Conference";

        when(categoryRepository.existsByName(newCategoryName)).thenReturn(false);

        // NEW WAY: Static Mocking Block
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUser).thenReturn(mockAdmin);

            categoryService.addCategory(newCategoryName);

            verify(categoryRepository, times(1)).save(any(Category.class));
        }
    }

    @Test
    void addCategory_ShouldThrowException_WhenNameAlreadyExists() {
        String existingCategory = "Tech Conference";

        when(categoryRepository.existsByName(existingCategory)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            categoryService.addCategory(existingCategory);
        });

        assertEquals("Category already exists", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ==========================================
    // UPDATE CATEGORY TESTS
    // ==========================================
    @Test
    void updateCategory_ShouldUpdateAndSave_WhenValidRequest() {
        UUID id = UUID.randomUUID();
        Category existingCategory = new Category();
        existingCategory.setName("Old Name");

        UpdateCategoryDTO updateDTO = new UpdateCategoryDTO();
        updateDTO.setName("New Name");
        updateDTO.setStatus(AppStatus.ACTIVE);

        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.findCategoryConflicts(updateDTO.getName(), id)).thenReturn(Collections.emptyList());

        // NEW WAY: Static Mocking Block
        try (MockedStatic<SecurityUtil> mockedSecurity = mockStatic(SecurityUtil.class)) {
            mockedSecurity.when(SecurityUtil::getCurrentUser).thenReturn(mockAdmin);

            categoryService.updateCategory(id, updateDTO);

            verify(categoryRepository, times(1)).save(existingCategory);
            assertEquals("New Name", existingCategory.getName());
        }
    }

    // ==========================================
    // FETCH CATEGORY TESTS
    // ==========================================
    @Test
    void getAllCategories_ShouldReturnList() {
        when(categoryRepository.findAll()).thenReturn(Collections.singletonList(new Category()));
        when(mapper.mapList(anyList(), eq(CategoryDTO.class))).thenReturn(Collections.singletonList(new CategoryDTO()));

        List<CategoryDTO> result = categoryService.getAllCategories();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void findCategory_ShouldReturnCategory_WhenIdExists() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.of(new Category()));

        Category result = categoryService.findCategory(id);

        assertNotNull(result);
    }
}