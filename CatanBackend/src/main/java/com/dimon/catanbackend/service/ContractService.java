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
