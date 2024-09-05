package com.dimon.catanbackend.utils;

import com.dimon.catanbackend.dtos.PlayerDTO;
import com.dimon.catanbackend.dtos.PropertyDTO;
import com.dimon.catanbackend.entities.Player;
import com.dimon.catanbackend.entities.Property;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * Utility class for converting entities to their corresponding Data Transfer Objects (DTOs).
 * This class provides methods to convert {@link Player} and {@link Property} entities to their
 * respective DTO representations, {@link PlayerDTO} and {@link PropertyDTO}.
 *
 * The {@link Component} annotation is used to mark this class as a Spring-managed bean,
 * making it available for dependency injection.
 *
 * Methods:
 * - {@code convertToPlayerDTO}: Converts a {@link Player} entity to a {@link PlayerDTO}.
 * - {@code convertToPropertyDTO}: Converts a {@link Property} entity to a {@link PropertyDTO}.
 *
 * Example usage:
 * <pre>
 * {@code
 * PlayerDTO playerDTO = convertor.convertToPlayerDTO(player);
 * PropertyDTO propertyDTO = convertor.convertToPropertyDTO(property);
 * }
 * </pre>
 *
 * @see Player
 * @see Property
 * @see PlayerDTO
 * @see PropertyDTO
 *
 */
@Component
public class Convertor {

    /**
     * Converts a {@link Player} entity to a {@link PlayerDTO}.
     *
     * @param player the {@link Player} entity to be converted
     * @return a {@link PlayerDTO} containing the converted player data
     */
    public PlayerDTO convertToPlayerDTO(Player player) {
        PlayerDTO playerDTO = new PlayerDTO();
        playerDTO.setId(player.getId());
        playerDTO.setUsername(player.getUsername());
        playerDTO.setCurrentPosition(player.getCurrentPosition());
        playerDTO.setX(player.getX());
        playerDTO.setY(player.getY());
        playerDTO.setMoney(player.getMoney());
        playerDTO.setColor(player.getColor());

        // Explicit type casting
        Set<Property> properties = player.getProperties();
        List<PropertyDTO> propertyDTOs = properties.stream()
                .map(this::convertToPropertyDTO)
                .collect(Collectors.toList());
        playerDTO.setProperties(propertyDTOs);

        return playerDTO;
    }

    /**
     * Converts a {@link Property} entity to a {@link PropertyDTO}.
     *
     * @param property the {@link Property} entity to be converted
     * @return a {@link PropertyDTO} containing the converted property data
     */
    public PropertyDTO convertToPropertyDTO(Property property) {
        return new PropertyDTO(
                property.getId(),
                property.getName(),
                property.getPosition(),
                property.getOffices(),
                property.getCost(),
                property.getCategory(),
                property.getBaseRent()
        );
    }
}
