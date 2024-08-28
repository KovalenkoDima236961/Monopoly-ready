import React, { useEffect } from "react";
import { useLocation } from "react-router-dom";
import Navbar from "../components/Navbar.jsx";
import { connect } from "react-redux";
import { checkAuthenticated, load_user } from "../actions/auth.js";

const Layout = ({ checkAuthenticated, load_user, children }) => {
    const location = useLocation();
    const hideNavbarRoutes = ["/game/"];

    useEffect(() => {
        checkAuthenticated();
        load_user();
    }, [checkAuthenticated, load_user]);

    const showNavbar = !hideNavbarRoutes.some((route) =>
        location.pathname.startsWith(route)
    );

    return (
        <div>
            {showNavbar && <Navbar />}
            <div className={showNavbar ? "content" : "game-content"}>
                {children}
            </div>
        </div>
    );
};

export default connect(null, { checkAuthenticated, load_user })(Layout);
