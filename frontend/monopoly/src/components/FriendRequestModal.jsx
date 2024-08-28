import React from "react";
import "../css/FriendRequestModal.css"; // Make sure to create the corresponding CSS file

const FriendRequestModal = ({ show, onClose, onAddFriend, friendName }) => {
    if (!show) {
        return null;
    }

    return (
        <div className="modal-overlay-friend">
            <div className="modal-content-friend">
                <h2>Add Friend</h2>
                <p>Do you want to add {friendName} as a friend?</p>
                <div className="modal-buttons-friend">
                    <button
                        className="modal-button-friend"
                        onClick={onAddFriend}
                    >
                        Yes
                    </button>
                    <button className="modal-button-friend" onClick={onClose}>
                        No
                    </button>
                </div>
            </div>
        </div>
    );
};

export default FriendRequestModal;
