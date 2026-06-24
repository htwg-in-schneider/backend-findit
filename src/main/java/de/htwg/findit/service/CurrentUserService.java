package de.htwg.findit.service;

import de.htwg.findit.model.User;
import de.htwg.findit.model.User.UserRole;
import de.htwg.findit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    @Value("${app.security.enabled:false}")
    private boolean securityEnabled;

    @Value("${app.auth.claim-namespace:https://findit.htwg-konstanz.de/}")
    private String claimNamespace;

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
            return updateRoleFromTokenIfNeeded(userBySubject.get(), jwt);
        }

        String email = readEmail(jwt);

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Das Token enthält keine E-Mail-Adresse."
            );
        }

        Optional<User> userByEmail = userRepository.findByEmailIgnoreCase(email);

        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            user.setAuthSubject(subject);
            user = updateRoleFromTokenIfNeeded(user, jwt);
            return userRepository.save(user);
        }

        User newUser = new User(
                readDisplayName(jwt, email),
                email.trim().toLowerCase(),
                subject,
                tokenContainsAdminRole(jwt) ? UserRole.ADMIN : UserRole.USER
        );

        return userRepository.save(newUser);
    }

    public boolean isAdmin(Jwt jwt) {
        if (!securityEnabled) {
            return true;
        }

        User currentUser = getRequiredUser(jwt);

        return currentUser.getRole() == UserRole.ADMIN || tokenContainsAdminRole(jwt);
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
                || tokenContainsAdminRole(jwt)
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

    private User updateRoleFromTokenIfNeeded(User user, Jwt jwt) {
        if (tokenContainsAdminRole(jwt) && user.getRole() != UserRole.ADMIN) {
            user.setRole(UserRole.ADMIN);
            return userRepository.save(user);
        }

        return user;
    }

    private boolean tokenContainsAdminRole(Jwt jwt) {
        List<String> roles = readStringListClaim(jwt, namespacedClaim("roles"));

        return roles.stream().anyMatch(role ->
                role.equalsIgnoreCase("ADMIN")
                        || role.equalsIgnoreCase("Admin")
                        || role.equalsIgnoreCase("findIT Admin")
        );
    }

    private String readEmail(Jwt jwt) {
        String email = jwt.getClaimAsString(namespacedClaim("email"));

        if (email != null && !email.isBlank()) {
            return email;
        }

        email = jwt.getClaimAsString("email");

        if (email != null && !email.isBlank()) {
            return email;
        }

        return null;
    }

    private String readDisplayName(Jwt jwt, String email) {
        String name = jwt.getClaimAsString("name");

        if (name != null && !name.isBlank()) {
            return name.trim();
        }

        String nickname = jwt.getClaimAsString("nickname");

        if (nickname != null && !nickname.isBlank()) {
            return nickname.trim();
        }

        String localPart = email.split("@")[0]
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .trim();

        if (localPart.isBlank()) {
            return "findIT Nutzer";
        }

        return Character.toUpperCase(localPart.charAt(0)) + localPart.substring(1);
    }

    private List<String> readStringListClaim(Jwt jwt, String claimName) {
        Object claim = jwt.getClaim(claimName);

        if (claim instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }

        return List.of();
    }

    private String namespacedClaim(String claimName) {
        String namespace = claimNamespace == null || claimNamespace.isBlank()
                ? "https://findit.htwg-konstanz.de/"
                : claimNamespace;

        if (!namespace.endsWith("/")) {
            namespace = namespace + "/";
        }

        return namespace + claimName;
    }
}