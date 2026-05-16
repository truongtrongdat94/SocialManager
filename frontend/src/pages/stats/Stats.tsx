import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import {
	CartesianGrid,
	Legend,
	Line,
	LineChart,
	ResponsiveContainer,
	Tooltip,
	XAxis,
	YAxis,
} from "recharts";
import { api, type AnalyticsMap, type ApiResponse, getApiErrorMessage } from "@/lib/api";

type Point = {
	name: string;
	value: number;
};

function toChartData(values: number[] = []): Point[] {
	return values.map((value, index) => ({
		name: `#${index + 1}`,
		value,
	}));
}

export function Stats() {
	const [analytics, setAnalytics] = useState<AnalyticsMap>({});
	const [loading, setLoading] = useState(false);

	useEffect(() => {
		const fetchAnalytics = async () => {
			try {
				setLoading(true);
				const response = await api.get<ApiResponse<AnalyticsMap>>("/api/analytics");
				setAnalytics(response.data?.data ?? {});
			} catch (error) {
				toast.error(getApiErrorMessage(error));
			} finally {
				setLoading(false);
			}
		};

		void fetchAnalytics();
	}, []);

	const cards = useMemo(() => {
		return Object.entries(analytics).map(([key, values]) => {
			const total = values.reduce((sum, item) => sum + item, 0);
			const avg = values.length ? Math.round(total / values.length) : 0;
			return { key, total, avg, count: values.length };
		});
	}, [analytics]);

	return (
		<div className="relative w-full overflow-hidden rounded-[30px] border border-sky-100 bg-gradient-to-br from-[#f2fbff] via-[#ecf8ff] to-[#def4ff] p-5 shadow-[0_12px_30px_rgba(56,146,183,0.15)] lg:p-7">
			<div className="pointer-events-none absolute -left-3 top-2 text-[64px] opacity-20 select-none">🐻‍❄️</div>
			<div className="pointer-events-none absolute -right-2 bottom-1 text-[60px] opacity-20 select-none">🐧</div>

			<div className="mb-8 rounded-[24px] border border-sky-100/80 bg-white/75 px-5 py-5 backdrop-blur-sm">
				<h1 className="text-3xl font-bold tracking-tight text-sky-800">Thống kê</h1>
				<p className="mt-2 text-base text-sky-700/80">Hiển thị dữ liệu từ endpoint `/api/analytics`.</p>
			</div>

			{loading ? (
				<div className="rounded-[24px] border border-sky-100 bg-white/90 p-5 text-sm text-sky-600">
					Đang tải dữ liệu thống kê...
				</div>
			) : cards.length === 0 ? (
				<div className="rounded-[24px] border border-sky-100 bg-white/90 p-5 text-sm text-sky-600">
					Chưa có dữ liệu analytics.
				</div>
			) : (
				<>
					<div className="mb-7 grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-4">
						{cards.map((item) => (
							<div key={item.key} className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)]">
								<p className="text-xs uppercase tracking-wide text-sky-500">{item.key}</p>
								<p className="mt-2 text-3xl font-semibold text-sky-900">{item.total}</p>
								<p className="mt-1 text-sm text-sky-600">
									Avg: {item.avg} • Samples: {item.count}
								</p>
							</div>
						))}
					</div>

					<div className="grid grid-cols-1 gap-7 xl:grid-cols-2">
						{Object.entries(analytics).map(([key, values]) => (
							<div key={key} className="rounded-[28px] border border-sky-100 bg-white/90 p-6 shadow-[0_8px_24px_rgba(64,164,202,0.14)]">
								<p className="mb-4 text-base font-semibold text-sky-700">{key}</p>
								<div className="h-80">
									<ResponsiveContainer width="100%" height="100%">
										<LineChart data={toChartData(values)}>
											<CartesianGrid strokeDasharray="3 3" stroke="#cde9f8" />
											<XAxis dataKey="name" stroke="#4b95be" />
											<YAxis stroke="#4b95be" />
											<Tooltip contentStyle={{ borderRadius: 12, borderColor: "#bae6fd" }} />
											<Legend />
											<Line
												type="monotone"
												dataKey="value"
												stroke="#0ea5e9"
												strokeWidth={2.5}
												dot={{ r: 3.5 }}
											/>
										</LineChart>
									</ResponsiveContainer>
								</div>
							</div>
						))}
					</div>
				</>
			)}
		</div>
	);
}
