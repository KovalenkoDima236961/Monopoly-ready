package com.dimon.catanbackend.entities;

import lombok.Getter;
import lombok.Setter;
import org.springframework.scheduling.TaskScheduler;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

//TODO Change that highest bid will be 0 in start or half of cost
@Getter
@Setter
public class AuctionState {
    private final Property property;
    private final Player startingPlayer;
    private Player highestBidder;
    private int highestBid;
    List<Player> players;
    private ScheduledFuture<?> scheduledFuture;
    private final TaskScheduler taskScheduler;
    private int currentPlayerIndex = 0;

    public AuctionState(Property property, Player startingPlayer, TaskScheduler taskScheduler) {
        this.property = property;
        this.startingPlayer = startingPlayer;
        this.taskScheduler = taskScheduler;
        this.highestBid = property.getCost() / 2;
        this.players = property.getGame().getPlayers();
    }

    public boolean placeBid(Player player, int bidAmount) {
        if(bidAmount > highestBid) {
            highestBidder = player;
            highestBid = bidAmount;
            if(scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            return true;
        }
        return false;
    }

    public void moveToNextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public void scheduleEnd(Runnable endTask) {
        scheduledFuture = taskScheduler.schedule(endTask, new Date(System.currentTimeMillis() + 10000));
    }

}
