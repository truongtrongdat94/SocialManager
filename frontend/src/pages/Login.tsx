import api from "../api/axios";
import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

export default function Login() {
    type Platform = 'FACEBOOK' | 'INSTAGRAM' | 'THREADS' | 'TIKTOK'

    const [form, setForm] = useState({ username: "", password: "" });
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [error, setError] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const login = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setError("");
        setIsLoading(true);

        try {
            const res = await api.post("/auth/login", form);
            const token = res.data.data.token;

            localStorage.setItem("token", token);
            localStorage.setItem("username", form.username);
            api.defaults.headers.common["Authorization"] = `Bearer ${token}`;

            setIsLoggedIn(true);
            navigate("/dashboard");
        } catch (err) {
            setError("Invalid username or password.");
        } finally {
            setIsLoading(false);
        }
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
        <div style={{ padding: "2rem", display: "flex", flexDirection: "column", gap: "0.75rem", maxWidth: "360px" }}>
            <h1>Login</h1>
            <p style={{ margin: 0 }}>Use the local demo account: devuser / devpass123.</p>

            <form onSubmit={login} style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
                <input
                    style={{ width: "100%" }}
                    placeholder="username"
                    autoComplete="username"
                    value={form.username}
                    onChange={(e) => setForm({ ...form, username: e.target.value })}
                />

                <input
                    style={{ width: "100%" }}
                    placeholder="password"
                    type="password"
                    autoComplete="current-password"
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                />

                <button type="submit" style={{ width: "fit-content" }} disabled={isLoading}>
                    {isLoading ? "Logging in..." : "Login"}
                </button>
            </form>

            {error && <div role="alert">{error}</div>}

            {isLoggedIn && (
                <div style={{marginTop: "1rem", display: "flex", flexDirection: "column", gap: "0.5rem"}}>
                    <h2>CONNECT SOCIAL</h2>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin('FACEBOOK')}>
                        Connect Facebook
                    </button>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin('INSTAGRAM')}>
                        Connect Instagram
                    </button>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin('THREADS')}>
                        Connect Threads
                    </button>
                    <button style={{width: "fit-content"}} onClick={() => handleLogin('TIKTOK')}>
                        Connect TikTok
                    </button>
                </div>
            )}
        </div>
    );
}
