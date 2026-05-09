import { useState, useEffect } from "react";
import { Button } from "@/components";
import { useSocialAccountStore, useModalStore } from "@/stores";
import { AccountSelection, SchedulePost, CaptionInput, MediaAttachment } from "./components";

interface MediaFile {
	id: string;
	file: File;
	previewUrl: string;
	type: "image" | "video";
}


export const Post = () => {
	const accounts = useSocialAccountStore((state) => state.accounts);
	const openModal = useModalStore((state) => state.open);

	const [selectedAccountIds, setSelectedAccountIds] = useState<string[]>([]);
	const [isDatePick, setIsDatePick] = useState(false);
	const [postDate, setPostDate] = useState<Date | undefined>(new Date());
	const [hour, setHour] = useState(6);
	const [minute, setMinute] = useState(50);
	const [caption, setCaption] = useState("");
	const [mediaFiles, setMediaFiles] = useState<MediaFile[]>([]);

	const handleToggleAccount = (accountId: string) => {
		setSelectedAccountIds((prevSelected) => {
			if (prevSelected.includes(accountId)) {
				return prevSelected.filter((id) => id !== accountId);
			}
			return [...prevSelected, accountId];
		});
	};

	const handleFilesSelect = (files: File[]) => {
		const newMedia: MediaFile[] = files.map(file => ({
			id: crypto.randomUUID(),
			file,
			previewUrl: URL.createObjectURL(file),
			type: file.type.startsWith("video/") ? "video" : "image" as const
		}));
		setMediaFiles(prev => [...prev, ...newMedia]);
	};

	const handleRemoveMedia = (idToRemove: string) => {
		setMediaFiles(prev => {
			const fileToRemove = prev.find(m => m.id === idToRemove);
			if (fileToRemove) {
				URL.revokeObjectURL(fileToRemove.previewUrl);
			}
			return prev.filter(m => m.id !== idToRemove);
		});
	};

	const handlePreviewClick = (media: MediaFile) => {
		openModal(
			"Xem trước tệp đính kèm",
			<div className="flex justify-center items-center w-full max-h-[80vh] overflow-hidden rounded-lg bg-black/5">
				{media.type === "video" ? (
					<video src={media.previewUrl} controls autoPlay className="max-w-full max-h-[75vh] object-contain rounded-lg"/>
				) : (
					<img src={media.previewUrl} alt="preview" className="max-w-full max-h-[75vh] object-contain rounded-lg"/>
				)}
			</div>
		);
	};

	useEffect(() => {
		return () => {
			mediaFiles.forEach(media => URL.revokeObjectURL(media.previewUrl));
		};
	}, []);

	return (
		<div className="flex h-full flex-col gap-4">
			<div className="flex justify-between">
				<div className="flex flex-col gap-1">
					<div className="text-2xl font-bold">Đăng bài</div>
					<div className="text-text-secondary text-sm">Tạo và đăng bài lên các nền tảng</div>
				</div>
				<Button variant="solid" color="primary">Đăng bài</Button>
			</div>

			<div className="flex flex-col h-full gap-4">
				<div className="flex gap-4 flex-4">
					<AccountSelection accounts={accounts} selectedIds={selectedAccountIds} onToggle={handleToggleAccount}/>
					<CaptionInput caption={caption} onChange={setCaption}/>
				</div>

				<div className="flex gap-4 flex-5 min-w-0">
					<SchedulePost
						isDatePick={isDatePick} setIsDatePick={setIsDatePick}
						postDate={postDate} setPostDate={setPostDate}
						hour={hour} setHour={setHour}
						minute={minute} setMinute={setMinute}
					/>
					<MediaAttachment
						mediaFiles={mediaFiles}
						onFilesSelect={handleFilesSelect}
						onRemove={handleRemoveMedia}
						onPreview={handlePreviewClick}
					/>
				</div>
			</div>
		</div>
	);
};