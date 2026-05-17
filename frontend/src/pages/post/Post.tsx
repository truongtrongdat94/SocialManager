import { useState, useEffect } from "react";
import { Button } from "@/components";
<<<<<<< HEAD
import axiosInstance from "@/libs/axios";
=======
>>>>>>> upstream/dev
import { useSocialAccountStore, useModalStore } from "@/stores";
import { AccountSelection, SchedulePost, CaptionInput, MediaAttachment } from "./components";

interface MediaFile {
	id: string;
	file: File;
	previewUrl: string;
	type: "image" | "video";
}

<<<<<<< HEAD
const MAX_IMAGE_BYTES = 10 * 1024 * 1024;
const MAX_VIDEO_BYTES = 150 * 1024 * 1024;

const getFileLimit = (media: MediaFile) => (media.type === "image" ? MAX_IMAGE_BYTES : MAX_VIDEO_BYTES);

const formatLimitText = (bytes: number) => `${Math.round(bytes / (1024 * 1024))}MB`;


export const Post = () => {
	const accounts = useSocialAccountStore((state) => state.accounts);
	const fetchAccounts = useSocialAccountStore((state) => state.fetchAccounts);
=======

export const Post = () => {
	const accounts = useSocialAccountStore((state) => state.accounts);
>>>>>>> upstream/dev
	const openModal = useModalStore((state) => state.open);

	const [selectedAccountIds, setSelectedAccountIds] = useState<string[]>([]);
	const [isDatePick, setIsDatePick] = useState(false);
	const [postDate, setPostDate] = useState<Date | undefined>(new Date());
	const [hour, setHour] = useState(6);
	const [minute, setMinute] = useState(50);
	const [caption, setCaption] = useState("");
	const [mediaFiles, setMediaFiles] = useState<MediaFile[]>([]);
<<<<<<< HEAD
	const [mediaUrls, setMediaUrls] = useState<string[]>([]);
	const [submitting, setSubmitting] = useState(false);
	const [error, setError] = useState("");
	const [success, setSuccess] = useState("");
	const [queued, setQueued] = useState(false);
	const [queuedPostIds, setQueuedPostIds] = useState<string[]>([]);

	const toLocalDatetimeInputValue = (date: Date) => {
		const pad = (value: number) => String(value).padStart(2, "0");
		return [date.getFullYear(), pad(date.getMonth() + 1), pad(date.getDate())].join("-") + "T" + [pad(date.getHours()), pad(date.getMinutes())].join(":");
	};

	useEffect(() => {
		fetchAccounts();
	}, [fetchAccounts]);
=======
>>>>>>> upstream/dev

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

<<<<<<< HEAD
	const buildScheduledTime = () => {
		if (!isDatePick) {
			return toLocalDatetimeInputValue(new Date(Date.now() + 60 * 1000));
		}
		if (!postDate) {
			return "";
		}
		const scheduled = new Date(postDate);
		scheduled.setHours(hour, minute, 0, 0);
		return toLocalDatetimeInputValue(scheduled);
	};

	const uploadMediaFiles = async () => {
		if (mediaFiles.length === 0) return [] as string[];
		const uploadedUrls: string[] = [];
		for (const media of mediaFiles) {
			const formData = new FormData();
			formData.append("file", media.file);
			const response = await axiosInstance.post<{ data: { secureUrl: string } }>("/media/upload", formData, {
				headers: { "Content-Type": "multipart/form-data" },
			});
			const url = response.data.data?.secureUrl;
			if (url) uploadedUrls.push(url);
		}
		return uploadedUrls;
	};

	const handleSubmit = async () => {
		setError("");
		setSuccess("");
		setQueued(false);

		if (selectedAccountIds.length === 0) {
			setError("Vui lòng chọn ít nhất một tài khoản.");
			return;
		}
		if (!caption.trim()) {
			setError("Vui lòng nhập nội dung bài viết.");
			return;
		}
		const scheduledTime = buildScheduledTime();
		if (!scheduledTime) {
			setError("Vui lòng chọn thời gian đăng bài.");
			return;
		}

		const oversizedMedia = mediaFiles.find((media) => media.file.size > getFileLimit(media));
		if (oversizedMedia) {
			const limit = getFileLimit(oversizedMedia);
			setError(`File \"${oversizedMedia.file.name}\" vượt giới hạn (${oversizedMedia.type === "image" ? "Ảnh" : "Video"} tối đa ${formatLimitText(limit)}).`);
			return;
		}

		setSubmitting(true);
		try {
			const uploadedUrls = await uploadMediaFiles();
			const incomingLinks = mediaUrls.map((m) => m.trim()).filter(Boolean);
			const mergedMediaUrls = Array.from(new Set([...(uploadedUrls || []), ...incomingLinks]));
			const payloadBase = {
				content: caption.trim(),
				scheduledTime,
				mediaUrls: mergedMediaUrls.length > 0 ? mergedMediaUrls : undefined,
				mediaUrl: mergedMediaUrls.length > 0 ? mergedMediaUrls[0] : undefined,
			};
			const createdIds: string[] = [];
			for (const accountId of selectedAccountIds) {
				const response = await axiosInstance.post<{ data: { id?: string } }>("/posts/schedule", {
					...payloadBase,
					socialAccountId: accountId,
				});
				const id = response.data?.data?.id;
				if (id) createdIds.push(id);
			}

			setSuccess(`Đã lên lịch đăng cho ${selectedAccountIds.length} tài khoản.`);
			setQueued(true);
			setQueuedPostIds(createdIds);
		} catch (err: any) {
			setError(err?.response?.data?.message || "Không thể đăng bài lúc này.");
		} finally {
			setSubmitting(false);
		}
	};

	useEffect(() => {
		if (!queued) return;
		const timer = setTimeout(() => setQueued(false), 12000);
		return () => clearTimeout(timer);
	}, [queued]);

	useEffect(() => {
		if (!queued || queuedPostIds.length === 0) return;
		let active = true;
		const poll = async () => {
			try {
				const response = await axiosInstance.get<{ data: { items: Array<{ id: string; status?: string }> } }>("/posts/history?page=0&size=20");
				const items = response.data?.data?.items ?? [];
				const statusById = new Map(items.map(item => [item.id, item.status]));
				const done = queuedPostIds.every((id) => {
					const status = statusById.get(id);
					return status && status !== "PENDING" && status !== "PROCESSING";
				});
				if (done && active) {
					setQueued(false);
					setQueuedPostIds([]);
				}
			} catch {
				// Ignore polling errors; next interval will retry.
			}
		};

		const interval = setInterval(poll, 5000);
		poll();
		return () => {
			active = false;
			clearInterval(interval);
		};
	}, [queued, queuedPostIds]);

=======
>>>>>>> upstream/dev
	return (
		<div className="flex h-full flex-col gap-4">
			<div className="flex justify-between">
				<div className="flex flex-col gap-1">
					<div className="text-2xl font-bold">Đăng bài</div>
					<div className="text-text-secondary text-sm">Tạo và đăng bài lên các nền tảng</div>
				</div>
<<<<<<< HEAD
				<Button variant="solid" color="primary" onClick={handleSubmit} disabled={submitting}>
					{submitting ? "Đang đăng..." : "Đăng bài"}
				</Button>
			</div>

			{error && <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
			{success && <div className="rounded-md border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">{success}</div>}
			{(submitting || queued) && (
				<div className="rounded-md border border-border bg-surface-secondary px-3 py-2 text-sm">
					<div className="flex items-center justify-between">
						<span>{submitting ? "Đang gửi yêu cầu đăng bài..." : "Bài viết đang được xử lý trong hàng chờ."}</span>
						<span className="text-text-secondary">Có thể mất vài phút</span>
					</div>
					<div className="mt-2 h-2 w-full rounded-full bg-border overflow-hidden">
						<div className="h-full w-1/2 animate-pulse rounded-full bg-accent" />
					</div>
				</div>
			)}

=======
				<Button variant="solid" color="primary">Đăng bài</Button>
			</div>

>>>>>>> upstream/dev
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
<<<<<<< HEAD
						mediaUrls={mediaUrls}
						onMediaUrlsChange={setMediaUrls}
=======
>>>>>>> upstream/dev
					/>
				</div>
			</div>
		</div>
	);
};