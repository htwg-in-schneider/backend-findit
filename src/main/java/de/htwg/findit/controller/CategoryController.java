package de.htwg.findit.controller;

import de.htwg.findit.model.Category;
import de.htwg.findit.repository.CategoryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category createCategory(@RequestBody @Valid CategoryRequest request) {
        String cleanedName = request.name().trim();
        String cleanedDescription = request.description() == null
                ? ""
                : request.description().trim();

        if (categoryRepository.findByNameIgnoreCase(cleanedName).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Diese Kategorie existiert bereits."
            );
        }

        Category category = new Category(cleanedName, cleanedDescription);

        return categoryRepository.save(category);
    }

    @PutMapping("/{id}")
    public Category updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid CategoryRequest request
    ) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kategorie wurde nicht gefunden."
                ));

        String cleanedName = request.name().trim();
        String cleanedDescription = request.description() == null
                ? ""
                : request.description().trim();

        Optional<Category> categoryWithSameName = categoryRepository.findByNameIgnoreCase(cleanedName);

        if (categoryWithSameName.isPresent()
                && !categoryWithSameName.get().getId().equals(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Diese Kategorie existiert bereits."
            );
        }

        category.setName(cleanedName);
        category.setDescription(cleanedDescription);

        return categoryRepository.save(category);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Kategorie wurde nicht gefunden."
            );
        }

        categoryRepository.deleteById(id);
    }

    public record CategoryRequest(
            @NotBlank(message = "Name darf nicht leer sein.")
            String name,

            String description
    ) {
    }
}