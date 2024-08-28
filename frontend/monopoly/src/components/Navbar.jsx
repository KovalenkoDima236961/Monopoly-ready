import React, { useState, useEffect } from "react";
import { connect } from "react-redux";
import { logout } from "../actions/auth";
import { Link } from "react-router-dom";
import "../css/Navbar.css";
import axios from "axios";

const Navbar = ({ logout, isAuthenticated, userId, roles }) => {
    const [redirect, setRedirect] = useState(false);
    const [dropdownOpen, setDropdownOpen] = useState(false);
    const [profilePhotoUrl, setProfilePhotoUrl] = useState(
        "/assets/default-photo.png"
    );

    useEffect(() => {
        if (isAuthenticated && userId) {
            axios
                .get(`/profile-photo/${userId}`, { responseType: "blob" })
                .then((response) => {
                    const imageUrl = URL.createObjectURL(response.data);
                    setProfilePhotoUrl(imageUrl);
                })
                .catch(() => {
                    setProfilePhotoUrl("/assets/default-photo.png");
                });
        }
    }, [isAuthenticated, userId]);

    const logout_user = () => {
        logout();
        setRedirect(true);
    };

    const toggleDropdown = () => {
        setDropdownOpen(!dropdownOpen);
    };
    console.log(roles);
    const isAdmin = roles && roles.some((role) => role.name === "ROLE_ADMIN");
    console.log(isAdmin);

    return (
        <nav className="navbar">
            <div className="logo">
                <span>Monopoly One</span>
            </div>
            <div className="menuWrapper">
                <div className="links-nav">
                    <Link className="button" to="/games">
                        ПОИСК ИГР
                    </Link>
                    <Link className="button" to="/games">
                        M1TV
                    </Link>
                    {isAuthenticated && (
                        <>
                            <Link className="button">Друзья</Link>
                            <Link className="button">Инвентарь</Link>
                            <Link className="button">Маркет</Link>
                        </>
                    )}
                </div>
            </div>
            <div className="profileWrapper">
                {isAuthenticated ? (
                    <div className="profileContainer">
                        <div className="profileIcon" onClick={toggleDropdown}>
                            <img
                                src={profilePhotoUrl}
                                alt="Profile"
                                className="profileImage"
                            />
                        </div>
                        {dropdownOpen && (
                            <div className="dropdownMenu">
                                {isAdmin && (
                                    <Link
                                        to="/admin"
                                        className="dropdownButton"
                                    >
                                        Admin
                                    </Link>
                                )}
                                <button
                                    onClick={logout_user}
                                    className="dropdownButton"
                                >
                                    Logout
                                </button>
                                <button className="dropdownButton">
                                    Settings
                                </button>
                            </div>
                        )}
                    </div>
                ) : (
                    <>
                        <Link to="/login" className="button">
                            Login
                        </Link>
                        <Link to="/signup" className="button">
                            Sign Up
                        </Link>
                    </>
                )}
            </div>
        </nav>
    );
};

const mapStateToProps = (state) => ({
    isAuthenticated: state.auth.isAuthenticated,
    userId: state.auth.user ? state.auth.user.id : null,
    roles: state.auth.user ? state.auth.user.roles : [],
});

export default connect(mapStateToProps, { logout })(Navbar);
