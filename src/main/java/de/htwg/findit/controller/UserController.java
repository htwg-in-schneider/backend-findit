package de.htwg.findit.controller;

import de.htwg.findit.model.User;
import de.htwg.findit.repository.ItemRepository;
import de.htwg.findit.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public UserController(UserRepository userRepository, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nutzer wurde nicht gefunden."
                ));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody @Valid UserRequest request) {
        String cleanedName = request.name().trim();
        String cleanedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(cleanedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Diese E-Mail-Adresse wird bereits verwendet."
            );
        }

        User user = new User(cleanedName, cleanedEmail);

        return userRepository.save(user);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody @Valid UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nutzer wurde nicht gefunden."
                ));

        String cleanedName = request.name().trim();
        String cleanedEmail = request.email().trim().toLowerCase();

        Optional<User> userWithSameEmail = userRepository.findByEmailIgnoreCase(cleanedEmail);

        if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Diese E-Mail-Adresse wird bereits verwendet."
            );
        }

        user.setName(cleanedName);
        user.setEmail(cleanedEmail);

        return userRepository.save(user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Nutzer wurde nicht gefunden."
                ));

        long itemCount = itemRepository.countByUserId(id);

        if (itemCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Dieser Nutzer kann nicht gelöscht werden, weil ihm noch "
                            + itemCount
                            + " Eintrag"
                            + (itemCount == 1 ? "" : "e")
                            + " zugeordnet sind."
            );
        }

        userRepository.delete(user);
    }

    public record UserRequest(
            @NotBlank(message = "Name darf nicht leer sein.")
            @Size(max = 100, message = "Name darf höchstens 100 Zeichen lang sein.")
            String name,

            @NotBlank(message = "E-Mail darf nicht leer sein.")
            @Email(message = "Bitte gib eine gültige E-Mail-Adresse ein.")
            @Size(max = 160, message = "E-Mail darf höchstens 160 Zeichen lang sein.")
            String email
    ) {
    }
}