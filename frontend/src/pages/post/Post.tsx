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
	const [postMode, setPostMode] = useState<"now" | "schedule">("now");
	const [scheduledAt, setScheduledAt] = useState("");

	const canPublish = useMemo(() => caption.trim().length > 0, [caption]);

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
				const body = { caption, mediaUrls: imageUrl ? [imageUrl] : [] };
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
							<label className="mb-2 block text-sm font-semibold text-sky-700">Image URL</label>
							<input
								value={imageUrl}
								onChange={(e) => setImageUrl(e.target.value)}
								className="h-11 w-full rounded-xl border border-sky-200 bg-[#f7fcff] px-4 text-sm text-sky-900 outline-none transition focus:border-sky-400 focus:shadow-[0_0_0_4px_rgba(56,189,248,0.18)]"
								placeholder="https://..."
							/>
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
