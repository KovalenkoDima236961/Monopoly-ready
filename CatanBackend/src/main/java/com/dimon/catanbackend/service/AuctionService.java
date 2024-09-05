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
/**
 * Service class responsible for handling auction-related logic in the game. The auction allows players
 * to bid for properties that are not yet owned, with the highest bidder acquiring the property at the end
 * of the auction. The service manages the auction process, including starting an auction, placing bids,
 * determining the next bidder, and ending the auction.
 *
 * The service interacts with the {@link GameRepository}, {@link PlayerRepository}, and {@link PropertyRepository}
 * to manage the state of the game, players, and properties. It also uses {@link SimpMessagingTemplate} to send
 * updates to game clients and the {@link TaskScheduler} to manage auction timing.
 *
 * Annotations used:
 * - {@link Service} to mark this class as a Spring service component.
 * - {@link Autowired} to inject the necessary dependencies.
 *
 * Fields:
 * - {@code auctionStateMap}: A concurrent map that stores the current auction state for each game by game ID.
 *
 * Methods:
 * - {@code startAuction}: Initiates an auction for a property, with an initial bid placed by the player.
 * - {@code placeBid}: Allows a player to place a bid in the ongoing auction, checking for valid bids.
 * - {@code getNextPlayer}: Determines the next player in the auction who is eligible to place a bid.
 * - {@code endAuction}: Ends the auction, awarding the property to the highest bidder and deducting their money.
 *
 * Messaging:
 * - Uses {@link SimpMessagingTemplate} to notify players in real-time about auction events (start, bid, and end).
 *
 * Scheduling:
 * - Uses {@link TaskScheduler} to schedule the automatic end of the auction after a specific duration.
 *
 * Example usage:
 * <pre>
 * {@code
 * auctionService.startAuction(message);  // Starts an auction with an initial bid
 * auctionService.placeBid(bidMessage);   // Allows players to place bids during the auction
 * }
 * </pre>
 *
 * Exceptions:
 * - Throws {@link RuntimeException} for various conditions such as when an auction is already in progress,
 *   when a property is already owned, or if a player places an invalid bid.
 *
 * @see AuctionState
 * @see Game
 * @see Player
 * @see Property
 * @see GameRepository
 * @see PlayerRepository
 * @see PropertyRepository
 * @see SimpMessagingTemplate
 * @see TaskScheduler
 *
 */
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

    /**
     * Starts an auction for the specified property in the game, with an initial bid placed by the player.
     *
     * @param message a map containing the game ID, username, property name, and initial bid
     * @throws RuntimeException if an auction is already in progress or if the property is already owned
     */
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

    /**
     * Allows a player to place a bid in the ongoing auction. Ensures that the bid is valid
     * (i.e., higher than the current highest bid and that the player has enough money).
     *
     * @param message a map containing the game ID, username, and bid amount
     * @throws RuntimeException if the auction is not found, the bid is too low, or the player lacks funds
     */
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

    /**
     * Determines the next player who can place a bid in the auction. Skips players who do not have enough money to bid.
     *
     * @param game the current game object
     * @param currentPlayer the current player who just placed a bid
     * @return the next player eligible to place a bid, or {@code null} if no players are eligible
     */
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

    /**
     * Ends the auction and awards the property to the highest bidder. Deducts the bid amount from the highest bidder's money.
     * Sends a message to all players with the auction result.
     *
     * @param gameId the ID of the game in which the auction is ending
     */
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
