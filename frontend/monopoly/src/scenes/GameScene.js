import Phaser from "phaser";
import { Stomp } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export default class GameScene extends Phaser.Scene {
    constructor() {
        super("GameScene");
        this.boardPositions = [];
        this.currentPlayerIndex = 0;
        this.previousPlayerId = null;
        this.hasBoughtOffice = false;
        this.isNeedToPay = false;
        this.start = false;

        this.auctionInProgress = false;
        this.auctionProperty = null;
        this.auctionHighestBid = 0;
        this.auctionHighestBidder = null;
        this.auctionTimer = null;
        this.auctionTimeLeft = 10;
        this.playerCards = []; // Array to hold player card references
        this.questionMarkEvents = [
            { type: "move", steps: 3, description: "Move forward 3 spaces." },
            {
                type: "moveToField",
                fieldName: "Go",
                description: "Move to the Go field.",
            },
            {
                type: "pay",
                amount: 1000,
                description: "Pay $1000 for services.",
            },
            {
                type: "receive",
                amount: 2000,
                description: "You won the lottery! Receive $2000.",
            },
            { type: "pay", amount: 500, description: "Pay $500 in taxes." },
            { type: "move", steps: -2, description: "Move back 2 spaces." },
            // Add more events as needed
        ];
    }

    init(data) {
        this.username = data.username;
        this.gameId = data.gameId;
        this.gameName = data.gameName;
        this.playersData = data.players;
        this.players = data.players || [];
        this.currentPlayerId = data.currentPlayerId;
        this.isObserver = data.isObserver || false;
    }

    create() {
        this.restoreGameState();
        this.events.on("startTurn", () => {
            this.hasBoughtOffice = false;
        });

        window.addEventListener("beforeunload", () => {
            this.saveGameState();
        });

        console.log("Username: " + this.username);
        this.currentPlayerIndex = 0;
        this.socket = new SockJS("http://localhost:8000/ws");
        this.stompClient = Stomp.over(this.socket);

        this.stompClient.connect({}, (frame) => {
            console.log("Connected: " + frame);
            this.setupSubscriptions();
            this.showCurrentPlayerButton({ players: this.players });
        });
        console.log("Current player id: " + this.currentPlayerId);

        this.createBackground();
        this.createBoard();
        this.createPlayers(this.playersData.length);
        this.createButton();
        this.createDice();
        this.createBidButton();
        console.log(`Player Name: ${this.username}`);
        this.showCurrentPlayerButton({ players: this.players });
    }

    setupSubscriptions() {
        this.stompClient.subscribe(`/topic/game/${this.gameId}`, (message) => {
            const playerData = JSON.parse(message.body);
            console.log("Player data when connect: " + playerData);
            this.handleGameUpdate(playerData);
        });

        this.stompClient.subscribe(
            `/topic/game/${this.gameId}/contract`,
            (message) => {
                const data = JSON.parse(message.body);
                if (data.contract) {
                    this.showContractReviewWindow(data.contract);
                }
            }
        );

        this.stompClient.subscribe(
            `/topic/auction/start/${this.gameId}`,
            (message) => {
                const data = JSON.parse(message.body);
                this.startAuction(data);
            }
        );

        this.stompClient.subscribe(
            `/topic/auction/bid/${this.gameId}`,
            (message) => {
                const data = JSON.parse(message.body);
                this.bidHandler(data);
            }
        );

        this.stompClient.subscribe(
            `/topic/auction/end/${this.gameId}`,
            (message) => {
                const data = JSON.parse(message.body);
                if (data.currentPlayerTurn === this.currentPlayerId) {
                    // Show the bidding UI for this player
                    this.showBiddingWindow(data);
                } else {
                    // Hide the bidding UI as it's not this player's turn
                    this.hideAuctionWindow();
                }
            }
        );
    }

    showBiddingWindow(data) {
        const menu = this.add.graphics();
        menu.fillStyle(0x000000, 0.8);
        menu.fillRect(100, 100, 600, 400);

        const title = this.add
            .text(400, 150, `Your Turn to Bid`, {
                fontSize: "20px",
                color: "#ffffff",
                align: "center",
            })
            .setOrigin(0.5);

        const currentBidText = this.add
            .text(400, 200, `Current Bid: $${data.highestBid}`, {
                fontSize: "20px",
                color: "#ffffff",
                align: "center",
            })
            .setOrigin(0.5);

        const bidInput = this.add.dom(400, 300, "input", {
            type: "number",
            value: data.highestBid + 100, // Suggest next bid
            min: data.highestBid + 100,
            style: "width: 200px; height: 40px; font-size: 20px;",
        });

        const bidButton = this.add
            .text(300, 400, "Place Bid", {
                fontSize: "20px",
                color: "#00ff00",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                const bidAmount = parseInt(bidInput.node.value);

                if (
                    bidAmount > data.highestBid &&
                    this.currentPlayerMoney >= bidAmount
                ) {
                    this.stompClient.send(
                        "/app/player/placeBid",
                        {},
                        JSON.stringify({
                            gameId: this.gameId,
                            username: this.username,
                            bidAmount: bidAmount,
                        })
                    );

                    this.saveGameState(); // Save the game state after buying a property

                    this.hideAuctionWindow();
                } else {
                    alert(
                        "Bid must be higher than the current bid and you must have enough money."
                    );
                }
            });

        const passButton = this.add
            .text(500, 400, "Pass", {
                fontSize: "20px",
                color: "#ff0000",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                this.hideAuctionWindow();
                // Notify the server that this player is passing
                this.stompClient.send(
                    "/app/player/passBid",
                    {},
                    JSON.stringify({
                        gameId: this.gameId,
                        username: this.username,
                    })
                );
            });

        // Store references to these elements for later removal
        this.auctionMenuGraphics = menu;
        this.auctionTitleText = title;
        this.auctionCurrentBidText = currentBidText;
        this.auctionBidInput = bidInput;
        this.auctionBidButton = bidButton;
        this.auctionPassButton = passButton;
    }

    handleGameUpdate(playerData) {
        if (playerData.players) {
            this.updatePlayerPosition(playerData);
        }
        if (playerData.property && playerData.owner) {
            const property = this.getBoardPosition(playerData.property.name);
            const owner = this.players.find(
                (p) => p.username === playerData.owner.username
            );
            if (property && owner) {
                property.baseRent = playerData.property.baseRent;
                property.originalBaseRent =
                    playerData.property.originalBaseRent;
                this.addPlayerOwnership(this.players.indexOf(owner), property);
            }
        }
        if (playerData.currentPlayerId) {
            this.previousPlayerId = this.currentPlayerId;
            this.currentPlayerId = playerData.currentPlayerId;
            console.log("This current player id: ", this.currentPlayerId);
            this.updateCurrentPlayerIndex();
            this.showCurrentPlayerButton(playerData);
        }
        if (
            playerData.randomNumber !== undefined &&
            playerData.isWinner !== undefined
        ) {
            alert(
                `Casino Game Result: ${
                    playerData.isWinner ? "You won!" : "You lost."
                } Random number was: ${playerData.randomNumber}.`
            );
        }

        if (playerData.bid) {
            this.placeBid(
                this.players.find((p) => p.username === playerData.username),
                playerData.bid
            );
        }

        if (playerData.playerWhoLeft) {
            this.handlePlayerLeft(playerData.playerWhoLeft);
        }
    }

    showContractReviewWindow(contract) {
        const menu = this.add.graphics();
        menu.fillStyle(0x000000, 0.8);
        menu.fillRect(100, 100, 600, 400);

        const title = this.add
            .text(400, 130, `Contract from ${contract.fromUsername}`, {
                fontSize: "20px",
                color: "#ffffff",
                align: "center",
            })
            .setOrigin(0.5);

        const offerText = this.add
            .text(150, 180, "Offer:", { fontSize: "20px", color: "#ffffff" })
            .setOrigin(0.5);

        const requestText = this.add
            .text(450, 180, "Request:", { fontSize: "20px", color: "#ffffff" })
            .setOrigin(0.5);

        const offerMoney = this.add
            .text(150, 220, `Money: ${contract.offer.money}`, {
                fontSize: "20px",
                color: "#ffffff",
            })
            .setOrigin(0.5);

        const requestMoney = this.add
            .text(450, 220, `Money: ${contract.request.money}`, {
                fontSize: "20px",
                color: "#ffffff",
            })
            .setOrigin(0.5);

        const offerProperty = this.add
            .text(150, 270, `Property: ${contract.offer.property}`, {
                fontSize: "20px",
                color: "#ffffff",
            })
            .setOrigin(0.5);

        const requestProperty = this.add
            .text(450, 270, `Property: ${contract.request.property}`, {
                fontSize: "20px",
                color: "#ffffff",
            })
            .setOrigin(0.5);

        const acceptButton = this.add
            .text(300, 350, "Accept", {
                fontSize: "20px",
                color: "#00ff00",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                this.stompClient.send(
                    "/app/player/acceptContract",
                    {},
                    JSON.stringify({ gameId: this.gameId, contract })
                );

                menu.clear();
                title.destroy();
                offerText.destroy();
                requestText.destroy();
                offerMoney.destroy();
                requestMoney.destroy();
                offerProperty.destroy();
                requestProperty.destroy();
                acceptButton.destroy();
                declineButton.destroy();
            });

        const declineButton = this.add
            .text(500, 350, "Decline", {
                fontSize: "20px",
                color: "#ff0000",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                // Handle decline
                menu.clear();
                title.destroy();
                offerText.destroy();
                requestText.destroy();
                offerMoney.destroy();
                requestMoney.destroy();
                offerProperty.destroy();
                requestProperty.destroy();
                acceptButton.destroy();
                declineButton.destroy();
            });

        menu.add(acceptButton);
        menu.add(declineButton);
    }

    bidHandler(data) {
        // Update the current highest bid and bidder
        this.auctionHighestBid = data.highestBid;
        this.auctionHighestBidder = this.players.find(
            (player) => player.username === data.highestBidder
        );

        // Notify the next player to place a bid
        this.notifyNextPlayer();
    }

    showAuctionWindow(currentBid, highestBidder) {
        if (this.isObserver) return;

        const menu = this.add.graphics();
        menu.fillStyle(0x000000, 0.8);
        menu.fillRect(100, 100, 600, 400);

        const title = this.add
            .text(400, 150, `Auction in Progress`, {
                fontSize: "20px",
                color: "#ffffff",
                align: "center",
            })
            .setOrigin(0.5);

        const currentBidText = this.add
            .text(400, 200, `Current Bid: $${currentBid}`, {
                fontSize: "20px",
                color: "#ffffff",
                align: "center",
            })
            .setOrigin(0.5);

        const bidInput = this.add.dom(400, 300, "input", {
            type: "number",
            value: currentBid + 100, // Suggest the next bid
            min: currentBid + 100,
            style: "width: 200px; height: 40px; font-size: 20px;",
        });

        const bidButton = this.add
            .text(300, 400, "Place Bid", {
                fontSize: "20px",
                color: "#00ff00",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                const bidAmount = parseInt(bidInput.node.value);

                if (
                    bidAmount > currentBid &&
                    this.currentPlayer.money >= bidAmount
                ) {
                    this.stompClient.send(
                        "/app/game/placeBid",
                        {},
                        JSON.stringify({
                            gameId: this.gameId,
                            property: this.auctionProperty.name,
                            highestBid: bidAmount,
                            highestBidder: this.username,
                        })
                    );

                    // Clean up UI elements after bid
                    menu.clear();
                    title.destroy();
                    currentBidText.destroy();
                    bidInput.destroy();
                    bidButton.destroy();
                    passButton.destroy();

                    this.hideAuctionWindow();
                } else {
                    alert(
                        "Bid must be higher than the current bid and you must have enough money."
                    );
                }
            });

        const passButton = this.add
            .text(500, 400, "Pass", {
                fontSize: "20px",
                color: "#ff0000",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                // Clean up UI elements if player passes
                menu.clear();
                title.destroy();
                currentBidText.destroy();
                bidInput.destroy();
                bidButton.destroy();
                passButton.destroy();

                this.stompClient.send(
                    "/app/game/passBid",
                    {},
                    JSON.stringify({
                        gameId: this.gameId,
                        username: this.username,
                    })
                );

                this.hideAuctionWindow();
            });
    }

    showAuctionWindow(currentBid, highestBidder) {
        const menu = this.add.graphics();
        menu.fillStyle(0x000000, 0.8);
        menu.fillRect(100, 100, 600, 400);

        const title = this.add
            .text(400, 150, `Auction in Progress`, {
                fontSize: "20px",
                color: "#ffffff",
                align: "center",
            })
            .setOrigin(0.5);

        const currentBidText = this.add
            .text(400, 200, `Current Bid: $${currentBid}`, {
                fontSize: "20px",
                color: "#ffffff",
                align: "center",
            })
            .setOrigin(0.5);

        const bidInput = this.add.dom(400, 300, "input", {
            type: "number",
            value: currentBid + 100, // Suggest the next bid
            min: currentBid + 100,
            style: "width: 200px; height: 40px; font-size: 20px;",
        });

        const bidButton = this.add
            .text(300, 400, "Place Bid", {
                fontSize: "20px",
                color: "#00ff00",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                const bidAmount = parseInt(bidInput.node.value);

                if (
                    bidAmount > currentBid &&
                    this.currentPlayer.money >= bidAmount
                ) {
                    this.stompClient.send(
                        "/app/game/placeBid",
                        {},
                        JSON.stringify({
                            gameId: this.gameId,
                            property: this.auctionProperty.name,
                            highestBid: bidAmount,
                            highestBidder: this.username,
                        })
                    );

                    // Clean up UI elements after bid
                    menu.clear();
                    title.destroy();
                    currentBidText.destroy();
                    bidInput.destroy();
                    bidButton.destroy();
                    passButton.destroy();

                    this.hideAuctionWindow();
                } else {
                    alert(
                        "Bid must be higher than the current bid and you must have enough money."
                    );
                }
            });

        const passButton = this.add
            .text(500, 400, "Pass", {
                fontSize: "20px",
                color: "#ff0000",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                // Clean up UI elements if player passes
                menu.clear();
                title.destroy();
                currentBidText.destroy();
                bidInput.destroy();
                bidButton.destroy();
                passButton.destroy();

                this.stompClient.send(
                    "/app/game/passBid",
                    {},
                    JSON.stringify({
                        gameId: this.gameId,
                        username: this.username,
                    })
                );

                this.hideAuctionWindow();
            });
    }

    hideAuctionWindow() {
        // If you have references to the UI elements stored in instance variables, destroy or hide them
        if (this.auctionMenuGraphics) {
            this.auctionMenuGraphics.clear();
            this.auctionMenuGraphics.destroy();
        }

        if (this.auctionTitleText) {
            this.auctionTitleText.destroy();
        }

        if (this.auctionCurrentBidText) {
            this.auctionCurrentBidText.destroy();
        }

        if (this.auctionBidInput) {
            this.auctionBidInput.destroy();
        }

        if (this.auctionBidButton) {
            this.auctionBidButton.destroy();
        }

        if (this.auctionPassButton) {
            this.auctionPassButton.destroy();
        }

        // Optionally, reset the variables that held these elements to null
        this.auctionMenuGraphics = null;
        this.auctionTitleText = null;
        this.auctionCurrentBidText = null;
        this.auctionBidInput = null;
        this.auctionBidButton = null;
        this.auctionPassButton = null;
    }

    handlePlayerLeft(playerWhoLeft) {
        console.log(`Player ${playerWhoLeft.username} has left the game.`);
        alert(`Player ${playerWhoLeft.username} has left the game.`);

        this.players = this.players.filter(
            (player) => player.username !== playerWhoLeft.username
        );

        this.updatePlayerCards();
    }

    updatePlayerCards() {
        // Clear the current player cards
        this.children.getAll().forEach((child) => {
            if (child.name && child.name.startsWith("playerCard-")) {
                child.destroy();
            }
        });

        // Recreate the player cards
        this.players.forEach((player, index) => {
            const color = player.playerObj.getData("color");
            this.createPlayerCard(player, color, index);
        });
    }

    updateCurrentPlayerIndex() {
        this.currentPlayerIndex = this.players.findIndex(
            (player) => player.id === this.currentPlayerId
        );
        console.log("Current Player index: ", this.currentPlayerIndex);
    }
    updatePlayerPosition(playerData) {
        console.log("Player Data: (updatePlayerPosition): ", playerData);
        console.log("All Players: ", this.players);

        const playerUpdate = playerData.players.find(
            (p) => p.id === this.previousPlayerId
        );

        if (playerUpdate) {
            const player = this.players.find((p) => p.id === playerUpdate.id);

            if (player) {
                // Update the player's current field index instead of pixel positions
                player.currentPosition = playerUpdate.currentPosition;

                // Move the player to the new field position
                this.movePlayerToField(player);

                // Update player money or other properties if needed
                player.money = playerUpdate.money;
                console.log("Player: ", player);
                this.updatePlayerMoney(player);

                console.log(
                    `Updated position for player: ${playerUpdate.username}`
                );
            } else {
                console.error(
                    `Player ${playerUpdate.username} not found for updating position.`
                );
            }
        }

        if (playerData.currentPlayerId !== this.currentPlayerId) {
            this.dice1.setVisible(false);
            this.dice2.setVisible(false);
        }

        if (playerData.currentPlayerId) {
            this.previousPlayerId = this.currentPlayerId;
            this.currentPlayerId = playerData.currentPlayerId;
            this.updateCurrentPlayerIndex();
            this.showCurrentPlayerButton(playerData);
        }

        if (
            playerData.landedProperty &&
            playerData.landedProperty.needToPayRent &&
            this.username === playerData.username // Only for the player who needs to pay
        ) {
            this.isNeedToPay = true;
            console.log("Showing pay rent button for: ", this.username);
            const payRentButton = this.renderPayRentButton(
                playerData.landedProperty
            );
            payRentButton.on("pointerdown", () => {
                this.payRent(playerData.landedProperty.rent);
                payRentButton.destroy();
                this.nextPlayerTurn();
            });
        } else {
            this.isNeedToPay = false;
        }
    }

    movePlayerToField(player) {
        const finalPos = this.boardPositions[player.currentPosition];

        const offset = 10 * (this.players.indexOf(player) % 4); // Adjust this as needed
        const offsetX =
            this.players.indexOf(player) % 2 === 0 ? offset : -offset;
        const offsetY =
            this.players.indexOf(player) < 2 ? -offset : -offset * 2;

        player.playerObj.x = finalPos.x + finalPos.width / 2 - 10 + offsetX;
        player.playerObj.y = finalPos.y + finalPos.height / 2 - 10 + offsetY;

        console.log(
            `Player moved to field: ${player.currentPosition}, X: ${player.playerObj.x}, Y: ${player.playerObj.y}`
        );
    }

    saveGameState() {
        const gameState = {
            players: this.players.map((player) => ({
                username: player.username,
                position: player.currentPosition,
                money: player.money,
                properties: player.properties,
                // Any other player-specific data
            })),
            currentPlayerIndex: this.currentPlayerIndex,
            currentPlayerId: this.currentPlayerId,
            auctionInProgress: this.auctionInProgress,
            auctionDetails: this.auctionInProgress
                ? {
                      property: this.auctionProperty,
                      highestBid: this.auctionHighestBid,
                      highestBidder: this.auctionHighestBidder
                          ? this.auctionHighestBidder.username
                          : null,
                      timeLeft: this.auctionTimeLeft,
                  }
                : null,
            diceValues: {
                dice1: this.dice1.frame.name,
                dice2: this.dice2.frame.name,
            },
            hasBoughtOffice: this.hasBoughtOffice,
            isNeedToPay: this.isNeedToPay,
            start: this.start,
            previousPlayerId: this.previousPlayerId,
            // Any other state variables
        };

        console.log(JSON.stringify({ state: JSON.stringify(gameState) }));

        const token = localStorage.getItem("token");
        fetch(`http://localhost:8000/api/games/${this.gameId}/state`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({ state: JSON.stringify(gameState) }),
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error("Failed to save game state");
                }
                console.log("Game state saved successfully");
            })
            .catch((error) => {
                console.error("Error saving game state:", error);
            });
    }

    // TODO FIX THE PROBLEM WITH ADDPLAYEROWNERSHIP

    restoreGameState() {
        const token = localStorage.getItem("token");
        fetch(`http://localhost:8000/api/games/${this.gameId}/state`, {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        })
            .then((response) => response.json())
            .then((savedState) => {
                if (savedState && savedState.state) {
                    const gameState = JSON.parse(savedState.state);

                    // Restore players' state
                    this.players = gameState.players.map((savedPlayer) => {
                        const player = this.players.find(
                            (p) => p.username === savedPlayer.username
                        );
                        if (player) {
                            player.currentPosition = savedPlayer.position;
                            player.money = savedPlayer.money;
                            player.properties = savedPlayer.properties;
                            this.movePlayerToField(player);
                            this.updatePlayerMoney(player);

                            // Restore properties ownership
                            savedPlayer.properties.forEach((propertyName) => {
                                const property =
                                    this.getBoardPosition(propertyName);
                                if (property) {
                                    this.addPlayerOwnership(
                                        this.players.indexOf(player),
                                        property
                                    );
                                }
                            });
                        }
                        return player;
                    });

                    // Restore game state variables
                    this.currentPlayerIndex = gameState.currentPlayerIndex;
                    this.currentPlayerId = gameState.currentPlayerId;
                    this.hasBoughtOffice = gameState.hasBoughtOffice;
                    this.isNeedToPay = gameState.isNeedToPay;
                    this.start = gameState.start;
                    this.previousPlayerId = gameState.previousPlayerId;

                    // Restore dice values
                    if (gameState.diceValues) {
                        this.dice1.setFrame(gameState.diceValues.dice1);
                        this.dice2.setFrame(gameState.diceValues.dice2);
                    }

                    // Restore auction state if needed
                    if (gameState.auctionInProgress) {
                        this.auctionInProgress = true;
                        this.auctionProperty =
                            gameState.auctionDetails.property;
                        this.auctionHighestBid =
                            gameState.auctionDetails.highestBid;
                        this.auctionHighestBidder = this.players.find(
                            (p) =>
                                p.username ===
                                gameState.auctionDetails.highestBidder
                        );
                        this.auctionTimeLeft =
                            gameState.auctionDetails.timeLeft;

                        this.showAuctionWindow(
                            this.auctionHighestBid,
                            this.auctionHighestBidder
                                ? this.auctionHighestBidder.username
                                : null
                        );
                    }

                    // Set the UI based on current player
                    this.showCurrentPlayerButton({ players: this.players });

                    console.log("Game state restored successfully");
                }
            })
            .catch((error) => {
                console.error("Error restoring game state:", error);
            });
    }

    preload() {
        this.load.spritesheet("dice", "/assets/dice.png", {
            frameWidth: 16,
            frameHeight: 16,
        });
        this.load.image("background", "/assets/backMonopoly.jpg");
        this.load.image("go", "/assets/goField.webp");
        this.load.image("jail", "/assets/jail.jpg");
        this.load.image("casino", "/assets/casino.jpg");
        this.load.image("police", "/assets/police.jpg");

        this.load.image(
            "chanel_image",
            "/assets/Chanel_logo_interlocking_cs.png"
        );
        this.load.image("question_mark_image", "/assets/question-mark.jpg");
        this.load.image("boss_image", "/assets/boss.png");
        this.load.image("money_image", "/assets/money.png");
        this.load.image("mercedes_image", "/assets/Mercedes-Logo.png");
        this.load.image("adidas_image", "/assets/Adidas_Logo.png");
        this.load.image("nike_image", "/assets/Nike-Logo.png");
        this.load.image("lacoste_image", "/assets/Lacoste-Logo.png");
        this.load.image("instagram_image", "/assets/Instagram_logo_2016.png");
        this.load.image("rockstar_image", "/assets/rocstar.jpg");
        this.load.image("x_image", "/assets/X-Logo.png");
        this.load.image("tiktok_image", "/assets/TikTok_logo.png");
        this.load.image("ferrari_image", "/assets/Ferrari-Emblem.png");
        this.load.image("coca_cola_image", "/assets/Coca-Cola_logo.png");
        this.load.image("pepsi_image", "/assets/Pepsi_2023.png");
        this.load.image("sprite_image", "/assets/Sprite-Logo-2019.png");
        this.load.image("ryanair_image", "/assets/ryanair.png");
        this.load.image(
            "british_airways_image",
            "/assets/british-airways.webp"
        );
        this.load.image(
            "qatar_airways_image",
            "/assets/Qatar_Airways_logo.png"
        );
        this.load.image("aston_martin_image", "/assets/aston_martin_logo.webp");
        this.load.image(
            "burger_king_image",
            "/assets/Burger_King_1999_logo.png"
        );
        this.load.image("mcdonalds_image", "/assets/McDonalds-Logo.png");
        this.load.image("activision_image", "/assets/Activision-logo.png");
        this.load.image("kfc_image", "/assets/KFC_logo-image.png");
        this.load.image("holidayinn_image", "/assets/holidayInn.png");
        this.load.image("radisson_blu_image", "/assets/Radisson_Blu_logo.png");
        this.load.image("novotel_image", "/assets/novotel.png");
        this.load.image("porsche_image", "/assets/Porsche_logo.png");
        this.load.image("diamond_image", "/assets/diamond.jpg");
        this.load.image("apple_image", "/assets/apple.png");
        this.load.image("nvidia_image", "/assets/NVIDIA_logo.png");
    }
    createBackground() {
        const { width, height } = this.sys.game.config;
        const bg = this.add.image(0, 0, "background").setOrigin(0, 0);
        bg.displayWidth = width;
        bg.displayHeight = height;
    }

    createBoard() {
        const screenWidth = this.scale.width;
        const screenHeight = this.scale.height;

        // Calculate cell and corner sizes dynamically based on the screen size
        const cellSize = Math.min(screenWidth, screenHeight) / 14; // Make cells longer
        const cornerSize = cellSize * 1.5;

        // Calculate offsets to center the board horizontally and vertically
        const offsetX = (screenWidth - 9 * cellSize - 2 * cornerSize) / 2;
        const offsetY = (screenHeight - 10 * cellSize - 2 * cornerSize) / 2; // Make middle smaller

        const categoryColors = {
            pink: 0xffc0cb,
            cars: 0x0000ff,
            yellow: 0xffff00,
            social: 0x00ff00,
            games: 0xff0000,
            drinks: 0x00ffff,
            hotels: 0xffa500,
            technology: 0x800080,
            utility: 0xffffff,
            aircompany: 0x00eb5794,
            fastfood: 0x00424ceb,
        };

        this.boardPositions = [
            // Top row (left to right)
            {
                name: "Go",
                x: offsetX,
                y: offsetY,
                width: cornerSize,
                height: cornerSize,
            },
            {
                name: "Chanel",
                x: offsetX + cornerSize,
                y: offsetY,
                width: cellSize,
                height: cornerSize,
                cost: 5000,
                category: "pink",
                image: "chanel_image",
                baseRent: 100,
                mortgaged: false,
            },
            {
                name: "Question Mark",
                x: offsetX + cornerSize + cellSize,
                y: offsetY,
                width: cellSize,
                height: cornerSize,
                category: "utility",
                image: "question_mark_image",
            },
            {
                name: "Boss",
                x: offsetX + cornerSize + 2 * cellSize,
                y: offsetY,
                width: cellSize,
                height: cornerSize,
                cost: 5000,
                category: "pink",
                image: "boss_image",
                baseRent: 110,
                mortgaged: false,
            },
            {
                name: "Money",
                x: offsetX + cornerSize + 3 * cellSize,
                y: offsetY,
                width: cellSize,
                height: cornerSize,
                category: "utility",
                image: "money_image",
            },
            {
                name: "Mercedes",
                x: offsetX + cornerSize + 4 * cellSize,
                y: offsetY,
                width: cellSize,
                height: cornerSize,
                cost: 3000,
                category: "cars",
                image: "mercedes_image",
                baseRent: 120,
                mortgaged: false,
            },
            {
                name: "Adidas",
                x: offsetX + cornerSize + 5 * cellSize,
                y: offsetY,
                width: cellSize,
                height: cornerSize,
                cost: 5000,
                category: "yellow",
                image: "adidas_image",
                baseRent: 130,
                mortgaged: false,
            },
            {
                name: "Question Mark",
                x: offsetX + cornerSize + 6 * cellSize,
                y: offsetY,
                width: cellSize,
                height: cornerSize,
                category: "utility",
                image: "question_mark_image",
            },
            {
                name: "Nike",
                x: offsetX + cornerSize + 7 * cellSize,
                y: offsetY,
                width: cellSize,
                height: cornerSize,
                cost: 4000,
                category: "yellow",
                image: "nike_image",
                baseRent: 140,
                mortgaged: false,
            },
            {
                name: "Lacoste",
                x: offsetX + cornerSize + 8 * cellSize,
                y: offsetY,
                width: cellSize,
                height: cornerSize,
                cost: 3000,
                category: "yellow",
                image: "lacoste_image",
                baseRent: 150,
                mortgaged: false,
            },
            {
                name: "Go to jail",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY,
                width: cornerSize,
                height: cornerSize,
            },

            // Right column (top to bottom)
            {
                name: "Instagram",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: Math.PI / 2,
                cost: 3000,
                category: "social",
                image: "instagram_image",
                baseRent: 160,
                mortgaged: false,
            },
            {
                name: "Rockstar",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY + cornerSize + cellSize,
                width: cornerSize,
                height: cellSize,
                rotation: Math.PI / 2,
                cost: 3000,
                category: "games",
                image: "rockstar_image",
                baseRent: 170,
                mortgaged: false,
            },
            {
                name: "X",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY + cornerSize + 2 * cellSize,
                width: cornerSize,
                height: cellSize,
                rotation: Math.PI / 2,
                cost: 3000,
                category: "social",
                image: "x_image",
                baseRent: 180,
                mortgaged: false,
            },
            {
                name: "Tik Tok",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY + cornerSize + 3 * cellSize,
                width: cornerSize,
                height: cellSize,
                rotation: Math.PI / 2,
                cost: 3000,
                category: "social",
                image: "tiktok_image",
                baseRent: 190,
                mortgaged: false,
            },
            {
                name: "Ferrari",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY + 4 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: Math.PI / 2,
                cost: 2000,
                category: "cars",
                image: "ferrari_image",
                baseRent: 200,
                mortgaged: false,
            },
            {
                name: "Coca Cola",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY + 5 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: Math.PI / 2,
                cost: 3000,
                category: "drinks",
                image: "coca_cola_image",
                baseRent: 210,
                mortgaged: false,
            },
            {
                name: "Question Mark",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY + 6 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: Math.PI / 2,
                category: "utility",
                image: "question_mark_image",
            },
            {
                name: "Pepsi",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY + 7 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: Math.PI / 2,
                cost: 3000,
                category: "drinks",
                image: "pepsi_image",
                baseRent: 220,
                mortgaged: false,
            },
            {
                name: "Sprite",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY + 8 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: Math.PI / 2,
                cost: 3000,
                category: "drinks",
                image: "sprite_image",
                baseRent: 230,
                mortgaged: false,
            },
            {
                name: "Casino",
                x: offsetX + cornerSize + 9 * cellSize,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cornerSize,
                height: cornerSize,
            },

            // Bottom row (right to left)
            {
                name: "Ryanair",
                x: offsetX + cornerSize + 8 * cellSize,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cellSize,
                height: cornerSize,
                cost: 3400,
                category: "aircompany",
                image: "ryanair_image",
                baseRent: 240,
                mortgaged: false,
            },
            {
                name: "Question Mark",
                x: offsetX + cornerSize + 7 * cellSize,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cellSize,
                height: cornerSize,
                category: "utility",
                image: "question_mark_image",
            },
            {
                name: "British Airways",
                x: offsetX + cornerSize + 6 * cellSize,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cellSize,
                height: cornerSize,
                cost: 3050,
                category: "aircompany",
                image: "british_airways_image",
                baseRent: 250,
                mortgaged: false,
            },
            {
                name: "Qatar Airways",
                x: offsetX + cornerSize + 5 * cellSize,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cellSize,
                height: cornerSize,
                cost: 3500,
                category: "aircompany",
                image: "qatar_airways_image",
                baseRent: 260,
                mortgaged: false,
            },
            {
                name: "Aston Martin",
                x: offsetX + cornerSize + 4 * cellSize,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cellSize,
                height: cornerSize,
                cost: 3000,
                category: "cars",
                image: "aston_martin_image",
                baseRent: 270,
                mortgaged: false,
            },
            {
                name: "Burger King",
                x: offsetX + cornerSize + 3 * cellSize,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cellSize,
                height: cornerSize,
                cost: 3000,
                category: "fastfood",
                image: "burger_king_image",
                baseRent: 280,
                mortgaged: false,
            },
            {
                name: "McDonalds",
                x: offsetX + cornerSize + 2 * cellSize,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cellSize,
                height: cornerSize,
                cost: 3000,
                category: "fastfood",
                image: "mcdonalds_image",
                baseRent: 290,
                mortgaged: false,
            },
            {
                name: "Activision",
                x: offsetX + cornerSize + 1 * cellSize,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cellSize,
                height: cornerSize,
                cost: 3000,
                category: "games",
                image: "activision_image",
                baseRent: 300,
                mortgaged: false,
            },
            {
                name: "KFC",
                x: offsetX + cornerSize,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cellSize,
                height: cornerSize,
                cost: 3000,
                category: "fastfood",
                image: "kfc_image",
                baseRent: 310,
                mortgaged: false,
            },
            {
                name: "Prison",
                x: offsetX,
                y: offsetY + 9 * cellSize + cornerSize,
                width: cornerSize,
                height: cornerSize,
            },

            // Left column (bottom to top)
            {
                name: "Holiday Inn",
                x: offsetX,
                y: offsetY + 8 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: -Math.PI / 2,
                cost: 4000,
                category: "hotels",
                image: "holidayinn_image",
                baseRent: 320,
                mortgaged: false,
            },
            {
                name: "Radisson Blu",
                x: offsetX,
                y: offsetY + 7 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: -Math.PI / 2,
                cost: 4000,
                category: "hotels",
                image: "radisson_blu_image",
                baseRent: 330,
                mortgaged: false,
            },
            {
                name: "Question Mark",
                x: offsetX,
                y: offsetY + 6 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: -Math.PI / 2,
                category: "utility",
                image: "question_mark_image",
            },
            {
                name: "Novotel",
                x: offsetX,
                y: offsetY + 5 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: -Math.PI / 2,
                cost: 4000,
                category: "hotels",
                image: "novotel_image",
                baseRent: 340,
                mortgaged: false,
            },
            {
                name: "Porsche",
                x: offsetX,
                y: offsetY + 4 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: -Math.PI / 2,
                category: "cars",
                cost: 3000,
                image: "porsche_image",
                baseRent: 350,
                mortgaged: false,
            },
            {
                name: "Diamond",
                x: offsetX,
                y: offsetY + 3 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: -Math.PI / 2,
                category: "utility",
                image: "diamond_image",
            },
            {
                name: "Apple",
                x: offsetX,
                y: offsetY + 2 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: -Math.PI / 2,
                category: "technology",
                cost: 5000,
                image: "apple_image",
                baseRent: 360,
                mortgaged: false,
            },
            {
                name: "Question Mark",
                x: offsetX,
                y: offsetY + 1 * cellSize + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: -Math.PI / 2,
                category: "utility",
                image: "question_mark_image",
            },
            {
                name: "Nvidia",
                x: offsetX,
                y: offsetY + cornerSize,
                width: cornerSize,
                height: cellSize,
                rotation: -Math.PI / 2,
                cost: 5500,
                category: "technology",
                image: "nvidia_image",
                baseRent: 370,
                mortgaged: false,
            },
        ];

        // Render the board positions
        this.boardPositions.forEach((pos) => {
            const graphics = this.add.graphics({
                fillStyle: { color: 0xffffff },
            });
            graphics.fillRect(pos.x, pos.y, pos.width, pos.height);
            graphics.lineStyle(2, 0x000000);
            graphics.strokeRect(pos.x, pos.y, pos.width, pos.height);

            graphics.setInteractive(
                new Phaser.Geom.Rectangle(pos.x, pos.y, pos.width, pos.height),
                Phaser.Geom.Rectangle.Contains
            );

            graphics.on("pointerdown", () => {
                this.handlePropertyClick(pos); // Call handlePropertyClick when the property is clicked
            });
            pos.office = 0;
            pos.officeGraphics = [];

            if (pos.cost) {
                const color = categoryColors[pos.category] || 0x000000; // Default to black if category color is not defined
                const textBackground = this.add.graphics();
                textBackground.fillStyle(color, 1.0);

                // Adjust the position for each side of the board
                if (pos.rotation === Math.PI / 2) {
                    // Right column
                    textBackground.fillRect(
                        pos.x + pos.width,
                        pos.y,
                        20,
                        pos.height
                    );
                    pos.priceText = this.add
                        .text(
                            pos.x + pos.width + 10,
                            pos.y + pos.height / 2,
                            `$${pos.cost}`,
                            {
                                fontSize: "14px",
                                color: "#000",
                                align: "center",
                            }
                        )
                        .setOrigin(0.5)
                        .setAngle(90);
                } else if (pos.rotation === -Math.PI / 2) {
                    // Left column
                    textBackground.fillRect(pos.x - 20, pos.y, 20, pos.height);
                    pos.priceText = this.add
                        .text(
                            pos.x - 10,
                            pos.y + pos.height / 2,
                            `$${pos.cost}`,
                            {
                                fontSize: "14px",
                                color: "#000",
                                align: "center",
                            }
                        )
                        .setOrigin(0.5)
                        .setAngle(-90);
                } else if (pos.y === this.boardPositions[0].y) {
                    // Top row
                    textBackground.fillRect(pos.x, pos.y - 25, pos.width, 20);
                    pos.priceText = this.add
                        .text(
                            pos.x + pos.width / 2,
                            pos.y - 15,
                            `$${pos.cost}`,
                            {
                                fontSize: "14px",
                                color: "#000",
                                align: "center",
                            }
                        )
                        .setOrigin(0.5);
                } else {
                    // Bottom row
                    textBackground.fillRect(pos.x, pos.y - 20, pos.width, 20);
                    pos.priceText = this.add
                        .text(
                            pos.x + pos.width / 2,
                            pos.y - 12,
                            `$${pos.cost}`,
                            {
                                fontSize: "14px",
                                color: "#000",
                                align: "center",
                            }
                        )
                        .setOrigin(0.5);
                }
            }

            if (pos.image) {
                const fieldImage = this.add.image(
                    pos.x + pos.width / 2,
                    pos.y + pos.height / 2,
                    pos.image
                );
                fieldImage.setDisplaySize(pos.width - 10, pos.height - 10);
                fieldImage.setDepth(0);
            }

            if (pos.name === "Go") {
                const goImage = this.add.image(
                    pos.x + pos.width / 2,
                    pos.y + pos.height / 2,
                    "go"
                );
                goImage.setDisplaySize(pos.width, pos.height);
                goImage.setDepth(0);
            }

            if (pos.name === "Go to jail") {
                const goImage = this.add.image(
                    pos.x + pos.width / 2,
                    pos.y + pos.height / 2,
                    "jail"
                );
                goImage.setDisplaySize(pos.width, pos.height);
                goImage.setDepth(0);
            }

            if (pos.name === "Casino") {
                const goImage = this.add.image(
                    pos.x + pos.width / 2,
                    pos.y + pos.height / 2,
                    "casino"
                );
                goImage.setDisplaySize(pos.width, pos.height);
                goImage.setDepth(0);
            }

            if (pos.name === "Prison") {
                const goImage = this.add.image(
                    pos.x + pos.width / 2,
                    pos.y + pos.height / 2,
                    "police"
                );
                goImage.setDisplaySize(pos.width, pos.height);
                goImage.setDepth(0);
            }
        });
    }

    startAuction(property) {
        if (this.auctionInProgress) {
            console.warn("Auction already in progress.");
            return;
        }

        this.auctionInProgress = true;
        this.auctionProperty = property;
        this.auctionHighestBid = property.cost / 2; // Starting bid is half the property cost
        this.auctionHighestBidder = null;

        // Notify the server to start the auction
        this.stompClient.send(
            "/app/player/startAuction",
            {},
            JSON.stringify({
                gameId: this.gameId,
                propertyName: property.name,
                initialBid: this.auctionHighestBid,
                username: this.username,
            })
        );

        // Once the auction is started, notify the next player to place a bid
        this.notifyNextPlayer();
    }

    notifyNextPlayer() {
        // Move to the next player
        this.currentPlayerIndex =
            (this.currentPlayerIndex + 1) % this.players.length;
        const nextPlayer = this.players[this.currentPlayerIndex];

        // Check if we've looped through all players and the next player is the highest bidder
        if (nextPlayer.username === this.auctionHighestBidder.username) {
            this.endAuction();
            return;
        }

        // If the next player is the current player, show the auction window
        if (nextPlayer.username === this.username) {
            this.showAuctionWindow(
                this.auctionHighestBid,
                this.auctionHighestBidder.username
            );
        } else {
            this.hideAuctionWindow();
        }
    }

    updateAuctionTimer() {
        this.auctionTimeLeft -= 1;

        this.stompClient.send(
            "/app/player/auctionTimeUpdate",
            {},
            JSON.stringify({
                gameId: this.gameId,
                timeLeft: this.auctionTimeLeft,
            })
        );

        if (this.auctionTimeLeft <= 0) {
            this.endAuction();
        }
    }

    placeBid(player, bidAmount) {
        if (!player || typeof bidAmount !== "number" || bidAmount <= 0) {
            console.warn("Invalid bid or player.");
            return;
        }
        if (this.auctionInProgress && bidAmount > this.auctionHighestBid) {
            this.auctionHighestBid = bidAmount;
            this.auctionHighestBidder = player;

            this.auctionTimeLeft = 10;

            this.stompClient.send(
                "/app/player/placeBid",
                {},
                JSON.stringify({
                    gameId: this.gameId,
                    highestBid: this.auctionHighestBid,
                    highestBidder: player.username,
                })
            );

            this.hideAuctionWindow();
        }
    }

    endAuction() {
        if (this.auctionHighestBidder) {
            // Deduct money from the highest bidder and assign the property
            this.auctionHighestBidder.money -= this.auctionHighestBid;
            this.updatePlayerMoney(this.auctionHighestBidder);

            // Notify the server about the property purchase
            //TODO I should something do with price, because i don't have this in backend
            this.stompClient.send(
                "/app/player/buyProperty",
                {},
                JSON.stringify({
                    gameId: this.gameId,
                    gameName: this.gameName,
                    username: this.auctionHighestBidder.username,
                    propertyName: this.auctionProperty.name,
                    price: this.auctionHighestBid,
                })
            );

            this.addPlayerOwnership(
                this.players.indexOf(this.auctionHighestBidder),
                this.auctionProperty
            );
        } else {
            alert("No bids were placed. The auction ends without a winner.");
        }

        // Notify the server that the auction has ended
        this.stompClient.send(
            "/app/game/endAuction",
            {},
            JSON.stringify({
                gameId: this.gameId,
                property: this.auctionProperty.name,
                highestBid: this.auctionHighestBid,
                highestBidder: this.auctionHighestBidder
                    ? this.auctionHighestBidder.username
                    : null,
            })
        );

        // Reset auction state
        this.auctionInProgress = false;
        this.auctionProperty = null;
        this.auctionHighestBid = 0;
        this.auctionHighestBidder = null;

        // Proceed to the next player's turn in the game
        this.nextPlayerTurn();
    }

    createPlayers(numberOfPlayers) {
        const playerSize = 20;
        const goPosition = this.getBoardPosition("Go");
        const playerColors = [0x000000, 0xff0000, 0x00ff00, 0x0000ff]; // Different colors for each player

        if (goPosition) {
            this.players.forEach((player, i) => {
                const offset = 10 * (i % 4); // Adjust this as needed
                const offsetX = i % 2 === 0 ? offset : -offset;
                const offsetY = i < 2 ? -offset : -offset * 2;

                const playerObj = this.add.rectangle(
                    goPosition.x + goPosition.width / 2 + offsetX,
                    goPosition.y + goPosition.height / 2 + offsetY,
                    playerSize,
                    playerSize,
                    playerColors[i % playerColors.length]
                );
                playerObj.setData("username", player.username);
                playerObj.setData("money", player.money);
                playerObj.setData(
                    "color",
                    playerColors[i % playerColors.length]
                );
                player.playerObj = playerObj;
                player.currentPosition = 0;
                player.properties = [];
                console.log("PLAYER X: " + player.x);
                console.log("Player Y: " + player.y);

                this.createPlayerCard(
                    player,
                    playerColors[i % playerColors.length],
                    i
                );

                this.savePlayerPosition(player);
            });
        }
    }

    createPlayerCard(player, color, index) {
        const playerCard = this.add.graphics();
        playerCard.fillStyle(color, 1.0);
        playerCard.fillRect(20, 20 + index * 100, 200, 80);

        const usernameText = this.add
            .text(90, 25 + index * 100, player.username, {
                fontSize: "18px",
                fill: "#ffffff",
                fontStyle: "bold",
            })
            .setOrigin(0, 0)
            .setName(`playerCard-${player.username}-username`);

        const moneyText = this.add
            .text(90, 55 + index * 100, "$" + player.money.toLocaleString(), {
                fontSize: "16px",
                fill: "#ffffff",
            })
            .setOrigin(0, 0)
            .setName(`playerCard-${player.username}-money`);

        const border = this.add.graphics();
        border.lineStyle(4, color, 1.0);
        border.strokeRect(20, 20 + index * 100, 200, 80);

        const playerCardContainer = this.add.container(0, 0);
        playerCardContainer.add([playerCard, usernameText, moneyText, border]);

        playerCardContainer.setInteractive(
            new Phaser.Geom.Rectangle(0, 0, 200, 80),
            Phaser.Geom.Rectangle.Contains
        );
        playerCardContainer.on("pointerdown", () => {
            this.handlePlayerCardClick(player.username);
        });

        this.playerCards.push({
            username: player.username,
            cardContainer: playerCardContainer,
            moneyText: moneyText,
        });
    }

    handlePlayerCardClick(clickedUsername) {
        const currentPlayer = this.players.find(
            (player) => player.username === this.username
        );

        const menu = this.add.graphics();
        menu.fillStyle(0x000000, 0.8);
        menu.fillRect(200, 150, 400, 200);

        const title = this.add
            .text(400, 180, `Option for ${clickedUsername}`, {
                fontSize: "20px",
                color: "#ffffff",
                align: "center",
            })
            .setOrigin(0.5);

        if (clickedUsername === currentPlayer.username) {
            this.createProfileButton(menu, 400, 240);
            this.createSurrenderButton(menu, 400, 300);
        } else {
            this.createProfileButton(menu, 400, 240);
            this.createContractButton(menu, 400, 300, clickedUsername);
        }

        const closeButton = this.add
            .text(400, 360, "Close", {
                fontSize: "20px",
                color: "#ffffff",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                menu.clear();
                title.destroy();
                closeButton.destroy();
            });
    }

    createProfileButton(menu, x, y) {
        const profileButton = this.add
            .text(x, y, "Go to Profile", {
                fontSize: "20px",
                color: "#00ff00",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                console.log("Go to Profile clicked");
                // Add logic to go to the profile
            });
        menu.add(profileButton);
    }

    createSurrenderButton(menu, x, y) {
        const surrenderButton = this.add
            .text(x, y, "Surrender", {
                fontSize: "20px",
                color: "#ff0000",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                console.log("Surrender clicked");
                this.stompClient.send(
                    "/app/player/surrender",
                    {},
                    JSON.stringify({
                        gameId: this.gameId,
                        username: clickedUsername,
                    })
                );
            });
        menu.add(surrenderButton);
    }

    createContractButton(menu, x, y, clickedUsername) {
        const contractButton = this.add
            .text(x, y, "Sign the Contract", {
                fontSize: "20px",
                color: "#00ff00",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                console.log("Sign the Contract clicked");
                this.showContractWindow(clickedUsername);
            });
        menu.add(contractButton);
    }

    showContractWindow(clickedUsername) {
        const menu = this.add.graphics();
        menu.fillStyle(0x000000, 0.8);
        menu.fillRect(100, 100, 600, 400);

        const title = this.add
            .text(400, 130, `Propose Contract to ${clickedUsername}`, {
                fontSize: "20px",
                color: "#ffffff",
                align: "center",
            })
            .setOrigin(0.5);

        const offerText = this.add
            .text(150, 180, "Offer:", { fontSize: "20px", color: "#ffffff" })
            .setOrigin(0.5);

        const requestText = this.add
            .text(450, 180, "Request:", { fontSize: "20px", color: "#ffffff" })
            .setOrigin(0.5);

        const offerMoneyInput = this.add.dom(150, 220, "input", {
            type: "number",
            placeholder: "Money",
            style: "width: 100px; height: 30px; font-size: 20px;",
        });

        const requestMoneyInput = this.add.dom(450, 220, "input", {
            type: "number",
            placeholder: "Money",
            style: "width: 100px; height: 30px; font-size: 20px;",
        });

        const offerPropertyInput = this.add.dom(150, 270, "input", {
            type: "text",
            placeholder: "Property",
            style: "width: 100px; height: 30px; font-size: 20px;",
        });

        const requestPropertyInput = this.add.dom(450, 270, "input", {
            type: "text",
            placeholder: "Property",
            style: "width: 100px; height: 30px; font-size: 20px;",
        });

        const doneButton = this.add
            .text(400, 350, "Done", {
                fontSize: "20px",
                color: "#00ff00",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                const offerMoney = parseInt(offerMoneyInput.node.value);
                const requestMoney = parseInt(requestMoneyInput.node.value);
                const offerProperty = offerPropertyInput.node.value;
                const requestProperty = requestPropertyInput.node.value;

                const contract = {
                    fromUsername: this.username,
                    toUsername: clickedUsername,
                    offer: { money: offerMoney, property: offerProperty },
                    request: { money: requestMoney, property: requestProperty },
                };

                this.stompClient.send(
                    "/app/player/proposeContract",
                    {},
                    JSON.stringify({ gameId: this.gameId, contract })
                );

                menu.clear();
                title.destroy();
                offerText.destroy();
                requestText.destroy();
                offerMoneyInput.destroy();
                requestMoneyInput.destroy();
                offerPropertyInput.destroy();
                requestPropertyInput.destroy();
                doneButton.destroy();
            });
        menu.add(doneButton);
    }

    createDice() {
        this.dice1 = this.add
            .sprite(400, 300, "dice")
            .setScale(4)
            .setVisible(false);
        this.dice2 = this.add
            .sprite(500, 300, "dice")
            .setScale(4)
            .setVisible(false);
        this.anims.create({
            key: "roll",
            frames: this.anims.generateFrameNumbers("dice", {
                start: 0,
                end: 5,
            }),
            frameRate: 10,
            repeat: -1,
        });
    }

    createButton() {
        const screenWidth = this.scale.width;
        const screenHeight = this.scale.height;

        this.rollDiceButton = this.add
            .text(screenWidth / 2, screenHeight * 0.15, "Roll Dice", {
                fontSize: "20px",
                fill: "#0f0",
                backgroundColor: "#000", // Optionally add a background color
                padding: { left: 20, right: 20, top: 10, bottom: 10 }, // Adjust padding as needed
            })
            .setInteractive()
            .on("pointerdown", () => {
                if (this.isCurrentPlayer() && !this.isObserver) {
                    this.rollDice();
                }
            })
            .setVisible(false) // Initially hidden
            .setOrigin(0.5, 0); // Center horizontally and align top vertically

        this.showCurrentPlayerButton({ players: this.players });
    }

    showCurrentPlayerButton(playerData) {
        this.rollDiceButton.setVisible(false).setInteractive(false);
        console.log("Before if");
        if (this.stompClient && this.stompClient.connected) {
            const currentPlayer = this.players.find(
                (player) => player.username === this.username
            );
            console.log(
                "I here when show currnetPlayerButton: " + currentPlayer
            );
            if (
                currentPlayer &&
                currentPlayer.id === this.currentPlayerId &&
                !this.isObserver
            ) {
                console.log("I am here where set visible true");
                this.rollDiceButton.setVisible(true);
                this.bidButton.setVisible(this.auctionInProgress);
            } else {
                console.log("i am here where set visible false");
                this.rollDiceButton.setVisible(false);
                this.bidButton.setVisible(false);
            }

            playerData.players.forEach((playerD) => {
                this.updatePlayerData(playerD);
            });
        } else {
            console.log("WebSocket is not connected");
        }
    }

    rollDice(isJailRoll = false) {
        // this.rollDiceButton.setVisible(false);
        this.rollDiceButton.disabled = true;
        this.dice1.setVisible(true);
        this.dice2.setVisible(true);

        // Play the roll animation
        this.dice1.play("roll");
        this.dice2.play("roll");

        this.time.delayedCall(1000, () => {
            this.dice1.stop();
            this.dice2.stop();
            const dice1Value = Phaser.Math.Between(0, 5);
            const dice2Value = Phaser.Math.Between(0, 5);
            this.dice1.setFrame(dice1Value);
            this.dice2.setFrame(dice2Value);

            const totalSteps = dice1Value + dice2Value + 2; // Adding 2 because frames are 0-based
            if (isJailRoll) {
                if (dice1Value === dice2Value) {
                    alert("You rolled doubles! You're out of jail!");
                    const player = this.players[this.currentPlayerIndex];
                    player.isInJail = false;

                    this.movePlayer(this.currentPlayerIndex, totalSteps);
                } else {
                    alert("You didn't roll doubles, you're still in jail.");
                    this.nextPlayerTurn(); // Stay in jail and move to the next player
                }
            } else {
                this.movePlayer(this.currentPlayerIndex, totalSteps); // Regular move
            }
        });
    }

    // isCurrentPlayer() {
    //   console.log("Checking if current player");
    //   const currentPlayer = this.players.find(
    //     (player) => player.id === this.currentPlayerId
    //   );
    //   console.log(currentPlayer);
    //   return currentPlayer && currentPlayer.username === this.username;
    // }

    isCurrentPlayer() {
        return (
            this.players[this.currentPlayerIndex] &&
            this.players[this.currentPlayerIndex].username === this.username
        );
    }

    createBidButton() {
        this.bidButton = this.add
            .text(400, 450, "Place Bid", { fontSize: "20px", fill: "#0f0" })
            .setInteractive()
            .on("pointerdown", () => {
                if (this.auctionInProgress) {
                    const bidAmount = prompt(
                        `Enter your bid (Current highest bid: $${this.auctionHighestBid})`
                    );
                    if (
                        bidAmount &&
                        parseInt(bidAmount) > this.auctionHighestBid
                    ) {
                        this.stompClient.send(
                            "/app/game/placeBid",
                            {},
                            JSON.stringify({
                                gameId: this.gameId,
                                username: this.username,
                                bid: parseInt(bidAmount),
                            })
                        );
                    }
                }
            })
            .setVisible(false);
    }

    movePlayer(playerIndex, steps) {
        if (this.isObserver) return;

        const playerData = this.players[playerIndex];
        let currentPosition = playerData.currentPosition;
        let payRentButton;

        this.tweens.addCounter({
            from: currentPosition,
            to: currentPosition + steps,
            duration: steps * 300,
            onUpdate: (tween) => {
                const value = tween.getValue();
                const newPosition =
                    Math.floor(value) % this.boardPositions.length;
                this.movePlayerToField({
                    ...playerData,
                    currentPosition: newPosition,
                });

                const name = this.boardPositions[newPosition].name;

                this.broadcastPlayerPosition(
                    playerData,
                    newPosition,
                    false,
                    name
                );
            },
            onComplete: () => {
                playerData.currentPosition =
                    (currentPosition + steps) % this.boardPositions.length;
                const finalPos =
                    this.boardPositions[playerData.currentPosition];

                this.savePlayerPosition(playerData);
                this.saveGameState(); // Save the game state after buying a property

                if (finalPos.name === "Prison") {
                    this.moveToGoToJail(playerData);
                } else if (finalPos.name === "Casino") {
                    this.showCasinoMenu(playerIndex);
                } else if (finalPos.name === "Go to jail") {
                    this.nextPlayerTurn();
                } else if (finalPos.name === "Money") {
                    // Handle landing on the "Money" field
                    this.promptPayMoney(playerData, finalPos, 2000);
                } else if (finalPos.name === "Diamon") {
                    this.promptPayMoney(playerData, finalPos, 1000);
                } else if (finalPos.name === "Question Mark") {
                    this.triggerQuestionMarkEvent(playerData);
                } else {
                    // Fetch owner data from server
                    // TODO HERE PROBLEM IS RESPONSE IS NOT JSON
                    fetch(
                        `http://localhost:8000/api/games/property/${this.gameId}/${finalPos.name}/owner`
                    )
                        .then((response) => {
                            if (response.ok) {
                                const contentType =
                                    response.headers.get("content-type");
                                if (
                                    contentType &&
                                    contentType.includes("application/json")
                                ) {
                                    return response.json();
                                } else {
                                    throw new Error("Response is not JSON");
                                }
                            } else {
                                throw new Error(
                                    `HTTP error! status: ${response.status}`
                                );
                            }
                        })
                        .then((data) => {
                            //TODO DESTROY BUTTON PAY
                            const ownerData = data.owner;
                            if (
                                ownerData &&
                                ownerData.username !== playerData.username
                            ) {
                                // Property is owned by another player
                                const rent = this.determineRent(finalPos);
                                payRentButton = this.renderPayRentButton({
                                    ...finalPos,
                                    needToPayRent: true,
                                    rent: rent,
                                });

                                payRentButton.on("pointerdown", () => {
                                    this.payRent(rent)
                                        .then(() => {
                                            payRentButton.destroy(); // Destroy the button after successful rent payment
                                            this.nextPlayerTurn(); // Proceed to the next player's turn
                                        })
                                        .catch((error) => {
                                            console.error(
                                                "Error paying rent: ",
                                                error
                                            ); // Handle any errors that occur
                                        });
                                });
                            } else if (
                                ownerData &&
                                ownerData.username === playerData.username
                            ) {
                                if (payRentButton) {
                                    payRentButton.destroy();
                                }
                                this.nextPlayerTurn();
                            } else {
                                if (payRentButton) {
                                    payRentButton.destroy();
                                }
                                // Property is not owned or owned by the current player
                                this.showPropertyMenu(playerIndex, finalPos);
                            }
                        })
                        .catch((error) => {
                            console.error(
                                "Error fetching property owner:",
                                error
                            );
                            // Handle the error case, maybe notify the user or log the issue.
                        });
                }
            },
        });
    }

    triggerQuestionMarkEvent(player) {
        // Randomly pick an event
        const randomIndex = Phaser.Math.Between(
            0,
            this.questionMarkEvents.length - 1
        );
        const event = this.questionMarkEvents[randomIndex];

        // Show a message to the player about the event
        alert(event.description);

        // Handle different types of events
        switch (event.type) {
            case "move":
                this.movePlayer(this.players.indexOf(player), event.steps);
                break;
            case "moveToField":
                this.movePlayerToFieldByName(player, event.fieldName);
                break;
            case "pay":
                this.promptPayMoney(player, null, event.amount);
                break;
            case "receive":
                player.money += event.amount;
                this.updatePlayerMoney(player);
                this.nextPlayerTurn();
                break;
            default:
                console.warn("Unknown event type:", event.type);
                this.nextPlayerTurn();
        }
    }

    movePlayerToFieldByName(player, fieldName) {
        const targetField = this.getBoardPosition(fieldName);
        if (targetField) {
            player.currentPosition = this.boardPositions.indexOf(targetField);
            this.movePlayerToField(player);
            this.savePlayerPosition(player);
            this.nextPlayerTurn();
        } else {
            console.error("Field not found:", fieldName);
            this.nextPlayerTurn();
        }
    }

    promptPayMoney(player, finalPos, amount) {
        this.isNeedToPay = true; // Set the flag to indicate the player needs to pay

        const payMoneyButton = this.add
            .text(400, 500, `Pay $${amount}`, {
                fontSize: "20px",
                fill: "#0f0",
                backgroundColor: "#000",
                padding: 10,
            })
            .setInteractive()
            .on("pointerdown", () => {
                if (player.money >= amount) {
                    player.money -= amount;
                    this.updatePlayerMoney(player);

                    // Notify the server that the player has paid
                    this.stompClient.send(
                        "/app/player/payMoney",
                        {},
                        JSON.stringify({
                            gameId: this.gameId,
                            username: player.username,
                            amount: amount,
                        })
                    );

                    payMoneyButton.destroy(); // Remove the button after payment
                    this.isNeedToPay = false; // Reset the flag
                    this.nextPlayerTurn(); // Proceed to the next player's turn
                } else {
                    alert("You don't have enough money to pay!");
                }
            });
    }

    //TODO Check if it works
    broadcastPlayerPosition(playerData, newPosition, isFinal, name) {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.send(
                "/app/player/move", // The same endpoint used for final positions
                {},
                JSON.stringify({
                    gameId: this.gameId,
                    gameName: this.gameName,
                    username: playerData.username,
                    newPosition: newPosition,
                    x: playerData.playerObj.x,
                    y: playerData.playerObj.y,
                    finalPos: isFinal,
                    start: this.start,
                })
            );
        }
        if (name !== "Go") this.start = true;
    }

    renderPayRentButton(landedProperty) {
        if (landedProperty && landedProperty.needToPayRent) {
            return this.add
                .text(400, 500, `Pay $${landedProperty.rent}`, {
                    fontSize: "20px",
                    fill: "#0f0",
                    backgroundColor: "#000",
                    padding: 10,
                })
                .setInteractive()
                .on("pointerdown", () => {
                    this.payRent(landedProperty.rent);
                });
        }
        return null;
    }

    payRent(rent) {
        return new Promise((resolve, reject) => {
            if (this.stompClient && this.stompClient.connected) {
                try {
                    this.stompClient.send(
                        "/app/player/payRent",
                        {},
                        JSON.stringify({
                            gameId: this.gameId,
                            username: this.username,
                            rent: rent,
                        })
                    );
                    resolve(); // Resolve the promise after sending the message
                } catch (error) {
                    reject(error); // Reject the promise if an error occurs
                }
            } else {
                reject(new Error("STOMP client is not connected")); // Reject if not connected
            }
        });
    }

    handleUtilityAction(playerIndex, finalPos) {
        // Handle utility-specific actions here
        console.log(
            `Player ${this.players[playerIndex].username} landed on a utility: ${finalPos.name}`
        );
        this.nextPlayerTurn(); // End the turn after the utility action
    }

    moveToGoToJail(player) {
        const goToJailPosition = this.getBoardPosition("Go to jail");

        player.currentPosition = this.boardPositions.indexOf(goToJailPosition);
        this.movePlayerToField(player);

        player.isInJail = true; // Mark the player as being in jail

        // Send the updated position to the backend
        this.savePlayerPosition(player);

        this.nextPlayerTurn();
    }

    getBoardPosition(name) {
        return this.boardPositions.find((pos) => pos.name === name);
    }

    showPropertyMenu(playerIndex, property) {
        if (this.isObserver) return;

        if (this.isNeedToPay) return;
        console.log("I show property Menu");

        const currentPlayer = this.players[playerIndex];
        const owner = this.players.find(
            (player) =>
                player.properties && player.properties.includes(property.name)
        );

        if (
            property.name === "Go" ||
            property.name === "Go to jail" ||
            property.name === "Casino" ||
            property.name === "Prison" ||
            property.name === "Question Mark"
        ) {
            console.log(
                "Here when property.name === Go, GO to jail, Casino, Prison"
            );

            this.nextPlayerTurn();
            return;
        }

        if (owner && owner.username !== currentPlayer.username) {
            // const rent = this.determineRent(property.name);
            // currentPlayer.money -= rent;
            // owner.money += rent;
            // this.updatePlayerMoney(currentPlayer);
            // this.updatePlayerMoney(owner);
            // this.savePlayerPosition(currentPlayer);
            // this.savePlayerPosition(owner);
            // this.nextPlayerTurn();
            // return;
        } else {
            const menu = this.add.graphics();
            menu.fillStyle(0x000000, 0.8);
            menu.fillRect(200, 150, 400, 200);

            const text = this.add
                .text(400, 200, property.name, {
                    fontSize: "20px",
                    color: "#ffffff",
                    align: "center",
                })
                .setOrigin(0.5);

            const buyButton = this.add
                .text(300, 300, "Buy", {
                    fontSize: "20px",
                    color: "#00ff00",
                    align: "center",
                })
                .setInteractive()
                .on("pointerdown", () => {
                    console.log("Buy clicked");
                    this.addPlayerOwnership(playerIndex, property);

                    this.stompClient.send(
                        "/app/player/buyProperty",
                        {},
                        JSON.stringify({
                            gameId: this.gameId,
                            gameName: this.gameName,
                            username: this.players[playerIndex].username,
                            propertyName: property.name,
                        })
                    );

                    menu.clear();
                    text.destroy();
                    buyButton.destroy();
                    auctionButton.destroy();

                    this.saveGameState(); // Save the game state after buying a property

                    this.nextPlayerTurn();
                });

            const auctionButton = this.add
                .text(500, 300, "Auction", {
                    fontSize: "20px",
                    color: "#ff0000",
                    align: "center",
                })
                .setInteractive()
                .on("pointerdown", () => {
                    console.log("Auction clicked");
                    menu.clear();
                    text.destroy();
                    buyButton.destroy();
                    auctionButton.destroy();

                    this.startAuction(property);
                });
        }
    }

    handlePropertyClick(property) {
        const currentPlayer = this.players[this.currentPlayerIndex];

        if (this.hasBoughtOffice) {
            return;
        }

        // Fetch the owner data from the server
        console.log("Before fetch");
        fetch(
            `http://localhost:8000/api/games/property/${this.gameId}/${property.name}/owner`
        )
            .then((response) => {
                if (response.ok) {
                    return response.json();
                } else {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
            })
            .then((data) => {
                console.log("In fetch Then");
                const ownerData = data.owner;

                if (
                    ownerData &&
                    ownerData.username === currentPlayer.username
                ) {
                    console.log(
                        "OwnerData.username === currentPlayer.username"
                    );

                    const menu = this.add.graphics();
                    menu.fillStyle(0x000000, 0.8);
                    menu.fillRect(200, 150, 400, 200);

                    const text = this.add
                        .text(400, 200, `${property.name}`, {
                            fontSize: "20px",
                            color: "#ffffff",
                            align: "center",
                        })
                        .setOrigin(0.5);

                    let buyButton,
                        mortgageButton,
                        unmortgageButton,
                        closeButton;
                    console.log("Property office == ", property.office);
                    if (property.mortgaged) {
                        unmortgageButton = this.add
                            .text(300, 250, "Unmortgage", {
                                fontSize: "28px", // Increased font size
                                color: "#00ff00", // Green color for better visibility
                                align: "center",
                            })
                            .setInteractive()
                            .on("pointerdown", () => {
                                console.log(
                                    "Unmortgage clicked for",
                                    property.name
                                );
                                this.unmortgageProperty(
                                    property,
                                    currentPlayer
                                );

                                menu.clear();
                                text.destroy();
                                unmortgageButton.destroy();
                                closeButton.destroy();
                            });
                    } else {
                        if (
                            property.office < 4 &&
                            property.category !== "cars"
                        ) {
                            console.log("create Buy Button for office");
                            buyButton = this.add
                                .text(300, 220, "Buy Office", {
                                    fontSize: "28px", // Increased font size
                                    color: "#00ff00", // Green color for better visibility
                                    align: "center",
                                })
                                .setInteractive()
                                .on("pointerdown", () => {
                                    console.log(
                                        "Buy Office clicked for",
                                        property.name
                                    );
                                    this.stompClient.send(
                                        "/app/player/buyOffice",
                                        {},
                                        JSON.stringify({
                                            gameId: this.gameId,
                                            username: currentPlayer.username,
                                            propertyName: property.name,
                                        })
                                    );
                                    property.office += 1;
                                    this.addOfficeToProperty(property);
                                    this.hasBoughtOffice = true;

                                    menu.clear();
                                    text.destroy();
                                    buyButton.destroy();
                                    mortgageButton.destroy();
                                    closeButton.destroy();
                                });
                        }

                        if (property.office > 0) {
                            mortgageButton = this.add
                                .text(300, 300, "Sell Office", {
                                    fontSize: "20px",
                                    color: "#ff0000",
                                    align: "center",
                                })
                                .setInteractive()
                                .on("pointerdown", () => {
                                    console.log(
                                        "Sell Office clicked for",
                                        property.name
                                    );
                                    this.stompClient.send(
                                        "/app/player/sellOffice",
                                        {},
                                        JSON.stringify({
                                            gameId: this.gameId,
                                            username: currentPlayer.username,
                                            propertyName: property.name,
                                        })
                                    );
                                    property.office -= 1;
                                    const officeGraphic =
                                        property.officeGraphics.pop();
                                    officeGraphic.destroy();
                                    this.hasBoughtOffice = false;

                                    menu.clear();
                                    text.destroy();
                                    buyButton.destroy();
                                    mortgageButton.destroy();
                                    closeButton.destroy();
                                });
                        } else {
                            console.log("create Mortgage Button");
                            mortgageButton = this.add
                                .text(300, 270, "Mortgage", {
                                    fontSize: "28px", // Increased font size
                                    color: "#ff0000", // Red color for better visibility
                                    align: "center",
                                })
                                .setInteractive()
                                .on("pointerdown", () => {
                                    console.log(
                                        "Mortgage clicked for",
                                        property.name
                                    );
                                    property.mortgaged = true;
                                    property.baseRent = 0;
                                    this.drawCrossOnProperty(property);

                                    this.stompClient.send(
                                        "/app/player/mortgageProperty",
                                        {},
                                        JSON.stringify({
                                            gameId: this.gameId,
                                            username: currentPlayer.username,
                                            propertyName: property.name,
                                        })
                                    );

                                    menu.clear();
                                    text.destroy();
                                    buyButton.destroy();
                                    mortgageButton.destroy();
                                    closeButton.destroy();
                                });
                        }
                    }

                    console.log("Create close button");

                    closeButton = this.add
                        .text(500, 350, "Close", {
                            fontSize: "28px", // Increased font size for visibility
                            color: "#ffffff", // White color
                            backgroundColor: "#000000", // Black background to make it stand out
                            align: "center",
                            padding: { left: 10, right: 10, top: 5, bottom: 5 }, // Padding for better appearance
                        })
                        .setInteractive()
                        .on("pointerdown", () => {
                            menu.clear();
                            text.destroy();
                            if (buyButton) buyButton.destroy();
                            if (mortgageButton) mortgageButton.destroy();
                            if (unmortgageButton) unmortgageButton.destroy();
                            closeButton.destroy();
                        });
                }
            })
            .catch((error) => {
                console.error("Error fetching property owner:", error);
            });
    }

    unmortgageProperty(property, player) {
        console.log("Player money: ", player.money);

        const unmortgageCost = property.cost / 2 + (property.cost / 2) * 0.1; // 10% interest
        console.log("Umortgage Cost: ", unmortgageCost);
        if (player.money >= unmortgageCost) {
            player.money -= unmortgageCost;
            property.mortgaged = false;
            property.baseRent = property.originalBaseRent; // Restore the base rent
            this.updatePlayerMoney(player);
            this.removeMortgageGraphics(property);

            this.stompClient.send(
                "/app/player/unmortgageProperty",
                {},
                JSON.stringify({
                    gameId: this.gameId,
                    username: player.username,
                    propertyName: property.name,
                })
            );
        } else {
            alert("You don't have enough money to unmortgage this property.");
        }
    }

    removeMortgageGraphics(property) {
        if (property.mortgageGraphics) {
            property.mortgageGraphics.destroy();
            property.mortgageGraphics = null;
        }
    }

    drawCrossOnProperty(property) {
        const crossGraphics = this.add.graphics();
        crossGraphics.lineStyle(5, 0xff0000); // Red color for the cross

        // Draw the cross lines
        crossGraphics.moveTo(property.x, property.y);
        crossGraphics.lineTo(
            property.x + property.width,
            property.y + property.height
        );
        crossGraphics.moveTo(property.x + property.width, property.y);
        crossGraphics.lineTo(property.x, property.y + property.height);
        crossGraphics.strokePath();

        // Store the graphics object so it can be removed later if needed
        property.mortgageGraphics = crossGraphics;
    }

    removeOfficeFromProperty(property) {
        if (property.officeGraphics && property.officeGraphics.length > 0) {
            const office = property.officeGraphics.pop();
            office.destroy();
        }
    }

    updatePlayerMoney(player) {
        if (!player || !player.username) {
            console.error("Invalid player object:", player);
            return;
        }

        console.log("This.children", this.children);

        const playerCard = this.playerCards.find(
            (card) => card.username === player.username
        );

        if (playerCard) {
            playerCard.moneyText.setText("$" + player.money.toLocaleString());
        } else {
            console.error(`No player card found for ${player.username}`);
        }
    }

    updatePlayerData(playerData) {
        const player = this.players.find(
            (p) => p.username === playerData.username
        );
        if (player) {
            player.money = playerData.money;
            player.properties = playerData.properties;
            this.updatePlayerMoney(player);
        }
    }

    addPlayerOwnership(playerIndex, property) {
        console.log("Player Index: ", playerIndex);

        if (!property) {
            console.error("Missed property");
        }

        if (playerIndex < 0) {
            console.error("playerIndex < 0");
        }
        if (playerIndex >= this.players.length) {
            console.log("this.players.length: ", this.players.length);

            console.error("playerIndex >= this.players.length");
        }

        if (
            !property ||
            playerIndex < 0 ||
            playerIndex >= this.players.length
        ) {
            console.error(
                "Invalid property or playerIndex:",
                property,
                playerIndex
            );
            return;
        }

        const player = this.players[playerIndex];
        if (!player) {
            console.error("Player not found:", playerIndex);
            return;
        }
        if (!player.properties) {
            player.properties = [];
        }
        player.properties.push(property.name);
        this.players[playerIndex].properties.push(property.name);
        console.log("PROPERTIES: ", player.properties);
        console.log(this.players);

        property.owner = player.username;
        const playerColor = player.playerObj.getData("color");
        const ownerPlate = this.add.graphics();
        ownerPlate.fillStyle(playerColor, 1.0);
        let x = property.x;
        let y = property.y;
        let width = property.width;
        let height = 10;

        if (property.name === "Go" || property.name === "Go to jail") {
            // Special cases for corners
            x = property.x;
            y = property.y + property.height - 10;
        } else if (property.rotation === Math.PI / 2) {
            // Right side
            x = property.x;
            y = property.y;
            width = 10;
            height = property.height - 10;
        } else if (property.rotation === -Math.PI / 2) {
            console.log("Left");
            //TODO Here change
            // Left side
            x = property.x + property.width - 10;
            y = property.y + 10;
            width = 10;
            height = property.height - 10;
        } else if (property.y === this.boardPositions[0].y) {
            // Top row
            y = property.y + property.height - 10;
            height = 10;
        } else if (property.y > this.boardPositions[0].y) {
            // Bottom row
            y = property.y + property.height - 10;
            height = 10;
        } else if (property.x === this.boardPositions[0].x) {
            // Left row
            x = property.x;
            y = property.y;
            width = 10;
            height = property.height;
        }

        ownerPlate.fillRect(x, y, width, height);
        ownerPlate.setDepth(10);

        if (property.priceText) {
            property.priceText.setText(`$${property.baseRent}`);
        }

        this.updatePlayerMoney(player);

        ownerPlate
            .setInteractive(
                new Phaser.Geom.Rectangle(0, 0, width, height),
                Phaser.Geom.Rectangle.Contains
            )
            .on("pointerdown", () => {
                console.log("ownerPlate clicked!");
                if (this.isCurrentPlayer()) {
                    this.handlePropertyClick(property);
                }
            });

        console.log(ownerPlate);

        property.officeGraphics.forEach((officeGraphic) =>
            officeGraphic.destroy()
        );
        property.officeGraphics = [];
        for (let i = 0; i < property.office; i++) {
            const officeColor = 0x00ff00; // Green color for the office
            const officeSize = 10; // Size of the office representation

            const offset = i * officeSize; // Position based on the number of offices
            const office = this.add.rectangle(
                property.x + offset,
                property.y + offset,
                officeSize,
                officeSize,
                officeColor
            );
            office.setDepth(2); // Ensure it is above other elements
            property.officeGraphics.push(office);
        }

        this.checkAndApplyCategoryBonus(player, property.category);
    }

    checkAndApplyCategoryBonus(player, category) {
        const categoryProperties = this.boardPositions.filter(
            (pos) => pos.category === category && pos.cost
        );

        const ownsAll = categoryProperties.every((prop) => {
            player.properties.includes(prop.name);
        });

        if (ownsAll) {
            categoryProperties.forEach((prop) => {
                prop.baseRent = Math.floor(prop.baseRent * 1.2);
                prop.originalBaseRent = prop.baseRent;
                if (prop.priceText) {
                    prop.priceText.setText(`$${prop.baseRent}`);
                }
            });
        }
    }

    showCasinoMenu(playerIndex) {
        const menu = this.add.graphics();
        menu.fillStyle(0x000000, 0.8);
        menu.fillRect(100, 100, 600, 400);

        const betText = this.add
            .text(200, 150, "Place your bet:", {
                fontSize: "20px",
                color: "#ffffff",
            })
            .setOrigin(0.5);
        const betInput = this.add.dom(400, 200, "input", {
            type: "number",
            min: "1",
            value: "100",
            style: "width: 200px; height: 40px; font-size: 20px;",
        });

        const numberText = this.add
            .text(200, 250, "Choose numbers (1-4):", {
                fontSize: "20px",
                color: "#ffffff",
            })
            .setOrigin(0.5);
        const numberInput = this.add.dom(400, 300, "input", {
            type: "text",
            placeholder: "e.g. 1,2",
            style: "width: 200px; height: 40px; font-size: 20px;",
        });

        const playButton = this.add
            .text(400, 350, "Play", {
                fontSize: "20px",
                color: "#00ff00",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                const bet = parseInt(betInput.node.value);
                const selectedNumbers = numberInput.node.value
                    .split(",")
                    .map(Number);

                if (
                    bet > 0 &&
                    selectedNumbers.length >= 1 &&
                    selectedNumbers.length <= 4 &&
                    selectedNumbers.every((num) => num >= 1 && num <= 6)
                ) {
                    this.stompClient.send(
                        "/app/player/casinoGame",
                        {},
                        JSON.stringify({
                            gameId: this.gameId,
                            username: this.players[playerIndex].username,
                            bet: bet,
                            selectedNumbers: selectedNumbers,
                        })
                    );

                    menu.clear();
                    betText.destroy();
                    betInput.destroy();
                    numberText.destroy();
                    numberInput.destroy();
                    playButton.destroy();

                    this.nextPlayerTurn();
                } else {
                    alert("Invalid bet or number selection");
                }
            });
        // Add the "Close" button
        const closeButton = this.add
            .text(400, 400, "Close", {
                fontSize: "20px",
                color: "#ff0000",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                // Destroy the menu and its elements when "Close" is clicked
                menu.clear();
                betText.destroy();
                betInput.destroy();
                numberText.destroy();
                numberInput.destroy();
                playButton.destroy();
                closeButton.destroy();

                // Proceed to the next player's turn if needed
                this.nextPlayerTurn();
            });
    }

    addOfficeToProperty(property) {
        const officeColor = 0x00ff00; // Green color for the office
        const officeSize = 10; // Size of the office representation

        const offset = property.offices * officeSize; // Position based on the number of offices
        const office = this.add.rectangle(
            property.x + offset,
            property.y + offset,
            officeSize,
            officeSize,
            officeColor
        );
        office.setDepth(2); // Ensure it is above other elements
        property.officeGraphics.push(office);
    }

    savePlayerPosition(player) {
        this.previousPlayerId = this.currentPlayerId;
        if (this.stompClient && this.stompClient.connected) {
            console.log("I send");
            this.stompClient.send(
                "/app/player/move",
                {},
                JSON.stringify({
                    gameId: this.gameId,
                    gameName: this.gameName,
                    username: player.username,
                    newPosition: player.currentPosition,
                    x: player.playerObj.x,
                    y: player.playerObj.y,
                    finalPos: true,
                })
            );
        }
        console.log("I save player position");
    }

    // TODO CHECK IF IT WORKS
    showJailOptions(player) {
        const menu = this.add.graphics();
        menu.fillStyle(0x000000, 0.8);
        menu.fillRect(100, 100, 600, 400);

        const title = this.add
            .text(400, 150, `You are in Jail`, {
                fontSize: "20px",
                color: "#ffffff",
                align: "center",
            })
            .setOrigin(0.5);

        const payButton = this.add
            .text(200, 300, "Pay $500 to get out", {
                fontSize: "20px",
                color: "#00ff00",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                if (player.money >= 500) {
                    player.money -= 500;
                    this.updatePlayerMoney(player);
                    player.isInJail = false;

                    menu.clear();
                    title.destroy();
                    payButton.destroy();
                    rollButton.destroy();
                    closeButton.destroy();

                    this.rollDice(); // Allow the player to roll the dice after paying
                } else {
                    alert("You don't have enough money to pay!");
                }
            });

        const rollButton = this.add
            .text(600, 300, "Roll 2 Dice to Try Luck", {
                fontSize: "20px",
                color: "#ffff00",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                this.rollDice(true); // Pass `true` to indicate this is a jail roll
                menu.clear();
                title.destroy();
                payButton.destroy();
                rollButton.destroy();
                closeButton.destroy();
            });

        const closeButton = this.add
            .text(400, 400, "Close", {
                fontSize: "20px",
                color: "#ff0000",
                backgroundColor: "#000000",
                padding: 10,
            })
            .setOrigin(0.5)
            .setInteractive()
            .on("pointerdown", () => {
                menu.clear();
                title.destroy();
                payButton.destroy();
                rollButton.destroy();
                closeButton.destroy();

                this.nextPlayerTurn(); // Proceed to the next player's turn if needed
            });
    }

    nextPlayerTurn() {
        console.log("End turn");

        const currentPlayer = this.players[this.currentPlayerIndex];
        const isInJail = currentPlayer.isInJail || false;

        if (isInJail) {
            this.showJailOptions(currentPlayer);
        } else {
            if (this.stompClient && this.stompClient.connected) {
                this.stompClient.send(
                    "/app/game/endTurn",
                    {},
                    JSON.stringify({
                        gameId: this.gameId,
                        gameName: this.gameName,
                    })
                );
            }
            this.saveGameState(); // Save the game state at the end of the turn
        }
    }

    determineRent(property) {
        if (property.baseRent === 0) {
            // Property is mortgaged
            return 0;
        }

        const baseRent = property.baseRent || 0; // Default to 0 if baseRent is not defined

        let officeMultiplier = 0;
        switch (property.offices) {
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

        return baseRent + (baseRent * officeMultiplier) / 100;
    }

    update() {
        // Game loop logic
    }
}
