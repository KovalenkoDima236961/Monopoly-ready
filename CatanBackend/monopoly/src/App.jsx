import React from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { Provider } from "react-redux";
import Layout from "./hocs/Layout";
import Home from "./page/Home";
import Login from "./page/Login";
import Signup from "./page/Signup";
import ResetPassword from "./page/ResetPassword";
import ResetPasswordConfirm from "./page/ResetPasswordConfirm";
import Activate from "./page/Activate";
import Rules from "./page/Rules";
import Settings from "./page/Settings";
import Games from "./page/Games";
import GameContainer from "./page/GameContainer";
import AdminPanel from "./page/AdminPanel";
import Game from "./page/Game";
import store from "./store";

const App = () => {
    return (
        <Provider store={store}>
            <Router>
                <Layout>
                    <Routes>
                        <Route exact path="/" element={<Home />} />
                        <Route exact path="/login" element={<Login />} />
                        <Route exact path="/rules" element={<Rules />} />
                        <Route exact path="/signup" element={<Signup />} />
                        <Route exact path="/games" element={<Games />} />
                        <Route exact path="/inventory" element={<Home />} />
                        <Route exact path="/market" element={<Home />} />
                        <Route
                            exact
                            path="/game/:gameId"
                            element={<GameContainer />}
                        />
                        <Route
                            exact
                            path="/settings/:userId"
                            element={<Settings />}
                        />
                        <Route
                            exact
                            path="/reset-password"
                            element={<ResetPassword />}
                        />
                        <Route
                            exact
                            path="/password/reset/confirm/:uid/:token"
                            element={<ResetPasswordConfirm />}
                        />
                        <Route exact path="/activate" element={<Activate />} />
                        <Route exact path="/admin" element={<AdminPanel />} />
                    </Routes>
                </Layout>
            </Router>
        </Provider>
    );
};

export default App;

