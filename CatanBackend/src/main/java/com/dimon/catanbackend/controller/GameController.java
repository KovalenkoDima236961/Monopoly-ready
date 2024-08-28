package com.dimon.catanbackend.controller;

import com.dimon.catanbackend.dtos.PlayerDTO;
import com.dimon.catanbackend.entities.Game;
import com.dimon.catanbackend.entities.Player;
import com.dimon.catanbackend.service.AuctionService;
import com.dimon.catanbackend.service.ContractService;
import com.dimon.catanbackend.service.GameService;
import com.dimon.catanbackend.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("")
    public ResponseEntity<?> createGame(@RequestBody Map<String, String> message, @AuthenticationPrincipal UserDetails userDetails) {
        if(userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        message.put("username", userDetails.getUsername());
        System.out.println("Creating game for user: " + userDetails.getUsername());
        try {
            Game createdGame = gameService.createGame(message);
            messagingTemplate.convertAndSend("/topic/games", gameService.findAllGames());
            return ResponseEntity.status(201).body(createdGame);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{gameId}/state")
    public ResponseEntity<?> saveGameState(@PathVariable String gameId, @RequestBody Map<String, String> message, @AuthenticationPrincipal UserDetails userDetails) {
        if(userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        String state = message.get("state");
        System.out.println("State: " + state);
        if(state == null) {
            return ResponseEntity.status(400).body(Map.of("error", "State is missing"));
        }

        gameService.saveGameState(gameId, state);
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/{gameId}/state")
    public ResponseEntity<?> getGameState(@PathVariable String gameId) {
        String state = gameService.getGameState(gameId);
        if(state == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Game state not found"));
        }

        return ResponseEntity.ok(Map.of("state", state));
    }

    @GetMapping("")
    public ResponseEntity<?> getAllGames() {
        List<Game> games = gameService.findAllGames();
        if (games.isEmpty()) {
            System.out.println("Is empty");
            return ResponseEntity.ok(Collections.emptyList());
        } else {
            System.out.println("Not empty");
            return ResponseEntity.ok(games);
        }
    }

    @MessageMapping("/player/payMoney")
    public void payMoney(Map<String, Object> message) {
        System.out.println("Pay Money");
        playerService.payMoney(message);
    }

    //TODO ALSO PROBLEM WITH SOCKETS
    @PostMapping("/{gameId}/join")
    public ResponseEntity<?> joinGame(@PathVariable String gameId, @RequestBody Map<String, String> message, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        message.put("username", userDetails.getUsername());
        System.out.println("Attempting to join game with ID: " + gameId);
        try {
            Game game = gameService.joinGame(gameId, message);

            // Notify all clients about the updated game state
            messagingTemplate.convertAndSend("/topic/game/" + gameId, game);

            // Notify all clients if the game has started
            if (game.isGameStarted()) {
                messagingTemplate.convertAndSend("/topic/game-started/" + gameId, game);
            }

            System.out.println("Game id: " + game.getId());
            return ResponseEntity.ok(game);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{gameId}/status")
    public ResponseEntity<Map<String, Object>> getGameStatus(@PathVariable String gameId) {
        Game game = gameService.findGameById(gameId);
        if (game == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game not found"));
        }
        Map<String, Object> response = Map.of(
                "gameId", game.getId(),
                "isGameStarted", game.isGameStarted()
        );
        return ResponseEntity.ok(response);
    }

    //TODO HERE ERROR 400 BAD REQUEST
    @PostMapping("/{gameId}/leave")
    public ResponseEntity<?> leaveGame(@PathVariable String gameId, @RequestBody Map<String, String> message, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        message.put("username", userDetails.getUsername());
        try {
            Game game = gameService.leaveGame(gameId, message);
            return ResponseEntity.ok(game);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/{gameId}")
    public ResponseEntity<Game> getGameById(@PathVariable String gameId) {
        Game game = gameService.findGameById(gameId);
        if (game != null) {
            System.out.println("Get game norm");
            return ResponseEntity.ok(game);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @MessageMapping("/game/create")
    @SendTo("/topic/game-created")
    public void createGameThroughWebSocket(Map<String, String> message) {
        gameService.createGame(message);
        messagingTemplate.convertAndSend("/topic/games", gameService.findAllGames());
    }

//    @MessageMapping("/game/join")
//    public void joinGame(Map<String, String> message) {
//        gameService.joinGame(message);
//    }

    //TODO HERE PROBLEM WITH FIRST OF ALL END TURN AND THEN MOVE PLAYER IN FRONTEND
    @MessageMapping("/player/move")
    public void movePlayer(Map<String, Object> message) {
            System.out.println(message.get("gameId"));
            System.out.println(message.get("gameName"));
            System.out.println(message.get("username"));
            System.out.println(message.get("newPosition"));
            System.out.println("Move player");
            playerService.movePlayer(message);
            System.out.println("I after move player");
    }

    // TODO FIX THIS FUNCTION
    @GetMapping("/property/{gameId}/{propertyName}/owner")
    public ResponseEntity<?> getPropertyOwner(@PathVariable String gameId, @PathVariable String propertyName) {
        System.out.println("I am getting property owner");
        Game game = gameService.findGameById(gameId);
        if (game == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game not found"));
        }

        Player owner = game.getPlayers().stream()
                .filter(player -> player.getProperties().stream()
                        .anyMatch(property -> property.getName().equals(propertyName)))
                .findFirst()
                .orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("owner", owner != null ? new PlayerDTO(owner) : null);

        if (owner != null) {
            return ResponseEntity.ok(response);
        } else {
            System.out.println("Owner is null");
            return ResponseEntity.ok(response);
        }

    }

    @MessageMapping("/player/payRent")
    public void payRent(Map<String, Object> message) {
        System.out.println("Pay Rent");
        playerService.payRent(message);
    }

    @MessageMapping("/game/endTurn")
    public void endTurn(Map<String, String> message) {
        System.out.println("End turn");
        gameService.endTurn(message);
    }


    @GetMapping("/api/games")
    @ResponseBody
    public List<Map<String, String>> getGames() {
        return gameService.getGames();
    }


    @MessageMapping("/player/buyProperty")
    public void buyProperty(Map<String, String> message) {
        System.out.println("I buy property");
        playerService.buyProperty(message);
    }

    @MessageMapping("/player/landOnField")
    public void landOnField(Map<String, String> message) {
        playerService.landOnField(message);
    }


    @MessageMapping("/player/buyOffice")
    public void buyOffice(Map<String, String> message) {
        playerService.buyOffice(message);
    }

    @MessageMapping("/player/casinoGame")
    public void playCasinoGame(Map<String, Object> message) {
        playerService.playCasinoGame(message);
    }

    @MessageMapping("/player/startAuction")
    public void startAuction(Map<String, Object> message) {
        System.out.println("I start Auction");
        auctionService.startAuction(message);
    }

    @MessageMapping("/player/placeBid")
    public void placeBid(Map<String, Object> message) {
        auctionService.placeBid(message);
    }

    @MessageMapping("/player/sellOffice")
    public void sellOffice(Map<String, String> message) {
        playerService.sellOffice(message);
    }

    @MessageMapping("/player/mortgageProperty")
    public void mortgageProperty(Map<String, String> message) {
        playerService.mortgageProperty(message);
    }

    @MessageMapping("/player/unmortgageProperty")
    public void unmortgageProperty(Map<String, String> message) {
        playerService.unmortgageProperty(message);
    }

    @MessageMapping("/player/surrender")
    public void surround(Map<String, String> message) {
        playerService.surrender(message);
    }

    @MessageMapping("/player/proposeContract")
    public void proposeContract(Map<String, Object> message) {
        contractService.proposeContract(message);
    }

    @MessageMapping("/player/acceptContract")
    public void acceptContract(Map<String, Object> message) {
        contractService.acceptContract(message);
    }

}
