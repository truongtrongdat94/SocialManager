import { useRef, useState } from "react";
import { Card, Button, FileInput, Input } from "@/components";
import { Info, Upload, X } from "lucide-react";
import { cn } from "@/utils";

export interface MediaFile {
	id: string;
	file: File;
	previewUrl: string;
	type: "image" | "video";
}

const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const MAX_VIDEO_BYTES = 150 * 1024 * 1024;

const formatBytes = (bytes: number) => {
	if (bytes < 1024) return `${bytes} B`;
	if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
	return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const getSizeLimitByType = (type: "image" | "video") => (type === "image" ? MAX_IMAGE_BYTES : MAX_VIDEO_BYTES);

interface MediaAttachmentProps {
	mediaFiles: MediaFile[];
	onFilesSelect: (files: File[]) => void;
	onRemove: (id: string) => void;
	onPreview: (media: MediaFile) => void;
	mediaUrls: string[];
	onMediaUrlsChange: (values: string[]) => void;
}

export const MediaAttachment = ({ mediaFiles, onFilesSelect, onRemove, onPreview, mediaUrls, onMediaUrlsChange }: MediaAttachmentProps) => {
	const inputRef = useRef<HTMLInputElement>(null);
	const [linkInput, setLinkInput] = useState<string>("");

	return (
		<Card className="flex flex-col gap-4 min-h-fit flex-9 h-full min-w-0">
			<div className="font-semibold">
				File đính kèm
			</div>
			<button onClick={() => inputRef.current?.click()}
			        className={cn("flex flex-col justify-center items-center gap-2 border border-dashed flex-1 border-border cursor-pointer rounded-lg",
				        "hover:bg-surface-secondary transition duration-200")}>
				<Upload size={32} strokeWidth={1.5}/>
				<div className="font-medium">Kéo thả file vào đây hoặc click để chọn</div>
				<div className="text-xs text-text-secondary">Giới hạn thực tế: Ảnh ≤ 10MB, Video ≤ 150MB</div>
				<FileInput
					ref={inputRef}
					accept="image/*,video/*"
					multiple
					onFilesSelect={onFilesSelect}
				/>
			</button>

			<div className="flex flex-col gap-2">
				<div>Hoặc tạo mới từ AI</div>
				<div className="flex gap-4 items-center">
					<Button variant="outline" color="primary" className="w-fit">Tạo ảnh bằng AI</Button>
					<div className="relative group flex items-center cursor-help">
						<Info size={20} strokeWidth={1.5} className="text-text-secondary"/>

						<div
							className={cn("absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-max max-w-60 px-3 py-2 text-sm text-white",
								"bg-accent rounded-md opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none z-50",
								"text-center shadow-lg")}>
							Ảnh do AI tạo ra sẽ lấy nội dung từ caption của bạn
							<div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-accent"></div>
						</div>
					</div>
				</div>
			</div>

			<div className="h-32 flex items-center gap-3 overflow-x-auto pb-2 text-text-secondary custom-scrollbar">
				{mediaFiles.length === 0 ? (
					<div className="w-full flex justify-center text-text-secondary">
						Chưa có file nào được chọn
					</div>
				) : (
					mediaFiles.map((media) => (
						<div
							key={media.id}
							className="relative w-36 h-24 shrink-0 rounded-xl overflow-hidden border border-border cursor-pointer group shadow-sm"
							onClick={() => onPreview(media)}
						>
							{media.type === "video" ? (
								<video src={media.previewUrl} className="w-full h-full object-cover"/>
							) : (
								<img src={media.previewUrl} alt="thumbnail" className="w-full h-full object-cover"/>
							)}

							<div className="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity duration-200"/>

							<button
								onClick={(e) => {
									e.stopPropagation();
									onRemove(media.id);
								}}
								className="absolute top-1.5 right-1.5 bg-white rounded-md p-1 opacity-90 hover:opacity-100 hover:bg-gray-100 transition-all shadow-sm cursor-pointer"
							>
								<X size={16} strokeWidth={1.5}/>
							</button>

							{media.type === "video" && (
								<div className="absolute bottom-1.5 left-1.5 bg-black/60 text-white text-xs px-1.5 py-0.5 rounded">
									Video
								</div>
							)}
						</div>
					))
				)}
			</div>

			<div className="text-text-secondary text-sm h-5">
				{mediaFiles.length > 0 ? `${mediaFiles.length} file đã chọn` : " "}
			</div>

			{mediaFiles.length > 0 ? (
				<div className="max-h-24 overflow-y-auto text-xs text-text-secondary rounded-md border border-border px-2 py-1">
					{mediaFiles.map((media) => {
						const limit = getSizeLimitByType(media.type);
						const withinLimit = media.file.size <= limit;
						return (
							<div key={`${media.id}-size`} className="flex items-center justify-between gap-2 py-0.5">
								<span className="truncate" title={media.file.name}>{media.file.name}</span>
								<span className={withinLimit ? "text-green-700" : "text-red-700"}>
									{formatBytes(media.file.size)} / {formatBytes(limit)} - {withinLimit ? "Trong giới hạn" : "Vượt giới hạn"}
								</span>
							</div>
						);
					})}
				</div>
			) : null}

			<div className="flex flex-col gap-2">
				<div>Link media (tuỳ chọn)</div>
				<div className="flex gap-2">
					<Input
						value={linkInput}
						onChange={(event) => setLinkInput(event.target.value)}
						placeholder="https://..."
					/>
					<Button
						variant="solid"
						color="primary"
						onClick={() => {
							const trimmed = linkInput.trim();
							if (!trimmed) return;
							const merged = Array.from(new Set([...(mediaUrls || []), trimmed]));
							onMediaUrlsChange(merged);
							setLinkInput("");
						}}
					>
						Thêm
					</Button>
				</div>
				<div className="text-text-secondary text-sm">Dùng khi bạn muốn đăng bằng link ảnh/video.</div>

				{mediaUrls && mediaUrls.length > 0 ? (
					<div className="flex flex-col gap-1 text-sm">
						{mediaUrls.map((url, idx) => (
							<div key={`link-${idx}`} className="flex items-center justify-between gap-2">
								<a href={url} target="_blank" rel="noreferrer" className="truncate text-accent">{url}</a>
								<button
									onClick={() => {
										const copy = [...mediaUrls];
										copy.splice(idx, 1);
										onMediaUrlsChange(copy);
									}}
									className="text-red-600 text-xs"
								>
									Xóa
								</button>
							</div>
						))}
					</div>
				) : null}
			</div>
		</Card>
	);
};