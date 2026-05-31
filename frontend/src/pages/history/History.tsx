import { useState, useEffect } from 'react';
import { Card } from '../../components';
import { api } from '../../lib/api';

interface PostHistory {
  id: string;
  socialAccountName: string;
  platform: string;
  externalPostId: string;
  content: string;
  mediaUrl: string | null;
  postType: 'TEXT' | 'PHOTO' | 'VIDEO' | 'LINK';
  publishedAt: string;
  createdBy: 'MANUAL' | 'AUTO_PILOT';
}

interface PostHistoryStats {
  totalPosts: number;
  manualPosts: number;
  autoPilotPosts: number;
  facebookPosts: number;
  instagramPosts: number;
  threadsPosts: number;
  tiktokPosts: number;
}

export function History() {
  const [history, setHistory] = useState<PostHistory[]>([]);
  const [stats, setStats] = useState<PostHistoryStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [filterPlatform, setFilterPlatform] = useState<string>('ALL');

  useEffect(() => {
    loadData();
  }, [page, filterPlatform]);

  const loadData = async () => {
    try {
      setLoading(true);
      
      const [historyRes, statsRes] = await Promise.all([
        filterPlatform === 'ALL'
          ? api.get(`/api/post-history?page=${page}&size=20`)
          : api.get(`/api/post-history/platform/${filterPlatform}?page=${page}&size=20`),
        page === 0 ? api.get('/api/post-history/stats') : Promise.resolve(null),
      ]);

      const historyData = historyRes.data.content || [];
      
      if (page === 0) {
        setHistory(historyData);
        if (statsRes) {
          setStats(statsRes.data);
        }
      } else {
        setHistory(prev => [...prev, ...historyData]);
      }

      setHasMore(!historyRes.data.last);
    } catch (error) {
      console.error('Error loading history:', error);
      alert('Failed to load post history');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Delete this post from history?')) {
      return;
    }

    try {
      await api.delete(`/api/post-history/${id}`);
      setHistory(prev => prev.filter(h => h.id !== id));
      alert('Post history deleted');
      
      // Reload stats
      const statsRes = await api.get('/api/post-history/stats');
      setStats(statsRes.data);
    } catch (error) {
      console.error('Error deleting history:', error);
      alert('Failed to delete post history');
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString('vi-VN');
  };

  const getPostTypeIcon = (type: string) => {
    switch (type) {
      case 'TEXT':
        return '📝';
      case 'PHOTO':
        return '📷';
      case 'VIDEO':
        return '🎥';
      case 'LINK':
        return '🔗';
      default:
        return '📄';
    }
  };

  const getCreatedByBadge = (createdBy: string) => {
    if (createdBy === 'AUTO_PILOT') {
      return (
        <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded-full text-xs font-medium">
          🤖 Auto Pilot
        </span>
      );
    }
    return (
      <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded-full text-xs font-medium">
        ✍️ Manual
      </span>
    );
  };

  if (loading && page === 0) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-xl">Loading...</div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">📜 Post History</h1>
        <p className="text-gray-600 mt-2">
          View all your published posts across platforms
        </p>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          <Card className="p-4">
            <div className="text-sm text-gray-600">Total Posts</div>
            <div className="text-2xl font-bold text-gray-900">{stats.totalPosts}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-gray-600">Manual</div>
            <div className="text-2xl font-bold text-blue-600">{stats.manualPosts}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-gray-600">Auto Pilot</div>
            <div className="text-2xl font-bold text-purple-600">{stats.autoPilotPosts}</div>
          </Card>
          <Card className="p-4">
            <div className="text-sm text-gray-600">Facebook</div>
            <div className="text-2xl font-bold text-indigo-600">{stats.facebookPosts}</div>
          </Card>
        </div>
      )}

      {/* Filter */}
      <div className="mb-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Filter by Platform
        </label>
        <select
          value={filterPlatform}
          onChange={(e) => {
            setFilterPlatform(e.target.value);
            setPage(0);
          }}
          className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          <option value="ALL">All Platforms</option>
          <option value="FACEBOOK">Facebook</option>
          <option value="INSTAGRAM">Instagram</option>
          <option value="THREADS">Threads</option>
          <option value="TIKTOK">TikTok</option>
        </select>
      </div>

      {/* History List */}
      {history.length === 0 ? (
        <Card className="p-8 text-center">
          <div className="text-6xl mb-4">📭</div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">
            No Post History Yet
          </h3>
          <p className="text-gray-600">
            Your published posts will appear here
          </p>
        </Card>
      ) : (
        <div className="space-y-4">
          {history.map((post) => (
            <Card key={post.id} className="p-6">
              <div className="flex justify-between items-start mb-4">
                <div className="flex items-center gap-3">
                  <span className="text-2xl">{getPostTypeIcon(post.postType)}</span>
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold text-gray-900">
                        {post.socialAccountName}
                      </h3>
                      <span className="px-2 py-1 bg-gray-100 text-gray-700 rounded text-xs">
                        {post.platform}
                      </span>
                      {getCreatedByBadge(post.createdBy)}
                    </div>
                    <p className="text-sm text-gray-600">
                      {formatDate(post.publishedAt)}
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => handleDelete(post.id)}
                  className="px-3 py-1 bg-red-500 hover:bg-red-600 text-white rounded text-sm"
                >
                  Delete
                </button>
              </div>

              {post.mediaUrl && (
                <div className="mb-4">
                  <img
                    src={post.mediaUrl}
                    alt="Post media"
                    className="w-full max-h-64 object-cover rounded-lg"
                  />
                </div>
              )}

              {post.content && (
                <div className="bg-gray-50 p-4 rounded-lg">
                  <p className="text-gray-800 whitespace-pre-wrap">{post.content}</p>
                </div>
              )}

              {post.externalPostId && (
                <div className="mt-3 text-xs text-gray-500">
                  Post ID: {post.externalPostId}
                </div>
              )}
            </Card>
          ))}

          {hasMore && (
            <div className="text-center py-4">
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={loading}
                className="px-6 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg disabled:opacity-50"
              >
                {loading ? 'Loading...' : 'Load More'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
