import React from "react";
import {
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

export const links = [
    {
        id: 1,
        url: "/admin/home",
        text: "home",
        icon: <FaHome />,
    },
    {
        id: 2,
        url: "/admin/team",
        text: "team",
        icon: <FaUserFriends />,
    },
    {
        id: 3,
        url: "/admin/projects",
        text: "projects",
        icon: <FaFolderOpen />,
    },
    {
        id: 4,
        url: "/admin/calendar",
        text: "calendar",
        icon: <FaCalendarAlt />,
    },
    {
        id: 5,
        url: "/admin/documents",
        text: "documents",
        icon: <FaWpforms />,
    },
];
