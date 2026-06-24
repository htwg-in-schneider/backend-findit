package de.htwg.findit.service;

import de.htwg.findit.model.User;
import de.htwg.findit.model.User.UserRole;
import de.htwg.findit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    @Value("${app.security.enabled:false}")
    private boolean securityEnabled;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    public User getRequiredUser(Jwt jwt) {
        if (!securityEnabled) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Security ist lokal deaktiviert. Es gibt keinen authentifizierten Backend-Nutzer."
            );
        }

        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentifizierung erforderlich."
            );
        }

        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token enthält kein Subject."
            );
        }

        Optional<User> userBySubject = userRepository.findByAuthSubject(subject);

        if (userBySubject.isPresent()) {
            return userBySubject.get();
        }

        String email = readEmail(jwt);

        if (email != null && !email.isBlank()) {
            Optional<User> userByEmail = userRepository.findByEmailIgnoreCase(email);

            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                user.setAuthSubject(subject);
                return userRepository.save(user);
            }
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Für diesen Auth0-Nutzer existiert kein findIT-Nutzer."
        );
    }

    public boolean isAdmin(Jwt jwt) {
        if (!securityEnabled) {
            return true;
        }

        return getRequiredUser(jwt).getRole() == UserRole.ADMIN;
    }

    public void requireAdmin(Jwt jwt) {
        if (!securityEnabled) {
            return;
        }

        if (!isAdmin(jwt)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Adminrechte erforderlich."
            );
        }
    }

    public boolean canManageUserOwnedResource(Jwt jwt, Long ownerUserId) {
        if (!securityEnabled) {
            return true;
        }

        User currentUser = getRequiredUser(jwt);

        return currentUser.getRole() == UserRole.ADMIN
                || currentUser.getId().equals(ownerUserId);
    }

    public void requireOwnerOrAdmin(Jwt jwt, Long ownerUserId) {
        if (!canManageUserOwnedResource(jwt, ownerUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Du darfst diese Ressource nicht verwalten."
            );
        }
    }

    private String readEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");

        if (email != null && !email.isBlank()) {
            return email;
        }

        email = jwt.getClaimAsString("https://findit.example.com/email");

        if (email != null && !email.isBlank()) {
            return email;
        }

        return null;
    }
}