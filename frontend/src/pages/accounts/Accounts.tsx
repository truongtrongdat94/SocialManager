import { Button } from "@/components";
import { useSocialAccountStore, useModalStore } from "@/stores";
import { AddSocialAccountModal } from "./components/AddSocialAccountModal.tsx";

import {
	icons,
	platforms
} from "@/constants";

import {
	RefreshCw,
	Trash2
} from "lucide-react";

export const Accounts = () => {
	const accounts = useSocialAccountStore(
		(state) => state.accounts
	);

	const openModal = useModalStore(
		(state) => state.open
	);

	const handleReconnect = (
		id: string
	) => {
		console.log("Reconnect:", id);
	};

	const handleDelete = (id: string) => {
		console.log("Delete:", id);
	};

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
										<img src={account.profilePictureUrl} alt={account.accountName} className="h-10 w-10 rounded-full object-cover"/>

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
										<Button title="Kết nối lại" variant="soft" color="primary" className="h-8 w-8 p-0"
										        onClick={() => handleReconnect(account.id)}>
											<RefreshCw size={16} strokeWidth={1.5}/>
										</Button>

										<Button title="Xoá" variant="soft" color="danger" className="h-8 w-8 p-0" onClick={() => handleDelete(account.id)}
										>
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