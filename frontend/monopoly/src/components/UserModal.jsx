import React from "react";
import "../css/UserModal.css";

const UserModal = ({ user, onClose, onDelete }) => {
    if (!user) return null;

    return (
        <div className="modal-overlay">
            <div className="modal-content">
                <h2>User Information</h2>
                <p>
                    <strong>Name:</strong> {user.username}
                </p>
                <p>
                    <strong>Email:</strong> {user.email}
                </p>
                {/* Add more user details here if needed */}

                <div className="modal-actions">
                    <button
                        className="action-button"
                        onClick={() => onDelete(user.id)}
                    >
                        Delete
                    </button>
                    <button className="action-button" onClick={onClose}>
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default UserModal;
