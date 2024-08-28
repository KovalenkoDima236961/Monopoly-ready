import React, { useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { connect } from "react-redux";
import { login } from "../actions/auth";
import validator from "validator"; // Import the validator library
import "../css/Login.css";

const Login = ({ login, isAuthenticated, error }) => {
    const [formData, setFormData] = useState({
        email: "",
        password: "",
    });
    const [validationError, setValidationError] = useState("");

    const { email, password } = formData;

    const onChange = (e) =>
        setFormData({ ...formData, [e.target.name]: e.target.value });

    const onSubmit = (e) => {
        e.preventDefault();

        // Validate email and password using the validator library
        if (!validator.isEmail(email)) {
            setValidationError("Please enter a valid email address.");
            return;
        }

        if (!validator.isLength(password, { min: 6 })) {
            setValidationError("Password must be at least 6 characters long.");
            return;
        }

        // If validation passes, clear the validation error and proceed with login
        setValidationError("");
        login(email, password);
    };

    if (isAuthenticated) {
        return <Navigate to="/" />;
    }

    return (
        <div className="login-container">
            <div className="login-card">
                <h1 className="login-title">Sign In</h1>
                <p className="login-subtitle">
                    Sign into your Monopoly One account
                </p>
                {validationError && (
                    <p className="login-error">{validationError}</p>
                )}{" "}
                {/* Display validation errors */}
                {error && <p className="login-error">{error}</p>}{" "}
                {/* Display server-side errors */}
                <form onSubmit={(e) => onSubmit(e)} className="login-form">
                    <div className="form-group">
                        <input
                            className="form-control"
                            type="email"
                            placeholder="Email"
                            name="email"
                            value={email}
                            onChange={(e) => onChange(e)}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <input
                            className="form-control"
                            type="password"
                            placeholder="Password"
                            name="password"
                            value={password}
                            onChange={(e) => onChange(e)}
                            minLength="6"
                            required
                        />
                    </div>
                    <button className="btn btn-primary" type="submit">
                        Login
                    </button>
                </form>
                <p className="login-text">
                    Don't have an account? <Link to="/signup">Sign Up</Link>
                </p>
                <p className="login-text">
                    Forgot your Password?{" "}
                    <Link to="/reset-password">Reset Password</Link>
                </p>
            </div>
        </div>
    );
};

const mapStateToProps = (state) => ({
    isAuthenticated: state.auth.isAuthenticated,
    error: state.auth.error,
});

export default connect(mapStateToProps, { login })(Login);
