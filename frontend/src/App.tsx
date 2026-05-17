import { createBrowserRouter, RouterProvider, Navigate } from "react-router";
import { Toaster, toast, ToastBar } from "react-hot-toast";
import { AuthLayout, DashboardLayout } from "@/layouts";
import Failed from "@/pages/Failed.tsx";
import AuthCallback from "@/pages/AuthCallback";
import { Accounts, Post, Stats, SetupGuide, Login, Register } from "@/pages";
import { Modal, ProtectedRoute } from "@/components";

const router = createBrowserRouter([
	{
		element: <AuthLayout />,
		children: [
			{ path: "/login", element: <Login /> },
			{ path: "/register", element: <Register /> },
			{ path: "/auth/callback", element: <AuthCallback /> },
		],
	},
	{
		path: "/dashboard",
		element: (
			<ProtectedRoute>
				<DashboardLayout />
			</ProtectedRoute>
		),
		children: [
			{ path: "accounts", element: <Accounts /> },
			{ path: "post", element: <Post /> },
			{ path: "stats", element: <Stats /> },
			{ path: "setup", element: <SetupGuide /> },
		],
	},
	{ path: "/", element: <Navigate to="/dashboard/accounts" replace /> },
	{ path: "*", element: <Navigate to="/dashboard/accounts" replace /> },
]);

function App() {
	return (
		<>
			<RouterProvider router={router} />
			<Toaster
				position="top-right"
				toastOptions={{
					duration: 4000,
					style: {
						background: '#363636',
						color: '#fff',
					},
					success: {
						duration: 3000,
						iconTheme: {
							primary: '#10b981',
							secondary: '#fff',
						},
					},
					error: {
						duration: 4000,
						iconTheme: {
							primary: '#ef4444',
							secondary: '#fff',
						},
					},
				}}
			>
				{(t) => (
					<div
						onClick={() => toast.dismiss(t.id)}
						style={{ cursor: 'pointer' }}
					>
						<ToastBar toast={t} />
					</div>
				)}
			</Toaster>
			<Modal />
		</>
	);
}

export default App;