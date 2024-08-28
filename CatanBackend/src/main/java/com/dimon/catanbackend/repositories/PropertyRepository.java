package com.dimon.catanbackend.repositories;

import com.dimon.catanbackend.entities.Player;
import com.dimon.catanbackend.entities.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, String> {
    List<Property> findByGameId(String gameId);
    Optional<Property> findByNameAndGameId(String name, String gameId);
    Optional<Property> findByGameIdAndName(String gameId, String name);
    Optional<Property> findByGameIdAndPosition(String gameId, int newPosition);
    List<Property> findByOwner(Player player);
    List<Property> findByGameIdAndCategory(String gameId, String category);
}
