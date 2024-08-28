import React, { useState } from "react";
import { connect } from "react-redux";
import { Navigate } from "react-router-dom";
import "../css/AdminPanel.css";
import AdminSidebar from "../components/AdminSidebar";
import { useGlobalContext } from "../context/context.jsx"; // Add this import
import UsersList from "../components/UsersList.jsx";
import GamesList from "../components/GamesList.jsx";

const AdminPanel = ({ roles, isAuthenticated, user }) => {
    const { openSidebar } = useGlobalContext();
    const [activeSection, setActiveSection] = useState("Home"); // Track the active section

    const handleSectionClick = (section) => {
        setActiveSection(section); // Update the active section
    };

    if (!roles.some((role) => role.name === "ROLE_ADMIN")) {
        console.log("Not admin");
        // return <Navigate to="/" />;
    }

    return (
        <div className="admin-panel">
            <AdminSidebar onSectionClick={handleSectionClick} />
            <button onClick={openSidebar} className="sidebar-toggle">
                Open Sidebar
            </button>
            <div className="admin-content">
                <h1>Admin Panel</h1>
                <p>
                    Welcome to the admin panel. Here you can manage the
                    application settings.
                </p>

                {activeSection === "Home" && <div>Home Content</div>}
                {activeSection === "Users" && <UsersList />}
                {activeSection === "Games" && <GamesList />}
                {activeSection === "Calendar" && <div>Calendar Content</div>}
                {activeSection === "Documents" && <div>Documents Content</div>}
            </div>
        </div>
    );
};
const mapStateToProps = (state) => ({
    isAuthenticated: state.auth.isAuthenticated,
    roles: state.auth.user ? state.auth.user.roles : [],
    user: state.auth.user,
});

export default connect(mapStateToProps)(AdminPanel);
