package com.dimon.catanbackend.repositories;

import com.dimon.catanbackend.entities.Game;
import com.dimon.catanbackend.entities.GameState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameStateRepository extends JpaRepository<GameState, Long> {
    Optional<GameState> findByGame(Game game);
    Optional<GameState> findByGameId(String gameId);
}
