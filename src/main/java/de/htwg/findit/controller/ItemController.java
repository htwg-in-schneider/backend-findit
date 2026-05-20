package de.htwg.findit.controller;

import de.htwg.findit.model.Item;
import de.htwg.findit.model.Item.ItemStatus;
import de.htwg.findit.model.Item.ItemType;
import de.htwg.findit.model.User;
import de.htwg.findit.repository.ItemRepository;
import de.htwg.findit.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "http://localhost:5173")
public class ItemController {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public ItemController(ItemRepository itemRepository, UserRepository userRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    @GetMapping("/{id}")
    public Item getItemById(@PathVariable Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));
    }

    @PostMapping
    public Item createItem(@RequestBody @Valid ItemRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Item item = new Item(
                request.title(),
                request.description(),
                request.type(),
                request.category(),
                request.location(),
                request.date(),
                request.status(),
                user
        );

        return itemRepository.save(item);
    }

    @PutMapping("/{id}")
    public Item updateItem(@PathVariable Long id, @RequestBody @Valid ItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setType(request.type());
        item.setCategory(request.category());
        item.setLocation(request.location());
        item.setDate(request.date());
        item.setStatus(request.status());
        item.setUser(user);

        return itemRepository.save(item);
    }

    @DeleteMapping("/{id}")
    public void deleteItem(@PathVariable Long id) {
        itemRepository.deleteById(id);
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

    public record ItemRequest(
            String title,
            String description,
            ItemType type,
            String category,
            String location,
            LocalDate date,
            ItemStatus status,
            Long userId
    ) {
    }
}