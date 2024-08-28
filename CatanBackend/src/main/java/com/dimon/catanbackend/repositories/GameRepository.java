package com.dimon.catanbackend.repositories;

import com.dimon.catanbackend.entities.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, String> {
    Optional<Game> findByName(String name);
}
