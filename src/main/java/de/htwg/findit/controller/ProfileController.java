package de.htwg.findit.controller;

import de.htwg.findit.model.User;
import de.htwg.findit.service.CurrentUserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;

    public ProfileController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        User user = currentUserService.getRequiredUser(jwt);

        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    public record ProfileResponse(
            Long id,
            String name,
            String email,
            String role
    ) {
    }
}