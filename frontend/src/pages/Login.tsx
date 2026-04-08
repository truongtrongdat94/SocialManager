import api from "../../../../SocialManager - Copy/frontend/src/api/axios.ts";
import { useState } from "react";

export default function Login() {
    const [form, setForm] = useState({ username: "", password: "" });

    const login = async () => {
        const res = await api.post("/api/auth/login", form);
        const token = res.data.data.token;

        // lưu JWT
        localStorage.setItem("token", token);

        // set default header cho axios
        api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
        alert("Logged in!");
    };

    const handleFacebookLogin = async () => {
        const res = await api.get("/api/social-accounts/connect/FACEBOOK");
        window.location.href = res.data.data;
    };

    const handleTikTokLogin = async () => {
        const res = await api.get("/api/social-accounts/connect/TIKTOK");
        window.location.href = res.data.data;
    };

    return (
        <div style={{ padding: "2rem" }}>
            <h1>Login</h1>

            <input
                placeholder="username"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
            />

            <input
                placeholder="password"
                type="text"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
            />

            <button onClick={login}>Login</button>

            <hr />

            <button onClick={handleFacebookLogin}>Connect Facebook</button>
            <button onClick={handleTikTokLogin}>Connect TikTok</button>
        </div>
    );
}