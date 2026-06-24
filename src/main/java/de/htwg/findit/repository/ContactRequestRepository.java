package de.htwg.findit.repository;

import de.htwg.findit.model.ContactRequest;
import de.htwg.findit.model.ContactRequest.ContactRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long> {

    List<ContactRequest> findByItemIdOrderByCreatedAtDesc(Long itemId);

    List<ContactRequest> findByStatus(ContactRequestStatus status);
}