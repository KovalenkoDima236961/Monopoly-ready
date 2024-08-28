import React, { useState } from "react";
import { Modal, Button, Form } from "react-bootstrap";
import "bootstrap/dist/css/bootstrap.min.css";
import "../css/CreateGameModal.css";

const CreateGameModal = ({ show, handleClose, handleCreate }) => {
    const [gameName, setGameName] = useState("");
    const [maxPlayers, setMaxPlayers] = useState(4);
    const [privateRoom, setPrivateRoom] = useState(false);
    const [autoStart, setAutoStart] = useState(true);

    const handleSubmit = (e) => {
        e.preventDefault();
        handleCreate({ gameName, maxPlayers, privateRoom, autoStart });
        handleClose();
    };

    return (
        <Modal show={show} onHide={handleClose}>
            <Modal.Header closeButton>
                <Modal.Title>Create Game</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <Form onSubmit={handleSubmit}>
                    <Form.Group controlId="gameName">
                        <Form.Label>Game Name</Form.Label>
                        <Form.Control
                            type="text"
                            placeholder="Enter game name"
                            value={gameName}
                            onChange={(e) => setGameName(e.target.value)}
                            required
                        />
                    </Form.Group>
                    <Form.Group controlId="maxPlayers">
                        <Form.Label>Players</Form.Label>
                        <div className="players-select">
                            {[2, 3, 4, 5].map((num) => (
                                <Button
                                    key={num}
                                    variant={
                                        maxPlayers === num
                                            ? "success"
                                            : "secondary"
                                    }
                                    onClick={() => setMaxPlayers(num)}
                                >
                                    {num}
                                </Button>
                            ))}
                            <Button variant="secondary" disabled>
                                2x2
                            </Button>
                        </div>
                    </Form.Group>
                    <Form.Group controlId="privateRoom">
                        <Form.Check
                            type="switch"
                            label="Private Room"
                            checked={privateRoom}
                            onChange={(e) => setPrivateRoom(e.target.checked)}
                        />
                    </Form.Group>
                    <Form.Group controlId="autoStart">
                        <Form.Check
                            type="switch"
                            label="Auto Start"
                            checked={autoStart}
                            onChange={(e) => setAutoStart(e.target.checked)}
                        />
                    </Form.Group>
                    <div className="vip-settings">
                        <p>
                            These are VIP settings. Get VIP status to play in
                            all modes and create private rooms, and a lot of
                            other cool features!
                        </p>
                        <Button variant="danger">Buy VIP</Button>
                    </div>
                    <Button
                        variant="primary"
                        type="submit"
                        className="create-room-button"
                    >
                        Create Room
                    </Button>
                </Form>
            </Modal.Body>
        </Modal>
    );
};

export default CreateGameModal;
