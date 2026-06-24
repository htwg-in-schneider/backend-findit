package de.htwg.findit.controller;

import de.htwg.findit.model.Category;
import de.htwg.findit.repository.CategoryRepository;
import de.htwg.findit.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public CategoryController(
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService
    ) {
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
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
    public Category createCategory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CategoryRequest request
    ) {
        currentUserService.requireAdmin(jwt);

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
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody @Valid CategoryRequest request
    ) {
        currentUserService.requireAdmin(jwt);

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
    public void deleteCategory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        currentUserService.requireAdmin(jwt);

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