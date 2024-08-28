import React, { useState, useEffect, useCallback } from "react";
import { Link, useNavigate } from "react-router-dom";
import { connect } from "react-redux";
import axios from "axios";
import { Stomp } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import CreateGameModal from "../components/CreateGameModal";
import GameLobby from "../components/GameLobby";
import "../css/Games.css"; // Make sure this file includes the updated styles

const Games = ({ isAuthenticated, user }) => {
    const [message, setMessage] = useState("");
    const [chatMessages, setChatMessages] = useState([]);
    const [games, setGames] = useState([]);
    const [showModal, setShowModal] = useState(false);
    const [userGame, setUserGame] = useState(null);
    const [showFriendModal, setShowFriendModal] = useState(false); // State for showing friend modal
    const [selectedFriend, setSelectedFriend] = useState(null); // State to store the selected friend
    const navigate = useNavigate();
    const [stompClient, setStompClient] = useState(null);

    const fetchGames = useCallback(async () => {
        try {
            const response = await axios.get("http://localhost:8000/api/games");
            if (Array.isArray(response.data)) {
                setGames(response.data);
            } else {
                setGames([]);
            }
        } catch (error) {
            console.error("There was an error fetching the games!", error);
            setGames([]);
        }
    }, []);

    const fetchChatMessages = async () => {
        try {
            const response = await axios.get(
                "http://localhost:8000/api/chat/messages"
            );
            setChatMessages(response.data);
        } catch (error) {
            console.error(
                "There was an error fetching the chat messages!",
                error
            );
        }
    };

    useEffect(() => {
        fetchGames();
        fetchChatMessages();

        const socket = new SockJS("http://localhost:8000/ws");
        const stompClient = Stomp.over(socket);
        setStompClient(stompClient);

        stompClient.connect({}, () => {
            // Subscribe to general games topic for updates
            stompClient.subscribe("/topic/games", (message) => {
                const updatedGames = JSON.parse(message.body);
                setGames(updatedGames);
            });

            // Subscribe to chat messages
            stompClient.subscribe("/topic/public", (message) => {
                const chatMessage = JSON.parse(message.body);
                setChatMessages((prevMessages) => [
                    ...prevMessages,
                    chatMessage,
                ]);
            });

            // Subscribe to game creation topic
            stompClient.subscribe("/topic/game-created", (message) => {
                const newGame = JSON.parse(message.body);
                setGames((prevGames) => [...prevGames, newGame]);
            });

            // Subscribe to game removal topic
            stompClient.subscribe("/topic/game-removed", (message) => {
                const { gameId } = JSON.parse(message.body);
                setGames((prevGames) =>
                    prevGames.filter((game) => game.id !== gameId)
                );
            });

            // Subscribe to specific game updates if the user is part of a game
            if (userGame) {
                subscribeToGameTopics(stompClient, userGame.id);
            }
        });

        return () => {
            if (stompClient) {
                stompClient.disconnect();
            }
        };
    }, [fetchGames, userGame]);

    const subscribeToGameTopics = (stompClient, gameId) => {
        // Subscribe to the specific game topic for updates related to that game
        stompClient.subscribe(`/topic/game/${gameId}`, (message) => {
            const updatedGame = JSON.parse(message.body);
            setGames((prevGames) =>
                prevGames.map((game) =>
                    game.id === updatedGame.gameId ? updatedGame : game
                )
            );
        });

        // Listen for the game start event
        stompClient.subscribe(`/topic/game-started/${gameId}`, (message) => {
            const gameData = JSON.parse(message.body);
            if (gameData.gameId === gameId) {
                navigate(`/game/${gameData.gameId}`);
            }
        });
    };

    const handleCreateGame = async (gameData) => {
        try {
            const response = await axios.post(
                "http://localhost:8000/api/games",
                { ...gameData, username: user.username },
                {
                    headers: {
                        Authorization: `Bearer ${localStorage.getItem(
                            "token"
                        )}`,
                    },
                }
            );
            if (response.data && response.data.id) {
                setUserGame(response.data);
                subscribeToGameTopics(stompClient, response.data.id);
            }
        } catch (error) {
            console.error("There was an error creating the game!", error);
        }
    };

    const handleJoinGame = async (gameId) => {
        try {
            const response = await axios.post(
                `http://localhost:8000/api/games/${gameId}/join`,
                {
                    username: user.username,
                    gameName: games.find((game) => game.id === gameId).name,
                },
                {
                    headers: {
                        Authorization: `Bearer ${localStorage.getItem(
                            "token"
                        )}`,
                    },
                }
            );
            if (response.data && response.data.id) {
                setUserGame(response.data);
                subscribeToGameTopics(stompClient, response.data.id);

                const isGameStarted = await checkGameStatus(response.data.id);
                if (isGameStarted) {
                    navigate(`/game/${response.data.id}`);
                }
            }
        } catch (error) {
            console.error("There was an error joining the game!", error);
        }
    };

    const checkGameStatus = async (gameId) => {
        try {
            const response = await axios.get(
                `http://localhost:8000/api/games/${gameId}/status`,
                {
                    headers: {
                        Authorization: `Bearer ${localStorage.getItem(
                            "token"
                        )}`,
                    },
                }
            );
            return response.data.isGameStarted;
        } catch (error) {
            console.error(
                "There was an error checking the game status!",
                error
            );
            return false;
        }
    };

    const handleLeaveGame = async (gameId) => {
        try {
            const response = await axios.post(
                `http://localhost:8000/api/games/${gameId}/leave`,
                { username: user.username },
                {
                    headers: {
                        Authorization: `Bearer ${localStorage.getItem(
                            "token"
                        )}`,
                    },
                }
            );

            const updatedGame = response.data;

            if (updatedGame.players.length === 0) {
                setGames((prevGames) =>
                    prevGames.filter((game) => game.id !== gameId)
                );
            } else {
                setGames((prevGames) =>
                    prevGames.map((game) =>
                        game.id === updatedGame.id ? updatedGame : game
                    )
                );
            }

            if (user.username === response.data.username) {
                setUserGame(null);
            }
        } catch (error) {
            console.error("There was an error leaving the game!", error);
        }
    };

    const handleSendMessage = (e) => {
        e.preventDefault();
        if (stompClient && message.trim()) {
            const chatMessage = {
                sender: user.username,
                content: message,
                type: "CHAT",
            };
            stompClient.send(
                "/app/chat.sendMessage",
                {},
                JSON.stringify(chatMessage)
            );
            setMessage("");
        }
    };

    const handleChatUserClick = (friendName) => {
        setSelectedFriend(friendName);
        setShowFriendModal(true);
    };

    const handleAddFriend = async () => {
        try {
            const response = await axios.post(
                "http://localhost:8000/api/friends/add",
                {
                    username: user.username, // The current user's username
                    friendUsername: selectedFriend, // The username of the friend to be added
                },
                {
                    headers: {
                        Authorization: `Bearer ${localStorage.getItem(
                            "token"
                        )}`, // Optional: Include auth token if needed
                    },
                }
            );
            console.log(response.data); // Handle success message or further actions if necessary
            setShowFriendModal(false); // Close the modal after adding the friend
        } catch (error) {
            console.error("There was an error adding the friend!", error);
        }
    };

    return (
        <div className="games-container">
            <div className="sidebar">
                <div className="tasks-section">
                    <h2>Задания</h2>
                    <ul className="tasks-list">
                        <li>
                            Откройте двери на «Портале»
                            <span>0 / 40</span>
                            <span>80 XP</span>
                        </li>
                        <li>
                            Выбросите трипл, используя быстрый кубик
                            <span>0 / 3</span>
                            <span>100 XP</span>
                        </li>
                        <li>
                            Получите деньги от Банка на поле «Шанс»
                            <span>0 / 30 000</span>
                            <span>400 XP</span>
                        </li>
                        <li>
                            Получите деньги за остановку на «Старте»
                            <span>0 / 25</span>
                            <span>400 XP</span>
                        </li>
                    </ul>
                </div>
                <div className="top-week">
                    <h2>Топ недели</h2>
                    <p>
                        Вы не входите в топ игроков недели — сначала выиграйте
                        соревновательный матч.
                    </p>
                </div>
                <div className="friends-online">
                    <h2>Друзья онлайн</h2>
                    <input
                        type="text"
                        placeholder="Поиск по друзьям"
                        className="friends-search"
                    />
                    <p>Нет друзей онлайн</p>
                </div>
            </div>
            <div className="main-content">
                <div className="vip-pass-section">
                    <h2>VIP пропуск</h2>
                    <p>
                        С VIP статусом можно создавать приватные комнаты, играть
                        в разных режимах и брать кредит во время игры.
                    </p>
                    <Link to="#">Подробнее о VIP статусе</Link>
                </div>
                <div className="chat-section">
                    <h2>Чат</h2>
                    <div className="chat-messages">
                        {chatMessages.map((msg, index) => (
                            <div
                                key={index}
                                className="chat-message"
                                onClick={() => handleChatUserClick(msg.sender)}
                            >
                                <span className="chat-user">{msg.sender}:</span>
                                <span className="chat-text">{msg.content}</span>
                            </div>
                        ))}
                    </div>
                    {isAuthenticated ? (
                        <form
                            onSubmit={handleSendMessage}
                            className="chat-form"
                        >
                            <input
                                type="text"
                                value={message}
                                onChange={(e) => setMessage(e.target.value)}
                                placeholder="Введите сообщение"
                                className="chat-input"
                            />
                            <button type="submit" className="chat-send-button">
                                Отправить
                            </button>
                        </form>
                    ) : (
                        <p>
                            Вы должны <Link to="/login">войти</Link>, чтобы
                            отправлять сообщения.
                        </p>
                    )}
                </div>
                <div className="games-section">
                    <h2>Ожидают игры</h2>
                    <div className="games-list">
                        {Array.isArray(games) &&
                            games.map((game) => (
                                <GameLobby
                                    key={game.id}
                                    game={game}
                                    user={user}
                                    userGame={userGame}
                                    handleJoinGame={handleJoinGame}
                                    handleLeaveGame={handleLeaveGame}
                                />
                            ))}
                        {isAuthenticated && userGame === null && (
                            <button
                                className="create-game-button"
                                onClick={() => setShowModal(true)}
                            >
                                Создать игру
                            </button>
                        )}
                    </div>
                </div>
            </div>
            <CreateGameModal
                show={showModal}
                handleClose={() => setShowModal(false)}
                handleCreate={handleCreateGame}
            />
            <FriendRequestModal
                show={showFriendModal}
                onClose={() => setShowFriendModal(false)}
                onAddFriend={handleAddFriend}
                friendName={selectedFriend}
            />
        </div>
    );
};

const mapStateToProps = (state) => ({
    isAuthenticated: state.auth.isAuthenticated,
    user: state.auth.user,
});

export default connect(mapStateToProps)(Games);
