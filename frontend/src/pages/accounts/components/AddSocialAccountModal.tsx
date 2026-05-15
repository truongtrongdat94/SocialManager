import { icons, platforms } from "@/constants";
import { Button } from "@/components";
import { useSocialAccountStore } from "@/stores";

export const AddSocialAccountModal = () => {
	const connectAccount = useSocialAccountStore((s) => s.connectAccount);

	const handleConnect = async (platform: string) => {
		await connectAccount(platform.toUpperCase() as any);
	};

	return (
		<div className="flex flex-col gap-4">
			<div>Chọn nền tảng</div>
			<div className="grid grid-cols-2 gap-2">
				{
					platforms.map((platform) => (
							<Button
								variant="soft"
								className="flex flex-col items-center gap-2 px-24 py-8 cursor-pointer h-fit"
								key={platform}
								onClick={() => handleConnect(platform)}
							>
								<img src={icons[platform.toLowerCase()]} alt={platform} className="w-12 h-12"/>
								<div>{platform}</div>
							</Button>
						)
					)
				}
			</div>
		</div>
	);
};