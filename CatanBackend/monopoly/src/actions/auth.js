import axios from "axios";
import {
    LOGIN_SUCCESS,
    LOGIN_FAIL,
    USER_LOADED_SUCCESS,
    USER_LOADED_FAIL,
    AUTHENTICATED_SUCCESS,
    AUTHENTICATED_FAIL,
    PASSWORD_RESET_SUCCESS,
    PASSWORD_RESET_FAIL,
    PASSWORD_RESET_CONFIRM_SUCCESS,
    PASSWORD_RESET_CONFIRM_FAIL,
    SIGNUP_SUCCESS,
    SIGNUP_FAIL,
    ACTIVATION_SUCCESS,
    ACTIVATION_FAIL,
    LOGOUT,
} from "./types";

export const load_user = () => async (dispatch) => {
    if (localStorage.getItem("token")) {
        const config = {
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${localStorage.getItem("token")}`,
                Accept: "application/json",
            },
        };

        try {
            const res = await axios.get(
                "http://localhost:8000/auth/me",
                config
            );

            dispatch({
                type: USER_LOADED_SUCCESS,
                payload: res.data,
            });
        } catch (err) {
            dispatch({
                type: USER_LOADED_FAIL,
            });
        }
    } else {
        dispatch({
            type: USER_LOADED_FAIL,
        });
    }
};

export const signup =
    (username, email, password, re_password) => async (dispatch) => {
        const config = {
            headers: {
                "Content-Type": "application/json",
            },
        };

        const body = JSON.stringify({ username, email, password, re_password });

        try {
            const res = await axios.post(
                "http://localhost:8000/auth/registration",
                body,
                config
            );

            console.log(res);

            dispatch({
                type: SIGNUP_SUCCESS,
                payload: res.data,
            });
        } catch (err) {
            dispatch({
                type: SIGNUP_FAIL,
            });
        }
    };

export const login = (email, password) => async (dispatch) => {
    const config = {
        headers: {
            "Content-Type": "application/json",
        },
    };

    const body = JSON.stringify({ email, password });

    try {
        const res = await axios.post(
            "http://localhost:8000/auth/login",
            body,
            config
        );

        dispatch({
            type: LOGIN_SUCCESS,
            payload: res.data,
        });

        dispatch(load_user());
    } catch (err) {
        dispatch({
            type: LOGIN_FAIL,
            payload: err.response.data.message || "Login failed",
        });
    }
};

export const checkAuthenticated = () => async (dispatch) => {
    if (localStorage.getItem("token")) {
        const config = {
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
            },
        };

        const body = JSON.stringify({ token: localStorage.getItem("token") });

        try {
            const res = await axios.post(
                "http://localhost:8000/auth/verify",
                body,
                config
            );

            if (res.data.code !== "token_not_valid") {
                dispatch({
                    type: AUTHENTICATED_SUCCESS,
                });
            } else {
                dispatch({
                    type: AUTHENTICATED_FAIL,
                });
            }
        } catch (err) {
            dispatch({
                type: AUTHENTICATED_FAIL,
            });
        }
    } else {
        dispatch({
            type: AUTHENTICATED_FAIL,
        });
    }
};

export const verify = (token) => async (dispatch) => {
    const config = {
        headers: {
            "Content-Type": "application/json",
        },
    };

    const body = JSON.stringify({ token });

    try {
        await axios.post(`http://localhost:8000/auth/activation`, body, config);

        dispatch({
            type: ACTIVATION_SUCCESS,
        });
    } catch (err) {
        dispatch({
            type: ACTIVATION_FAIL,
        });
    }
};

export const reset_password = (email) => async (dispatch) => {
    const config = {
        headers: {
            "Content-Type": "application/json",
        },
    };

    const body = JSON.stringify({ email });

    try {
        await axios.post(
            `http://localhost:8000/auth/reset_password/`,
            body,
            config
        );

        dispatch({
            type: PASSWORD_RESET_SUCCESS,
        });
    } catch (err) {
        dispatch({
            type: PASSWORD_RESET_FAIL,
        });
    }
};

export const reset_password_confirm =
    (uid, token, new_password, re_new_password) => async (dispatch) => {
        const config = {
            headers: {
                "Content-Type": "application/json",
            },
        };

        const body = JSON.stringify({
            uid,
            token,
            new_password,
            re_new_password,
        });

        try {
            await axios.post(
                `http://localhost:8000/auth/reset_password_confirm/`,
                body,
                config
            );

            dispatch({
                type: PASSWORD_RESET_CONFIRM_SUCCESS,
            });
        } catch (err) {
            dispatch({
                type: PASSWORD_RESET_CONFIRM_FAIL,
            });
        }
    };

export const logout = () => (dispatch) => {
    dispatch({
        type: LOGOUT,
    });
};
