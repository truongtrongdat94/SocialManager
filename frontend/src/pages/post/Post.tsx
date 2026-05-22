import { useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router";
import toast from "react-hot-toast";
import { api, type ApiResponse, type SocialAccountDto, getApiErrorMessage } from "@/lib/api";

type IncomingAiState = {
	caption?: string;
	imageUrl?: string;
};

export function Post() {
	const location = useLocation();
	const incoming = (location.state as IncomingAiState | null) ?? null;

	const [caption, setCaption] = useState(incoming?.caption ?? "");
	const [imageUrl, setImageUrl] = useState(incoming?.imageUrl ?? "");
	const [extraImageUrls, setExtraImageUrls] = useState<string[]>([]);
	const [linkInput, setLinkInput] = useState("");
	const [uploading, setUploading] = useState(false);
	const [uploadBytes, setUploadBytes] = useState<number | null>(null);
	const [uploadFileName, setUploadFileName] = useState<string | null>(null);
	const [uploadFailed, setUploadFailed] = useState(false);
    const [uploadErrorMessage, setUploadErrorMessage] = useState<string | null>(null);
    const UPLOAD_MAX_MB = 10;
	const [postMode, setPostMode] = useState<"now" | "schedule">("now");
	const [scheduledAt, setScheduledAt] = useState("");

	const canPublish = useMemo(() => caption.trim().length > 0, [caption]);
	const mediaUrls = useMemo(() => [imageUrl, ...extraImageUrls].filter((u) => !!u), [imageUrl, extraImageUrls]);

	const [accounts, setAccounts] = useState<SocialAccountDto[]>([]);
	const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
	const [publishing, setPublishing] = useState(false);

	useEffect(() => {
		const fetchAccounts = async () => {
			try {
				const res = await api.get<ApiResponse<SocialAccountDto[]>>("/api/social-accounts");
				setAccounts(res.data.data ?? []);
				const fb = (res.data.data ?? []).find((a) => a.platform === "FACEBOOK");
				if (fb) setSelectedAccountId(fb.id);
			} catch (err) {
				// silent - user may not have accounts yet
			}
		};
		void fetchAccounts();
	}, []);

	const handlePublish = () => {
		// Prevent publishing when upload is in progress or previously failed
		if (uploading) {
			toast.error("Đang upload ảnh, vui lòng đợi hoặc huỷ upload trước khi đăng.");
			return;
		}

		if (uploadFailed) {
			toast.error("Upload ảnh thất bại. Vui lòng thử lại hoặc bỏ ảnh trước khi đăng.");
			return;
		}
		if (!canPublish) {
			toast.error("Vui lòng nhập caption trước khi đăng.");
			return;
		}

		if (postMode === "schedule" && !scheduledAt) {
			toast.error("Vui lòng chọn thời gian lên lịch.");
			return;
		}

		if (!selectedAccountId) {
			toast.error("Vui lòng chọn tài khoản Facebook để đăng.");
			return;
		}

		setPublishing(true);
		void (async () => {
			try {
				const body = { caption, mediaUrls };
				if (postMode === "schedule") {
					const scheduledBody = { ...body, scheduledTime: scheduledAt };
					await api.post<ApiResponse<string>>(`/api/social-accounts/${selectedAccountId}/facebook/schedule`, scheduledBody);
					toast.success("Đã lên lịch bài đăng.");
				} else {
					await api.post<ApiResponse<string>>(`/api/social-accounts/${selectedAccountId}/facebook/publish`, body);
					toast.success("Đăng lên Facebook thành công.");
				}
			} catch (err) {
				toast.error(getApiErrorMessage(err));
			} finally {
				setPublishing(false);
			}
		})();
	};

	const removeMediaAt = (index: number) => {
		if (index < 0 || index >= mediaUrls.length) return;
		if (index === 0) {
			const rest = [...extraImageUrls];
			const nextPrimary = rest.shift() ?? "";
			setImageUrl(nextPrimary);
			setExtraImageUrls(rest);
			return;
		}
		const target = mediaUrls[index];
		setExtraImageUrls((prev) => prev.filter((u) => u !== target));
	};

	const handleAddLinks = () => {
		const rawLinks = linkInput
			.split(/\r?\n|,/)
			.map((s) => s.trim())
			.filter(Boolean);

		if (rawLinks.length === 0) {
			toast.error("Vui lòng nhập ít nhất 1 link ảnh.");
			return;
		}

		const merged = [...mediaUrls];
		for (const link of rawLinks) {
			if (!/^https?:\/\//i.test(link)) {
				continue;
			}
			if (!merged.includes(link)) {
				merged.push(link);
			}
		}

		const added = merged.length - mediaUrls.length;
		const [nextPrimary, ...rest] = merged;
		setImageUrl(nextPrimary ?? "");
		setExtraImageUrls(rest);

		setLinkInput("");
		if (added > 0) toast.success(`Đã thêm ${added} link ảnh.`);
		else toast.error("Không có link hợp lệ để thêm.");
	};

	return (
		<div className="relative w-full overflow-hidden rounded-[30px] border border-sky-100 bg-gradient-to-br from-[#f2fbff] via-[#ecf8ff] to-[#def4ff] p-5 shadow-[0_12px_30px_rgba(56,146,183,0.15)] lg:p-7">
			<div className="pointer-events-none absolute -left-3 top-2 text-[64px] opacity-20 select-none">🐻‍❄️</div>
			<div className="pointer-events-none absolute -right-2 bottom-1 text-[60px] opacity-20 select-none">🐧</div>

			<div className="mb-8 rounded-[24px] border border-sky-100/80 bg-white/75 px-5 py-5 backdrop-blur-sm">
				<h1 className="text-3xl font-bold tracking-tight text-sky-800">Đăng bài</h1>
				<p className="mt-2 text-base text-sky-700/80">
					Trang này nhận dữ liệu từ AI Dashboard qua router state: caption + imageUrl.
				</p>
			</div>

			<div className="grid grid-cols-1 gap-7 2xl:grid-cols-2">
				<section className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)] lg:p-7">
					<h2 className="mb-5 text-xl font-bold text-sky-800">Nội dung</h2>

					<div className="space-y-4">
						{/** Account selector for Facebook publishing */}
						<div>
							<label className="mb-2 block text-sm font-semibold text-sky-700">Chọn tài khoản Facebook</label>
							<select
								value={selectedAccountId ?? ""}
								onChange={(e) => setSelectedAccountId(e.target.value || null)}
								className="h-11 w-full rounded-xl border border-sky-200 bg-[#f7fcff] px-4 text-sm text-sky-900 outline-none transition focus:border-sky-400"
							>
								<option value="">-- Chọn tài khoản --</option>
								{accounts
									.filter((a) => a.platform === "FACEBOOK")
									.map((a) => (
										<option key={a.id} value={a.id}>{a.accountName} ({a.accountId})</option>
									))}
							</select>
							</div>
						<div>
							<label className="mb-2 block text-sm font-semibold text-sky-700">Caption</label>
							<textarea
								rows={8}
								value={caption}
								onChange={(e) => setCaption(e.target.value)}
								className="w-full rounded-2xl border border-sky-200 bg-[#f7fcff] px-4 py-3 text-[15px] text-sky-900 outline-none transition focus:border-sky-400 focus:shadow-[0_0_0_4px_rgba(56,189,248,0.18)]"
								placeholder="Nhập nội dung bài đăng..."
							/>
						</div>

						<div>
							<label className="mb-2 block text-sm font-semibold text-sky-700">Image URL (nhiều link: xuống dòng hoặc dấu phẩy)</label>
							<textarea
								rows={3}
								value={linkInput}
								onChange={(e) => setLinkInput(e.target.value)}
								className="w-full rounded-2xl border border-sky-200 bg-[#f7fcff] px-4 py-3 text-[15px] text-sky-900 outline-none transition focus:border-sky-400 focus:shadow-[0_0_0_4px_rgba(56,189,248,0.18)]"
								placeholder="https://..."
							/>
							<div className="mt-2">
								<button
									type="button"
									onClick={handleAddLinks}
									className="h-10 rounded-full border border-sky-200 bg-white px-4 text-sm font-semibold text-sky-700 transition hover:bg-sky-50"
								>
									Thêm link ảnh
								</button>
							</div>
							<div className="mt-2 flex items-center gap-3">
								<label className="inline-flex items-center gap-3">
									<input
										type="file"
										accept="image/*"
										multiple
										className="hidden"
										onChange={async (e) => {
											const files = Array.from(e.target.files ?? []);
											if (files.length === 0) return;

											setUploadFailed(false);
											setUploadErrorMessage(null);
											setUploading(true);
											let primaryUrl = imageUrl;
											const additionalUrls = [...extraImageUrls];

											for (const f of files) {
												const maxMB = UPLOAD_MAX_MB;
												setUploadBytes(f.size);
												setUploadFileName(f.name);

												if (f.size > maxMB * 1024 * 1024) {
													setUploadFailed(true);
													setUploadErrorMessage(`File ${f.name} vượt quá giới hạn ${maxMB} MB`);
													toast.error(`File ${f.name} vượt quá ${maxMB} MB`);
													continue;
												}

												const fd = new FormData();
												fd.append("file", f);

												try {
													const token = localStorage.getItem("token");
													const res = await fetch(`${import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080"}/api/cloudinary/upload`, {
														method: "POST",
														headers: token ? { Authorization: `Bearer ${token}` } : undefined,
														body: fd,
													});
													const apiResp = await res.json();

													if (res.ok && apiResp && apiResp.success && apiResp.data) {
														const data = apiResp.data;
														if (!primaryUrl) {
															primaryUrl = data.url;
														} else {
															additionalUrls.push(data.url);
														}
														setUploadBytes(data.bytes ?? null);
														setUploadFailed(false);
														setUploadErrorMessage(null);
													} else {
														setUploadFailed(true);
														const msg = (apiResp && apiResp.message) ? apiResp.message : `Upload thất bại: ${f.name}`;
														setUploadErrorMessage(msg);
														toast.error(msg);
													}
												} catch (err) {
													const msg = err instanceof Error ? err.message : `Upload thất bại: ${f.name}`;
													setUploadFailed(true);
													setUploadErrorMessage(msg);
													toast.error(msg);
												}
											}
											setImageUrl(primaryUrl);
											setExtraImageUrls(additionalUrls);

											setUploading(false);
											if (files.length > 1) {
												toast.success(`Đã xử lý ${files.length} file`);
											}
										}}
									/>
									<span className="h-10 inline-flex items-center rounded-full border border-sky-200 bg-white px-4 text-sm font-semibold text-sky-700 hover:bg-sky-50">Chọn tệp ảnh</span>
								</label>

								<div className="text-sm text-sky-600">
									{uploading ? (
										"Đang upload..."
									) : uploadFailed ? (
										<div>
											{uploadBytes ? <div>{`Kích thước: ${(uploadBytes / 1024 / 1024).toFixed(2)} MB`}{uploadBytes > UPLOAD_MAX_MB * 1024 * 1024 ? " (vượt quá giới hạn)" : ""}</div> : null}
											{uploadErrorMessage ? <div className="text-rose-600">{uploadErrorMessage}</div> : <div>Upload thất bại</div>}
										</div>
									) : uploadFileName ? (
										<div>{uploadFileName}{uploadBytes ? <span>{` • ${(uploadBytes / 1024 / 1024).toFixed(2)} MB`}</span> : null}</div>
									) : (
										<div>{`Giới hạn upload: ${UPLOAD_MAX_MB} MB`}</div>
									)
									}
								</div>
							</div>

							{mediaUrls.length > 0 && (
								<div className="mt-3 rounded-xl border border-sky-200 bg-white p-3">
									<p className="mb-2 text-xs font-semibold uppercase tracking-wide text-sky-600">Media đã thêm ({mediaUrls.length})</p>
									<div className="space-y-2">
										{mediaUrls.map((url, idx) => (
											<div key={`${url}-${idx}`} className="flex items-center justify-between gap-3 rounded-lg border border-sky-100 bg-sky-50/60 px-3 py-2">
												<a href={url} target="_blank" rel="noreferrer" className="truncate text-xs font-medium text-sky-700 underline-offset-2 hover:underline">
													{url}
												</a>
												<button
													type="button"
													onClick={() => removeMediaAt(idx)}
													className="rounded-full border border-rose-200 bg-white px-3 py-1 text-xs font-semibold text-rose-600 hover:bg-rose-50"
												>
													Xóa
												</button>
											</div>
										))}
									</div>
								</div>
							)}
						</div>
					</div>
				</section>

				<section className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)] lg:p-7">
					<h2 className="mb-5 text-xl font-bold text-sky-800">Preview & Publish</h2>

					<div className="space-y-4">
						<div className="overflow-hidden rounded-[28px] border border-sky-200 bg-gradient-to-br from-[#f6fcff] to-[#e9f7ff]">
							{imageUrl ? (
								<img src={imageUrl} alt="Post media preview" className="h-[320px] w-full object-cover" />
							) : (
								<div className="flex h-[320px] items-center justify-center text-sm text-sky-600">Chưa có ảnh preview</div>
							)}
						</div>

						<div className="rounded-2xl border border-sky-200 bg-[#f8fdff] p-5">
							<p className="mb-3 text-sm font-semibold text-sky-700">Chế độ đăng</p>
							<div className="flex gap-2">
								<button
									type="button"
									onClick={() => setPostMode("now")}
									className={`rounded-full border px-4 py-2 text-sm font-semibold transition ${
										postMode === "now"
											? "border-sky-500 bg-sky-500 text-white"
											: "border-sky-200 bg-white text-sky-700 hover:bg-sky-50"
									}`}
								>
									Đăng ngay
								</button>
								<button
									type="button"
									onClick={() => setPostMode("schedule")}
									className={`rounded-full border px-4 py-2 text-sm font-semibold transition ${
										postMode === "schedule"
											? "border-sky-500 bg-sky-500 text-white"
											: "border-sky-200 bg-white text-sky-700 hover:bg-sky-50"
									}`}
								>
									Lên lịch
								</button>
							</div>

							{postMode === "schedule" && (
								<div className="mt-3">
									<input
										type="datetime-local"
										value={scheduledAt}
										onChange={(e) => setScheduledAt(e.target.value)}
										className="h-11 w-full rounded-xl border border-sky-200 bg-white px-4 text-sm text-sky-900 outline-none transition focus:border-sky-400 focus:shadow-[0_0_0_4px_rgba(56,189,248,0.18)]"
									/>
								</div>
							)}
						</div>

						<button
							type="button"
							onClick={handlePublish}
							disabled={!canPublish || publishing}
							className="h-12 w-full rounded-full bg-sky-600 px-5 text-sm font-semibold text-white transition hover:bg-sky-700 disabled:cursor-not-allowed disabled:opacity-60"
						>
							{publishing ? "Đang đăng..." : postMode === "schedule" ? "Xác nhận đăng lịch" : "Xác nhận đăng bài"}
						</button>
					</div>
				</section>
			</div>
		</div>
	);
}
