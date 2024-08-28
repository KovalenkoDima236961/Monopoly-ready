package com.dimon.catanbackend.service;

import com.dimon.catanbackend.entities.AuctionState;
import com.dimon.catanbackend.entities.Game;
import com.dimon.catanbackend.entities.Player;
import com.dimon.catanbackend.entities.Property;
import com.dimon.catanbackend.repositories.GameRepository;
import com.dimon.catanbackend.repositories.PlayerRepository;
import com.dimon.catanbackend.repositories.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuctionService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TaskScheduler taskScheduler;

    private final Map<String, AuctionState> auctionStateMap = new ConcurrentHashMap<>();

    public void startAuction(Map<String, Object> message) {
        String gameId = (String) message.get("gameId");
        String username = (String) message.get("username");
        String propertyName = (String) message.get("propertyName");
        int initialBid = (Integer) message.get("initialBid");


        System.out.println("Starting auction for property: " + propertyName + " in game: " + gameId);
        if (auctionStateMap.containsKey(gameId)) {
            throw new RuntimeException("Auction already in progress for this game.");
        }

        Game game = gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game not found"));
        Player player = playerRepository.findByUsernameAndGameId(username, gameId).orElseThrow(() -> new RuntimeException("Player not found"));
        Property property = propertyRepository.findByGameIdAndName(gameId, propertyName)
                .orElseThrow(() -> new RuntimeException("Property '" + propertyName + "' not found in game '" + gameId + "'"));

        if (property.getOwner() != null) {
            throw new RuntimeException("Property already owned");
        }

        AuctionState auctionState = new AuctionState(property, player, taskScheduler);
        auctionStateMap.put(gameId, auctionState);

        auctionState.placeBid(player, initialBid);

        // Notify all players that the auction has started and the first bid has been placed
        Map<String, Object> response = Map.of(
                "gameId", gameId,
                "gameName", game.getName(),
                "property", property,
                "highestBid", auctionState.getHighestBid(),
                "highestBidder", auctionState.getHighestBidder(),
                "currentPlayerTurn", player.getId()
        );

        messagingTemplate.convertAndSendToUser(username,"/topic/auction/bid/" + game.getId(), response);
        auctionState.scheduleEnd(() -> endAuction(gameId));
    }

    public void placeBid(Map<String, Object> message) {
        String gameId = (String) message.get("gameId");
        String username = (String) message.get("username");
        int bidAmount = (Integer) message.get("bidAmount");

        AuctionState auctionState = auctionStateMap.get(gameId);
        if (auctionState == null) {
            throw new RuntimeException("AuctionState not found");
        }
        Game game = gameRepository.findById(gameId).get();
        Player player = playerRepository.findByUsernameAndGameId(username, gameId).orElseThrow(() -> new RuntimeException("Player not found"));

        if (bidAmount <= auctionState.getHighestBid()) {
            throw new RuntimeException("Bid amount must be higher than the current highest bid.");
        }

        if (player.getMoney() < bidAmount) {
            throw new RuntimeException("You don't have enough money");
        }

        if (auctionState.placeBid(player, bidAmount)) {
            // Determine the next player
            Player nextPlayer = getNextPlayer(game, player);
            if (nextPlayer == null) {
                endAuction(gameId);
                return;
            }

            // Notify the next player to place their bid
            Map<String, Object> response = Map.of(
                    "gameId", gameId,
                    "gameName", game.getName(),
                    "highestBidder", player,
                    "highestBid", bidAmount,
                    "nextBidder", nextPlayer.getUsername()
            );
            messagingTemplate.convertAndSend("/topic/auction/bid/" + game.getId(), response);
            // Reschedule the auction end time
            auctionState.scheduleEnd(() -> endAuction(gameId));
        }
    }

    private Player getNextPlayer(Game game, Player currentPlayer) {
        // Logic to determine the next player
        // This could be based on a turn order list or any other rule you define
        List<Player> players = playerRepository.findByGameId(game.getId());
        int currentIndex = players.indexOf(currentPlayer);
        if (currentIndex < 0) {
            return null;
        }

        int nextIndex = (currentIndex + 1) % players.size();
        while (nextIndex != currentIndex) {
            Player nextPlayer = players.get(nextIndex);
            if (nextPlayer.getMoney() >= auctionStateMap.get(game.getId()).getHighestBid()) {
                return nextPlayer;
            }
            nextIndex = (nextIndex + 1) % players.size();
        }

        return null;  // No eligible players left to bid, auction ends
    }

    private void endAuction(String gameId) {
        AuctionState auctionState = auctionStateMap.remove(gameId);
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game not found"));
        if (auctionState != null) {
            Property property = auctionState.getProperty();
            Player highestBidder = auctionState.getHighestBidder();
            int highestBid = auctionState.getHighestBid();

            if (highestBidder != null) {
                highestBidder.setMoney(highestBidder.getMoney() - highestBid);
                property.setOwner(highestBidder);
                playerRepository.save(highestBidder);
                propertyRepository.save(property);
            }

            List<Player> players = playerRepository.findByGameId(gameId);

            Map<String, Object> response = Map.of(
                    "gameId", gameId,
                    "gameName", game.getName(),
                    "property", property,
                    "highestBidder", highestBidder,
                    "highestBid", highestBid,
                    "players", players
            );

            messagingTemplate.convertAndSend("/topic/auction/end/" + game.getId(), response);
        }
    }

}
