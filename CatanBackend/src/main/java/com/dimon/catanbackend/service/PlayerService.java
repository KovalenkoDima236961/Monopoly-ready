package com.dimon.catanbackend.service;


import com.dimon.catanbackend.entities.Game;
import com.dimon.catanbackend.entities.Player;
import com.dimon.catanbackend.entities.Property;
import com.dimon.catanbackend.exceptions.*;
import com.dimon.catanbackend.repositories.GameRepository;
import com.dimon.catanbackend.repositories.PlayerRepository;
import com.dimon.catanbackend.repositories.PropertyRepository;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForArraysOfShort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class PlayerService {
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void movePlayer(Map<String, Object> message) {
        String gameId = (String) message.get("gameId");
        String gameName = (String) message.get("gameName");
        String username = (String) message.get("username");
        int newPosition = (Integer) message.get("newPosition");
        float x = ((Number) message.get("x")).floatValue();
        float y = ((Number) message.get("y")).floatValue();
        boolean isFinalPosition = (Boolean) message.get("finalPos");
        boolean isStart = (Boolean) message.get("start");

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        Player player = playerRepository.findByUsernameAndGameId(username, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));

        if (newPosition < player.getCurrentPosition() || newPosition == 0 && isStart) {
            player.setMoney(player.getMoney() + 2000);
        }

        player.setCurrentPosition(newPosition);
        player.setX(x);
        player.setY(y);

        Property landedProperty = propertyRepository.findByGameIdAndPosition(gameId, newPosition).orElse(null);
        boolean needToPayRent = false;
        int rent = 0;
        String ownerUsername = null;

        if (landedProperty != null && landedProperty.getOwner() != null && isFinalPosition) {
            ownerUsername = landedProperty.getOwner().getUsername();
            if (!ownerUsername.equals(username)) {
                rent = determineRent(landedProperty);
                needToPayRent = true;
            }
        }


        playerRepository.save(player);

        Map<String, Object> landedPropertyMap = null;
        if (landedProperty != null && isFinalPosition) {
            landedPropertyMap = new HashMap<>();
            landedPropertyMap.put("position", newPosition);
            landedPropertyMap.put("owner", ownerUsername);
            landedPropertyMap.put("needToPayRent", needToPayRent);
            landedPropertyMap.put("rent", rent);
        }

        game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        System.out.println("MOVE PLAYER");
        for(Player player1: game.getPlayers()) {
            System.out.println(player1);
        }

        List<Player> players = playerRepository.findByGameId(gameId);
        for (Player p : players) {
            p.getProperties().size();
        }

        Map<String, Object> res = new HashMap<>();
        res.put("gameId", gameId);
        res.put("gameName", gameName);
        res.put("players", players);
        res.put("username", username);
        res.put("currentPlayerId", player.getId());
        res.put("landedProperty", landedPropertyMap);

        messagingTemplate.convertAndSend("/topic/game/" + gameId, res);
    }

    // TODO HERE PROBLEM WITH SQL
    @Transactional
    public void payRent(Map<String, Object> message) {
        String gameId = (String) message.get("gameId");
        String username = (String) message.get("username");
        int rent = (Integer) message.get("rent");

        Player player = playerRepository.findByUsernameAndGameId(username, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));

        Property landedProperty = propertyRepository.findByGameIdAndPosition(gameId, player.getCurrentPosition())
                .orElseThrow(() -> new PropertyNotFoundException("Property not found at position: " + player.getCurrentPosition()));

        Player owner = landedProperty.getOwner();
        if (owner != null && !owner.getUsername().equals(username)) {
            player.setMoney(player.getMoney() - rent);
            owner.setMoney(owner.getMoney() + rent);

            playerRepository.save(player);
            playerRepository.save(owner);
        }

        List<Player> players = playerRepository.findByGameId(gameId);
        for (Player p : players) {
            p.getProperties().size();
        }

        Map<String, Object> response = Map.of(
                "gameId", gameId,
                "players", players,
                "currentPlayerId", player.getId()
        );

        messagingTemplate.convertAndSend("/topic/game/" + gameId, response);
    }

    public boolean isOwnedBySomeone(Map<String,String> message) {
        return false;
    }

    @Transactional
    public void buyProperty(Map<String, String> message) {
        String gameId = message.get("gameId");
        String gameName = message.get("gameName");
        String username = message.get("username");
        String propertyName = message.get("propertyName");

        if (propertyName.equals("Go") || propertyName.equals("Go to jail") ||
                propertyName.equals("Casino") || propertyName.equals("Prison")) {
            throw new InvalidActionException("This property cannot be bought");
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        Player player = playerRepository.findByUsernameAndGameId(username, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));
        Property property = propertyRepository.findByGameIdAndName(gameId, propertyName)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with name: " + propertyName));

        int cost = property.getCost();

        if (player.getMoney() < cost) {
            throw new InsufficientFundsException("Insufficient funds to buy the property");
        }

        property.setOwner(player);
        player.setMoney(player.getMoney() - cost);

        // Check if the player now owns all properties in this category
        checkAndApplyCategoryBonus(player, property.getCategory(), gameId);

        propertyRepository.save(property);
        playerRepository.save(player);

        List<Player> players = playerRepository.findByGameId(gameId);
        for (Player p : players) {
            p.getProperties().size(); // Ensure properties are fetched
        }

        Map<String, Object> response = Map.of(
                "gameId", game.getId(),
                "gameName", game.getName(),
                "property", property,
                "owner", player,
                "players", players
        );

        messagingTemplate.convertAndSend("/topic/game/" + gameId, response);
    }

    private void checkAndApplyCategoryBonus(Player player, String category, String gameId) {
        // Get all properties in the category
        List<Property> categoryProperties = propertyRepository.findByGameIdAndCategory(gameId, category);

        // Count how many properties the player owns in this category
        long ownedCount = categoryProperties.stream()
                .filter(property -> property.getOwner() != null && property.getOwner().equals(player))
                .count();

        if ("cars".equalsIgnoreCase(category)) {
            // Special case for car category
            int newBaseRent;
            if (ownedCount == 2) {
                newBaseRent = 500;
            } else if (ownedCount == 3) {
                newBaseRent = 1000;
            } else if (ownedCount >= 4) {
                newBaseRent = 2000;
            } else {
                return; // No bonus applied if the player owns less than 2 car properties
            }

            // Update base rent for all car properties owned by the player
            for (Property property : categoryProperties) {
                if (property.getOwner().equals(player)) {
                    property.setBaseRent(newBaseRent);
                    property.setOriginalBaseRent(newBaseRent);
                    propertyRepository.save(property);
                }
            }
        } else {
            // General case for other categories: increase base rent by 20% if the player owns all properties
            boolean ownsAll = categoryProperties.stream()
                    .allMatch(property -> property.getOwner() != null && property.getOwner().equals(player));

            if (ownsAll) {
                for (Property property : categoryProperties) {
                    int newBaseRent = (int) (property.getBaseRent() * 1.2);
                    property.setBaseRent(newBaseRent);
                    property.setOriginalBaseRent(newBaseRent);
                    propertyRepository.save(property);
                }
            }
        }
    }

    @Transactional
    public void landOnField(Map<String, String> message) {
        String gameId = message.get("gameId");
        String gameName = message.get("gameName");
        String username = message.get("username");
        String fieldName = message.get("fieldName");

        if (fieldName.equals("Prison")) {
            moveToJail(gameId, username, gameName);
        }
    }
    @Transactional
    public void buyOffice(Map<String, String> message) {
        String gameId = message.get("gameId");
        String username = message.get("username");
        String propertyName = message.get("propertyName");
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        Player player = playerRepository.findByUsernameAndGameId(username, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));
        Property property = propertyRepository.findByGameIdAndName(gameId, propertyName)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with name: " + propertyName));

        if ("cars".equalsIgnoreCase(property.getCategory())) {
            throw new InvalidActionException("Cannot buy offices for properties in the cars category.");
        }

        if (property.getOffices() >= 4) {
            throw new InvalidActionException("Maximum number of offices reached for this property");
        }

        if (player.getMoney() < 2000) {
            throw new InsufficientFundsException("Insufficient funds to buy an office");
        }

        property.setOffices(property.getOffices() + 1);
        player.setMoney(player.getMoney() - 2000);

        propertyRepository.save(property);
        playerRepository.save(player);

        List<Player> players = playerRepository.findByGameId(gameId);
        for (Player p : players) {
            p.getProperties().size();
        }

        Map<String, Object> response = Map.of(
                "gameId", gameId,
                "gameName", game.getName(),
                "property", property,
                "owner", player,
                "players", players
        );

        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), response);
    }
    @Transactional
    public void sellOffice(Map<String, String> message) {
        String gameId = message.get("gameId");
        String username = message.get("username");
        String propertyName = message.get("propertyName");

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        Player player = playerRepository.findByUsernameAndGameId(username, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));
        Property property = propertyRepository.findByGameIdAndName(gameId, propertyName)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with name: " + propertyName));

        if (property.getOffices() > 0) {
            property.setOffices(property.getOffices() - 1);
            player.setMoney(player.getMoney() + 2000); // Assuming selling an office gives back 2000

            propertyRepository.save(property);
            playerRepository.save(player);

            List<Player> players = playerRepository.findByGameId(gameId);
            for (Player p : players) {
                p.getProperties().size();
            }

            Map<String, Object> response = Map.of(
                    "gameId", gameId,
                    "gameName", game.getName(),
                    "property", property,
                    "owner", player,
                    "players", players
            );

            messagingTemplate.convertAndSend("/topic/game/" + game.getId(), response);
        } else {
            throw new InvalidActionException("No offices to sell");
        }
    }
    @Transactional
    public void mortgageProperty(Map<String, String> message) {
        String gameId = message.get("gameId");
        String username = message.get("username");
        String propertyName = message.get("propertyName");

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        Player player = playerRepository.findByUsernameAndGameId(username, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));
        Property property = propertyRepository.findByGameIdAndName(gameId, propertyName)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with name: " + propertyName));

        if (!property.isMortgaged() && property.getOffices() == 0) {
            property.setMortgaged(true);
            property.setBaseRent(0);
            player.setMoney(player.getMoney() + property.getMortgageValue()); // Assuming mortgaging gives half the property cost

            propertyRepository.save(property);
            playerRepository.save(player);

            List<Player> players = playerRepository.findByGameId(gameId);
            for (Player p : players) {
                p.getProperties().size();
            }

            Map<String, Object> response = Map.of(
                    "gameId", gameId,
                    "gameName", game.getName(),
                    "property", property,
                    "owner", player,
                    "players", players
            );

            messagingTemplate.convertAndSend("/topic/game/" + game.getId(), response);
        } else {
            throw new InvalidActionException("Cannot mortgage property with offices or already mortgaged");
        }
    }
    //TODO CHECK IF THIS FUNCTION CORRECT WORK
    @Transactional
    public void payMoney(Map<String,Object> message) {
        String gameId = (String) message.get("gameId");
        String username = (String) message.get("username");
        Integer amount = (Integer) message.get("amount");
        String propertyName = (String) message.get("propertyName"); // Get the property name

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        Player player = playerRepository.findByUsernameAndGameId(username, game.getId())
                .orElseThrow(() -> new RuntimeException("Player not found"));


        if (player.getMoney() >= amount) {
            player.setMoney(player.getMoney() - amount);
            playerRepository.save(player);
            System.out.println(username + " has paid " + amount);
        } else {
            throw new RuntimeException("Player does not have enough money to pay");
        }

        List<Player> players = game.getPlayers();
        for(Player player1: players) {
            player1.getProperties().size();
        }

        Map<String, Object> response = Map.of(
                "gameId", gameId,
                "gameName", game.getName(),
                "players", players,
                "currentPlayerId", player.getId()
        );

        messagingTemplate.convertAndSend("/topic/game/" + gameId, response);
    }



    @Transactional
    public void unmortgageProperty(Map<String, String> message) {
        String gameId = message.get("gameId");
        String username = message.get("username");
        String propertyName = message.get("propertyName");

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        Player player = playerRepository.findByUsernameAndGameId(username, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));
        Property property = propertyRepository.findByGameIdAndName(gameId, propertyName)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with name: " + propertyName));

        if (property.isMortgaged()) {
            int unmortgageCost = (int) (property.getMortgageValue() * 1.1); // Assuming unmortgaging costs 10% more
            if (player.getMoney() >= unmortgageCost) {
                property.setMortgaged(false);
                property.setBaseRent(property.getOriginalBaseRent());
                player.setMoney(player.getMoney() - unmortgageCost);

                propertyRepository.save(property);
                playerRepository.save(player);

                List<Player> players = playerRepository.findByGameId(gameId);
                for (Player p : players) {
                    p.getProperties().size();
                }

                Map<String, Object> response = Map.of(
                        "gameId", gameId,
                        "gameName", game.getName(),
                        "property", property,
                        "owner", player,
                        "players", players
                );

                messagingTemplate.convertAndSend("/topic/game/" + game.getId(), response);
            } else {
                throw new InsufficientFundsException("Insufficient funds to unmortgage the property");
            }
        } else {
            throw new InvalidActionException("Property is not mortgaged");
        }
    }
    @Transactional
    public void surrender(Map<String, String> message) {
        String gameId = message.get("gameId");
        String username = message.get("username");

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        Player player = playerRepository.findByUsernameAndGameId(username, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));

        List<Property> properties = propertyRepository.findByOwner(player);
        for (Property property : properties) {
            property.setOwner(null);
        }

        propertyRepository.saveAll(properties);

        game.getPlayers().remove(player);
        gameRepository.save(game);
        playerRepository.delete(player);

        List<Player> players = playerRepository.findByGameId(gameId);
        for (Player p : players) {
            p.getProperties().size();
        }

        Map<String, Object> response = Map.of(
                "players", players,
                "playerWhoLeave", player,
                "gameId", gameId,
                "gameName", game.getName()
        );

        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), response);
    }

    private void moveToJail(String gameId, String username, String gameName) {
        Player player = playerRepository.findByUsernameAndGameId(username, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));
        player.setCurrentPosition(10);
        playerRepository.save(player);

        Map<String, Object> response = Map.of(
                "gameId", gameId,
                "gameName", gameName,
                "username", username,
                "newPosition", player.getCurrentPosition()
        );

        messagingTemplate.convertAndSend("/topic/game/" + gameId, response);
    }
    @Transactional
    public void playCasinoGame(Map<String, Object> message) {
        String gameId = (String) message.get("gameId");
        String username = (String) message.get("username");
        int bet = (Integer) message.get("bet");
        List<Integer> selectedNumbers = (List<Integer>) message.get("selectedNumbers");

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found with id: " + gameId));
        Player player = playerRepository.findByUsernameAndGameId(username, gameId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with username: " + username));

        int randomNumber = new Random().nextInt(6) + 1;
        boolean isWinner = selectedNumbers.contains(randomNumber);
        int multiplier = 0;

        switch (selectedNumbers.size()) {
            case 1:
                multiplier = 100;
                break;
            case 2:
                multiplier = 60;
                break;
            case 3:
                multiplier = 40;
                break;
            case 4:
                multiplier = 20;
                break;
        }

        if (isWinner) {
            player.setMoney(player.getMoney() + bet + (bet * multiplier / 100));
        } else {
            player.setMoney(player.getMoney() - bet);
        }

        playerRepository.save(player);
        List<Player> players = playerRepository.findByGameId(gameId);

        Map<String, Object> response = Map.of(
                "gameId", gameId,
                "gameName", game.getName(),
                "players", players,
                "currentPlayerId", game.getCurrentPlayerId(),
                "randomNumber", randomNumber,
                "isWinner", isWinner,
                "multiplier", multiplier
        );

        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), response);
    }

    private int determineRent(Property property) {
        int baseRent = property.getBaseRent();

        int officeMultiplier = 0;
        switch (property.getOffices()) {
            case 1:
                officeMultiplier = 10;
                break;
            case 2:
                officeMultiplier = 20;
                break;
            case 3:
                officeMultiplier = 30;
                break;
            case 4:
                officeMultiplier = 50;
                break;
        }

        return baseRent + (baseRent * officeMultiplier / 100);
    }



}
