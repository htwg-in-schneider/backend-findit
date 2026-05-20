package de.htwg.findit.repository;

import de.htwg.findit.model.Item;
import de.htwg.findit.model.Item.ItemStatus;
import de.htwg.findit.model.Item.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrLocationContainingIgnoreCase(
            String title,
            String description,
            String location
    );

    List<Item> findByType(ItemType type);

    List<Item> findByCategoryIgnoreCase(String category);

    List<Item> findByStatus(ItemStatus status);
}