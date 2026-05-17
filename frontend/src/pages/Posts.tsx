import type { ChangeEvent, FormEvent } from 'react'
import { useEffect, useMemo } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import usePostStore from '@/stores/usePostStore'

type Platform = 'FACEBOOK' | 'INSTAGRAM' | 'THREADS' | 'TIKTOK'


type ComposeMode = 'MANUAL' | 'AI'

type AiSourceOption = {
  id: string
  contentPreview: string
  mediaCount: number
  createdAt: string
}


 




export default function Posts() {
  const navigate = useNavigate()
  const hasAuth = Boolean(localStorage.getItem('token'))

  const {
    accounts,
    composeMode,
    setComposeMode,
    aiGenerationLogId,
    setAiGenerationLogId,
    imageGenerationId,
    setImageGenerationId,
    aiGenerationOptions,
    imageGenerationOptions,
    selectedAccountId,
    setSelectedAccountId,
    content,
    setContent,
    mediaUrl,
    setMediaUrl,
    uploadedMediaUrls,
    scheduledTime,
    setScheduledTime,
    preview,
    summary,
    recent,
    history,
    historyStatus,
    historyPlatform,
    historySearch,
    historyPage,
    historyPageSize,
    historyTotal,
    historyHasNext,
    loading,
    submitting,
    historyLoading,
    actioningPostId,
    message,
    error,
    loadData,
    loadHistory,
    loadAiSources,
    uploadMediaFiles,
    clearUploadedMedia: storeClearUploadedMedia,
    previewPost,
    schedulePost,
    postNow,
    cancelPost,
    rescheduleInOneHour,
    setHistoryFilters,
    setHistoryPage,
    setSelectedHistoryId,
    selectedHistoryId,
  } = usePostStore()

  const selectedAccount = useMemo(
    () => accounts.find((account) => account.id === selectedAccountId) ?? null,
    [accounts, selectedAccountId],
  )

  const selectedHistoryItem = useMemo(
    () => history.find((item) => item.id === selectedHistoryId) ?? null,
    [history, selectedHistoryId],
  )
  useEffect(() => {
    void loadData()
  }, [loadData])

  useEffect(() => {
    void loadHistory()
  }, [historyStatus, historyPlatform, historySearch, historyPage, historyPageSize, loadHistory])

  useEffect(() => {
    setHistoryPage(0)
  }, [historyStatus, historyPlatform, historySearch, setHistoryPage])

  useEffect(() => {
    if (history.length === 0) {
      setSelectedHistoryId('')
      return
    }

    if (!selectedHistoryId || !history.some((item) => item.id === selectedHistoryId)) {
      setSelectedHistoryId(history[0].id)
    }
  }, [history, selectedHistoryId, setSelectedHistoryId])

  useEffect(() => {
    if (!selectedAccount) return
    if (composeMode === 'MANUAL' && !content) {
      setContent(`Post for ${selectedAccount.accountName ?? selectedAccount.accountAlias ?? selectedAccount.platform}`)
    }
  }, [composeMode, selectedAccount, content, setContent])


  

  const formatAiOption = (item: AiSourceOption) => {
    const preview = item.contentPreview || '(empty caption)'
    return `${preview} • ${item.mediaCount} media • ${item.createdAt}`
  }

  

  const handleMediaUpload = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files ? Array.from(event.target.files) : []
    event.target.value = ''

    if (files.length === 0) {
      return
    }
    // delegate upload logic to store
    await uploadMediaFiles(files)
  }

  const handleClearUploadedMedia = () => storeClearUploadedMedia()

  const getPublishedPostHref = (
    platform: Platform | undefined,
    publishedPostId?: string | null,
    publishedPostUrl?: string | null,
  ) => {
    if (publishedPostUrl && publishedPostUrl.trim()) {
      return publishedPostUrl
    }

    if (!publishedPostId || !publishedPostId.trim()) {
      return ''
    }

    const safeId = publishedPostId.trim()
    switch (platform) {
      case 'FACEBOOK':
        return `https://www.facebook.com/${safeId}`
      case 'INSTAGRAM':
        return `https://www.instagram.com/p/${safeId}/`
      case 'THREADS':
        return `https://www.threads.net/post/${safeId}`
      case 'TIKTOK':
        return `https://www.tiktok.com/@/video/${safeId}`
      default:
        return ''
    }
  }

  const mediaUrlWarning = useMemo(() => {
    if (uploadedMediaUrls.length > 0) {
      return ''
    }

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
  }, [uploadedMediaUrls.length, mediaUrl, selectedAccount])

  const handlePreview = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (composeMode === 'AI' && !content.trim() && !aiGenerationLogId && !imageGenerationId) {
      // keep same validation as before
      // store will set error/message flags as needed
      usePostStore.setState({ error: 'Choose at least one AI source or provide content override.' })
      return
    }

    await previewPost({ ai: composeMode === 'AI' })
  }

  const handleSchedule = async () => {
    if (composeMode === 'AI' && !content.trim() && !aiGenerationLogId && !imageGenerationId) {
      usePostStore.setState({ error: 'Choose at least one AI source or provide content override.' })
      return
    }

    await schedulePost({ ai: composeMode === 'AI' })
  }

  const handlePostNow = async () => {
    if (composeMode === 'AI' && !content.trim() && !aiGenerationLogId && !imageGenerationId) {
      usePostStore.setState({ error: 'Choose at least one AI source or provide content override.' })
      return
    }

    await postNow()
  }

  const handleCancel = async (postId: string) => {
    await cancelPost(postId)
  }

  const handleRescheduleInOneHour = async (postId: string) => {
    await rescheduleInOneHour(postId)
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
              Upload image/video
              <input
                type="file"
                accept="image/*,video/*"
                multiple
                onChange={(event) => void handleMediaUpload(event)}
                disabled={submitting}
              />
            </label>

            {uploadedMediaUrls.length > 0 ? (
              <div className="detail-stack">
                <div>
                  <span>Uploaded media</span>
                  {uploadedMediaUrls.map((url) => (
                    <p key={url}>{url}</p>
                  ))}
                </div>
                <button type="button" className="ghost-button" onClick={handleClearUploadedMedia} disabled={submitting}>
                  Clear uploaded media
                </button>
              </div>
            ) : null}

            <label>
              Media URL fallback
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
              <button type="button" className="ghost-button" onClick={() => void handlePostNow()} disabled={submitting || !selectedAccountId}>
                Post now
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
              <select value={historyStatus} onChange={(event) => setHistoryFilters({ status: event.target.value, platform: historyPlatform, search: historySearch })}>
                <option value="">All</option>
                <option value="PENDING">PENDING</option>
                <option value="PROCESSING">PROCESSING</option>
                <option value="POSTED">POSTED</option>
                <option value="FAILED">FAILED</option>
              </select>
            </label>

            <label>
              Platform
              <select value={historyPlatform} onChange={(event) => setHistoryFilters({ status: historyStatus, platform: event.target.value, search: historySearch })}>
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
            <input value={historySearch} onChange={(event) => setHistoryFilters({ status: historyStatus, platform: historyPlatform, search: event.target.value })} placeholder="content, error, or account name" />
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
                  {item.retryCount > 0 ? <span>Retry {item.retryCount}</span> : null}
                  {item.status === 'FAILED' ? <span>{item.autoPilot ? 'Autopilot' : 'Manual'}</span> : null}
                  {getPublishedPostHref(item.platform, item.publishedPostId, item.publishedPostUrl) ? (
                    <span>
                      Published{' '}
                      <a
                        href={getPublishedPostHref(item.platform, item.publishedPostId, item.publishedPostUrl)}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        View
                      </a>
                    </span>
                  ) : item.publishedPostId ? (
                    <span>Published {item.publishedPostId}</span>
                  ) : null}
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
              <button type="button" className="ghost-button" onClick={() => setHistoryPage(Math.max(historyPage - 1, 0))} disabled={historyLoading || historyPage === 0}>
                Previous
              </button>
              <button type="button" className="ghost-button" onClick={() => setHistoryPage(historyPage + 1)} disabled={historyLoading || !historyHasNext}>
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
                {getPublishedPostHref(
                  selectedHistoryItem.platform,
                  selectedHistoryItem.publishedPostId,
                  selectedHistoryItem.publishedPostUrl,
                ) ? (
                  <div>
                    <span>Published Post</span>
                    <p>
                      <a
                        href={getPublishedPostHref(
                          selectedHistoryItem.platform,
                          selectedHistoryItem.publishedPostId,
                          selectedHistoryItem.publishedPostUrl,
                        )}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        View on platform
                      </a>
                    </p>
                  </div>
                ) : selectedHistoryItem.publishedPostId ? (
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
                  {item.retryCount > 0 ? <span>Retry {item.retryCount}</span> : null}
                  {item.publishedPostUrl ? (
                    <span>
                      Published <a href={item.publishedPostUrl} target="_blank" rel="noopener noreferrer">View</a>
                    </span>
                  ) : item.publishedPostId ? (
                    <span>Published {item.publishedPostId}</span>
                  ) : null}
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
