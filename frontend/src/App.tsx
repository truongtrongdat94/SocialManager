import { NavLink, Navigate, Outlet, RouterProvider, createBrowserRouter } from "react-router";
import { Toaster } from "react-hot-toast";
import { Login } from "@/pages/login/Login";
import { Register } from "@/pages/register/Register";
import { OAuthCallback } from "@/pages/auth/OAuthCallback";
import AiMediaDashboard from "@/pages/AiMediaDashboard";
import { Post } from "@/pages/post/Post";
import { Accounts } from "@/pages/accounts/Accounts";
import { Stats } from "@/pages/stats/Stats";
// import { AutoPilot } from "@/pages/autopilot/AutoPilot";
import { History } from "@/pages/history/History";

function ProtectedRoute() {
	const token = localStorage.getItem("accessToken");
	if (!token) {
		return <Navigate to="/login" replace />;
	}
	return <Outlet />;
}

function DashboardLayout() {
	const navItems: Array<{ to: string; label: string }> = [
		{ to: "/dashboard/ai", label: "AI Media" },
		{ to: "/dashboard/post", label: "Đăng bài" },
		{ to: "/dashboard/accounts", label: "Tài khoản" },
		{ to: "/dashboard/stats", label: "Thống kê" },
		// { to: "/dashboard/autopilot", label: "Auto Pilot" },
		{ to: "/dashboard/history", label: "Lịch sử" },
	];

	return (
		<div className="min-h-screen bg-gradient-to-br from-[#dff4ff] via-[#edf9ff] to-[#f7fdff] text-sky-950">
			<div className="mx-auto flex max-w-[1500px] gap-6 p-4 md:p-8">
				<aside className="w-full max-w-[300px] rounded-[30px] border border-sky-100/80 bg-white/80 p-5 shadow-[0_14px_36px_rgba(56,146,183,0.18)] backdrop-blur-sm md:p-6">
					<div className="mb-6 rounded-2xl border border-sky-100 bg-gradient-to-br from-white to-[#f1f9ff] p-4">
						<h1 className="text-2xl font-extrabold tracking-tight text-sky-700">SocialManager</h1>
						<p className="mt-1 text-sm text-sky-600/80">Creator Workspace</p>
						<span className="mt-3 inline-flex items-center rounded-full border border-sky-200 bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-700">
							● Online
						</span>
					</div>

					<nav className="flex flex-col gap-2">
						{navItems.map((item) => (
							<NavLink
								key={item.to}
								to={item.to}
								className={({ isActive }) =>
									[
										"group flex items-center gap-3 rounded-2xl border px-4 py-3 text-sm font-semibold transition-all",
										isActive
											? "border-sky-500 bg-sky-500 text-white shadow-[0_10px_22px_rgba(14,165,233,0.35)]"
											: "border-sky-100 bg-white/90 text-sky-700 hover:border-sky-300 hover:bg-sky-50",
									].join(" ")
								}
							>
								<span>{item.label}</span>
							</NavLink>
						))}

						<div className="mt-2 border-t border-sky-100 pt-3">
							<button
								type="button"
								onClick={() => {
									localStorage.removeItem("accessToken");
									localStorage.removeItem("refreshToken");
									window.location.href = "/login";
								}}
								className="w-full rounded-2xl border border-sky-200 bg-white px-4 py-3 text-left text-sm font-semibold text-sky-700 transition hover:border-sky-300 hover:bg-sky-50"
							>
								Đăng xuất
							</button>
						</div>
					</nav>
				</aside>

				<div className="min-w-0 flex-1">
					<main className="rounded-[28px] border border-sky-100/80 bg-white/90 p-4 shadow-[0_14px_32px_rgba(56,146,183,0.12)] md:p-7">
						<Outlet />
					</main>
				</div>
			</div>
		</div>
	);
}

const router = createBrowserRouter([
	{ path: "/login", element: <Login /> },
	{ path: "/register", element: <Register /> },
	{ path: "/auth/callback", element: <OAuthCallback /> },
	{
		element: <ProtectedRoute />,
		children: [
			{
				path: "/dashboard",
				element: <DashboardLayout />,
				children: [
					{ path: "ai", element: <AiMediaDashboard /> },
					{ path: "post", element: <Post /> },
					{ path: "accounts", element: <Accounts /> },
					{ path: "stats", element: <Stats /> },
					// { path: "autopilot", element: <AutoPilot /> },
					{ path: "history", element: <History /> },
					{ path: "", element: <Navigate to="/dashboard/ai" replace /> },
				],
			},
		],
	},
	{ path: "/", element: <Navigate to="/dashboard/ai" replace /> },
	{ path: "*", element: <Navigate to="/dashboard/ai" replace /> },
]);

export default function App() {
	return (
		<>
			<RouterProvider router={router} />
			<Toaster
				position="top-right"
				toastOptions={{
					style: {
						background: "#e0f4ff",
						color: "#0c4a6e",
						border: "1px solid #7dd3fc",
						borderRadius: "12px",
						padding: "10px 14px",
						boxShadow: "0 8px 18px rgba(14, 116, 144, 0.18)",
						fontWeight: 600,
					},
					success: {
						style: {
							background: "#dcfce7",
							color: "#166534",
							border: "1px solid #86efac",
						},
						iconTheme: {
							primary: "#16a34a",
							secondary: "#dcfce7",
						},
					},
					error: {
						style: {
							background: "#e0f2fe",
							color: "#0c4a6e",
							border: "1px solid #7dd3fc",
						},
						iconTheme: {
							primary: "#0284c7",
							secondary: "#e0f2fe",
						},
					},
				}}
			/>
		</>
	);
}
