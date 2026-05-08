import { Card } from "@/components";
import { icons } from "@/constants";

export const AccountSelection = ({ accounts, selectedIds, onToggle }) => (
	<Card className="flex flex-col gap-4 flex-4 h-full">
		<div className="font-semibold">Chọn tài khoản</div>
		<div className="flex-1">
			{accounts.length > 0 ? (
				<div className="flex flex-col gap-4 overflow-y-auto">
					{accounts.map((account) => (
						<div className="flex items-center justify-between" key={account.id}>
							<div className="flex items-center gap-2">
								<div className="relative">
									<img className="w-10 h-10 rounded-full" src={account.profilePictureUrl} alt="" />
									<img className="w-4 h-4 absolute bottom-0 right-0" src={icons[account.platform.toLowerCase()]} alt="" />
								</div>
								<div>{account.accountName}</div>
							</div>
							<input
								type="checkbox"
								checked={selectedIds.includes(account.id)}
								onChange={() => onToggle(account.id)}
								className="cursor-pointer"
							/>
						</div>
					))}
				</div>
			) : (
				<div className="text-text-secondary">Bạn chưa kết nối tài khoản mạng xã hội nào</div>
			)}
		</div>
		<div className="text-text-secondary text-sm">
			<span className="font-semibold">{selectedIds.length}/{accounts.length}</span> tài khoản đã chọn
		</div>
	</Card>
);