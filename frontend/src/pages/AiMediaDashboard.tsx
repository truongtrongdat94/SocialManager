import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import toast from "react-hot-toast";
import {
	api,
	type AiGenerationLog,
	type ApiResponse,
	type ImageGeneration,
	getApiErrorMessage,
} from "@/lib/api";

type DashboardTab = "ai-content" | "cloudinary";
type Platform = "Facebook" | "Instagram" | "Threads" | "TikTok";

const PLATFORM_OPTIONS: Platform[] = ["Facebook", "Instagram", "Threads", "TikTok"];

export default function AiMediaDashboard() {
	const navigate = useNavigate();

	const [activeTab, setActiveTab] = useState<DashboardTab>("ai-content");
	const [topic, setTopic] = useState("");
	const [platform, setPlatform] = useState<Platform>("Facebook");

	const [isGenerating, setIsGenerating] = useState(false);
	const [caption, setCaption] = useState("");
	const [imageUrl, setImageUrl] = useState("");
	const [generationId, setGenerationId] = useState("");

	const [history, setHistory] = useState<ImageGeneration[]>([]);
	const [loadingHistory, setLoadingHistory] = useState(false);

	const hashtags = useMemo(() => {
		const tags = caption.match(/#[\p{L}\p{N}_]+/gu) ?? [];
		return Array.from(new Set(tags));
	}, [caption]);

	const fetchHistory = async () => {
		try {
			setLoadingHistory(true);
			const response = await api.get<ApiResponse<ImageGeneration[]>>("/api/ai/history");
			setHistory(Array.isArray(response.data.data) ? response.data.data : []);
		} catch (error) {
			toast.error(getApiErrorMessage(error));
		} finally {
			setLoadingHistory(false);
		}
	};

	useEffect(() => {
		void fetchHistory();
	}, []);

	const handleGenerate = async () => {
		if (!topic.trim()) {
			toast.error("Vui lòng nhập chủ đề để tạo nội dung.");
			return;
		}

		try {
			setIsGenerating(true);

			const captionResponse = await api.post<ApiResponse<AiGenerationLog>>("/api/ai/generate-caption", {
				topic: topic.trim(),
				platform,
			});

			const generatedCaption = captionResponse.data?.data?.resultCaption ?? "";
			setCaption(generatedCaption);

			const imageResponse = await api.post<{ generationId?: string; message?: string }>(
				"/api/ai/generate-image",
				{ prompt: topic.trim() },
			);

			const newGenerationId = imageResponse.data?.generationId ?? "";
			setGenerationId(newGenerationId);

			let resolvedImageUrl = "";
			for (let i = 0; i < 8; i++) {
				await new Promise((r) => setTimeout(r, 2000));
				const historyRes = await api.get<ApiResponse<ImageGeneration[]>>("/api/ai/history");
				const rows = historyRes.data?.data ?? [];

				const found = rows.find(
					(item) =>
						(newGenerationId && item.leonardoGenerationId === newGenerationId) ||
						(item.prompt?.includes(topic.trim()) ?? false),
				);

				const candidate = found?.cloudinaryUrl ?? found?.cloudinaryUrls?.[0] ?? "";
				if (candidate) {
					resolvedImageUrl = candidate;
					break;
				}
			}

			setImageUrl(resolvedImageUrl);
			await fetchHistory();

			if (!resolvedImageUrl) {
				toast("Caption đã có, ảnh vẫn đang xử lý. Hãy bấm làm mới thư viện sau.");
			} else {
				toast.success("Tạo caption và ảnh thành công.");
			}
		} catch (error) {
			toast.error(getApiErrorMessage(error));
		} finally {
			setIsGenerating(false);
		}
	};

	const mediaUrls = useMemo(() => {
		const urls: string[] = [];
		history.forEach((item) => {
			if (item.cloudinaryUrl) urls.push(item.cloudinaryUrl);
			if (Array.isArray(item.cloudinaryUrls)) urls.push(...item.cloudinaryUrls.filter(Boolean));
		});
		return Array.from(new Set(urls));
	}, [history]);

	const pushToPost = () => {
		if (!caption || !imageUrl) {
			toast.error("Cần có cả caption và ảnh trước khi chuyển sang trang đăng bài.");
			return;
		}

		navigate("/dashboard/post", {
			state: { caption, imageUrl },
		});
	};

	const copyUrl = async (url: string) => {
		try {
			await navigator.clipboard.writeText(url);
			toast.success("Đã copy URL");
		} catch {
			toast.error("Không thể copy URL");
		}
	};

	return (
		<div className="relative w-full overflow-hidden rounded-[30px] border border-sky-100 bg-gradient-to-br from-[#f2fbff] via-[#ecf8ff] to-[#def4ff] p-5 shadow-[0_12px_30px_rgba(56,146,183,0.15)] lg:p-7">
			<style>{`
        @keyframes snowDrift {
          0% { transform: translateY(-8px); opacity: .15; }
          100% { transform: translateY(12px); opacity: .55; }
        }
        @keyframes floaty {
          0%,100% { transform: translateY(0px); }
          50% { transform: translateY(-4px); }
        }
        .snow-dot {
          animation: snowDrift 2.8s ease-in-out infinite alternate;
        }
        .floaty {
          animation: floaty 2.6s ease-in-out infinite;
        }
        @media (prefers-reduced-motion: reduce) {
          .snow-dot, .floaty {
            animation: none !important;
          }
        }
      `}</style>

			<div className="pointer-events-none absolute inset-0">
				<div className="snow-dot absolute left-8 top-8 h-2 w-2 rounded-full bg-white/80" />
				<div className="snow-dot absolute left-1/4 top-14 h-1.5 w-1.5 rounded-full bg-sky-100" style={{ animationDelay: "0.4s" }} />
				<div className="snow-dot absolute right-16 top-10 h-2.5 w-2.5 rounded-full bg-white/70" style={{ animationDelay: "0.7s" }} />
				<div className="snow-dot absolute right-1/3 top-24 h-1.5 w-1.5 rounded-full bg-sky-200/70" style={{ animationDelay: "0.2s" }} />
			</div>

			<div className="relative mb-6 flex items-start justify-between gap-4 rounded-[24px] border border-sky-100/80 bg-white/75 px-5 py-5 shadow-[inset_0_1px_0_rgba(255,255,255,0.7)] backdrop-blur-sm">
				<div>
					<h1 className="text-3xl font-extrabold tracking-tight text-sky-700">Arctic AI Media Dashboard</h1>
					<p className="mt-2 text-base text-sky-700/80">Tạo caption, tạo ảnh và chuyển nhanh sang trang đăng bài theo phong cách Bắc Cực.</p>
				</div>
				<div className="floaty hidden rounded-2xl border border-sky-200 bg-[#eef9ff] px-4 py-3 text-sm font-semibold text-sky-700 md:block">
					🐻‍❄️ + 🐧 Arctic Mode
				</div>
			</div>

			<div className="mb-6 flex flex-wrap gap-3">
				<button
					type="button"
					onClick={() => setActiveTab("ai-content")}
					className={`rounded-full border px-5 py-2.5 text-sm font-semibold transition ${
						activeTab === "ai-content"
							? "border-sky-500 bg-sky-500 text-white shadow-[0_8px_18px_rgba(14,165,233,0.35)]"
							: "border-sky-200 bg-white text-sky-700 hover:border-sky-300 hover:bg-sky-50"
					}`}
				>
					Tạo nội dung AI
				</button>
				<button
					type="button"
					onClick={() => setActiveTab("cloudinary")}
					className={`rounded-full border px-5 py-2.5 text-sm font-semibold transition ${
						activeTab === "cloudinary"
							? "border-sky-500 bg-sky-500 text-white shadow-[0_8px_18px_rgba(14,165,233,0.35)]"
							: "border-sky-200 bg-white text-sky-700 hover:border-sky-300 hover:bg-sky-50"
					}`}
				>
					Thư viện ảnh
				</button>
			</div>

			{activeTab === "ai-content" && (
				<div className="grid grid-cols-1 gap-7 2xl:grid-cols-2">
					<section className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)] backdrop-blur-sm lg:p-7">
						<h2 className="mb-5 text-xl font-bold text-sky-800">Input</h2>

						<div className="space-y-4">
							<div>
								<label className="mb-2 block text-sm font-semibold text-sky-700">Chủ đề</label>
								<textarea
									rows={7}
									value={topic}
									onChange={(e) => setTopic(e.target.value)}
									placeholder="Nhập chủ đề muốn AI tạo nội dung..."
									className="w-full rounded-2xl border border-sky-200 bg-[#f7fcff] px-4 py-3 text-[15px] text-sky-900 outline-none transition focus:border-sky-400 focus:shadow-[0_0_0_4px_rgba(56,189,248,0.18)]"
									disabled={isGenerating}
								/>
							</div>

							<div>
								<label className="mb-2 block text-sm font-semibold text-sky-700">Nền tảng</label>
								<select
									value={platform}
									onChange={(e) => setPlatform(e.target.value as Platform)}
									className="h-12 w-full rounded-full border border-sky-200 bg-[#f7fcff] px-4 text-[15px] text-sky-900 outline-none transition focus:border-sky-400 focus:shadow-[0_0_0_4px_rgba(56,189,248,0.18)]"
									disabled={isGenerating}
								>
									{PLATFORM_OPTIONS.map((item) => (
										<option key={item} value={item}>
											{item}
										</option>
									))}
								</select>
							</div>

							<button
								type="button"
								onClick={handleGenerate}
								disabled={isGenerating}
								className="h-12 w-full rounded-full bg-sky-500 px-5 text-sm font-semibold text-white transition hover:bg-sky-600 disabled:cursor-not-allowed disabled:opacity-60"
							>
								{isGenerating ? "Đang tạo..." : "Tạo nội dung bằng AI"}
							</button>
						</div>
					</section>

					<section className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)] backdrop-blur-sm lg:p-7">
						<h2 className="mb-5 text-xl font-bold text-sky-800">Kết quả</h2>

						<div className="space-y-4">
							<div className="rounded-2xl border border-sky-100 bg-[#f8fdff] p-4">
								<p className="mb-2 text-xs font-semibold uppercase tracking-wide text-sky-500">Caption</p>
								<p className="whitespace-pre-wrap text-[15px] leading-7 text-sky-900">{caption || "Chưa có caption"}</p>
							</div>

							<div className="rounded-2xl border border-sky-100 bg-[#f8fdff] p-4">
								<p className="mb-2 text-xs font-semibold uppercase tracking-wide text-sky-500">Hashtags</p>
								<div className="flex flex-wrap gap-2">
									{hashtags.length ? (
										hashtags.map((tag) => (
											<span key={tag} className="rounded-full border border-sky-200 bg-white px-3 py-1 text-xs text-sky-700">
												{tag}
											</span>
										))
									) : (
										<span className="text-sm text-sky-500">Chưa có hashtag</span>
									)}
								</div>
							</div>

							<div className="overflow-hidden rounded-[32px] border border-sky-200 bg-gradient-to-br from-[#f6fcff] to-[#e9f7ff]">
								{imageUrl ? (
									<img src={imageUrl} alt="AI generated" className="h-[360px] w-full object-cover" />
								) : (
									<div className="flex h-[360px] flex-col items-center justify-center gap-2 text-sm text-sky-600">
										<div className="text-2xl">❄️</div>
										<div>Chưa có ảnh</div>
									</div>
								)}
							</div>

							<div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
								<div className="rounded-2xl border border-sky-200 bg-[#f8fdff] px-4 py-3 text-xs text-sky-600">
									Generation ID: {generationId || "N/A"}
								</div>
								<button
									type="button"
									onClick={pushToPost}
									className="h-12 rounded-full bg-sky-600 px-5 text-sm font-semibold text-white transition hover:bg-sky-700"
								>
									Đẩy sang bộ đăng bài
								</button>
							</div>
						</div>
					</section>
				</div>
			)}

			{activeTab === "cloudinary" && (
				<section className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)] backdrop-blur-sm lg:p-7">
					<div className="mb-4 flex items-center justify-between">
						<h2 className="text-lg font-bold text-sky-800">Cloudinary Library</h2>
						<button
							type="button"
							onClick={fetchHistory}
							disabled={loadingHistory}
							className="h-11 rounded-full border border-sky-200 bg-white px-5 text-sm font-semibold text-sky-700 transition hover:border-sky-300 hover:bg-sky-50"
						>
							{loadingHistory ? "Đang tải..." : "Làm mới"}
						</button>
					</div>

					{mediaUrls.length === 0 ? (
						<div className="rounded-xl border border-dashed border-sky-200 bg-[#f8fdff] p-5 text-sm text-sky-500">
							Chưa có ảnh trong lịch sử. 🐧
						</div>
					) : (
						<div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
							{mediaUrls.map((url) => (
								<div key={url} className="overflow-hidden rounded-2xl border border-sky-200 bg-white">
									<div className="aspect-square">
										<img src={url} alt="Cloudinary item" className="h-full w-full object-cover" />
									</div>
									<div className="border-t border-sky-100 p-2.5">
										<button
											type="button"
											onClick={() => copyUrl(url)}
											className="h-10 w-full rounded-full border border-sky-200 text-xs font-semibold text-sky-700 transition hover:bg-sky-50"
										>
											Copy URL
										</button>
									</div>
								</div>
							))}
						</div>
					)}
				</section>
			)}
		</div>
	);
}
