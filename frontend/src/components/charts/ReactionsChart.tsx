import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const REACTION_COLORS: Record<string, string> = {
  like: '#3b82f6',
  love: '#ef4444',
  wow: '#f59e0b',
  haha: '#facc15',
  sorry: '#8b5cf6',
  anger: '#f97316',
};

const REACTION_LABELS: Record<string, string> = {
  like: '👍 Like',
  love: '❤️ Yêu thích',
  wow: '😮 Wow',
  haha: '😆 Haha',
  sorry: '😢 Buồn',
  anger: '😡 Phẫn nộ',
};

interface Props {
  data: { type: string; value: number }[];
}

export function ReactionsChart({ data }: Props) {
  const formatted = data.map(d => ({
    name: REACTION_LABELS[d.type] ?? d.type,
    value: d.value,
    color: REACTION_COLORS[d.type] ?? '#94a3b8',
  }));

  return (
    <ResponsiveContainer width="100%" height={280}>
      <PieChart>
        <Pie
          data={formatted}
          cx="50%"
          cy="50%"
          innerRadius={70}
          outerRadius={110}
          paddingAngle={3}
          dataKey="value"
        >
          {formatted.map((entry, i) => (
            <Cell key={i} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip formatter={(v: number) => v.toLocaleString()} />
        <Legend />
      </PieChart>
    </ResponsiveContainer>
  );
}
