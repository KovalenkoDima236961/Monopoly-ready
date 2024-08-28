package com.dimon.catanbackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// TODO Use Mongodb


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String state;

    @OneToOne(fetch = FetchType.EAGER)  // Changed to EAGER
    @JoinColumn(name = "game_id", referencedColumnName = "id", nullable = false)
    private Game game;
}
