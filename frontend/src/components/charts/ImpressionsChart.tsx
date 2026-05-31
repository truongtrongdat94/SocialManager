import {
  LineChart, Line, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import { format } from 'date-fns';

interface DataPoint {
  date: string;
  page_impressions_unique: number;
  page_posts_impressions_organic: number;
  page_posts_impressions_paid: number;
}

export function ImpressionsChart({ data }: { data: DataPoint[] }) {
  const formatted = data.map(d => ({
    ...d,
    date: format(new Date(d.date), 'dd/MM'),
  }));

  return (
    <ResponsiveContainer width="100%" height={320}>
      <LineChart data={formatted}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" />
        <YAxis />
        <Tooltip formatter={(v: number) => v.toLocaleString()} />
        <Legend />
        <Line type="monotone" dataKey="page_impressions_unique" name="Reach (unique)" stroke="#22c55e" strokeWidth={2} dot={false} />
        <Line type="monotone" dataKey="page_posts_impressions_organic" name="Organic" stroke="#f59e0b" strokeWidth={2} dot={false} />
        <Line type="monotone" dataKey="page_posts_impressions_paid" name="Paid" stroke="#ef4444" strokeWidth={2} dot={false} />
      </LineChart>
    </ResponsiveContainer>
  );
}
