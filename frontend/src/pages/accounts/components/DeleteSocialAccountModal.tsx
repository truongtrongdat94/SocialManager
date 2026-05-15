import { Button } from "@/components";
import { useSocialAccountStore, useModalStore } from "@/stores";

interface DeleteSocialAccountModalProps {
	accountId: string;
	accountName: string;
}

export const DeleteSocialAccountModal = ({ accountId, accountName }: DeleteSocialAccountModalProps) => {
	const deleteAccount = useSocialAccountStore((s) => s.deleteAccount);
	const closeModal = useModalStore((s) => s.close);

	const handleDelete = async () => {
		await deleteAccount(accountId);
		closeModal();
	};

	return (
		<div className="flex flex-col gap-6 max-w-136">
			<div className="text-text-secondary">
				Bạn có chắc chắn muốn xoá kết nối với tài khoản <span className="font-bold text-text-primary">{accountName}</span> không? Hành động này không thể hoàn tác.
			</div>
			<div className="flex items-center justify-end gap-3">
				<Button
					variant="soft"
					color="default"
					onClick={closeModal}
				>
					Huỷ
				</Button>
				<Button
					variant="solid"
					color="danger"
					onClick={handleDelete}
				>
					Xoá tài khoản
				</Button>
			</div>
		</div>
	);
};