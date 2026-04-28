import { FormEvent, useEffect, useMemo, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import api from '../api/axios'

type Platform = 'FACEBOOK' | 'INSTAGRAM' | 'THREADS' | 'TIKTOK'

type SocialAccount = {
  id: string
  platform: Platform
  accountName?: string | null
  accountAlias?: string | null
  autoPilot: boolean
}

type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
}

type ComposeMode = 'MANUAL' | 'AI'

type AiSourceOption = {
  id: string
  contentPreview: string
  mediaCount: number
  createdAt: string
}

type AiPostSourcesResponse = {
  aiGenerationLogs: AiSourceOption[]
  imageGenerations: AiSourceOption[]
}

type AiPostComposeResponse = {
  post: ScheduledPostPreview
  contentSource: string
  resolvedMediaUrls: string[]
  aiGenerationLogId?: string
  imageGenerationId?: string
}

type ScheduledPostPreview = {
  id: string
  content: string
  mediaUrl: string
  scheduledTime: string
  status: string
  socialAccountName: string
}

type MonitorSummary = {
  pending: number
  processing: number
  posted: number
  failed: number
}

type MonitorItem = {
  id: string
  status: string
  retryCount: number
  scheduledTime: string
  lastAttemptAt: string
  publishedPostId: string
  errorMessage: string
}

type PostHistoryItem = {
  id: string
  socialAccountId: string
  socialAccountName: string
  platform: Platform
  content: string
  mediaUrl: string
  scheduledTime: string
  status: string
  publishedPostId: string
  errorMessage: string
  retryCount: number
  lastAttemptAt: string
  autoPilot: boolean
  createdAt: string
}

type PagedHistoryResponse = {
  items: PostHistoryItem[]
  total: number
  page: number
  size: number
  hasNext: boolean
}

export default function Posts() {
  const navigate = useNavigate()
  const hasAuth = Boolean(localStorage.getItem('token'))

  const [accounts, setAccounts] = useState<SocialAccount[]>([])
  const [composeMode, setComposeMode] = useState<ComposeMode>('MANUAL')
  const [aiGenerationLogId, setAiGenerationLogId] = useState('')
  const [imageGenerationId, setImageGenerationId] = useState('')
  const [aiGenerationOptions, setAiGenerationOptions] = useState<AiSourceOption[]>([])
  const [imageGenerationOptions, setImageGenerationOptions] = useState<AiSourceOption[]>([])
  const [selectedAccountId, setSelectedAccountId] = useState('')
  const [content, setContent] = useState('')
  const [mediaUrl, setMediaUrl] = useState('')
  const [scheduledTime, setScheduledTime] = useState('')
  const [preview, setPreview] = useState<ScheduledPostPreview | null>(null)
  const [summary, setSummary] = useState<MonitorSummary | null>(null)
  const [recent, setRecent] = useState<MonitorItem[]>([])
  const [history, setHistory] = useState<PostHistoryItem[]>([])
  const [historyStatus, setHistoryStatus] = useState('')
  const [historyPlatform, setHistoryPlatform] = useState('')
  const [historySearch, setHistorySearch] = useState('')
  const [historyPage, setHistoryPage] = useState(0)
  const [historyPageSize] = useState(6)
  const [historyTotal, setHistoryTotal] = useState(0)
  const [historyHasNext, setHistoryHasNext] = useState(false)
  const [selectedHistoryId, setSelectedHistoryId] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [actioningPostId, setActioningPostId] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const selectedAccount = useMemo(
    () => accounts.find((account) => account.id === selectedAccountId) ?? null,
    [accounts, selectedAccountId],
  )

  const selectedHistoryItem = useMemo(
    () => history.find((item) => item.id === selectedHistoryId) ?? null,
    [history, selectedHistoryId],
  )

  const loadData = async () => {
    setLoading(true)
    setError('')

    try {
      const [accountsResponse, summaryResponse, recentResponse, aiSourcesResponse] = await Promise.all([
        api.get<ApiResponse<SocialAccount[]>>('/social-accounts'),
        api.get<ApiResponse<MonitorSummary>>('/posts/monitor/summary'),
        api.get<ApiResponse<MonitorItem[]>>('/posts/monitor/recent'),
        api.get<ApiResponse<AiPostSourcesResponse>>('/posts/ai/sources?limit=20').catch(() => null),
      ])

      const loadedAccounts = accountsResponse.data.data ?? []
      setAccounts(loadedAccounts)
      setSummary(summaryResponse.data.data ?? null)
      setRecent(recentResponse.data.data ?? [])
      setAiGenerationOptions(aiSourcesResponse?.data?.data?.aiGenerationLogs ?? [])
      setImageGenerationOptions(aiSourcesResponse?.data?.data?.imageGenerations ?? [])

      if (!selectedAccountId && loadedAccounts.length > 0) {
        setSelectedAccountId(loadedAccounts[0].id)
      }
    } catch {
      setError('Cannot load posting data right now.')
    } finally {
      setLoading(false)
    }
  }

  const loadHistory = async () => {
    setHistoryLoading(true)
    setError('')

    try {
      const params = new URLSearchParams()
      params.set('page', String(historyPage))
      params.set('size', String(historyPageSize))
      if (historyStatus) {
        params.set('status', historyStatus)
      }
      if (historyPlatform) {
        params.set('platform', historyPlatform)
      }
      if (historySearch) {
        params.set('search', historySearch)
      }

      const response = await api.get<ApiResponse<PagedHistoryResponse>>(`/posts/history?${params.toString()}`)
      const payload = response.data.data
      setHistory(payload?.items ?? [])
      setHistoryTotal(payload?.total ?? 0)
      setHistoryHasNext(payload?.hasNext ?? false)
    } catch {
      setError('Cannot load post history right now.')
    } finally {
      setHistoryLoading(false)
    }
  }

  useEffect(() => {
    void loadData()
  }, [])

  useEffect(() => {
    void loadHistory()
  }, [historyStatus, historyPlatform, historySearch, historyPage, historyPageSize])

  useEffect(() => {
    setHistoryPage(0)
  }, [historyStatus, historyPlatform, historySearch])

  useEffect(() => {
    if (history.length === 0) {
      setSelectedHistoryId('')
      return
    }

    if (!selectedHistoryId || !history.some((item) => item.id === selectedHistoryId)) {
      setSelectedHistoryId(history[0].id)
    }
  }, [history, selectedHistoryId])

  useEffect(() => {
    if (!selectedAccount) {
      return
    }

    if (!mediaUrl) {
      setMediaUrl('https://images.unsplash.com/photo-1523275335684-37898b6baf30')
    }

    if (composeMode === 'MANUAL' && !content) {
      setContent(`Post for ${selectedAccount.accountName ?? selectedAccount.accountAlias ?? selectedAccount.platform}`)
    }
  }, [composeMode, selectedAccount, content, mediaUrl])

  const loadAiSources = async () => {
    try {
      const response = await api.get<ApiResponse<AiPostSourcesResponse>>('/posts/ai/sources?limit=20')
      setAiGenerationOptions(response.data.data?.aiGenerationLogs ?? [])
      setImageGenerationOptions(response.data.data?.imageGenerations ?? [])
    } catch {
      setError('Cannot load AI sources right now.')
    }
  }

  const buildRequest = () => ({
    socialAccountId: selectedAccountId,
    content,
    mediaUrl,
    scheduledTime,
  })

  const buildAiRequest = () => ({
    socialAccountId: selectedAccountId,
    scheduledTime,
    aiGenerationLogId: aiGenerationLogId || undefined,
    imageGenerationId: imageGenerationId || undefined,
    contentOverride: content.trim() ? content : undefined,
    mediaUrl: mediaUrl.trim() ? mediaUrl : undefined,
  })

  const formatContentSource = (value?: string) => {
    if (!value) {
      return 'unknown source'
    }

    return value.toLowerCase().replace(/_/g, ' ')
  }

  const formatAiOption = (item: AiSourceOption) => {
    const preview = item.contentPreview || '(empty caption)'
    return `${preview} • ${item.mediaCount} media • ${item.createdAt}`
  }

  const mediaUrlWarning = useMemo(() => {
    const value = mediaUrl.trim()
    if (!value) {
      return ''
    }

    const normalized = value.toLowerCase()
    if (!/^https?:\/\//.test(normalized)) {
      return 'Media URL should start with http:// or https://.'
    }

    const hasKnownExtension = /\.(jpg|jpeg|png|webp|gif|mp4|mov|webm)(?:$|[?#])/i.test(normalized)
    if (hasKnownExtension) {
      return ''
    }

    if (selectedAccount?.platform === 'TIKTOK') {
      return 'TikTok expects a video URL. Extensionless links only work if the host exposes a video content type.'
    }

    return 'This URL does not end in a known image/video extension. The backend will try content-type detection, but the post may still fail if the host blocks it.'
  }, [mediaUrl, selectedAccount])

  const handlePreview = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    setMessage('')

    if (composeMode === 'AI' && !content.trim() && !aiGenerationLogId && !imageGenerationId) {
      setError('Choose at least one AI source or provide content override.')
      setSubmitting(false)
      return
    }

    try {
      if (composeMode === 'AI') {
        const response = await api.post<ApiResponse<AiPostComposeResponse>>('/posts/ai/preview', buildAiRequest())
        const payload = response.data.data
        setPreview(payload?.post ?? null)
        if (!mediaUrl && payload?.resolvedMediaUrls?.length) {
          setMediaUrl(payload.resolvedMediaUrls[0])
        }
        setMessage(`AI preview loaded (${formatContentSource(payload?.contentSource)}).`)
      } else {
        const response = await api.post<ApiResponse<ScheduledPostPreview>>('/posts/preview', buildRequest())
        setPreview(response.data.data ?? null)
        setMessage('Preview loaded.')
      }
    } catch {
      setError('Cannot load preview right now.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleSchedule = async () => {
    setSubmitting(true)
    setError('')
    setMessage('')

    if (composeMode === 'AI' && !content.trim() && !aiGenerationLogId && !imageGenerationId) {
      setError('Choose at least one AI source or provide content override.')
      setSubmitting(false)
      return
    }

    try {
      if (composeMode === 'AI') {
        const response = await api.post<ApiResponse<AiPostComposeResponse>>('/posts/ai/schedule', buildAiRequest())
        const payload = response.data.data
        setPreview(payload?.post ?? null)
        if (!mediaUrl && payload?.resolvedMediaUrls?.length) {
          setMediaUrl(payload.resolvedMediaUrls[0])
        }
        setMessage(`AI post scheduled (${formatContentSource(payload?.contentSource)}).`)
      } else {
        const response = await api.post<ApiResponse<ScheduledPostPreview>>('/posts/schedule', buildRequest())
        setPreview(response.data.data ?? null)
        setMessage('Post scheduled successfully.')
      }
      await loadData()
    } catch {
      setError('Cannot schedule post right now.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleCancel = async (postId: string) => {
    setActioningPostId(postId)
    setError('')
    setMessage('')

    try {
      await api.patch<ApiResponse<ScheduledPostPreview>>(`/posts/${postId}/cancel`)
      setMessage('Post cancelled.')
      await Promise.all([loadData(), loadHistory()])
    } catch {
      setError('Cannot cancel this post right now.')
    } finally {
      setActioningPostId('')
    }
  }

  const handleRescheduleInOneHour = async (postId: string) => {
    const scheduledAt = new Date(Date.now() + 60 * 60 * 1000)
    const localIso = toLocalDatetimeInputValue(scheduledAt)

    setActioningPostId(postId)
    setError('')
    setMessage('')

    try {
      await api.patch<ApiResponse<ScheduledPostPreview>>(`/posts/${postId}/reschedule`, {
        scheduledTime: localIso,
      })
      setMessage('Post rescheduled for one hour from now.')
      await Promise.all([loadData(), loadHistory()])
    } catch {
      setError('Cannot reschedule this post right now.')
    } finally {
      setActioningPostId('')
    }
  }

  const toLocalDatetimeInputValue = (date: Date) => {
    const pad = (value: number) => String(value).padStart(2, '0')

    return [
      date.getFullYear(),
      pad(date.getMonth() + 1),
      pad(date.getDate()),
    ].join('-') + 'T' + [pad(date.getHours()), pad(date.getMinutes())].join(':')
  }

  if (!hasAuth) {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="dashboard-shell dashboard-page">
      <div className="dashboard-hero">
        <div>
          <p className="eyebrow">Social Manager</p>
          <h1>Post Scheduler</h1>
          <p className="muted">Preview, schedule, and monitor posts from one screen.</p>
        </div>
        <div className="hero-actions">
          <button type="button" className="ghost-button" onClick={() => navigate('/dashboard')}>
            Accounts
          </button>
          <button type="button" onClick={() => void loadData()} disabled={loading}>
            Refresh
          </button>
        </div>
      </div>

      <div className="dashboard-grid posts-grid">
        <section className="dashboard-card dashboard-form-card">
          <div className="card-header">
            <div>
              <p className="section-label">Compose</p>
              <h2>Schedule a post</h2>
            </div>
          </div>

          <form className="account-form" onSubmit={handlePreview}>
            <label>
              Compose Mode
              <select value={composeMode} onChange={(event) => setComposeMode(event.target.value as ComposeMode)}>
                <option value="MANUAL">Manual</option>
                <option value="AI">AI-assisted</option>
              </select>
            </label>

            <label>
              Social Account
              <select value={selectedAccountId} onChange={(event) => setSelectedAccountId(event.target.value)}>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {(account.accountName ?? account.accountAlias ?? account.id) + ' · ' + account.platform}
                  </option>
                ))}
              </select>
            </label>

            {composeMode === 'AI' ? (
              <>
                <div className="button-row">
                  <button type="button" className="ghost-button" onClick={() => void loadAiSources()} disabled={submitting}>
                    Refresh AI sources
                  </button>
                </div>

                <label>
                  AI Caption Source (optional)
                  <select value={aiGenerationLogId} onChange={(event) => setAiGenerationLogId(event.target.value)}>
                    <option value="">None</option>
                    {aiGenerationOptions.map((item) => (
                      <option key={item.id} value={item.id}>
                        {formatAiOption(item)}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  AI Image Source (optional)
                  <select value={imageGenerationId} onChange={(event) => setImageGenerationId(event.target.value)}>
                    <option value="">None</option>
                    {imageGenerationOptions.map((item) => (
                      <option key={item.id} value={item.id}>
                        {formatAiOption(item)}
                      </option>
                    ))}
                  </select>
                </label>
              </>
            ) : null}

            <label>
              {composeMode === 'AI' ? 'Content Override (optional)' : 'Content'}
              <textarea
                rows={6}
                value={content}
                onChange={(event) => setContent(event.target.value)}
                placeholder={composeMode === 'AI' ? 'Leave blank to use selected AI source content' : ''}
              />
            </label>

            <label>
              Media URL
              <input value={mediaUrl} onChange={(event) => setMediaUrl(event.target.value)} />
            </label>

            {mediaUrlWarning ? <p className="warning-text">{mediaUrlWarning}</p> : null}

            <label>
              Scheduled Time
              <input
                type="datetime-local"
                value={scheduledTime}
                onChange={(event) => setScheduledTime(event.target.value)}
              />
            </label>

            {error ? <p className="error-text">{error}</p> : null}
            {message ? <p className="success-text">{message}</p> : null}

            <div className="button-row">
              <button type="submit" disabled={submitting || !selectedAccountId}>
                {submitting ? 'Working...' : 'Preview'}
              </button>
              <button type="button" className="ghost-button" onClick={() => void handleSchedule()} disabled={submitting || !selectedAccountId}>
                Schedule
              </button>
            </div>
          </form>

          <div className="preview-panel">
            <div className="card-header">
              <div>
                <p className="section-label">Preview</p>
                <h2>Scheduled post preview</h2>
              </div>
            </div>

            {preview ? (
              <div className="preview-card">
                <strong>{preview.socialAccountName}</strong>
                <p>{preview.content}</p>
                <span>{preview.mediaUrl}</span>
                <span>{preview.scheduledTime}</span>
                <span className={preview.status === 'PENDING' ? 'pill muted-pill' : 'pill success'}>{preview.status}</span>
              </div>
            ) : (
              <p className="muted">Preview will appear here after you click Preview.</p>
            )}
          </div>
        </section>

        <section className="dashboard-card dashboard-list-card">
          <div className="card-header">
            <div>
              <p className="section-label">History</p>
              <h2>Post history</h2>
            </div>
            <button type="button" className="ghost-button" onClick={() => void loadHistory()} disabled={historyLoading}>
              Refresh history
            </button>
          </div>

          <div className="two-column history-filters">
            <label>
              Status
              <select value={historyStatus} onChange={(event) => setHistoryStatus(event.target.value)}>
                <option value="">All</option>
                <option value="PENDING">PENDING</option>
                <option value="PROCESSING">PROCESSING</option>
                <option value="POSTED">POSTED</option>
                <option value="FAILED">FAILED</option>
              </select>
            </label>

            <label>
              Platform
              <select value={historyPlatform} onChange={(event) => setHistoryPlatform(event.target.value)}>
                <option value="">All</option>
                <option value="FACEBOOK">FACEBOOK</option>
                <option value="INSTAGRAM">INSTAGRAM</option>
                <option value="THREADS">THREADS</option>
                <option value="TIKTOK">TIKTOK</option>
              </select>
            </label>
          </div>

          <label>
            Search
            <input value={historySearch} onChange={(event) => setHistorySearch(event.target.value)} placeholder="content, error, or account name" />
          </label>

          {historyLoading ? <p className="muted">Loading history...</p> : null}

          <div className="recent-list history-list">
            {history.map((item) => (
              <div key={item.id} className="history-row">
                <div className="history-main">
                  <div className="history-title-row">
                    <strong>{item.socialAccountName ?? 'Unknown account'}</strong>
                    <span className={`pill ${item.status === 'POSTED' ? 'success' : item.status === 'FAILED' ? 'muted-pill' : 'muted-pill'}`}>
                      {item.status}
                    </span>
                  </div>
                  <span>{item.platform}</span>
                  <p>{item.content}</p>
                  <span>{item.scheduledTime}</span>
                </div>
                <div className="recent-meta">
                  <span>Retry {item.retryCount}</span>
                  <span>{item.autoPilot ? 'Autopilot' : 'Manual'}</span>
                  {item.publishedPostId ? <span>Published {item.publishedPostId}</span> : null}
                  {item.errorMessage ? <span className="error-text">{item.errorMessage}</span> : null}
                  <div className="history-actions">
                    <button type="button" className="ghost-button" onClick={() => setSelectedHistoryId(item.id)}>
                      View details
                    </button>
                    {item.status === 'PENDING' ? (
                      <button type="button" className="ghost-button" onClick={() => void handleCancel(item.id)} disabled={actioningPostId === item.id}>
                        {actioningPostId === item.id ? 'Working...' : 'Cancel'}
                      </button>
                    ) : null}
                    {item.status === 'FAILED' ? (
                      <button type="button" className="ghost-button" onClick={() => void handleRescheduleInOneHour(item.id)} disabled={actioningPostId === item.id}>
                        {actioningPostId === item.id ? 'Working...' : 'Retry in 1h'}
                      </button>
                    ) : null}
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="history-pagination">
            <span className="muted">
              Showing {history.length === 0 ? 0 : historyPage * historyPageSize + 1}-{Math.min((historyPage + 1) * historyPageSize, historyTotal)} of {historyTotal}
            </span>
            <div className="history-pagination-actions">
              <button type="button" className="ghost-button" onClick={() => setHistoryPage((value) => Math.max(value - 1, 0))} disabled={historyLoading || historyPage === 0}>
                Previous
              </button>
              <button type="button" className="ghost-button" onClick={() => setHistoryPage((value) => value + 1)} disabled={historyLoading || !historyHasNext}>
                Next
              </button>
            </div>
          </div>

          {selectedHistoryItem ? (
            <div className="detail-panel">
              <div className="card-header">
                <div>
                  <p className="section-label">Details</p>
                  <h2>Selected post</h2>
                </div>
                <span className={`pill ${selectedHistoryItem.status === 'POSTED' ? 'success' : 'muted-pill'}`}>
                  {selectedHistoryItem.status}
                </span>
              </div>

              <div className="detail-grid">
                <div>
                  <span>Account</span>
                  <strong>{selectedHistoryItem.socialAccountName ?? 'Unknown account'}</strong>
                </div>
                <div>
                  <span>Platform</span>
                  <strong>{selectedHistoryItem.platform}</strong>
                </div>
                <div>
                  <span>Scheduled</span>
                  <strong>{selectedHistoryItem.scheduledTime}</strong>
                </div>
                <div>
                  <span>Created</span>
                  <strong>{selectedHistoryItem.createdAt}</strong>
                </div>
                <div>
                  <span>Retries</span>
                  <strong>{selectedHistoryItem.retryCount}</strong>
                </div>
                <div>
                  <span>Mode</span>
                  <strong>{selectedHistoryItem.autoPilot ? 'Autopilot' : 'Manual'}</strong>
                </div>
              </div>

              <div className="detail-stack">
                <div>
                  <span>Content</span>
                  <p>{selectedHistoryItem.content}</p>
                </div>
                {selectedHistoryItem.mediaUrl ? (
                  <div>
                    <span>Media URL</span>
                    <p>{selectedHistoryItem.mediaUrl}</p>
                  </div>
                ) : null}
                {selectedHistoryItem.publishedPostId ? (
                  <div>
                    <span>Published Post ID</span>
                    <p>{selectedHistoryItem.publishedPostId}</p>
                  </div>
                ) : null}
                {selectedHistoryItem.lastAttemptAt ? (
                  <div>
                    <span>Last Attempt</span>
                    <p>{selectedHistoryItem.lastAttemptAt}</p>
                  </div>
                ) : null}
                {selectedHistoryItem.errorMessage ? (
                  <div>
                    <span>Error</span>
                    <p className="error-text">{selectedHistoryItem.errorMessage}</p>
                  </div>
                ) : null}
              </div>
            </div>
          ) : null}
        </section>

        <section className="dashboard-card dashboard-list-card">
          <div className="card-header">
            <div>
              <p className="section-label">Monitor</p>
              <h2>Queue status</h2>
            </div>
          </div>

          {summary ? (
            <div className="summary-grid">
              <div><strong>{summary.pending}</strong><span>Pending</span></div>
              <div><strong>{summary.processing}</strong><span>Processing</span></div>
              <div><strong>{summary.posted}</strong><span>Posted</span></div>
              <div><strong>{summary.failed}</strong><span>Failed</span></div>
            </div>
          ) : (
            <p className="muted">No monitor summary loaded yet.</p>
          )}

          <div className="recent-list">
            {recent.map((item) => (
              <div key={item.id} className="recent-row">
                <div>
                  <strong>{item.status}</strong>
                  <span>{item.scheduledTime}</span>
                </div>
                <div className="recent-meta">
                  <span>Retry {item.retryCount}</span>
                  {item.publishedPostId ? <span>Published {item.publishedPostId}</span> : null}
                  {item.errorMessage ? <span className="error-text">{item.errorMessage}</span> : null}
                </div>
              </div>
            ))}
          </div>

          {loading ? <p className="muted">Refreshing queue...</p> : null}
        </section>
      </div>
    </div>
  )
}