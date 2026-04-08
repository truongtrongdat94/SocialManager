import api from "../api/axios";

export default function Login() {
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
            <p>Login page — coming soon</p>

            <button onClick={handleFacebookLogin}>Login with Facebook</button>
            <button onClick={handleTikTokLogin}>Login with TikTok</button>
        </div>
    );
}
