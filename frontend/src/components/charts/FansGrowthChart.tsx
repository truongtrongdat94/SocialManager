import {
  ComposedChart, Bar, Line, XAxis, YAxis,
  CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import { format } from 'date-fns';

interface DataPoint {
  date: string;
  page_fans: number;
  page_fan_adds_unique: number;
  page_fan_removes_unique: number;
  page_daily_follows_unique: number;
}

export function FansGrowthChart({ data }: { data: DataPoint[] }) {
  const formatted = data.map(d => ({
    ...d,
    date: format(new Date(d.date), 'dd/MM'),
    page_fan_removes_unique: -d.page_fan_removes_unique,
  }));

  return (
    <ResponsiveContainer width="100%" height={320}>
      <ComposedChart data={formatted}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" />
        <YAxis yAxisId="left" />
        <YAxis yAxisId="right" orientation="right" />
        <Tooltip />
        <Legend />
        <Bar yAxisId="left" dataKey="page_fan_adds_unique" name="Likes mới" fill="#22c55e" />
        <Bar yAxisId="left" dataKey="page_fan_removes_unique" name="Unlikes" fill="#ef4444" />
        <Bar yAxisId="left" dataKey="page_daily_follows_unique" name="Follows mới" fill="#60a5fa" />
        <Line yAxisId="right" type="monotone" dataKey="page_fans" name="Tổng fans" stroke="#6366f1" strokeWidth={2} dot={false} />
      </ComposedChart>
    </ResponsiveContainer>
  );
}
