import { useEffect } from "react";
import { useSearchParams } from "react-router";
import toast from "react-hot-toast";
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
	const accounts = useSocialAccountStore((s) => s.accounts);
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
						{accounts.map((account) => (
							<tr
								key={account.id}
								className="border-b border-border transition-colors hover:bg-surface-secondary"
							>
								<td className="px-4 py-3">
									<div className="flex items-center gap-2">
										<img src={account.profilePictureUrl} alt="" className="h-10 w-10 rounded-full object-cover"/>

										<div className="font-medium">
											{account.accountName}
										</div>
									</div>
								</td>

								<td className="px-4 py-3">
									<div className="flex items-center gap-2">
										<img className="h-6 w-6" src={icons[account.platform.toLowerCase()]} alt={account.platform}/>
										<div>
											{platforms.find((platform) => platform.toUpperCase() === account.platform)}
										</div>
									</div>
								</td>

								<td className="px-4 py-3">
									<div className="flex items-center gap-2">
										<Button title="Kết nối lại" variant="soft" color="primary" className="h-8 w-8 p-0">
											<RefreshCw size={16} strokeWidth={1.5}/>
										</Button>

										<Button title="Xoá" variant="soft" color="danger" className="h-8 w-8 p-0"
										        onClick={() =>
											        openModal(
												        "Xác nhận xoá",
												        <DeleteSocialAccountModal accountId={account.id} accountName={account.accountName}/>
											        )
										        }>
											<Trash2 size={16} strokeWidth={1.5}/>
										</Button>
									</div>
								</td>
							</tr>
						))}
					</tbody>
				</table>
			</div>
		</div>
	);
};