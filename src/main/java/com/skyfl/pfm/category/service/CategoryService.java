package com.skyfl.pfm.category.service;

import com.skyfl.pfm.category.dto.CategoryRequest;
import com.skyfl.pfm.category.dto.CategoryResponse;
import com.skyfl.pfm.category.entity.Category;
import com.skyfl.pfm.category.entity.CategoryType;
import com.skyfl.pfm.category.repository.CategoryRepository;
import com.skyfl.pfm.common.exception.BusinessException;
import com.skyfl.pfm.transaction.repository.TransactionRepository;
import com.skyfl.pfm.user.entity.User;
import com.skyfl.pfm.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository,
            TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID userId) {
        return categoryRepository.findVisibleCategories(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(UUID userId, CategoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
        Category category = new Category();
        category.setUser(user);
        category.setSystem(false);
        apply(userId, category, request);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID userId, UUID categoryId, CategoryRequest request) {
        Category category = getEntity(userId, categoryId);
        if (category.isSystem()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "System categories cannot be modified");
        }
        apply(userId, category, request);
        return toResponse(category);
    }

    @Transactional
    public void delete(UUID userId, UUID categoryId) {
        Category category = getEntity(userId, categoryId);
        if (category.isSystem()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "System categories cannot be deleted");
        }
        if (categoryRepository.existsByParentIdAndDeletedFalse(categoryId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Delete child categories first");
        }
        if (transactionRepository.existsByUserIdAndCategoryIdAndDeletedFalse(userId, categoryId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Category is used by transactions");
        }
        category.setDeleted(true);
    }

    public Category getEntity(UUID userId, UUID categoryId) {
        return categoryRepository.findAccessibleById(categoryId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    public Category getEntityByName(UUID userId, String categoryName, CategoryType expectedType) {
        List<Category> matches = categoryRepository.findAccessibleByName(userId, categoryName == null ? "" : categoryName.trim());
        return matches.stream()
                .filter(category -> category.getType() == CategoryType.BOTH || category.getType() == expectedType)
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Category not found: " + categoryName));
    }

    private void apply(UUID userId, Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setIcon(request.icon());
        category.setColor(request.color());
        if (request.parentId() != null) {
            Category parent = getEntity(userId, request.parentId());
            category.setParent(parent);
        } else {
            category.setParent(null);
        }
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.getName(),
                category.getType(),
                category.getIcon(),
                category.getColor(),
                category.isSystem()
        );
    }
}
