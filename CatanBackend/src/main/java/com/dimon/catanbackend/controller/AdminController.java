package com.dimon.catanbackend.controller;

import com.dimon.catanbackend.entities.Game;
import com.dimon.catanbackend.entities.User;
import com.dimon.catanbackend.exceptions.GameNotFoundException;
import com.dimon.catanbackend.exceptions.UserNotFoundException;
import com.dimon.catanbackend.service.GameService;
import com.dimon.catanbackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/adminPanel")
public class AdminController {

    private final UserService service;
    private final GameService gameService;

    @Autowired
    public AdminController(UserService userService, GameService gameService) {
        this.service = userService;
        this.gameService = gameService;
    }

    @GetMapping("/getAllUser")
    public ResponseEntity<?> getAllUser() {
        List<User> users = service.getAllUser();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/deleteUser/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            service.deleteUserById(userId);
            return ResponseEntity.ok().body("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(404).body("User not found");
        }
    }

    @GetMapping("/getAllGames")
    public ResponseEntity<?> getAllGames() {
        List<Game> games = gameService.findAllGames();
        return ResponseEntity.ok(games);
    }

    @DeleteMapping("/deleteGame/{gameId}")
    public ResponseEntity<?> deleteGame(@PathVariable String gameId) {
        try {
            gameService.deleteGameById(gameId);
            return ResponseEntity.ok().body("Game deleted successfully");
        } catch (GameNotFoundException e) {
            return ResponseEntity.status(404).body("Game not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while deleting the game");
        }
    }

    @PostMapping("/watchGame/{gameId}")
    public ResponseEntity<String> watchGame(@PathVariable String gameId, @RequestParam Long userId) {
        try {
            gameService.addObserverToGame(gameId, userId);
            return ResponseEntity.ok("User is now observing the game");
        } catch (GameNotFoundException | UserNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while trying to watch the game");
        }
    }

}
