package com.dimon.catanbackend.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

@Entity
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
public class Property {
    @Id
    private String id;
    private String name;
    private int position;
    private int offices = 0;
    private int cost;
    private String category;
    private int baseRent;
    private boolean mortgaged = false;
    private int mortgageValue;
    private int originalBaseRent;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonBackReference
    private Player owner;

    @ManyToOne
    @JoinColumn(name = "game_id")
    @JsonBackReference(value = "game-properties")
    private Game game;

    public Property(String id, String name, int position, int cost, String category, Game game, Player owner, int baseRent) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.cost = cost;
        this.category = category;
        this.game = game;
        this.owner = owner;
        this.baseRent = baseRent;
        this.originalBaseRent = baseRent;
        this.mortgageValue = (int) (cost / 2);
    }
}
