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
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private static final int MINIMUM_MATCH_SCORE = 45;

    private static final Set<String> STOP_WORDS = Set.of(
            "der",
            "die",
            "das",
            "ein",
            "eine",
            "einen",
            "einem",
            "einer",
            "und",
            "oder",
            "mit",
            "ohne",
            "bei",
            "auf",
            "im",
            "in",
            "am",
            "an",
            "den",
            "dem",
            "des",
            "ist",
            "war",
            "wurde",
            "habe",
            "ich",
            "mein",
            "meine",
            "meinen",
            "verloren",
            "gefunden",
            "vermutlich",
            "wahrscheinlich",
            "bitte",
            "gegenstand"
    );

    private static final Map<String, String> TOKEN_ALIASES = Map.ofEntries(
            Map.entry("airpods", "kopfhoerer"),
            Map.entry("airpod", "kopfhoerer"),
            Map.entry("kopfhörer", "kopfhoerer"),
            Map.entry("kopfhoerer", "kopfhoerer"),
            Map.entry("headphones", "kopfhoerer"),
            Map.entry("earbuds", "kopfhoerer"),
            Map.entry("ohrhoerer", "kopfhoerer"),

            Map.entry("handy", "smartphone"),
            Map.entry("telefon", "smartphone"),
            Map.entry("iphone", "smartphone"),
            Map.entry("android", "smartphone"),

            Map.entry("laptop", "notebook"),
            Map.entry("macbook", "notebook"),

            Map.entry("rucksack", "rucksack"),
            Map.entry("tasche", "tasche"),
            Map.entry("beutel", "tasche"),

            Map.entry("schluessel", "schluessel"),
            Map.entry("schlüssel", "schluessel"),
            Map.entry("schluesselbund", "schluessel"),
            Map.entry("schlüsselbund", "schluessel"),
            Map.entry("transponder", "schluessel"),

            Map.entry("studierendenausweis", "ausweis"),
            Map.entry("studentenausweis", "ausweis"),
            Map.entry("ausweis", "ausweis"),
            Map.entry("karte", "ausweis"),
            Map.entry("id", "ausweis"),

            Map.entry("geldbeutel", "geldbeutel"),
            Map.entry("portemonnaie", "geldbeutel"),
            Map.entry("portmonee", "geldbeutel"),
            Map.entry("wallet", "geldbeutel"),

            Map.entry("flasche", "trinkflasche"),
            Map.entry("trinkflasche", "trinkflasche"),
            Map.entry("wasserflasche", "trinkflasche")
    );

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
                .map(candidate -> new MatchCandidate(candidate, calculateMatchScore(baseItem, candidate)))
                .filter(candidate -> candidate.score() >= MINIMUM_MATCH_SCORE)
                .sorted(Comparator
                        .comparingInt(MatchCandidate::score)
                        .reversed()
                        .thenComparing(candidate -> candidate.item().getDate(), Comparator.reverseOrder())
                )
                .limit(8)
                .map(MatchCandidate::item)
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
                request.latitude(),
                request.longitude(),
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
        item.setLatitude(request.latitude());
        item.setLongitude(request.longitude());
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

        score += calculateCategoryScore(baseItem, candidate);
        score += calculateTextScore(baseItem, candidate);
        score += calculateLocationNameScore(baseItem, candidate);
        score += calculateDistanceScore(baseItem, candidate);
        score += calculateDateScore(baseItem, candidate);

        if (isVeryWeakMatch(baseItem, candidate, score)) {
            return 0;
        }

        return Math.min(score, 100);
    }

    private int calculateCategoryScore(Item baseItem, Item candidate) {
        if (sameNormalizedText(baseItem.getCategory(), candidate.getCategory())) {
            return 30;
        }

        return 0;
    }

    private int calculateTextScore(Item baseItem, Item candidate) {
        Set<String> baseTokens = extractMeaningfulTokens(
                baseItem.getTitle() + " " + baseItem.getDescription()
        );

        Set<String> candidateTokens = extractMeaningfulTokens(
                candidate.getTitle() + " " + candidate.getDescription()
        );

        if (baseTokens.isEmpty() || candidateTokens.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(baseTokens);
        intersection.retainAll(candidateTokens);

        if (intersection.isEmpty()) {
            return 0;
        }

        Set<String> union = new HashSet<>(baseTokens);
        union.addAll(candidateTokens);

        double similarity = (double) intersection.size() / (double) union.size();

        int score = 0;

        if (similarity >= 0.5) {
            score += 30;
        } else if (similarity >= 0.3) {
            score += 22;
        } else if (similarity >= 0.18) {
            score += 14;
        } else {
            score += 8;
        }

        if (hasStrongObjectToken(intersection)) {
            score += 10;
        }

        return Math.min(score, 35);
    }

    private int calculateLocationNameScore(Item baseItem, Item candidate) {
        if (sameNormalizedText(baseItem.getLocation(), candidate.getLocation())) {
            return 15;
        }

        Set<String> baseLocationTokens = extractMeaningfulTokens(baseItem.getLocation());
        Set<String> candidateLocationTokens = extractMeaningfulTokens(candidate.getLocation());

        if (baseLocationTokens.isEmpty() || candidateLocationTokens.isEmpty()) {
            return 0;
        }

        baseLocationTokens.retainAll(candidateLocationTokens);

        if (!baseLocationTokens.isEmpty()) {
            return 8;
        }

        return 0;
    }

    private int calculateDistanceScore(Item baseItem, Item candidate) {
        if (!hasCoordinates(baseItem) || !hasCoordinates(candidate)) {
            return 0;
        }

        double distanceMeters = calculateDistanceMeters(
                baseItem.getLatitude(),
                baseItem.getLongitude(),
                candidate.getLatitude(),
                candidate.getLongitude()
        );

        if (distanceMeters <= 25) {
            return 25;
        }

        if (distanceMeters <= 75) {
            return 20;
        }

        if (distanceMeters <= 150) {
            return 14;
        }

        if (distanceMeters <= 300) {
            return 8;
        }

        if (distanceMeters <= 600) {
            return 3;
        }

        return -8;
    }

    private int calculateDateScore(Item baseItem, Item candidate) {
        if (baseItem.getDate() == null || candidate.getDate() == null) {
            return 0;
        }

        long daysBetween = Math.abs(ChronoUnit.DAYS.between(
                baseItem.getDate(),
                candidate.getDate()
        ));

        if (daysBetween <= 1) {
            return 15;
        }

        if (daysBetween <= 3) {
            return 12;
        }

        if (daysBetween <= 7) {
            return 8;
        }

        if (daysBetween <= 14) {
            return 4;
        }

        if (daysBetween <= 30) {
            return 1;
        }

        return -6;
    }

    private boolean isVeryWeakMatch(Item baseItem, Item candidate, int score) {
        boolean sameCategory = sameNormalizedText(baseItem.getCategory(), candidate.getCategory());
        boolean sameLocation = sameNormalizedText(baseItem.getLocation(), candidate.getLocation());
        boolean hasTextOverlap = !getTokenIntersection(baseItem, candidate).isEmpty();
        boolean nearby = hasCoordinates(baseItem)
                && hasCoordinates(candidate)
                && calculateDistanceMeters(
                baseItem.getLatitude(),
                baseItem.getLongitude(),
                candidate.getLatitude(),
                candidate.getLongitude()
        ) <= 300;

        if (score < MINIMUM_MATCH_SCORE) {
            return true;
        }

        return !sameCategory && !hasTextOverlap && !nearby && !sameLocation;
    }

    private Set<String> getTokenIntersection(Item baseItem, Item candidate) {
        Set<String> baseTokens = extractMeaningfulTokens(
                baseItem.getTitle() + " " + baseItem.getDescription()
        );

        Set<String> candidateTokens = extractMeaningfulTokens(
                candidate.getTitle() + " " + candidate.getDescription()
        );

        baseTokens.retainAll(candidateTokens);

        return baseTokens;
    }

    private Set<String> extractMeaningfulTokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(normalize(value).split(" "))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .map(this::normalizeAlias)
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeAlias(String token) {
        return TOKEN_ALIASES.getOrDefault(token, token);
    }

    private boolean hasStrongObjectToken(Set<String> tokens) {
        Set<String> strongTokens = Set.of(
                "kopfhoerer",
                "smartphone",
                "notebook",
                "rucksack",
                "tasche",
                "schluessel",
                "ausweis",
                "geldbeutel",
                "trinkflasche"
        );

        return tokens.stream().anyMatch(strongTokens::contains);
    }

    private boolean sameNormalizedText(String first, String second) {
        if (first == null || second == null) {
            return false;
        }

        return normalize(first).equals(normalize(second));
    }

    private boolean hasCoordinates(Item item) {
        return item.getLatitude() != null
                && item.getLongitude() != null
                && item.getLatitude() >= -90
                && item.getLatitude() <= 90
                && item.getLongitude() >= -180
                && item.getLongitude() <= 180;
    }

    private double calculateDistanceMeters(
            double firstLatitude,
            double firstLongitude,
            double secondLatitude,
            double secondLongitude
    ) {
        final double earthRadiusMeters = 6_371_000;

        double firstLatRad = Math.toRadians(firstLatitude);
        double secondLatRad = Math.toRadians(secondLatitude);
        double deltaLatRad = Math.toRadians(secondLatitude - firstLatitude);
        double deltaLonRad = Math.toRadians(secondLongitude - firstLongitude);

        double haversine =
                Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
                        + Math.cos(firstLatRad)
                        * Math.cos(secondLatRad)
                        * Math.sin(deltaLonRad / 2)
                        * Math.sin(deltaLonRad / 2);

        double angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));

        return earthRadiusMeters * angularDistance;
    }

    private String normalize(String value) {
        return value
                .trim()
                .toLowerCase()
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")
                .replace("ß", "ss")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record MatchCandidate(Item item, int score) {
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

            @NotNull(message = "Breitengrad muss angegeben werden.")
            @DecimalMin(value = "-90.0", message = "Breitengrad muss mindestens -90 sein.")
            @DecimalMax(value = "90.0", message = "Breitengrad darf höchstens 90 sein.")
            Double latitude,

            @NotNull(message = "Längengrad muss angegeben werden.")
            @DecimalMin(value = "-180.0", message = "Längengrad muss mindestens -180 sein.")
            @DecimalMax(value = "180.0", message = "Längengrad darf höchstens 180 sein.")
            Double longitude,

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