import React, { useState } from "react";
import { Navigate } from "react-router-dom";
import { connect } from "react-redux";
import { reset_password } from "../actions/auth";
import "../css/ResetPassword.css"; // Import the CSS file

const ResetPassword = ({ reset_password }) => {
    const [requestSent, setRequestSent] = useState(false);
    const [formData, setFormData] = useState({
        email: "",
    });
    const [error, setError] = useState("");

    const { email } = formData;

    const onChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
        setError(""); // Clear error when user starts typing
    };

    const onSubmit = (e) => {
        e.preventDefault();

        if (email.trim() === "") {
            setError("Email is required");
            return;
        }

        reset_password(email);
        setRequestSent(true);
    };

    if (requestSent) {
        return <Navigate to="/" />;
    }

    return (
        <div className="reset-container">
            <div className="reset-card">
                <h1 className="reset-title">Request Password Reset</h1>
                <p className="reset-subtitle">
                    Enter your email to reset your password
                </p>
                <form onSubmit={onSubmit} className="reset-form">
                    <div className="form-group">
                        <input
                            className={`form-control ${
                                error ? "is-invalid" : ""
                            }`}
                            type="email"
                            placeholder="Email"
                            name="email"
                            value={email}
                            onChange={onChange}
                            required
                        />
                        {error && (
                            <div className="invalid-feedback">{error}</div>
                        )}
                    </div>
                    <button className="btn btn-primary" type="submit">
                        Reset Password
                    </button>
                </form>
            </div>
        </div>
    );
};

export default connect(null, { reset_password })(ResetPassword);
