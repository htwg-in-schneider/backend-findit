package de.htwg.findit.config;

import de.htwg.findit.model.Category;
import de.htwg.findit.model.ContactRequest;
import de.htwg.findit.model.ContactRequest.ContactRequestStatus;
import de.htwg.findit.model.Item;
import de.htwg.findit.model.Item.ItemStatus;
import de.htwg.findit.model.Item.ItemType;
import de.htwg.findit.model.User;
import de.htwg.findit.model.User.UserRole;
import de.htwg.findit.repository.CategoryRepository;
import de.htwg.findit.repository.ContactRequestRepository;
import de.htwg.findit.repository.ItemRepository;
import de.htwg.findit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    @Value("${app.demo-data.enabled:true}")
    private boolean demoDataEnabled;

    @Bean
    CommandLineRunner initializeDemoData(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ItemRepository itemRepository,
            ContactRequestRepository contactRequestRepository
    ) {
        return args -> {
            if (!demoDataEnabled) {
                return;
            }

            User max = findOrCreateUser(
                    userRepository,
                    "Max Mustermann",
                    "max.mustermann@htwg-konstanz.de",
                    UserRole.USER
            );

            User dennis = findOrCreateUser(
                    userRepository,
                    "Dennis Müller",
                    "dennis.mueller@htwg-konstanz.de",
                    UserRole.USER
            );

            findOrCreateUser(
                    userRepository,
                    "Admin findIT",
                    "admin@findit.htwg-konstanz.de",
                    UserRole.ADMIN
            );

            findOrCreateCategory(
                    categoryRepository,
                    "Elektronik",
                    "Technische Geräte wie Kopfhörer, Smartphones, Laptops oder Ladegeräte."
            );

            findOrCreateCategory(
                    categoryRepository,
                    "Tasche",
                    "Rucksäcke, Handtaschen, Sporttaschen oder Beutel."
            );

            findOrCreateCategory(
                    categoryRepository,
                    "Dokumente",
                    "Ausweise, Studierendenausweise, Karten oder Unterlagen."
            );

            findOrCreateCategory(
                    categoryRepository,
                    "Kleidung",
                    "Jacken, Pullover, Mützen, Schals oder andere Kleidungsstücke."
            );

            findOrCreateCategory(
                    categoryRepository,
                    "Schlüssel",
                    "Einzelne Schlüssel, Schlüsselbunde oder Transponder."
            );

            findOrCreateCategory(
                    categoryRepository,
                    "Sonstiges",
                    "Alle Gegenstände, die keiner anderen Kategorie eindeutig zugeordnet werden können."
            );

            if (itemRepository.count() > 0) {
                return;
            }

            LocalDate today = LocalDate.now();

            Item foundHeadphones = createItem(
                    itemRepository,
                    "AirPods in schwarzer Hülle gefunden",
                    "In der Bibliothek wurde eine schwarze Hülle mit kabellosen Kopfhörern gefunden.",
                    ItemType.FOUND,
                    "Elektronik",
                    "Bibliothek",
                    47.66772,
                    9.17105,
                    today.minusDays(2),
                    ItemStatus.OPEN,
                    dennis
            );

            Item lostHeadphones = createItem(
                    itemRepository,
                    "Schwarze Kopfhörer verloren",
                    "Ich habe meine schwarzen kabellosen Kopfhörer vermutlich in der Bibliothek verloren.",
                    ItemType.LOST,
                    "Elektronik",
                    "Bibliothek",
                    47.66772,
                    9.17105,
                    today.minusDays(1),
                    ItemStatus.IN_PROGRESS,
                    max
            );

            Item foundStudentCard = createItem(
                    itemRepository,
                    "Studierendenausweis gefunden",
                    "Ein Studierendenausweis wurde in der Mensa gefunden und kann abgeholt werden.",
                    ItemType.FOUND,
                    "Dokumente",
                    "Mensa",
                    47.66718,
                    9.17178,
                    today.minusDays(3),
                    ItemStatus.OPEN,
                    max
            );

            createItem(
                    itemRepository,
                    "Studierendenausweis verloren",
                    "Mein Studierendenausweis ist wahrscheinlich in der Mensa liegen geblieben.",
                    ItemType.LOST,
                    "Dokumente",
                    "Mensa",
                    47.66718,
                    9.17178,
                    today.minusDays(2),
                    ItemStatus.OPEN,
                    dennis
            );

            Item foundKeys = createItem(
                    itemRepository,
                    "Schlüsselbund mit blauem Anhänger gefunden",
                    "Vor Gebäude F wurde ein Schlüsselbund mit blauem Anhänger gefunden.",
                    ItemType.FOUND,
                    "Schlüssel",
                    "Gebäude F",
                    47.66718,
                    9.17242,
                    today.minusDays(5),
                    ItemStatus.OPEN,
                    dennis
            );

            Item lostKeys = createItem(
                    itemRepository,
                    "Schlüsselbund verloren",
                    "Ich vermisse meinen Schlüsselbund mit blauem Anhänger. Vermutlich bei Gebäude F verloren.",
                    ItemType.LOST,
                    "Schlüssel",
                    "Gebäude F",
                    47.66718,
                    9.17242,
                    today.minusDays(4),
                    ItemStatus.IN_PROGRESS,
                    max
            );

            createItem(
                    itemRepository,
                    "Schwarzer Rucksack verloren",
                    "Schwarzer Rucksack mit Laptopfach wurde vermutlich in der Bibliothek vergessen.",
                    ItemType.LOST,
                    "Tasche",
                    "Bibliothek",
                    47.66772,
                    9.17105,
                    today.minusDays(7),
                    ItemStatus.OPEN,
                    max
            );

            createItem(
                    itemRepository,
                    "Trinkflasche zurückgegeben",
                    "Eine blaue Trinkflasche wurde gefunden und bereits an den Besitzer zurückgegeben.",
                    ItemType.FOUND,
                    "Sonstiges",
                    "Sporthalle",
                    47.66693,
                    9.17205,
                    today.minusDays(6),
                    ItemStatus.RETURNED,
                    dennis
            );

            if (contactRequestRepository.count() == 0) {
                ContactRequest headphoneRequest = new ContactRequest(
                        lostHeadphones,
                        "Dennis Müller",
                        "dennis.mueller@htwg-konstanz.de",
                        "Ich habe sehr ähnliche Kopfhörer in der Bibliothek gefunden. Das könnte dein verlorener Gegenstand sein."
                );
                headphoneRequest.setStatus(ContactRequestStatus.IN_PROGRESS);
                contactRequestRepository.save(headphoneRequest);

                ContactRequest keyRequest = new ContactRequest(
                        lostKeys,
                        "Dennis Müller",
                        "dennis.mueller@htwg-konstanz.de",
                        "Ich habe einen Schlüsselbund mit blauem Anhänger bei Gebäude F gefunden."
                );
                keyRequest.setStatus(ContactRequestStatus.NEW);
                contactRequestRepository.save(keyRequest);

                ContactRequest cardRequest = new ContactRequest(
                        foundStudentCard,
                        "Dennis Müller",
                        "dennis.mueller@htwg-konstanz.de",
                        "Der gefundene Studierendenausweis könnte meiner sein. Ich kann Details dazu nennen."
                );
                cardRequest.setStatus(ContactRequestStatus.RESOLVED);
                contactRequestRepository.save(cardRequest);

                ContactRequest foundKeyRequest = new ContactRequest(
                        foundKeys,
                        "Max Mustermann",
                        "max.mustermann@htwg-konstanz.de",
                        "Ich vermisse einen Schlüsselbund mit blauem Anhänger. Das könnte meiner sein."
                );
                foundKeyRequest.setStatus(ContactRequestStatus.IN_PROGRESS);
                contactRequestRepository.save(foundKeyRequest);
            }

            foundHeadphones.setStatus(ItemStatus.IN_PROGRESS);
            itemRepository.save(foundHeadphones);
        };
    }

    private User findOrCreateUser(
            UserRepository userRepository,
            String name,
            String email,
            UserRole role
    ) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(existingUser -> {
                    boolean changed = false;

                    if (existingUser.getRole() != role) {
                        existingUser.setRole(role);
                        changed = true;
                    }

                    if (!existingUser.getName().equals(name)) {
                        existingUser.setName(name);
                        changed = true;
                    }

                    if (changed) {
                        return userRepository.save(existingUser);
                    }

                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(
                        new User(name, email.toLowerCase(), null, role)
                ));
    }

    private Category findOrCreateCategory(
            CategoryRepository categoryRepository,
            String name,
            String description
    ) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.save(new Category(name, description)));
    }

    private Item createItem(
            ItemRepository itemRepository,
            String title,
            String description,
            ItemType type,
            String category,
            String location,
            Double latitude,
            Double longitude,
            LocalDate date,
            ItemStatus status,
            User user
    ) {
        Item item = new Item(
                title,
                description,
                type,
                category,
                location,
                latitude,
                longitude,
                date,
                status,
                user
        );

        return itemRepository.save(item);
    }
}