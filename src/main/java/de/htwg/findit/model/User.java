package de.htwg.findit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_user")
public class User {

    public enum UserRole {
        USER,
        ADMIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 160)
    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Size(max = 240)
    @Column(unique = true, length = 240)
    private String authSubject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Column(nullable = false, length = 20)
    private String displayColor = "#2563eb";

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    private List<Item> items = new ArrayList<>();

    public User() {
    }

    public User(String name, String email) {
        this.name = name;
        this.email = email;
        this.role = UserRole.USER;
        this.displayColor = "#2563eb";
    }

    public User(String name, String email, String authSubject, UserRole role) {
        this.name = name;
        this.email = email;
        this.authSubject = authSubject;
        this.role = role == null ? UserRole.USER : role;
        this.displayColor = "#2563eb";
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAuthSubject() {
        return authSubject;
    }

    public UserRole getRole() {
        return role;
    }

    public String getDisplayColor() {
        return displayColor;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAuthSubject(String authSubject) {
        this.authSubject = authSubject;
    }

    public void setRole(UserRole role) {
        this.role = role == null ? UserRole.USER : role;
    }

    public void setDisplayColor(String displayColor) {
        this.displayColor = displayColor == null || displayColor.isBlank()
                ? "#2563eb"
                : displayColor;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}