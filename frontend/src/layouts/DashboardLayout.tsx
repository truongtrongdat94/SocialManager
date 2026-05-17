import { Button, Dropdown, type DropdownZone } from "@/components";
<<<<<<< HEAD
import { EllipsisVertical, Globe, Send, ChartColumn, LogOut, Pencil, User, Wrench } from "lucide-react";
import { useEffect } from "react";
import { Outlet, useLocation, useNavigate } from "react-router";
import { cn } from "@/utils";
import { getValidToken } from "@/utils/auth";
=======
import { EllipsisVertical, Globe, Send, ChartColumn, LogOut, Pencil, User } from "lucide-react";
import { Outlet, useLocation, useNavigate } from "react-router";
import { cn } from "@/utils";
import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import AuthApi from "@/apis/auth.api.ts";
>>>>>>> upstream/dev

const sideBarButtons = [
	{
		title: "Danh sách tài khoản",
		path: "accounts",
		icon: Globe,
	},
	{
		title: "Đăng bài",
		path: "post",
		icon: Send,
	},
	{
		title: "Thống kê",
		path: "stats",
		icon: ChartColumn,
	},
<<<<<<< HEAD
	{
		title: "Cấu hình",
		path: "setup",
		icon: Wrench,
	},
=======
>>>>>>> upstream/dev
];

export const DashboardLayout = () => {
	const { pathname } = useLocation();
	const navigate = useNavigate();
<<<<<<< HEAD

	useEffect(() => {
		const token = getValidToken();
		if (!token) {
			navigate("/login", { replace: true });
		}
	}, [navigate]);

	const handleLogout = () => {
		localStorage.removeItem("token");
		localStorage.removeItem("username");
		navigate("/login", { replace: true });
	};

	const isActive = (path: string) => pathname.includes(path);
	const profileActions: DropdownZone[] = [
=======
	const isActive = (path: string) => pathname.includes(path);
	const [user, setUser] = useState<{ name: string; username: string } | null>(null);

	useEffect(() => {
		// Lấy thông tin user khi component mount
		const fetchUser = async () => {
			try {
				const response = await AuthApi.getMe();
				setUser({
					name: response.data.data.name,
					username: response.data.data.username,
				});
			} catch (error) {
				console.error("Failed to fetch user:", error);
			}
		};

		fetchUser();
	}, []);

	const handleLogout = () => {
		localStorage.removeItem("token");
		toast.success("Đã đăng xuất");
		navigate("/login");
	};

	const actionZones: DropdownZone[] = [
>>>>>>> upstream/dev
		{
			actions: [
				{
					label: "Chỉnh sửa thông tin",
					icon: <Pencil size={16} strokeWidth={1.5} />,
					variant: "info",
					onClick: () => {},
				},
				{
					label: "Đăng xuất",
					icon: <LogOut size={16} strokeWidth={1.5} />,
					variant: "danger",
					onClick: handleLogout,
				},
			],
		}
	];

	return (
		<div className="flex h-screen w-full text-text-primary bg-bg p-4 gap-8">
			<div className="flex flex-col bg-surface-primary p-4 shadow-card rounded-xl w-64">
				<div className="flex justify-between items-center border-b border-border pb-4 mb-4">
					<div className="flex gap-2 items-center">
						<div className="font-semibold text-sm text-white w-10 h-10 flex justify-center items-center bg-accent rounded-md">SM</div>
						<div className="text-base font-bold">SocialManager</div>
					</div>
					<EllipsisVertical size={18} strokeWidth={1} className="translate-x-2 text-text-secondary" />
				</div>
				<div className="flex flex-col gap-2">
					{sideBarButtons.map(({ title, path, icon: Icon }) => {
						const active = isActive(path);
						return (
						<Button
							key={path}
							onClick={() => navigate(path)}
							variant={active ? "solid" : "outline"}
							color={active ? "primary" : "default"}
							disableHover={active}
							className="justify-start gap-3 px-2 border-none"
						>
							<Icon
								size={16}
								strokeWidth={isActive(path) ? 1.75 : 1.5}
							/>
							<div className={cn(active ? "font-medium" : "font-normal")}>{title}</div>
						</Button>
					)})}
				</div>
				<div className="mt-auto w-full">
					<Dropdown
						width="w-full"
						position="top"
						trigger={
							<Button variant="outline" className="flex justify-between w-full rounded-lg gap-2 h-fit p-2">
								<div className="w-10 h-10 rounded-full bg-gray-100 flex justify-center items-center shrink-0">
									<User strokeWidth={1.5} size={20} />
								</div>
								<div className="flex flex-col min-w-0 flex-1">
<<<<<<< HEAD
									<div className="text-left font-semibold truncate">Tên hiển thị</div>
									<div className="text-sm text-left text-text-secondary">@username</div>
								</div>
							</Button>
						}
						zones={profileActions}
=======
									<div className="text-left font-semibold truncate">{user?.name || "Loading..."}</div>
									<div className="text-sm text-left text-text-secondary">@{user?.username || "..."}</div>
								</div>
							</Button>
						}
						zones={actionZones}
>>>>>>> upstream/dev
					/>
				</div>

			</div>
			<div className="flex-1 min-w-0 min-h-0">
				<Outlet />
			</div>
		</div>
	);
};