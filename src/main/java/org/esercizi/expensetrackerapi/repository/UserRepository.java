package org.esercizi.expensetrackerapi.repository;

import org.esercizi.expensetrackerapi.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
}
