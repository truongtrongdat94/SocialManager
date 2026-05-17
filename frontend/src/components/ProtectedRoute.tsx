import { Navigate } from "react-router";
import { getValidToken } from "@/utils/auth";

interface ProtectedRouteProps {
	children: React.ReactNode;
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
	const token = getValidToken();

	if (!token) {
		return <Navigate to="/login" replace />;
	}

	return <>{children}</>;
}
