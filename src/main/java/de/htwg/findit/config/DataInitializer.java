package de.htwg.findit.config;

import de.htwg.findit.model.Item;
import de.htwg.findit.model.Item.ItemStatus;
import de.htwg.findit.model.Item.ItemType;
import de.htwg.findit.model.User;
import de.htwg.findit.repository.ItemRepository;
import de.htwg.findit.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public DataInitializer(UserRepository userRepository, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public void run(String... args) {
        if (itemRepository.count() > 0) {
            return;
        }

        User max = userRepository.save(new User("Max Mustermann", "max.mustermann@htwg-konstanz.de"));
User dennis = userRepository.save(new User("Dennis Müller", "dennis.mueller@htwg-konstanz.de"));

        itemRepository.save(new Item(
                "Kopfhörer",
                "Schwarze Bluetooth-Kopfhörer in der Bibliothek gefunden.",
                ItemType.FOUND,
                "Elektronik",
                "Bibliothek",
                LocalDate.of(2026, 4, 1),
                ItemStatus.OPEN,
                max
        ));

        itemRepository.save(new Item(
                "Rucksack",
                "Blauer Rucksack mit Laptopfach in der Mensa verloren.",
                ItemType.LOST,
                "Tasche",
                "Mensa",
                LocalDate.of(2026, 4, 2),
                ItemStatus.IN_PROGRESS,
                dennis
        ));

        itemRepository.save(new Item(
                "Schlüsselbund",
                "Schlüsselbund mit rotem Anhänger vor Gebäude F gefunden.",
                ItemType.FOUND,
                "Schlüssel",
                "Gebäude F",
                LocalDate.of(2026, 4, 3),
                ItemStatus.OPEN,
                max
        ));
    }
}