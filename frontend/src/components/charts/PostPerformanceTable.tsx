import { useState } from 'react';
import { sumReactions } from '../../utils/transformPostInsights';

interface PostInsights {
  post_impressions_unique: number;
  post_reactions_by_type_total: Record<string, number>;
  post_clicks: number;
}

interface Post {
  id: string;
  message: string;
  created_time: string;
  permalink_url: string;
  insights?: PostInsights; // Make insights optional
}

type SortKey = 'post_impressions_unique' | 'post_reactions_total' | 'post_clicks';

const COLS: { key: SortKey; label: string }[] = [
  { key: 'post_impressions_unique', label: 'Reach' },
  { key: 'post_reactions_total', label: 'Reactions' },
  { key: 'post_clicks', label: 'Clicks' },
];

export function PostPerformanceTable({ posts }: { posts: Post[] }) {
  const [sortBy, setSortBy] = useState<SortKey>('post_impressions_unique');

  // Filter out posts without insights
  const postsWithInsights = posts.filter(p => p.insights);

  // If no posts have insights, show a message
  if (postsWithInsights.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        <p className="font-medium">Chưa có dữ liệu insights cho các bài viết</p>
        <p className="text-sm mt-2">
          Facebook chỉ cung cấp insights cho bài viết:
        </p>
        <ul className="text-sm mt-2 space-y-1">
          <li>• Đã được đăng (không phải draft/lên lịch)</li>
          <li>• Tồn tại ít nhất 24 giờ</li>
          <li>• Có đủ lượt tương tác</li>
        </ul>
        <p className="text-sm mt-3 text-gray-400">
          Vui lòng quay lại sau khi bài viết đã đăng được 1-2 ngày
        </p>
      </div>
    );
  }

  const enriched = postsWithInsights.map(p => ({
    ...p,
    computed: {
      post_impressions_unique: p.insights?.post_impressions_unique ?? 0,
      post_reactions_total: sumReactions(p.insights?.post_reactions_by_type_total ?? {}),
      post_clicks: p.insights?.post_clicks ?? 0,
    },
  }));

  const sorted = [...enriched].sort((a, b) => b.computed[sortBy] - a.computed[sortBy]);
  const maxVal = Math.max(...sorted.map(p => p.computed[sortBy]), 1);

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b">
            <th className="text-left py-2 pr-4">Bài viết</th>
            {COLS.map(c => (
              <th
                key={c.key}
                onClick={() => setSortBy(c.key)}
                className={`text-right py-2 px-3 cursor-pointer select-none hover:text-indigo-500
                  ${sortBy === c.key ? 'text-indigo-600 font-bold' : 'font-medium'}`}
              >
                {c.label} {sortBy === c.key ? '↓' : ''}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sorted.map(post => (
            <tr key={post.id} className="border-b hover:bg-gray-50">
              <td className="py-2 pr-4 max-w-xs">
                <a
                  href={post.permalink_url}
                  target="_blank"
                  rel="noreferrer"
                  className="text-indigo-600 hover:underline line-clamp-2"
                >
                  {post.message?.slice(0, 80) ?? '(no caption)'}
                </a>
                <div className="text-xs text-gray-400 mt-0.5">
                  {new Date(post.created_time).toLocaleDateString('vi-VN')}
                </div>
                <div className="mt-1 h-1.5 bg-gray-200 rounded">
                  <div
                    className="h-1.5 bg-indigo-500 rounded transition-all"
                    style={{ width: `${(post.computed[sortBy] / maxVal) * 100}%` }}
                  />
                </div>
              </td>
              {COLS.map(c => (
                <td key={c.key} className="text-right py-2 px-3 tabular-nums">
                  {post.computed[c.key].toLocaleString('vi-VN')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
