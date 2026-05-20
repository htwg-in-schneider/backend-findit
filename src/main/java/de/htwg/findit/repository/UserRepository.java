package de.htwg.findit.repository;

import de.htwg.findit.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}