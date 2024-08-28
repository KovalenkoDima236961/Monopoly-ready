package com.dimon.catanbackend.controller;

import com.dimon.catanbackend.exceptions.GameNotFoundException;
import com.dimon.catanbackend.exceptions.UserNotFoundException;
import com.dimon.catanbackend.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/m1tv")
public class M1TVController {
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public M1TVController(GameService gameService, SimpMessagingTemplate simpMessagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = simpMessagingTemplate;
    }

    @GetMapping("/getAllGames")
    public ResponseEntity<?> getAllGames() {
        try {
            return ResponseEntity.ok(gameService.findAllGames());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while fetching games");
        }
    }

    @PostMapping("/watchGame/{gameId}")
    public ResponseEntity<String> watchGame(@PathVariable String gameId, @RequestParam(required = false) Long userId) {
        try {
            if (userId != null) {
                gameService.addObserverToGame(gameId, userId);
            }
            return ResponseEntity.ok("User is now observing the game");
        } catch (GameNotFoundException | UserNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while trying to watch the game");
        }
    }
}
