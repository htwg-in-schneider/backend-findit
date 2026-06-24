package de.htwg.findit.controller;

import de.htwg.findit.model.Item;
import de.htwg.findit.model.Item.ItemStatus;
import de.htwg.findit.model.Item.ItemType;
import de.htwg.findit.model.User;
import de.htwg.findit.model.User.UserRole;
import de.htwg.findit.repository.ItemRepository;
import de.htwg.findit.repository.UserRepository;
import de.htwg.findit.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public ItemController(
            ItemRepository itemRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    @GetMapping("/{id}")
    public Item getItemById(@PathVariable Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Eintrag wurde nicht gefunden."
                ));
    }

    @GetMapping("/{id}/matches")
    public List<Item> getPossibleMatches(@PathVariable Long id) {
        Item baseItem = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Eintrag wurde nicht gefunden."
                ));

        ItemType oppositeType = baseItem.getType() == ItemType.LOST
                ? ItemType.FOUND
                : ItemType.LOST;

        return itemRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(baseItem.getId()))
                .filter(candidate -> candidate.getType() == oppositeType)
                .filter(candidate -> candidate.getStatus() != ItemStatus.RETURNED)
                .filter(candidate -> calculateMatchScore(baseItem, candidate) >= 3)
                .sorted(Comparator.comparingInt((Item candidate) ->
                        calculateMatchScore(baseItem, candidate)).reversed())
                .limit(6)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Item createItem(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ItemRequest request
    ) {
        User user = resolveRequestedUser(jwt, request.userId());

        Item item = new Item(
                request.title().trim(),
                request.description().trim(),
                request.type(),
                request.category().trim(),
                request.location().trim(),
                request.date(),
                request.status(),
                user
        );

        return itemRepository.save(item);
    }

    @PutMapping("/{id}")
    public Item updateItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody @Valid ItemRequest request
    ) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Eintrag wurde nicht gefunden."
                ));

        currentUserService.requireOwnerOrAdmin(jwt, item.getUser().getId());

        User user = resolveRequestedUser(jwt, request.userId());

        item.setTitle(request.title().trim());
        item.setDescription(request.description().trim());
        item.setType(request.type());
        item.setCategory(request.category().trim());
        item.setLocation(request.location().trim());
        item.setDate(request.date());
        item.setStatus(request.status());
        item.setUser(user);

        return itemRepository.save(item);
    }

    @PutMapping("/{id}/return")
    public Item confirmReturn(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Eintrag wurde nicht gefunden."
                ));

        currentUserService.requireOwnerOrAdmin(jwt, item.getUser().getId());

        item.setStatus(ItemStatus.RETURNED);

        return itemRepository.save(item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Eintrag wurde nicht gefunden."
                ));

        currentUserService.requireOwnerOrAdmin(jwt, item.getUser().getId());

        itemRepository.delete(item);
    }

    @GetMapping("/search")
    public List<Item> searchItems(@RequestParam String query) {
        return itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrLocationContainingIgnoreCase(
                        query,
                        query,
                        query
                );
    }

    @GetMapping("/filter")
    public List<Item> filterItems(
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ItemStatus status
    ) {
        return itemRepository.findAll().stream()
                .filter(item -> type == null || item.getType() == type)
                .filter(item -> category == null || category.isBlank()
                        || item.getCategory().equalsIgnoreCase(category))
                .filter(item -> status == null || item.getStatus() == status)
                .toList();
    }

    private User resolveRequestedUser(Jwt jwt, Long requestedUserId) {
        if (!currentUserService.isSecurityEnabled()) {
            return userRepository.findById(requestedUserId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Nutzer wurde nicht gefunden."
                    ));
        }

        User currentUser = currentUserService.getRequiredUser(jwt);

        if (currentUser.getRole() == UserRole.ADMIN) {
            return userRepository.findById(requestedUserId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Nutzer wurde nicht gefunden."
                    ));
        }

        return currentUser;
    }

    private int calculateMatchScore(Item baseItem, Item candidate) {
        int score = 0;

        if (sameText(baseItem.getCategory(), candidate.getCategory())) {
            score += 4;
        }

        if (containsSimilarWords(baseItem.getTitle(), candidate.getTitle())) {
            score += 3;
        }

        if (containsSimilarWords(baseItem.getLocation(), candidate.getLocation())) {
            score += 2;
        }

        if (datesAreClose(baseItem.getDate(), candidate.getDate())) {
            score += 1;
        }

        return score;
    }

    private boolean sameText(String first, String second) {
        if (first == null || second == null) {
            return false;
        }

        return normalize(first).equals(normalize(second));
    }

    private boolean containsSimilarWords(String first, String second) {
        if (first == null || second == null) {
            return false;
        }

        String normalizedFirst = normalize(first);
        String normalizedSecond = normalize(second);

        if (normalizedFirst.contains(normalizedSecond)
                || normalizedSecond.contains(normalizedFirst)) {
            return true;
        }

        String[] firstWords = normalizedFirst.split(" ");
        String[] secondWords = normalizedSecond.split(" ");

        for (String firstWord : firstWords) {
            if (firstWord.length() < 4) {
                continue;
            }

            for (String secondWord : secondWords) {
                if (secondWord.length() < 4) {
                    continue;
                }

                if (firstWord.equals(secondWord)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean datesAreClose(LocalDate first, LocalDate second) {
        if (first == null || second == null) {
            return false;
        }

        long daysBetween = Math.abs(ChronoUnit.DAYS.between(first, second));

        return daysBetween <= 14;
    }

    private String normalize(String value) {
        return value
                .trim()
                .toLowerCase()
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss")
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ");
    }

    public record ItemRequest(
            @NotBlank(message = "Titel darf nicht leer sein.")
            @Size(max = 120, message = "Titel darf höchstens 120 Zeichen lang sein.")
            String title,

            @NotBlank(message = "Beschreibung darf nicht leer sein.")
            @Size(max = 1000, message = "Beschreibung darf höchstens 1000 Zeichen lang sein.")
            String description,

            @NotNull(message = "Typ muss angegeben werden.")
            ItemType type,

            @NotBlank(message = "Kategorie darf nicht leer sein.")
            @Size(max = 80, message = "Kategorie darf höchstens 80 Zeichen lang sein.")
            String category,

            @NotBlank(message = "Ort darf nicht leer sein.")
            @Size(max = 120, message = "Ort darf höchstens 120 Zeichen lang sein.")
            String location,

            @NotNull(message = "Datum muss angegeben werden.")
            @PastOrPresent(message = "Datum darf nicht in der Zukunft liegen.")
            LocalDate date,

            @NotNull(message = "Status muss angegeben werden.")
            ItemStatus status,

            @NotNull(message = "Nutzer muss angegeben werden.")
            Long userId
    ) {
    }
}