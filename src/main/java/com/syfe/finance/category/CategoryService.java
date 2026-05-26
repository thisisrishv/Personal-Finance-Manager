package com.syfe.finance.category;

import com.syfe.finance.auth.CurrentUser;
import com.syfe.finance.common.ApiException;
import com.syfe.finance.common.MessageResponse;
import com.syfe.finance.transaction.TransactionRepository;
import com.syfe.finance.user.UserAccount;
import com.syfe.finance.user.UserService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public CategoryService(
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            UserService userService
    ) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public CategoriesResponse getCategories(CurrentUser currentUser) {
        UserAccount user = userService.requireUser(currentUser);
        List<CategoryResponse> categories = categoryRepository.findAccessibleCategories(user)
                .stream()
                .map(this::toResponse)
                .toList();
        return new CategoriesResponse(categories);
    }

    @Transactional
    public CategoryResponse create(CurrentUser currentUser, CategoryRequest request) {
        UserAccount user = userService.requireUser(currentUser);
        String name = normalizeName(request.name());
        if (categoryRepository.existsByOwnerAndNameIgnoreCase(user, name)
                || categoryRepository.existsByOwnerIsNullAndNameIgnoreCase(name)) {
            throw ApiException.conflict("Category name already exists");
        }
        Category category = categoryRepository.save(new Category(name, request.type(), true, user));
        return toResponse(category);
    }

    @Transactional
    public MessageResponse delete(CurrentUser currentUser, String name) {
        UserAccount user = userService.requireUser(currentUser);
        String normalizedName = normalizeName(name);
        if (categoryRepository.existsByOwnerIsNullAndNameIgnoreCase(normalizedName)) {
            throw ApiException.badRequest("Default categories cannot be deleted");
        }
        Category category = categoryRepository.findByOwnerAndNameIgnoreCase(user, normalizedName)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        if (transactionRepository.existsByCategory(category)) {
            throw ApiException.badRequest("Category is referenced by transactions");
        }
        categoryRepository.delete(category);
        return new MessageResponse("Category deleted successfully");
    }

    @Transactional(readOnly = true)
    public Category findAccessibleByName(UserAccount user, String name) {
        String normalizedName = normalizeName(name);
        return categoryRepository.findByOwnerAndNameIgnoreCase(user, normalizedName)
                .or(() -> categoryRepository.findByOwnerIsNullAndNameIgnoreCase(normalizedName))
                .orElseThrow(() -> ApiException.badRequest("Category does not exist"));
    }

    @Transactional(readOnly = true)
    public Category findAccessibleById(UserAccount user, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        if (category.getOwner() != null && !category.getOwner().getId().equals(user.getId())) {
            throw ApiException.forbidden("Category belongs to another user");
        }
        return category;
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getType(), category.isCustom());
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            throw ApiException.badRequest("Category name is required");
        }
        return normalized;
    }
}
