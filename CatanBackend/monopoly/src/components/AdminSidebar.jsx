import React from "react";
import { useGlobalContext } from "../context/context.jsx"; // Add this import
import "../css/AdminSidebar.css";
import {
    FaTimes,
    FaBehance,
    FaFacebook,
    FaLinkedin,
    FaTwitter,
    FaSketch,
    FaHome,
    FaUserFriends,
    FaFolderOpen,
    FaCalendarAlt,
    FaWpforms,
} from "react-icons/fa";

const AdminSidebar = ({ onSectionClick }) => {
    const { isSidebarOpen, closeSidebar } = useGlobalContext();

    const links = [
        { id: 1, text: "Home", icon: <FaHome />, section: "Home" },
        { id: 2, text: "Users", icon: <FaUserFriends />, section: "Users" },
        { id: 3, text: "Games", icon: <FaFolderOpen />, section: "Games" },
        {
            id: 4,
            text: "Calendar",
            icon: <FaCalendarAlt />,
            section: "Calendar",
        },
        { id: 5, text: "Documents", icon: <FaWpforms />, section: "Documents" },
    ];

    const social = [
        { id: 1, url: "https://www.facebook.com", icon: <FaFacebook /> },
        { id: 2, url: "https://www.twitter.com", icon: <FaTwitter /> },
        { id: 3, url: "https://www.linkedin.com", icon: <FaLinkedin /> },
        { id: 4, url: "https://www.behance.net", icon: <FaBehance /> },
        { id: 5, url: "https://www.sketch.com", icon: <FaSketch /> },
    ];

    return (
        <aside
            className={`${isSidebarOpen ? "sidebar show-sidebar" : "sidebar"}`}
        >
            <div className="sidebar-header">
                <img src="./logo.svg" className="logo" alt="Admin Panel" />
                <button className="close-btn" onClick={closeSidebar}>
                    <FaTimes />
                </button>
            </div>
            <ul className="links">
                {links.map((link) => (
                    <li key={link.id}>
                        <a
                            href="#"
                            onClick={(e) => {
                                e.preventDefault();
                                onSectionClick(link.section); // Trigger section click handler
                            }}
                        >
                            {link.icon}
                            {link.text}
                        </a>
                    </li>
                ))}
            </ul>
            <ul className="social-icons">
                {social.map((link) => {
                    const { id, url, icon } = link;
                    return (
                        <li key={id}>
                            <a href={url}>{icon}</a>
                        </li>
                    );
                })}
            </ul>
        </aside>
    );
};

export default AdminSidebar;
