import { useState } from "react";
import { useNavigate } from "react-router";
import { Button, Input } from "@/components";
import {icons} from "@/constants/icons.ts";
import { Eye, EyeClosed } from "lucide-react";
// import axios from "../libs/axios";
// import { useState, useEffect } from "react";

export function Login() {
	// enum Platform {
	//     FACEBOOK = "FACEBOOK",
	//     INSTAGRAM = "INSTAGRAM",
	//     THREADS = "THREADS",
	//     TIKTOK = "TIKTOK",
	// }
	//
	// const [form, setForm] = useState({ username: "", password: "" });
	// const [isLoggedIn, setIsLoggedIn] = useState(false);
	//
	// const login = async () => {
	//     const res = await axios.post("/auth/login", form);
	//     const token = res.data.data.token;
	//
	//     localStorage.setItem("token", token);
	//     axios.defaults.headers.common["Authorization"] = `Bearer ${token}`;
	//
	//     setIsLoggedIn(true)
	//     alert("Logged in!");
	// };
	//
	// const handleLogin = async (platform: Platform) => {
	//     try {
	//         const res = await axios.get(`/social-accounts/connect/${platform}`);
	//         window.location.href = res.data.data;
	//     } catch (err) {
	//         console.error(err);
	//         alert("Login failed");
	//     }
	// };
	//
	// useEffect(() => {
	//     const token = localStorage.getItem("token");
	//     if (token) {
	//         setIsLoggedIn(true);
	//
	//         // set lại header (quan trọng nếu reload page)
	//         axios.defaults.headers.common["Authorization"] = `Bearer ${token}`;
	//     }
	// }, []);

	const [showPassword, setShowPassword] = useState(false);
	const navigate = useNavigate();

	return (
		<div className="flex flex-col justify-center items-center flex-1 w-100 mx-auto">
			<div className="text-xl font-bold mb-8">Đăng nhập</div>
			<div className="w-full flex flex-col items-center gap-6">
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Username</div>
					<Input className="flex-1">
					</Input>
				</div>
				<div className="flex flex-col gap-2 w-full">
					<div className="font-medium">Mật khẩu</div>
					<Input iconButtonFunction={() =>
						setShowPassword((prev) => !prev)
					} iconPosition="right" type="password" className="flex-1">
						{!showPassword ? (
							<Eye size={20} strokeWidth={1.5}/>
						) : (
							<EyeClosed size={20} strokeWidth={1.5}/>
						)}
					</Input>
				</div>
				<Button variant="solid" color="primary" size="lg" className="w-full">
					Đăng nhập
				</Button>
				<a className="cursor-pointer text-accent font-medium">Quên mật khẩu?</a>
				<div className="relative flex items-center justify-center w-full">
					<div className="h-px flex-1 bg-divider"/>
					<span className="px-3 text-sm">
				        Hoặc
				    </span>
					<div className="h-px flex-1 bg-divider"/>
				</div>
				<a className="cursor-pointer"><img src={icons.google} alt="google" className="w-6 h-6"/></a>
				<div>
					<span>Chưa có tài khoản?&nbsp;</span>
					<a onClick={() => navigate("/register")} className="text-accent font-medium cursor-pointer">Đăng ký ngay</a>
				</div>
			</div>
		</div>
	);
}

/*
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

 */