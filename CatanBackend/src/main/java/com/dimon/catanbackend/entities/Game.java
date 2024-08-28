package com.dimon.catanbackend.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Game {
    @Id
    private String id;
    private String name;
    @Column(nullable = false)
    private boolean isGameStarted;
    private int maxPlayers;
    private String currentPlayerId;
    private LocalDateTime createdTime;

    @OneToMany(mappedBy = "game",cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Player> players = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    @JsonManagedReference(value = "game-properties")
    private List<Property> properties = new ArrayList<>();

    public void addPlayer(Player player) {
        players.add(player);
        player.setGame(this);
    }

    public void removePlayer(Player player) {
        players.remove(player);
        player.setGame(null);
    }

    @ManyToMany
    @JoinTable(
            name = "game_observers",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> observers = new HashSet<>();

    public void addObserver(User user) {
        observers.add(user);
    }

    public void removeObserver(User user) {
        observers.remove(user);
    }
}