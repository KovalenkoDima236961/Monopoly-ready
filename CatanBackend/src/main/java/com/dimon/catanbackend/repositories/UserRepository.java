package com.dimon.catanbackend.repositories;

import com.dimon.catanbackend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByActivationToken(String token);
    Optional<User> findByEmail(String email);
}
