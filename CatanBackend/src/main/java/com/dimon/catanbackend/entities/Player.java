package com.dimon.catanbackend.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Player {
    @Id
    private String id;
    private String username;
    private String color;
    private float x;
    private float y;
    private int money;
    private int currentPosition;

    @ManyToOne
    @JoinColumn(name = "game_id")
    @JsonBackReference
    private Game game;

    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonBackReference
    @Builder.Default
    private Set<Property> properties = new HashSet<>();

    @ElementCollection
    @JsonIgnore
    @Builder.Default
    private Set<String> propertiesWithOffices = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference(value = "user-player")
    private User user;

    @PreRemove
    private void preRemove() {
        if (game != null) {
            System.out.println("I remove player from this game: " + game.getName());
            game.getPlayers().remove(this);
        }
    }
}
