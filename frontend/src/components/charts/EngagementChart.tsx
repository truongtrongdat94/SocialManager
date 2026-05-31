import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import { format } from 'date-fns';

interface DataPoint {
  date: string;
  page_post_engagements: number;
}

export function EngagementChart({ data }: { data: DataPoint[] }) {
  const formatted = data.map(d => ({
    ...d,
    date: format(new Date(d.date), 'dd/MM'),
  }));

  return (
    <ResponsiveContainer width="100%" height={280}>
      <BarChart data={formatted}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" />
        <YAxis />
        <Tooltip />
        <Legend />
        <Bar dataKey="page_post_engagements" name="Tổng tương tác" fill="#6366f1" radius={4} />
      </BarChart>
    </ResponsiveContainer>
  );
}
