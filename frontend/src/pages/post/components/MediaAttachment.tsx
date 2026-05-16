import { useRef } from "react";
import { Card, Button, FileInput, Input } from "@/components";
import { Info, Upload, X } from "lucide-react";
import { cn } from "@/utils";

export interface MediaFile {
	id: string;
	file: File;
	previewUrl: string;
	type: "image" | "video";
}

interface MediaAttachmentProps {
	mediaFiles: MediaFile[];
	onFilesSelect: (files: File[]) => void;
	onRemove: (id: string) => void;
	onPreview: (media: MediaFile) => void;
	mediaUrl: string;
	onMediaUrlChange: (value: string) => void;
}

export const MediaAttachment = ({ mediaFiles, onFilesSelect, onRemove, onPreview, mediaUrl, onMediaUrlChange }: MediaAttachmentProps) => {
	const inputRef = useRef<HTMLInputElement>(null);

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

			<div className="flex flex-col gap-2">
				<div>Link media (tuỳ chọn)</div>
				<Input
					value={mediaUrl}
					onChange={(event) => onMediaUrlChange(event.target.value)}
					placeholder="https://..."
				/>
				<div className="text-text-secondary text-sm">Dùng khi bạn muốn đăng bằng link ảnh/video.</div>
			</div>
		</Card>
	);
};