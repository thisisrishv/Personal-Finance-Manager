package com.syfe.finance.category;

import com.syfe.finance.auth.CurrentUser;
import com.syfe.finance.common.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public CategoriesResponse getCategories(@AuthenticationPrincipal CurrentUser currentUser) {
        return categoryService.getCategories(currentUser);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(currentUser, request));
    }

    @DeleteMapping("/{name}")
    public MessageResponse delete(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String name
    ) {
        return categoryService.delete(currentUser, name);
    }
}
