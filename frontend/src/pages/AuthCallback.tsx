import { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

export default function AuthCallback() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    useEffect(() => {
        const token = searchParams.get("token");

        if (token) {
            localStorage.setItem("token", token);
            navigate("/dashboard", { replace: true });
            return;
        }

        navigate("/failed", { replace: true });
    }, [navigate, searchParams]);

    return <div style={{ padding: "2rem" }}>Signing you in...</div>;
}