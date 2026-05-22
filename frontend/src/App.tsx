import { NavLink, Navigate, Outlet, RouterProvider, createBrowserRouter } from "react-router";
import { Toaster } from "react-hot-toast";
import { Login } from "@/pages/login/Login";
import { Register } from "@/pages/register/Register";
import { OAuthCallback } from "@/pages/auth/OAuthCallback";
import AiMediaDashboard from "@/pages/AiMediaDashboard";
import { Post } from "@/pages/post/Post";
import { PostHistory } from "@/pages/post-history/PostHistory";
import { Accounts } from "@/pages/accounts/Accounts";
import { Stats } from "@/pages/stats/Stats";
import { AutoPilot } from "@/pages/autopilot/AutoPilot";
import { History } from "@/pages/history/History";

function ProtectedRoute() {
	const token = localStorage.getItem("accessToken");
	if (!token) {
		return <Navigate to="/login" replace />;
	}
	return <Outlet />;
}

function DashboardLayout() {
	const navItems = [
		{ to: "/dashboard/ai", label: "AI Media" },
		{ to: "/dashboard/post", label: "Đăng bài" },
		{ to: "/dashboard/post-history", label: "Lịch sử bài đăng" },
		{ to: "/dashboard/accounts", label: "Tài khoản" },
		{ to: "/dashboard/stats", label: "Thống kê" },
		{ to: "/dashboard/autopilot", label: "🤖 Auto Pilot" },
		{ to: "/dashboard/history", label: "📜 Lịch sử" },
	];

	return (
		<div className="min-h-screen bg-white text-zinc-900">
			<div className="mx-auto flex max-w-[1400px] gap-6 p-4 md:p-8">
				<aside className="w-full max-w-[280px] rounded-[24px] border border-zinc-100 bg-white p-4 md:p-6">
					<div className="mb-6">
						<h1 className="text-2xl font-semibold tracking-tight">SocialManager</h1>
						<p className="mt-1 text-sm text-zinc-500">Meta Flat Workspace</p>
					</div>
					<nav className="flex flex-col gap-2">
						{navItems.map((item) => (
							<NavLink
								key={item.to}
								to={item.to}
								className={({ isActive }) =>
									[
										"rounded-full border px-4 py-2.5 text-sm font-semibold transition",
										isActive
											? "border-zinc-900 bg-zinc-900 text-white"
											: "border-zinc-100 bg-white text-zinc-700",
									].join(" ")
								}
							>
								{item.label}
							</NavLink>
						))}
						<button
							type="button"
							onClick={() => {
								localStorage.removeItem("accessToken");
								localStorage.removeItem("refreshToken");
								window.location.href = "/login";
							}}
							className="mt-4 rounded-full border border-zinc-100 px-4 py-2.5 text-left text-sm font-semibold text-zinc-700"
						>
							Đăng xuất
						</button>
					</nav>
				</aside>

				<main className="min-w-0 flex-1 rounded-[24px] border border-zinc-100 bg-white p-4 md:p-8">
					<Outlet />
				</main>
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
					{ path: "post-history", element: <PostHistory /> },
					{ path: "accounts", element: <Accounts /> },
					{ path: "stats", element: <Stats /> },
					{ path: "autopilot", element: <AutoPilot /> },
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
