package de.htwg.findit.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
public class ContactRequest {

    public enum ContactRequestStatus {
        NEW,
        IN_PROGRESS,
        RESOLVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    @NotBlank
    private String senderName;

    @NotBlank
    @Email
    private String senderEmail;

    @NotBlank
    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private ContactRequestStatus status = ContactRequestStatus.NEW;

    private LocalDateTime createdAt;

    public ContactRequest() {
    }

    public ContactRequest(Item item, String senderName, String senderEmail, String message) {
        this.item = item;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.message = message;
        this.status = ContactRequestStatus.NEW;
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public String getMessage() {
        return message;
    }

    public ContactRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatus(ContactRequestStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}