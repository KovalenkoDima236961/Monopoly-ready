import React from "react";
import "../css/GameLobby.css"; // Import the CSS file for styling
import { FaPlus } from "react-icons/fa"; // Import the Plus icon

const GameLobby = ({
    game,
    user,
    userGame,
    handleJoinGame,
    handleLeaveGame,
}) => {
    const renderPlayerSlots = () => {
        const slots = [];
        for (let i = 0; i < game.maxPlayers; i++) {
            if (i < game.players.length) {
                const player = game.players[i];
                slots.push(
                    <div key={player.id} className="player-slot filled">
                        <img
                            src={player.avatar || "default-avatar.png"}
                            alt="Avatar"
                            className="player-avatar"
                        />
                        <p className="player-username">{player.username}</p>
                        {player.username === user.username && (
                            <button
                                onClick={() => handleLeaveGame(game.id)}
                                className="leave-button"
                            >
                                Выйти
                            </button>
                        )}
                    </div>
                );
            } else {
                slots.push(
                    <div
                        key={i}
                        className="player-slot empty"
                        onClick={() =>
                            userGame === null && handleJoinGame(game.id)
                        }
                    >
                        <FaPlus className="plus-icon" />
                    </div>
                );
            }
        }
        return slots;
    };

    return (
        <div className="game-lobby">
            <div className="game-header">
                <h2 className="game-name">{game.name}</h2>
            </div>
            <div className="player-slots">{renderPlayerSlots()}</div>
        </div>
    );
};

export default GameLobby;
