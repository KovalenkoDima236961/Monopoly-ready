import React, { useEffect, useRef, useState } from "react";
import Phaser from "phaser";
import GameScene from "../scenes/GameScene";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import axios from "axios";
import { connect } from "react-redux";
import { Stomp } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const GameContainer = ({ user }) => {
    const gameRef = useRef(null);
    const { gameId } = useParams();
    const [gameData, setGameData] = useState(null);
    const location = useLocation();
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const stompClient = useRef(null);

    useEffect(() => {
        if (!user || !user.username) {
            console.error("User is not defined or username is missing");
            return;
        }

        const fetchGameData = async () => {
            try {
                const response = await axios.get(
                    `http://localhost:8000/api/games/${gameId}`
                );
                const game = response.data;

                // Check if the current user is a player in the game
                const playerInGame = game.players.some(
                    (player) => player.username === user.username
                );

                if (!playerInGame) {
                    const queryParams = new URLSearchParams(location.search);
                    const isObserver = queryParams.get("mode") === "observer";

                    if (!isObserver) {
                        navigate("/games");
                        return;
                    }
                }
                setGameData(game);
                setLoading(false);
            } catch (error) {
                console.error("Error fetching game data", error);
                navigate("/games");
            }
        };

        console.log("Fetch game data");

        fetchGameData();
    }, [gameId, user.username, navigate]);

    useEffect(() => {
        if (gameData && !loading) {
            const queryParams = new URLSearchParams(location.search);
            const isObserver = queryParams.get("mode") === "observer";
            const config = {
                type: Phaser.AUTO,
                parent: "game-container",
                scene: [GameScene],
                scale: {
                    mode: Phaser.Scale.FIT,
                    autoCenter: Phaser.Scale.CENTER_BOTH,
                    width: window.innerWidth,
                    height: window.innerHeight,
                },
                physics: {
                    default: "arcade",
                    arcade: {
                        debug: true,
                    },
                },
                dom: {
                    createContainer: true,
                },
            };

            if (!gameRef.current) {
                const game = new Phaser.Game(config);
                game.scene.start("GameScene", {
                    gameId: gameData.id,
                    gameName: gameData.name,
                    username: user.username,
                    players: gameData.players,
                    currentPlayerId: gameData.currentPlayerId,
                    isObserver,
                });
                gameRef.current = game;
            }

            const socket = new SockJS("http://localhost:8000/ws");
            stompClient.current = Stomp.over(socket);
            stompClient.current.connect({}, () => {
                stompClient.current.subscribe(
                    `/topic/game/${gameId}`,
                    (message) => {
                        const updatedGame = JSON.parse(message.body);
                        setGameData(updatedGame);
                    }
                );
            });

            return () => {
                if (gameRef.current) {
                    gameRef.current.destroy(true);
                    gameRef.current = null;
                }
                if (stompClient.current) {
                    stompClient.current.disconnect();
                }
            };
        }
    }, [user.username, loading]);

    if (loading) {
        return <div>Loading...</div>;
    }

    return <div id="game-container" />;
};

const mapStateToProps = (state) => ({
    user: state.auth.user || {},
});

export default connect(mapStateToProps)(GameContainer);
