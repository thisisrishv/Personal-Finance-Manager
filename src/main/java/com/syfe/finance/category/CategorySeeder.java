package com.syfe.finance.category;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CategorySeeder implements ApplicationRunner {

    private static final List<Category> DEFAULTS = List.of(
            new Category("Salary", CategoryType.INCOME, false, null),
            new Category("Food", CategoryType.EXPENSE, false, null),
            new Category("Rent", CategoryType.EXPENSE, false, null),
            new Category("Transportation", CategoryType.EXPENSE, false, null),
            new Category("Entertainment", CategoryType.EXPENSE, false, null),
            new Category("Healthcare", CategoryType.EXPENSE, false, null),
            new Category("Utilities", CategoryType.EXPENSE, false, null)
    );

    private final CategoryRepository categoryRepository;

    public CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Category category : DEFAULTS) {
            if (!categoryRepository.existsByOwnerIsNullAndNameIgnoreCase(category.getName())) {
                categoryRepository.save(new Category(category.getName(), category.getType(), false, null));
            }
        }
    }
}
