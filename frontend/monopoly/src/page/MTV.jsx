import React, { useEffect, useState } from "react";
import axios from "axios";
import { connect } from "react-redux";
import "../css/M1TV.css";
// TODO ADD SOCKETS
const M1TV = ({ user }) => {
    const [games, setGames] = useState([]);
    const [error, setError] = useState(null);

    useEffect(() => {
        // Fetch all games when the component mounts
        const fetchGames = async () => {
            try {
                const response = await axios.get(
                    "http://localhost:8000/m1tv/getAllGames"
                );
                setGames(response.data);
            } catch (error) {
                setError("Failed to load games");
            }
        };

        fetchGames();
    }, []);

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

    if (error) {
        return <p>{error}</p>;
    }

    return (
        <div className="m1tv">
            <h1>Watch Games on M1TV</h1>
            {games.length > 0 ? (
                games.map((game) => (
                    <div key={game.id} className="game-item">
                        <h2>{game.name}</h2>
                        <button onClick={() => handleWatchGame(game.id)}>
                            Watch
                        </button>
                    </div>
                ))
            ) : (
                <p>No games available to watch</p>
            )}
        </div>
    );
};

const mapStateToProps = (state) => ({
    user: state.auth.user,
});

export default connect(mapStateToProps)(M1TV);
