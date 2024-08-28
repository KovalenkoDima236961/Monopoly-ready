import "../css/usersList.css";
import { connect } from "react-redux";
import AdminPanel from "../page/AdminPanel";
import React, { useState, useEffect } from "react";
import axios from "axios";
import UserModal from "./UserModal";

const UsersList = () => {
    const [users, setUsers] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        // Fetch the list of users when the component mounts
        const fetchUsers = async () => {
            try {
                const response = await axios.get(
                    "http://localhost:8000/adminPanel/getAllUser"
                );
                setUsers(response.data);
                setLoading(false);
            } catch (error) {
                setError("Failed to fetch users");
                setLoading(false);
            }
        };

        fetchUsers();
    }, []);

    const handleDeleteUser = async (userId) => {
        try {
            await axios.delete(
                `http://localhost:8000/adminPanel/deleteUser/${userId}`
            );
            // Remove the user from the local state
            setUsers(users.filter((user) => user.id !== userId));
            setSelectedUser(null); // Close the modal after deletion
        } catch (error) {
            console.error("Failed to delete user:", error);
            setError("Failed to delete user");
        }
    };

    const handleViewUser = (user) => {
        setSelectedUser(user); // Set the selected user and open the modal
    };

    const handleCloseModal = () => {
        setSelectedUser(null); // Close the modal
    };

    if (loading) {
        return <p>Loading users...</p>;
    }

    if (error) {
        return <p>{error}</p>;
    }

    return (
        <div className="users-list">
            {users.map((user) => (
                <div key={user.id} className="user-item">
                    <span>{user.username}</span>
                    <div className="user-actions">
                        <button
                            className="action-button"
                            onClick={() => handleViewUser(user)}
                        >
                            View
                        </button>
                        <button
                            className="action-button"
                            onClick={() => handleDeleteUser(user.id)}
                        >
                            Delete
                        </button>
                    </div>
                </div>
            ))}
            {selectedUser && (
                <UserModal
                    user={selectedUser}
                    onClose={handleCloseModal}
                    onDelete={handleDeleteUser}
                />
            )}
        </div>
    );
};

export default UsersList;
