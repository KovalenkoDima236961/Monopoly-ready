package com.dimon.catanbackend.repositories;

import com.dimon.catanbackend.entities.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {
    List<Player> findByGameId(String gameId);
    Optional<Player> findByUsernameAndGameId(String username, String gameId);
    void deleteByUsername(String username);
    void delete(Player player);
    Optional<Player> findByUsername(String username);
}
