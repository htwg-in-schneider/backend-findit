package de.htwg.findit.controller;

import de.htwg.findit.model.User;
import de.htwg.findit.repository.UserRepository;
import de.htwg.findit.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public ProfileController(
            CurrentUserService currentUserService,
            UserRepository userRepository
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        User user = currentUserService.getRequiredUser(jwt);

        return toProfileResponse(user);
    }

    @PutMapping
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ProfileUpdateRequest request
    ) {
        User user = currentUserService.getRequiredUser(jwt);

        String cleanedName = request.name().trim();
        String cleanedEmail = request.email().trim().toLowerCase();
        String cleanedColor = request.displayColor().trim();

        Optional<User> userWithSameEmail = userRepository.findByEmailIgnoreCase(cleanedEmail);

        if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Diese E-Mail-Adresse wird bereits verwendet."
            );
        }

        user.setName(cleanedName);
        user.setEmail(cleanedEmail);
        user.setDisplayColor(cleanedColor);

        User savedUser = userRepository.save(user);

        return toProfileResponse(savedUser);
    }

    private ProfileResponse toProfileResponse(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getDisplayColor()
        );
    }

    public record ProfileResponse(
            Long id,
            String name,
            String email,
            String role,
            String displayColor
    ) {
    }

    public record ProfileUpdateRequest(
            @NotBlank(message = "Name darf nicht leer sein.")
            @Size(max = 100, message = "Name darf höchstens 100 Zeichen lang sein.")
            String name,

            @NotBlank(message = "E-Mail darf nicht leer sein.")
            @Email(message = "Bitte gib eine gültige E-Mail-Adresse ein.")
            @Size(max = 160, message = "E-Mail darf höchstens 160 Zeichen lang sein.")
            String email,

            @NotBlank(message = "Anzeigefarbe darf nicht leer sein.")
            @Pattern(
                    regexp = "^#[0-9A-Fa-f]{6}$",
                    message = "Anzeigefarbe muss ein gültiger Hex-Farbwert sein."
            )
            String displayColor
    ) {
    }
}