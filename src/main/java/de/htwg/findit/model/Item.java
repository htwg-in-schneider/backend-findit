package de.htwg.findit.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
public class Item {

    public enum ItemType {
        LOST,
        FOUND
    }

    public enum ItemStatus {
        OPEN,
        IN_PROGRESS,
        RETURNED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @NotNull
    private ItemType type;

    private String category;
    private String location;
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.OPEN;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Item() {
    }

    public Item(String title, String description, ItemType type, String category, String location,
                LocalDate date, ItemStatus status, User user) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.category = category;
        this.location = location;
        this.date = date;
        this.status = status;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ItemType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getDate() {
        return date;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public void setUser(User user) {
        this.user = user;
    }
}