import React, { useState, useEffect } from "react";
import axios from "axios";
import UserModal from "./UserModal";
import "../css/GamesList.css";

const GamesList = () => {
    const [games, setGames] = useState([]);
    const [selectedPlayer, setSelectedPlayer] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchGames = async () => {
            try {
                const response = await axios.get(
                    "http://localhost:8000/adminPanel/getAllGames"
                );
                setGames(response.data);
                setLoading(false);
            } catch (error) {
                setError("Failed to fetch games");
                setLoading(false);
            }
        };

        fetchGames();
    }, []);

    const handleDeleteGame = async (gameId) => {
        try {
            await axios.delete(
                `http://localhost:8000/adminPanel/deleteGame/${gameId}`
            );
            setGames(games.filter((game) => game.id !== gameId));
            setSelectedPlayer(null); // Close the modal after deletion
        } catch (error) {
            console.error("Failed to delete game:", error);
            setError("Failed to delete game");
        }
    };

    const handlePlayerClick = (player) => {
        setSelectedPlayer(player); // Set the selected player and open the modal
    };

    const handleCloseModal = () => {
        setSelectedPlayer(null); // Close the modal
    };

    const handleWatchGame = async (gameId) => {
        try {
            await axios.post(`http://localhost:8000/m1tv/watchGame/${gameId}`, {
                userId: user ? user.id : null,
            });
            // Navigate to the game page with observer mode
            navigate(`/game/${gameId}?mode=observer`);
        } catch (error) {
            setError("Failed to start watching the game");
        }
    };

    if (loading) {
        return <p>Loading games...</p>;
    }

    if (error) {
        return <p>{error}</p>;
    }

    return (
        <div className="games-list">
            {games.map((game) => (
                <div key={game.id} className="game-item">
                    <h3>{game.name}</h3>
                    <ul>
                        {game.players.map((player) => (
                            <li
                                key={player.id}
                                onClick={() => handlePlayerClick(player)}
                            >
                                {player.name}
                            </li>
                        ))}
                    </ul>
                    <button
                        className="action-button"
                        onClick={() => handleDeleteGame(game.id)}
                    >
                        Delete Game
                    </button>
                    <button
                        className="action-button"
                        onClick={() => handleWatchGame(game.id)}
                    >
                        Watch Game
                    </button>
                </div>
            ))}
            {selectedPlayer && (
                <UserModal
                    user={selectedPlayer}
                    onClose={handleCloseModal}
                    onDelete={handleDeleteGame}
                />
            )}
        </div>
    );
};

export default GamesList;
