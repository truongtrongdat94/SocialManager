import { useMemo } from "react";
import { useSearchParams } from "react-router-dom";

export default function Failed() {
    const [searchParams] = useSearchParams();

    const reason = useMemo(() => {
        const raw = searchParams.get("reason");
        if (!raw) {
            return "Kết nối thất bại. Vui lòng thử lại.";
        }
        return raw;
    }, [searchParams]);

    return (
        <div style={{ padding: "2rem" }}>
            <h1>Social Manager</h1>
            <p>{reason}</p>
            <button onClick={() => window.location.href = "/login"}>Back to Login</button>
        </div>
    );
}
