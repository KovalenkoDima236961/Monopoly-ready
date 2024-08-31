package com.dimon.catanbackend.service;

import com.dimon.catanbackend.dtos.PlayerDTO;
import com.dimon.catanbackend.entities.*;
import com.dimon.catanbackend.exceptions.GameNotFoundException;
import com.dimon.catanbackend.exceptions.InvalidActionException;
import com.dimon.catanbackend.exceptions.PlayerNotFoundException;
import com.dimon.catanbackend.exceptions.UserNotFoundException;
import com.dimon.catanbackend.repositories.GameRepository;
import com.dimon.catanbackend.repositories.GameStateRepository;
import com.dimon.catanbackend.repositories.PlayerRepository;
import com.dimon.catanbackend.repositories.UserRepository;
import com.dimon.catanbackend.utils.CompressionUtils;
import com.dimon.catanbackend.utils.Convertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameStateRepository gameStateRepository;

    @Autowired
    private Convertor convertor;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "game_state:";

    public Game findGameById(String gameId) {
        return gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException("Game not found with id: "+ gameId));
    }


    public void saveGameState(String gameId, String state) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));

        GameState gameState = new GameState(gameId, state, game);
        redisTemplate.opsForValue().set(PREFIX + gameId, gameState);
    }

    public String getGameState(String gameId) {
        GameState gameState = (GameState) redisTemplate.opsForValue().get(PREFIX + gameId);
        return (gameState != null) ? gameState.getState() : null;
    }

    @Transactional
    public Game createGame(Map<String, String> message) {
        try {
            String gameName = message.get("gameName");
            String username = message.get("username");
            int maxPlayers = Integer.parseInt(message.get("maxPlayers"));

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + username));

            Game game = Game.builder()
                    .id(UUID.randomUUID().toString())
                    .name(gameName)
                    .isGameStarted(false)
                    .maxPlayers(maxPlayers)
                    .createdTime(LocalDateTime.now())
                    .players(new ArrayList<>())
                    .build();

            Player player = Player.builder()
                    .id(UUID.randomUUID().toString())
                    .username(user.getUsername())
                    .game(game)
                    .x(0)
                    .y(0)
                    .money(100000)
                    .currentPosition(0)
                    .color("red")
                    .properties(new HashSet<>())
                    .user(user)
                    .build();

            game.addPlayer(player);
            game.setCurrentPlayerId(player.getId());
            gameRepository.save(game);

            propertyService.initializeProperties(game);

            List<PlayerDTO> playerDTOs = playerRepository.findByGameId(game.getId()).stream()
                    .map(convertor::convertToPlayerDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                    "gameId", game.getId(),
                    "gameName", game.getName(),
                    "players", playerDTOs,
                    "currentPlayerId", game.getCurrentPlayerId()
            );
            messagingTemplate.convertAndSend("/topic/game-created/" + game.getId(), response);

            return game;
        } catch (Exception e) {
            // Log the error
            logger.error("Error creating game: ",e);
            throw new RuntimeException("Error creating game: " + e.getMessage());
        }
    }

    //TODO Now game set to null, but it doesn't delete player from db
    @Transactional
    public Game leaveGame(String gameId, Map<String, String> message) {
        String username = message.get("username");
        System.out.println(username);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + username));
        Player player = playerRepository.findByUsernameAndGameId(user.getUsername(), gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found in this game"));

        System.out.println("I would like to delete player: " + player.getUsername());

        // Remove player from the game's player list
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        game.removePlayer(player);
        int count = 0;
        for(Player player1 : game.getPlayers()) {
            count++;
            System.out.println("Player [" + count + "] with username: " + player1.getUsername());
        }

        System.out.println(player.getGame());
        gameRepository.save(game);
        playerRepository.save(player);
        Player deletePlayer = playerRepository.findByUsername(user.getUsername()).orElseThrow(() -> new RuntimeException("Player not found"));
        playerRepository.delete(deletePlayer);
//        playerRepository.deleteByUsername(user.getUsername());


        System.out.println("After deleting");
        System.out.println(game.getPlayers());
        if (game.getPlayers().isEmpty()) {
            System.out.println("Empty");
            gameRepository.delete(game);
            Map<String, String> response = Map.of(
                    "gameId", gameId,
                    "end","end"
            );
            messagingTemplate.convertAndSend("/topic/game-removed", response);
        } else {
            System.out.println("Not empty");
            List<PlayerDTO> playerDTOs = game.getPlayers().stream()
                    .map(convertor::convertToPlayerDTO)
                    .collect(Collectors.toList());

            for (PlayerDTO playerDTO: playerDTOs) {
                System.out.println(playerDTO.getUsername());
            }

            Map<String, Object> response = Map.of(
                    "gameId", game.getId(),
                    "gameName", game.getName(),
                    "players", playerDTOs,
                    "currentPlayerId", game.getCurrentPlayerId()
            );

            messagingTemplate.convertAndSend("/topic/game/" + game.getId(), response);
        }
        return game;
    }
    //TODO Doesn't work properly when i started game
    @Transactional
    public Game joinGame(String gameId, Map<String, String> message) {
        try {
            String username = message.get("username");
            String gameName = message.get("gameName");

            if (username == null || gameName == null) {
                throw new RuntimeException("Username or Game Name is missing");
            }

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + username));
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));

            boolean isAlreadyInGame = game.getPlayers().stream()
                    .anyMatch(player -> player.getUsername().equals(username));

            if (isAlreadyInGame) {
                throw new InvalidActionException("User already in the game");
            }

            Player player = Player.builder()
                    .id(UUID.randomUUID().toString())
                    .username(user.getUsername())
                    .game(game)
                    .x(0)
                    .y(0)
                    .money(100000)
                    .currentPosition(0)
                    .color("blue")
                    .user(user)
                    .build();

            game.addPlayer(player);

            boolean gameStarted = game.isGameStarted();
            if (game.getPlayers().size() >= game.getMaxPlayers() && !gameStarted) {
                game.setGameStarted(true);
                gameStarted = true;
            }

            gameRepository.save(game);

            Map<String, Object> response = Map.of(
                    "gameId", game.getId(),
                    "gameName", gameName,
                    "game", game,
                    "players", game.getPlayers().stream()
                            .map(convertor::convertToPlayerDTO)
                            .collect(Collectors.toList()),
                    "isGameStarted", game.isGameStarted(),
                    "currentPlayerId", game.getCurrentPlayerId()
            );

            messagingTemplate.convertAndSend("/topic/game/" + game.getId(), response);

            // Ensure the new player also gets the game started message if the game has started
            if (gameStarted) {
                messagingTemplate.convertAndSend("/topic/game-started/" + game.getId(), response);
            }

            return game;
        } catch (Exception e) {
            logger.error("Error joining game: ", e);
            throw new RuntimeException("Error joining game: " + e.getMessage());
        }
    }

    public void endTurn(Map<String, String> message) {
        String gameId = message.get("gameId");
        String gameName = message.get("gameName");

        System.out.println("Game id: " + gameId);
        System.out.println("Game name: " + gameName);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));

        List<Player> players = game.getPlayers();
        String currentPlayerId = game.getCurrentPlayerId();
        System.out.println("Current player id: " + currentPlayerId);
        int currentIndex = players.stream().map(Player::getId).toList().indexOf(currentPlayerId);
        System.out.println("current index: " + currentIndex);

        if (currentIndex != -1) {
            int nextIndex = (currentIndex + 1) % players.size();
            System.out.println("Next index: " + nextIndex);
            game.setCurrentPlayerId(players.get(nextIndex).getId());
            System.out.println("Game current player id: " + game.getCurrentPlayerId());
            gameRepository.save(game);

            List<PlayerDTO> playerDTOs = players.stream()
                    .map(convertor::convertToPlayerDTO)
                    .toList();

            List<Player> playerss = playerRepository.findByGameId(gameId);
            for (Player p : players) {
                p.getProperties().size();
            }

            Map<String, Object> response = Map.of(
                    "gameId", game.getId(),
                    "gameName", game.getName(),
                    "players", playerss,
                    "currentPlayerId", game.getCurrentPlayerId()
            );

            messagingTemplate.convertAndSend("/topic/game/" + game.getId(), response);
        }
    }

    public List<Map<String, String>> getGames() {
        List<Game> games = gameRepository.findAll();
        if (games.isEmpty()) {
            return Collections.emptyList();
        }
        return games.stream()
                .map(game -> Map.of("id", game.getId(), "name", game.getName()))
                .collect(Collectors.toList());
    }

    public List<Game> findAllGames() {
        return gameRepository.findAll()
                .stream()
                .filter(game -> !game.isGameStarted())
                .collect(Collectors.toList());
    }

    public Optional<PlayerDTO> getPropertyOwner(String gameId, String propertyName) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game not found"));
        Optional<Property> property = propertyService.findByGameIdAndPropertyName(gameId, propertyName);

        for(Player player : game.getPlayers()) {
            if(player.getProperties().contains(property.get())) {
                return Optional.of(new PlayerDTO(player));
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void deleteGameById(String gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));

        for(Player player : game.getPlayers()) {
            playerRepository.delete(player);
        }

        for(Property property : game.getProperties()) {
            propertyService.delete(property);
        }

        gameRepository.delete(game);
    }

    @Transactional
    public void addObserverToGame(String gameId, Long userId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if(!game.getObservers().contains(user)) {
            game.addObserver(user);
            gameRepository.save(game);

            Map<String, Object> response = Map.of(
                    "gameId", game.getId(),
                    "gameName", game.getName(),
                    "players", game.getPlayers().stream()
                            .map(convertor::convertToPlayerDTO)
                            .collect(Collectors.toList()),
                    "currentPlayerId", game.getCurrentPlayerId(),
                    "state", getGameState(game.getId())
            );
            messagingTemplate.convertAndSend("/topic/game-watch/" + game.getId(), response);
        }
    }
}
