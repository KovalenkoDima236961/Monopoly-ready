import React, { useEffect, useState } from "react";
import Board from "../components/Board";
import axios from "axios";
import PlayerList from "../components/PlayerList";
import "../css/Game.css"; // Assume we have a CSS file for styling
import { useParams } from "react-router-dom"; // Import useParams to get gameId from URL

const Game = () => {
    const { gameId } = useParams();
    const [game, setGame] = useState(null); // Initialize as null

    useEffect(() => {
        const fetchGame = async () => {
            try {
                const response = await axios.get(
                    `http://localhost:8000/api/games/${gameId}`
                );
                setGame(response.data);
            } catch (error) {
                console.error("There was an error fetching the game!", error);
            }
        };

        fetchGame();
    }, [gameId]);

    if (!game) {
        return <div>Loading...</div>;
    }

    return (
        <div className="game-container">
            <PlayerList players={game.players || []} />{" "}
            {/* Ensure players is an array */}
            <Board />
        </div>
    );
};

export default Game;
