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

@Component
public class Convertor {
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
