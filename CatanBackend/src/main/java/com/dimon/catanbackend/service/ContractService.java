package com.dimon.catanbackend.service;

import com.dimon.catanbackend.entities.Game;
import com.dimon.catanbackend.entities.Player;
import com.dimon.catanbackend.entities.Property;
import com.dimon.catanbackend.exceptions.GameNotFoundException;
import com.dimon.catanbackend.exceptions.PlayerNotFoundException;
import com.dimon.catanbackend.exceptions.PropertyNotFoundException;
import com.dimon.catanbackend.repositories.GameRepository;
import com.dimon.catanbackend.repositories.PlayerRepository;
import com.dimon.catanbackend.repositories.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
/**
 * Service class responsible for managing contract proposals and acceptance between players
 * in the game. Contracts allow players to trade money and properties. This service handles
 * the proposing, accepting, and executing of contracts between players, updating their assets
 * and notifying all players in the game of changes.
 *
 * The service interacts with the {@link GameRepository}, {@link PlayerRepository}, and {@link PropertyRepository}
 * to manage game, player, and property state. It also uses {@link SimpMessagingTemplate} to send updates
 * to the players in real-time.
 *
 * Annotations used:
 * - {@link Service} to mark this class as a Spring service component.
 * - {@link Autowired} to inject the necessary dependencies.
 *
 * Methods:
 * - {@code proposeContract}: Proposes a contract between two players, notifying all players in the game.
 * - {@code acceptContract}: Accepts a contract, executes the exchange of money and properties,
 *   and updates the game state for all players.
 *
 * Messaging:
 * - Uses {@link SimpMessagingTemplate} to notify players in real-time about contract proposals and acceptance.
 *
 * Exceptions:
 * - Throws {@link GameNotFoundException} if the game is not found.
 * - Throws {@link PlayerNotFoundException} if a player is not found.
 * - Throws {@link PropertyNotFoundException} if a property involved in the contract is not found.
 *
 * Example usage:
 * <pre>
 * {@code
 * contractService.proposeContract(message);  // Proposes a trade contract between two players
 * contractService.acceptContract(acceptMessage);  // Accepts and executes the trade contract
 * }
 * </pre>
 *
 * @see GameRepository
 * @see PlayerRepository
 * @see PropertyRepository
 * @see Game
 * @see Player
 * @see Property
 * @see SimpMessagingTemplate
 * @see GameNotFoundException
 * @see PlayerNotFoundException
 * @see PropertyNotFoundException
 *
 */
@Service
public class ContractService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Proposes a contract between two players in the specified game. This contract can include
     * money and/or property offers from each player. The proposed contract is broadcast to all
     * players in the game via WebSocket.
     *
     * @param message the message containing the contract details (gameId, fromUsername, toUsername, offer, and request)
     * @throws GameNotFoundException if the game is not found
     * @throws PlayerNotFoundException if either the proposing or receiving player is not found
     */
    public void proposeContract(Map<String, Object> message) {
        String gameId = (String) message.get("gameId");
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        Map<String, Object> contract = (Map<String, Object>) message.get("contract");

        String fromUsername = (String) contract.get("fromUsername");
        String toUsername = (String) contract.get("toUsername");

        Player fromPlayer = playerRepository.findByUsernameAndGameId(fromUsername, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + fromUsername));
        Player toPlayer = playerRepository.findByUsernameAndGameId(toUsername, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + toUsername));

        Map<String, Object> response = Map.of(
                "contract", contract,
                "gameId", gameId,
                "gameName", game.getName()
        );

        messagingTemplate.convertAndSend("/topic/game/" + game.getId() + "/contract", response);
    }

    /**
     * Accepts a proposed contract between two players, executing the exchange of money and properties
     * as specified in the contract. After the contract is accepted and executed, all players in the game
     * are notified of the updated game state.
     *
     * @param message the message containing the contract details (gameId, fromUsername, toUsername, offer, and request)
     * @throws GameNotFoundException if the game is not found
     * @throws PlayerNotFoundException if either the proposing or receiving player is not found
     * @throws PropertyNotFoundException if any property involved in the contract is not found
     */
    public void acceptContract(Map<String, Object> message) {
        String gameId = (String) message.get("gameId");
        Map<String, Object> contract = (Map<String, Object>) message.get("contract");

        String fromUsername = (String) contract.get("fromUsername");
        String toUsername = (String) contract.get("toUsername");

        Player fromPlayer = playerRepository.findByUsernameAndGameId(fromUsername, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + fromUsername));
        Player toPlayer = playerRepository.findByUsernameAndGameId(toUsername, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + toUsername));

        Map<String, Object> offer = (Map<String, Object>) contract.get("offer");
        Map<String, Object> request = (Map<String, Object>) contract.get("request");

        int offerMoney = (int) offer.get("money");
        int requestMoney = (int) request.get("money");
        String offerProperty = (String) offer.get("property");
        String requestProperty = (String) request.get("property");

        fromPlayer.setMoney(fromPlayer.getMoney() - offerMoney + requestMoney);
        toPlayer.setMoney(toPlayer.getMoney() - requestMoney + offerMoney);

        // Transfer properties if applicable
        if (offerProperty != null && !offerProperty.isEmpty()) {
            Property property = propertyRepository.findByNameAndGameId(offerProperty, gameId)
                    .orElseThrow(() -> new PropertyNotFoundException("Property not found with name: " + offerProperty));
            property.setOwner(toPlayer);
            propertyRepository.save(property);
        }
        if (requestProperty != null && !requestProperty.isEmpty()) {
            Property property = propertyRepository.findByNameAndGameId(requestProperty, gameId)
                    .orElseThrow(() -> new PropertyNotFoundException("Property not found with name: " + requestProperty));
            property.setOwner(fromPlayer);
            propertyRepository.save(property);
        }

        playerRepository.save(fromPlayer);
        playerRepository.save(toPlayer);

        // Notify all players about the updated game state
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        List<Player> players = game.getPlayers();

        Map<String, Object> response = Map.of(
                "players", players,
                "gameId", gameId,
                "gameName", game.getName()
        );
        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), response);
    }
}
