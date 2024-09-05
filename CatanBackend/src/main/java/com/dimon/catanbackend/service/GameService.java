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
/**
 * Service class responsible for managing the lifecycle of games, including creating, joining,
 * leaving, and observing games. It also handles saving and retrieving game state, managing turns,
 * and managing players and properties in the context of the game.
 *
 * The service interacts with several repositories such as {@link GameRepository}, {@link GameStateRepository},
 * {@link PlayerRepository}, and {@link UserRepository}. Additionally, it utilizes the
 * {@link SimpMessagingTemplate} to send updates to game clients in real-time.
 *
 * Annotations used:
 * - {@link Service} to mark this as a Spring service component.
 * - {@link Transactional} to ensure operations are handled in a transactional context where needed.
 * - {@link Autowired} to inject dependencies.
 *
 * Methods:
 * - {@code findGameById}: Finds a game by its ID.
 * - {@code saveGameState}: Saves the current game state to Redis.
 * - {@code getGameState}: Retrieves the saved game state from Redis.
 * - {@code createGame}: Creates a new game and adds the first player (game creator).
 * - {@code leaveGame}: Allows a player to leave a game.
 * - {@code joinGame}: Allows a player to join an existing game.
 * - {@code endTurn}: Ends the current player's turn and switches to the next player.
 * - {@code getGames}: Retrieves a list of all games.
 * - {@code findAllGames}: Finds all available games that have not started yet.
 * - {@code getPropertyOwner}: Retrieves the owner of a property by game and property name.
 * - {@code deleteGameById}: Deletes a game and its associated players and properties.
 * - {@code addObserverToGame}: Adds an observer to a game.
 *
 * Messaging:
 * - Uses {@link SimpMessagingTemplate} to send real-time updates to game clients via WebSocket.
 *
 * Exceptions:
 * - {@link GameNotFoundException} if the game is not found.
 * - {@link PlayerNotFoundException} if the player is not found.
 * - {@link UserNotFoundException} if the user is not found.
 * - {@link InvalidActionException} if an invalid action is attempted (e.g., joining a game twice).
 *
 * Helper utilities:
 * - {@link CompressionUtils} for handling data compression in the game state.
 * - {@link Convertor} for converting entity objects (e.g., {@link Player}) to DTOs.
 *
 * Caching:
 * - Uses {@link RedisTemplate} to cache game state information for fast retrieval.
 *
 * @see Game
 * @see Player
 * @see Property
 * @see User
 * @see SimpMessagingTemplate
 * @see GameRepository
 * @see PlayerRepository
 * @see GameStateRepository
 * @see RedisTemplate
 * @see Convertor
 * @see CompressionUtils
 *
 */
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

    /**
     * Finds a game by its ID.
     *
     * @param gameId the ID of the game to find
     * @return the game found
     * @throws GameNotFoundException if the game is not found
     */
    public Game findGameById(String gameId) {
        return gameRepository.findById(gameId).orElseThrow(() -> new GameNotFoundException("Game not found with id: "+ gameId));
    }

    /**
     * Saves the current state of the game to Redis.
     *
     * @param gameId the ID of the game
     * @param state the game state as a string
     */
    public void saveGameState(String gameId, String state) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));

        GameState gameState = new GameState(gameId, state, game);
        redisTemplate.opsForValue().set(PREFIX + gameId, gameState);
    }

    /**
     * Retrieves the saved state of the game from Redis.
     *
     * @param gameId the ID of the game
     * @return the saved game state as a string, or null if not found
     */
    public String getGameState(String gameId) {
        GameState gameState = (GameState) redisTemplate.opsForValue().get(PREFIX + gameId);
        return (gameState != null) ? gameState.getState() : null;
    }

    /**
     * Creates a new game with the specified parameters and adds the first player (the creator) to the game.
     *
     * @param message a map containing game details (gameName, username, maxPlayers)
     * @return the created game
     * @throws RuntimeException if an error occurs during game creation
     */
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

    /**
     * Allows a player to leave the game. If all players leave, the game is deleted.
     *
     * @param gameId the ID of the game
     * @param message a map containing the username of the player leaving the game
     * @return the updated game state after the player leaves
     * @throws RuntimeException if the player cannot be deleted
     */
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

    /**
     * Allows a player to join an existing game if they are not already in the game.
     *
     * @param gameId the ID of the game to join
     * @param message a map containing the username and game details
     * @return the updated game state after the player joins
     * @throws RuntimeException if an error occurs during the join process
     */
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

    /**
     * Ends the current player's turn and switches to the next player in the game.
     *
     * @param message a map containing the gameId and gameName
     */
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

    /**
     * Retrieves a list of all active games in the system.
     *
     * @return a list of maps containing game details (id and name)
     */
    public List<Map<String, String>> getGames() {
        List<Game> games = gameRepository.findAll();
        if (games.isEmpty()) {
            return Collections.emptyList();
        }
        return games.stream()
                .map(game -> Map.of("id", game.getId(), "name", game.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all available games that have not yet started.
     *
     * @return a list of games that have not started
     */
    public List<Game> findAllGames() {
        return gameRepository.findAll()
                .stream()
                .filter(game -> !game.isGameStarted())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the owner of a property by game ID and property name.
     *
     * @param gameId the ID of the game
     * @param propertyName the name of the property
     * @return an {@link Optional} containing the owner as a {@link PlayerDTO}, or empty if no owner found
     */
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

    /**
     * Deletes a game and all its associated players and properties by game ID.
     *
     * @param gameId the ID of the game to delete
     */
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

    /**
     * Adds an observer (spectator) to a game, allowing them to watch the game.
     *
     * @param gameId the ID of the game
     * @param userId the ID of the user who wants to observe
     */
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
