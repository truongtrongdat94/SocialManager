export const isTokenValid = (token: string | null): boolean => {
	if (!token) return false;
	const normalized = token.trim().toLowerCase();
	return normalized !== "" && normalized !== "null" && normalized !== "undefined";
};

export const getValidToken = (): string | null => {
	const token = localStorage.getItem("token");
	return isTokenValid(token) ? token : null;
};
