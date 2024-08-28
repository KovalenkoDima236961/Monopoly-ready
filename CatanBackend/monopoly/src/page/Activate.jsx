import React, { useState, useEffect } from "react";
import { Navigate, useSearchParams } from "react-router-dom";
import { connect } from "react-redux";
import { verify } from "../actions/auth";

const Activate = ({ verify }) => {
    const [verified, setVerified] = useState(false);
    const [searchParams] = useSearchParams();
    const token = searchParams.get("token");

    useEffect(() => {
        if (token) {
            verify(token)
                .then(() => {
                    setVerified(true);
                })
                .catch((error) => {
                    console.error("Verification failed:", error);
                });
        }
    }, [token, verify]);
    if (verified) {
        // return <Navigate to="/" />;
    }

    return (
        <div className="container">
            <div
                className="d-flex flex-column justify-content-center align-items-center"
                style={{ marginTop: "200px" }}
            >
                <h1>Verify your Account:</h1>
                <p>We are verifying your account. Please wait...</p>
            </div>
        </div>
    );
};

export default connect(null, { verify })(Activate);
