import React, { useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { connect } from "react-redux";
import { signup } from "../actions/auth";
import validator from "validator";
import "../css/Signup.css";

const Signup = ({ signup, isAuthenticated }) => {
    const [accountCreated, setAccountCreated] = useState(false);
    const [formData, setFormData] = useState({
        username: "",
        email: "",
        password: "",
        re_password: "",
        profilePhoto: null,
    });
    const [errors, setErrors] = useState({});

    const { username, email, password, re_password, profilePhoto } = formData;

    const onChange = (e) =>
        setFormData({ ...formData, [e.target.name]: e.target.value });

    const onFileChange = (e) => {
        setFormData({ ...formData, profilePhoto: e.target.files[0] });
    };

    const validateFields = () => {
        let errors = {};

        if (validator.isEmpty(username)) {
            errors.username = "Username is required";
        }

        if (!validator.isEmail(email)) {
            errors.email = "Invalid email";
        }

        if (validator.isEmpty(email)) {
            errors.email = "Email is required";
        }

        if (!validator.isLength(password, { min: 6 })) {
            errors.password = "Password must be at least 6 characters";
        }

        if (validator.isEmpty(password)) {
            errors.password = "Password is required";
        }

        if (!validator.equals(password, re_password)) {
            errors.re_password = "Passwords do not match";
        }

        if (validator.isEmpty(re_password)) {
            errors.re_password = "Please confirm your password";
        }

        // Validate profile photo
        if (profilePhoto) {
            const allowedTypes = ["image/jpeg", "image/png"];
            if (!allowedTypes.includes(profilePhoto.type)) {
                errors.profilePhoto = "Only JPG and PNG files are allowed";
            }

            if (profilePhoto.size > 2 * 1024 * 1024) {
                // 2 MB
                errors.profilePhoto = "File size exceeds the 2 MB limit";
            }
        }

        return errors;
    };

    const onSubmit = (e) => {
        e.preventDefault();

        const errors = validateFields();
        if (Object.keys(errors).length === 0) {
            const formData = new FormData();
            formData.append("username", username);
            formData.append("email", email);
            formData.append("password", password);
            formData.append("re_password", re_password);
            if (profilePhoto) {
                formData.append("profilePhoto", profilePhoto);
            }

            signup(formData);
            setAccountCreated(true);
        } else {
            setErrors(errors);
        }
    };

    if (isAuthenticated) {
        return <Navigate to="/" />;
    }

    if (accountCreated) {
        return <Navigate to="/login" />;
    }

    return (
        <div className="signup-container">
            <div className="signup-card">
                <h1 className="signup-title">Sign Up</h1>
                <p className="signup-subtitle">Create your Account</p>
                <form onSubmit={onSubmit} className="signup-form">
                    <div className="form-group">
                        <input
                            className={`form-control ${
                                errors.username ? "is-invalid" : ""
                            }`}
                            type="text"
                            placeholder="Username"
                            name="username"
                            value={username}
                            onChange={onChange}
                            required
                        />
                        {errors.username && (
                            <div className="invalid-feedback">
                                {errors.username}
                            </div>
                        )}
                    </div>
                    <div className="form-group">
                        <input
                            className={`form-control ${
                                errors.email ? "is-invalid" : ""
                            }`}
                            type="email"
                            placeholder="Email"
                            name="email"
                            value={email}
                            onChange={onChange}
                            required
                        />
                        {errors.email && (
                            <div className="invalid-feedback">
                                {errors.email}
                            </div>
                        )}
                    </div>
                    <div className="form-group">
                        <input
                            className={`form-control ${
                                errors.password ? "is-invalid" : ""
                            }`}
                            type="password"
                            placeholder="Password"
                            name="password"
                            value={password}
                            onChange={onChange}
                            minLength="6"
                            required
                        />
                        {errors.password && (
                            <div className="invalid-feedback">
                                {errors.password}
                            </div>
                        )}
                    </div>
                    <div className="form-group">
                        <input
                            className={`form-control ${
                                errors.re_password ? "is-invalid" : ""
                            }`}
                            type="password"
                            placeholder="Confirm Password"
                            name="re_password"
                            value={re_password}
                            onChange={onChange}
                            minLength="6"
                            required
                        />
                        {errors.re_password && (
                            <div className="invalid-feedback">
                                {errors.re_password}
                            </div>
                        )}
                    </div>
                    <div className="form-group">
                        <input
                            className={`form-control ${
                                errors.profilePhoto ? "is-invalid" : ""
                            }`}
                            type="file"
                            accept="image/jpeg, image/png"
                            onChange={onFileChange}
                        />
                        {errors.profilePhoto && (
                            <div className="invalid-feedback">
                                {errors.profilePhoto}
                            </div>
                        )}
                    </div>
                    <button className="btn btn-primary" type="submit">
                        Sign Up
                    </button>
                </form>
                <p className="signup-text">
                    Already have an account? <Link to="/login">Login</Link>
                </p>
            </div>
        </div>
    );
};

const mapStateToProps = (state) => ({
    isAuthenticated: state.auth.isAuthenticated,
});

export default connect(mapStateToProps, { signup })(Signup);
