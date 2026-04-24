import api from "../api/axios";
import { useState, useEffect } from "react";

export default function Login() {
    enum Platform {
        FACEBOOK = "FACEBOOK",
        INSTAGRAM = "INSTAGRAM",
        THREADS = "THREADS",
        TIKTOK = "TIKTOK",
    }

    const [form, setForm] = useState({ username: "", password: "" });
    const [isLoggedIn, setIsLoggedIn] = useState(false);

    const login = async () => {
        const res = await api.post("/auth/login", form);
        const token = res.data.data.token;

        localStorage.setItem("token", token);
        api.defaults.headers.common["Authorization"] = `Bearer ${token}`;

        setIsLoggedIn(true)
        alert("Logged in!");
    };

    const handleLogin = async (platform: Platform) => {
        try {
            const res = await api.get(`/social-accounts/connect/${platform}`);
            window.location.href = res.data.data;
        } catch (err) {
            console.error(err);
            alert("Login failed");
        }
    };

    useEffect(() => {
        const token = localStorage.getItem("token");
        if (token) {
            setIsLoggedIn(true);

            // set lại header (quan trọng nếu reload page)
            api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
        }
    }, []);

    return (
        <div style={{ padding: "2rem", display: "flex", flexDirection: "column", gap: "0.5rem" }}>
            <h1>Login</h1>

            <div>

            </div>

            <input
                style={{width: "fit-content"}}
                placeholder="username"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
            />

            <input
                style={{width: "fit-content"}}
                placeholder="password"
                type="password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
            />

            <button style={{width: "fit-content"}} onClick={login}>Login</button>

            {isLoggedIn && (
                <div style={{marginTop: "1rem", display: "flex", flexDirection: "column", gap: "0.5rem"}}>
                    <h2>CONNECT SOCIAL</h2>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin(Platform.FACEBOOK)}>
                        Connect Facebook
                    </button>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin(Platform.INSTAGRAM)}>
                        Connect Instagram
                    </button>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin(Platform.THREADS)}>
                        Connect Threads
                    </button>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin(Platform.TIKTOK)}>
                        Connect TikTok
                    </button>
                </div>
            )}
        </div>
    );
}