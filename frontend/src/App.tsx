import { createBrowserRouter, RouterProvider, Navigate, redirect } from "react-router";
import { AuthLayout, DashboardLayout } from "@/layouts";
import Success from "@/pages/Success";
import Failed from "@/pages/Failed.tsx";
import AuthCallback from "@/pages/AuthCallback";
import { Accounts, Post, Stats, SetupGuide, Login, Register } from "@/pages";
import { Modal } from "@/components";
import { getValidToken } from "@/utils";

const requireAuth = () => {
	if (!getValidToken()) {
		throw redirect("/login");
	}
	return null;
};

const router = createBrowserRouter([
	{
		element: <AuthLayout />,
		children: [
			{ path: "/login", element: <Login /> },
			{ path: "/register", element: <Register /> },
		],
	},
	{ path: "/auth/callback", element: <AuthCallback /> },
	{
		path: "/dashboard",
		element: <DashboardLayout />,
		loader: requireAuth,
		children: [
			{ path: "accounts", element: <Accounts /> },
			{ path: "post", element: <Post /> },
			{ path: "stats", element: <Stats /> },
			{ path: "setup", element: <SetupGuide /> },
		],
	},
	{ path: "/success", element: <Success /> },
	{ path: "/failed", element: <Failed /> },
	{ path: "*", element: <Navigate to="/dashboard/accounts" replace /> },
]);

function App() {
	return (
		<>
			<RouterProvider router={router} />

			<Modal />
		</>
	);
}

export default App;