<<<<<<< HEAD
<<<<<<< HEAD
import { useEffect } from "react";
=======
>>>>>>> upstream/dev
=======
import { useEffect } from "react";
import { useSearchParams } from "react-router";
import toast from "react-hot-toast";
>>>>>>> 3445fc14962b3c9fae27fefc3d4a774127a5382e
import { Button } from "@/components";
import { useSocialAccountStore, useModalStore } from "@/stores";
import { AddSocialAccountModal } from "./components/AddSocialAccountModal.tsx";
import { DeleteSocialAccountModal } from "./components/DeleteSocialAccountModal.tsx";

import {
	icons,
	platforms
} from "@/constants";

import {
	RefreshCw,
	Trash2
} from "lucide-react";

export const Accounts = () => {
<<<<<<< HEAD
<<<<<<< HEAD
	const AccountAvatar = ({ src, platform, alt }: { src?: string; platform: string; alt?: string }) => {
		const fallback = icons[platform.toLowerCase()];
		return (
			<img
				src={src || fallback}
				alt={alt}
				className="h-10 w-10 rounded-full object-cover"
				onError={(event) => {
					const target = event.currentTarget;
					target.onerror = null;
					target.src = fallback;
				}}
			/>
		);
	};
	const accounts = useSocialAccountStore(
		(state) => state.accounts
	);
	const loading = useSocialAccountStore((state) => state.loading);
	const fetchAccounts = useSocialAccountStore((state) => state.fetchAccounts);
=======
	const accounts = useSocialAccountStore(
		(state) => state.accounts
	);
>>>>>>> upstream/dev
=======
	const accounts = useSocialAccountStore((s) => s.accounts);
	const fetchAccounts = useSocialAccountStore((s) => s.fetchAccounts);
	const openModal = useModalStore((s) => s.open);
>>>>>>> 3445fc14962b3c9fae27fefc3d4a774127a5382e

	const [searchParams, setSearchParams] = useSearchParams();

<<<<<<< HEAD
<<<<<<< HEAD
	useEffect(() => {
		fetchAccounts();
	}, [fetchAccounts]);

=======
>>>>>>> upstream/dev
	const handleReconnect = (
		id: string
	) => {
		console.log("Reconnect:", id);
	};
=======
	useEffect(() => {
		const status = searchParams.get("status");
>>>>>>> 3445fc14962b3c9fae27fefc3d4a774127a5382e

		if (status === "success") {
			toast.success("Kết nối tài khoản thành công!");
		} else if (status === "error") {
			toast.error("Đã xảy ra lỗi khi kết nối!");
		}

		if (status) {
			searchParams.delete("status");
			setSearchParams(searchParams, { replace: true });
		}

		void fetchAccounts();
	}, []);

	return (
		<div className="flex h-full flex-col gap-4">
			<div className="flex justify-between">
				<div className="flex flex-col gap-1">
					<div className="text-2xl font-bold">
						Danh sách tài khoản
					</div>
					<div className="text-text-secondary text-sm">Quản lý các tài khoản mạng xã hội đã kết nối</div>
				</div>

				<Button
					variant="solid"
					color="primary"
					onClick={() =>
						openModal(
							"Thêm tài khoản",
							<AddSocialAccountModal/>
						)
					}
				>
					Thêm tài khoản
				</Button>
			</div>

			<Button
				variant="outline"
				color="primary"
				className="w-fit"
				onClick={fetchAccounts}
			>
				Tải lại
			</Button>

			<div className="max-h-full overflow-y-auto rounded-xl border border-border bg-surface-primary shadow-card">
				<table className="w-full border-collapse">
					<thead className="sticky top-0 bg-surface-secondary">
						<tr className="border-b border-border text-left">
							<th className="px-4 py-3">Tài khoản</th>
							<th className="px-4 py-3">Nền tảng</th>
							<th className="px-4 py-3">Hành động</th>
						</tr>
					</thead>

					<tbody>
<<<<<<< HEAD
						{loading ? (
							<tr>
								export const Accounts = () => {
									const AccountAvatar = ({ src, platform, alt }: { src?: string; platform: string; alt?: string }) => {
										const fallback = icons[platform.toLowerCase()];
										return (
											<img
												src={src || fallback}
												alt={alt}
												className="h-10 w-10 rounded-full object-cover"
												onError={(event) => {
													const target = event.currentTarget as HTMLImageElement;
													target.onerror = null;
													target.src = fallback;
												}}
											/>
										);
									};

									const accounts = useSocialAccountStore((s) => s.accounts);
									const loading = useSocialAccountStore((s) => s.loading);
									const fetchAccounts = useSocialAccountStore((s) => s.fetchAccounts);
									const openModal = useModalStore((s) => s.open);

									const [searchParams, setSearchParams] = useSearchParams();

									useEffect(() => {
										const status = searchParams.get("status");

										if (status === "success") {
											toast.success("Kết nối tài khoản thành công!");
										} else if (status === "error") {
											toast.error("Đã xảy ra lỗi khi kết nối!");
										}

										if (status) {
											searchParams.delete("status");
											setSearchParams(searchParams, { replace: true });
										}

										void fetchAccounts();
									}, []);

									const handleReconnect = (id: string) => {
										console.log("Reconnect:", id);
									};

									const handleDelete = (id: string, name?: string) => {
										openModal(
											"Xác nhận xoá",
											<DeleteSocialAccountModal accountId={id} accountName={name} />,
										);
									};

									return (
										<div className="flex h-full flex-col gap-4">
											<div className="flex justify-between">
												<div className="flex flex-col gap-1">
													<div className="text-2xl font-bold">Danh sách tài khoản</div>
													<div className="text-text-secondary text-sm">Quản lý các tài khoản mạng xã hội đã kết nối</div>
												</div>

												<Button
													variant="solid"
													color="primary"
													onClick={() =>
														openModal("Thêm tài khoản", <AddSocialAccountModal />)
													}
												>
													Thêm tài khoản
												</Button>
											</div>

											<Button variant="outline" color="primary" className="w-fit" onClick={fetchAccounts}>
												Tải lại
											</Button>

											<div className="max-h-full overflow-y-auto rounded-xl border border-border bg-surface-primary shadow-card">
												<table className="w-full border-collapse">
													<thead className="sticky top-0 bg-surface-secondary">
														<tr className="border-b border-border text-left">
															<th className="px-4 py-3">Tài khoản</th>
															<th className="px-4 py-3">Nền tảng</th>
															<th className="px-4 py-3">Hành động</th>
														</tr>
													</thead>

													<tbody>
														{loading ? (
															<tr>
																<td colSpan={3} className="px-4 py-8 text-center text-text-secondary">
																	Đang tải...
																</td>
															</tr>
														) : accounts.length === 0 ? (
															<tr>
																<td colSpan={3} className="px-4 py-8 text-center text-text-secondary">
																	Chưa có tài khoản nào. Hãy thêm tài khoản mới.
																</td>
															</tr>
														) : (
															accounts.map((account) => (
																<tr key={account.id} className="border-b border-border transition-colors hover:bg-surface-secondary">
																	<td className="px-4 py-3">
																		<div className="flex items-center gap-2">
																			<AccountAvatar src={account.profilePictureUrl} platform={account.platform} alt={account.accountName} />

																			<div className="font-medium">{account.accountName}</div>
																		</div>
																	</td>

																	<td className="px-4 py-3">
																		<div className="flex items-center gap-2">
																			<img className="h-6 w-6" src={icons[account.platform.toLowerCase()]} alt={account.platform} />
																			<div>{platforms.find((p) => p.toUpperCase() === account.platform)}</div>
																		</div>
																	</td>

																	<td className="px-4 py-3">
																		<div className="flex items-center gap-2">
																			<Button title="Kết nối lại" variant="soft" color="primary" className="h-8 w-8 p-0" onClick={() => handleReconnect(account.id)}>
																				<RefreshCw size={16} strokeWidth={1.5} />
																			</Button>

																			<Button title="Xoá" variant="soft" color="danger" className="h-8 w-8 p-0" onClick={() => handleDelete(account.id, account.accountName)}>
																				<Trash2 size={16} strokeWidth={1.5} />
																			</Button>
																		</div>
																	</td>
																</tr>
															))
														)}
													</tbody>
												</table>
											</div>
										</div>
									);
								};