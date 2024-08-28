import React from "react";
import "../css/PlayerList.css";

const PlayerList = ({ players = [] }) => {
    // Default to an empty array
    return (
        <div className="player-list">
            {players.map((player, index) => (
                <div key={player.id || index} className="player-item">
                    <p>Name: {player.username}</p>
                    <p>Money: ${player.money}</p>
                </div>
            ))}
        </div>
    );
};

export default PlayerList;
