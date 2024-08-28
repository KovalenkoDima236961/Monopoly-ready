package com.dimon.catanbackend.dtos;

import com.dimon.catanbackend.entities.Player;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PlayerDTO {
    private String id;
    private String username;
    private int currentPosition;
    private float x;
    private float y;
    private int money;
    private String color;
    private List<PropertyDTO> properties;

    public PlayerDTO(Player player) {
        this.id = player.getId();
        this.username = player.getUsername();
        this.currentPosition = player.getCurrentPosition();
        this.x = player.getX();
        this.y = player.getY();
        this.money = player.getMoney();
        this.color = player.getColor();
        this.properties = player.getProperties().stream().map(PropertyDTO::new).collect(Collectors.toList());
    }
}
