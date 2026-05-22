import { useEffect, useState } from 'react';
import { api } from '../lib/api';

interface InsightOptions {
  pageId: string;
  metrics: string[];
  since: string;   // YYYY-MM-DD
  until: string;   // YYYY-MM-DD
  period?: 'day' | 'week' | 'days_28' | 'lifetime';
}

export function usePageInsights({
  pageId,
  metrics,
  since,
  until,
  period = 'day',
}: InsightOptions) {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!pageId) return;
    setLoading(true);

    api
      .get(`/api/analytics/page/${pageId}`, {
        params: { metric: metrics.join(','), since, until, period },
      })
      .then(res => setData(res.data.data || res.data))
      .catch(err => setError(err.response?.data?.message ?? err.message))
      .finally(() => setLoading(false));
  }, [pageId, metrics.join(','), since, until, period]);

  return { data, loading, error };
}
