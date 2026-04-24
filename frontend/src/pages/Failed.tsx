export default function Failed() {
    return (
        <div style={{ padding: "2rem" }}>
            <h1>Social Manager</h1>
            <p>FAILED FAILED FAILED</p>
            <button onClick={() => window.location.href = "/login"}>Back to Login</button>
        </div>
    );
}
