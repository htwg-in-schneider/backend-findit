package de.htwg.findit.controller;

import de.htwg.findit.model.ContactRequest;
import de.htwg.findit.model.ContactRequest.ContactRequestStatus;
import de.htwg.findit.model.Item;
import de.htwg.findit.repository.ContactRequestRepository;
import de.htwg.findit.repository.ItemRepository;
import de.htwg.findit.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/contact-requests")
public class ContactRequestController {

    private final ContactRequestRepository contactRequestRepository;
    private final ItemRepository itemRepository;
    private final CurrentUserService currentUserService;

    public ContactRequestController(
            ContactRequestRepository contactRequestRepository,
            ItemRepository itemRepository,
            CurrentUserService currentUserService
    ) {
        this.contactRequestRepository = contactRequestRepository;
        this.itemRepository = itemRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ContactRequest> getAllContactRequests(@AuthenticationPrincipal Jwt jwt) {
        currentUserService.requireAdmin(jwt);

        return contactRequestRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(ContactRequest::getCreatedAt).reversed())
                .toList();
    }

    @GetMapping("/item/{itemId}")
    public List<ContactRequest> getContactRequestsByItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long itemId
    ) {
        currentUserService.requireAdmin(jwt);

        return contactRequestRepository.findByItemIdOrderByCreatedAtDesc(itemId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactRequest createContactRequest(@RequestBody @Valid ContactRequestRequest request) {
        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Eintrag wurde nicht gefunden."
                ));

        ContactRequest contactRequest = new ContactRequest(
                item,
                request.senderName().trim(),
                request.senderEmail().trim().toLowerCase(),
                request.message().trim()
        );

        if (item.getStatus() == Item.ItemStatus.OPEN) {
            item.setStatus(Item.ItemStatus.IN_PROGRESS);
            itemRepository.save(item);
        }

        return contactRequestRepository.save(contactRequest);
    }

    @PutMapping("/{id}/status")
    public ContactRequest updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestParam ContactRequestStatus status
    ) {
        currentUserService.requireAdmin(jwt);

        ContactRequest contactRequest = contactRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kontaktanfrage wurde nicht gefunden."
                ));

        contactRequest.setStatus(status);

        return contactRequestRepository.save(contactRequest);
    }

    public record ContactRequestRequest(
            @NotNull(message = "Eintrag muss angegeben werden.")
            Long itemId,

            @NotBlank(message = "Name darf nicht leer sein.")
            String senderName,

            @NotBlank(message = "E-Mail darf nicht leer sein.")
            @Email(message = "Bitte gib eine gültige E-Mail-Adresse ein.")
            String senderEmail,

            @NotBlank(message = "Nachricht darf nicht leer sein.")
            String message
    ) {
    }
}