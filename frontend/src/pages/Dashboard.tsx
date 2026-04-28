import { FormEvent, useEffect, useMemo, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import api from '../api/axios'

type Platform = 'FACEBOOK' | 'INSTAGRAM' | 'THREADS' | 'TIKTOK'

type SocialAccount = {
  id: string
  platform: Platform
  externalAccountId?: string | null
  accountAlias?: string | null
  accountName?: string | null
  profilePictureUrl?: string | null
  autoPilot: boolean
  expiresAt?: string | null
  scopes?: string | null
  createdAt?: string | null
  hasAccessToken: boolean
  hasRefreshToken: boolean
}

type SocialAccountResponse = {
  success: boolean
  message: string
  data: SocialAccount[] | SocialAccount | null
}

type ConnectAccountForm = {
  platform: Platform
  externalAccountId: string
  accountAlias: string
  accountName: string
  profilePictureUrl: string
  accessToken: string
  refreshToken: string
  expiresAt: string
  scopes: string
  autoPilot: boolean
}

export default function Dashboard() {
  const navigate = useNavigate()
  const hasAuth = Boolean(localStorage.getItem('token'))
  const username = localStorage.getItem('username') ?? 'signed-in user'

  const [accounts, setAccounts] = useState<SocialAccount[]>([])
  const [selectedId, setSelectedId] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [connecting, setConnecting] = useState(false)
  const [actionError, setActionError] = useState('')
  const [actionMessage, setActionMessage] = useState('')
  const [form, setForm] = useState({
    platform: 'FACEBOOK' as Platform,
    externalAccountId: '',
    accountAlias: '',
    accountName: '',
    profilePictureUrl: '',
    accessToken: '',
    refreshToken: '',
    expiresAt: '',
    scopes: '',
    autoPilot: false,
  })
  const [connectForm, setConnectForm] = useState<ConnectAccountForm>({
    platform: 'FACEBOOK',
    externalAccountId: '',
    accountAlias: '',
    accountName: '',
    profilePictureUrl: '',
    accessToken: '',
    refreshToken: '',
    expiresAt: '',
    scopes: '',
    autoPilot: true,
  })

  const selectedAccount = useMemo(
    () => accounts.find((account) => account.id === selectedId) ?? null,
    [accounts, selectedId],
  )

  const handleOAuthConnect = async (platform: Platform) => {
    setActionError('')
    setActionMessage('')

    try {
      const response = await api.get(`/social-accounts/connect/${platform}`)
      window.location.href = response.data.data
    } catch {
      setActionError(`Cannot start ${platform} OAuth right now.`)
    }
  }

  const loadAccounts = async () => {
    setLoading(true)
    setActionError('')

    try {
      const response = await api.get<SocialAccountResponse>('/social-accounts')
      const loadedAccounts = Array.isArray(response.data.data) ? response.data.data : []
      setAccounts(loadedAccounts)

      if (!selectedId && loadedAccounts.length > 0) {
        setSelectedId(loadedAccounts[0].id)
      }
    } catch {
      setActionError('Cannot load social accounts right now.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadAccounts()
  }, [])

  useEffect(() => {
    if (!selectedAccount) {
      return
    }

    setForm({
      platform: selectedAccount.platform,
      externalAccountId: selectedAccount.externalAccountId ?? '',
      accountAlias: selectedAccount.accountAlias ?? '',
      accountName: selectedAccount.accountName ?? '',
      profilePictureUrl: selectedAccount.profilePictureUrl ?? '',
      accessToken: '',
      refreshToken: '',
      expiresAt: selectedAccount.expiresAt ? selectedAccount.expiresAt.slice(0, 16) : '',
      scopes: selectedAccount.scopes ?? '',
      autoPilot: selectedAccount.autoPilot,
    })
  }, [selectedAccount])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedAccount) {
      return
    }

    setSaving(true)
    setActionError('')
    setActionMessage('')

    try {
      await api.put(`/social-accounts/${selectedAccount.id}`, {
        platform: form.platform,
        externalAccountId: form.externalAccountId || null,
        accountAlias: form.accountAlias || null,
        accountName: form.accountName || null,
        profilePictureUrl: form.profilePictureUrl || null,
        accessToken: form.accessToken || null,
        refreshToken: form.refreshToken || null,
        expiresAt: form.expiresAt ? `${form.expiresAt}:00` : null,
        scopes: form.scopes || null,
        autoPilot: form.autoPilot,
      })

      setActionMessage('Account updated successfully.')
      await loadAccounts()
    } catch {
      setActionError('Cannot update this account right now.')
    } finally {
      setSaving(false)
    }
  }

  const handleConnectAccount = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setConnecting(true)
    setActionError('')
    setActionMessage('')

    try {
      const response = await api.post<SocialAccountResponse>('/social-accounts', {
        platform: connectForm.platform,
        externalAccountId: connectForm.externalAccountId || null,
        accountAlias: connectForm.accountAlias || null,
        accountName: connectForm.accountName || null,
        profilePictureUrl: connectForm.profilePictureUrl || null,
        accessToken: connectForm.accessToken,
        refreshToken: connectForm.refreshToken || null,
        expiresAt: connectForm.expiresAt ? `${connectForm.expiresAt}:00` : null,
        scopes: connectForm.scopes || null,
        autoPilot: connectForm.autoPilot,
      })

      const createdAccount = response.data.data as SocialAccount | null
      if (createdAccount?.id) {
        setSelectedId(createdAccount.id)
      }

      setActionMessage('Account connected successfully.')
      setConnectForm({
        platform: 'FACEBOOK',
        externalAccountId: '',
        accountAlias: '',
        accountName: '',
        profilePictureUrl: '',
        accessToken: '',
        refreshToken: '',
        expiresAt: '',
        scopes: '',
        autoPilot: true,
      })
      await loadAccounts()
    } catch {
      setActionError('Cannot connect this account right now.')
    } finally {
      setConnecting(false)
    }
  }

  const toggleAutoPilot = async (enabled: boolean) => {
    if (!selectedAccount) {
      return
    }

    setSaving(true)
    setActionError('')
    setActionMessage('')

    try {
      await api.patch(`/api/social-accounts/${selectedAccount.id}/autopilot?enabled=${enabled}`)
      setActionMessage(enabled ? 'Autopilot enabled.' : 'Autopilot disabled.')
      await loadAccounts()
    } catch {
      setActionError('Cannot change autopilot right now.')
    } finally {
      setSaving(false)
    }
  }

  const deleteAccount = async () => {
    if (!selectedAccount) {
      return
    }

    const confirmed = window.confirm(`Delete ${selectedAccount.accountName ?? selectedAccount.id}?`)
    if (!confirmed) {
      return
    }

    setSaving(true)
    setActionError('')
    setActionMessage('')

    try {
      await api.delete(`/api/social-accounts/${selectedAccount.id}`)
      setActionMessage('Account deleted.')
      await loadAccounts()
      setSelectedId('')
    } catch {
      setActionError('Cannot delete this account right now.')
    } finally {
      setSaving(false)
    }
  }

  if (!hasAuth) {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="dashboard-shell dashboard-page">
      <div className="dashboard-hero">
        <div>
          <p className="eyebrow">Social Manager</p>
          <h1>Posting Accounts</h1>
          <p className="muted">Signed in as {username}. Manage the accounts used by the posting API.</p>
        </div>
        <div className="hero-actions">
          <div className="hero-badge">{accounts.length} connected</div>
          <button type="button" className="ghost-button" onClick={() => navigate('/posts')}>
            Go to posts
          </button>
        </div>
      </div>

      <section className="dashboard-card connect-card">
        <div className="card-header">
          <div>
            <p className="section-label">Connect new account</p>
            <h2>Add a posting account</h2>
            <p className="muted">Use OAuth for a real Facebook login, or fill this form manually with an access token.</p>
          </div>
        </div>

        <div className="button-row" style={{ marginBottom: '1rem' }}>
          <button type="button" onClick={() => void handleOAuthConnect('FACEBOOK')}>
            Connect Facebook with OAuth
          </button>
          <button type="button" className="ghost-button" onClick={() => void handleOAuthConnect('INSTAGRAM')}>
            Connect Instagram with OAuth
          </button>
          <button type="button" className="ghost-button" onClick={() => void handleOAuthConnect('THREADS')}>
            Connect Threads with OAuth
          </button>
          <button type="button" className="ghost-button" onClick={() => void handleOAuthConnect('TIKTOK')}>
            Connect TikTok with OAuth
          </button>
        </div>

        <form className="account-form" onSubmit={handleConnectAccount}>
          <div className="two-column">
            <label>
              Platform
              <select value={connectForm.platform} onChange={(event) => setConnectForm({ ...connectForm, platform: event.target.value as Platform })}>
                <option value="FACEBOOK">FACEBOOK</option>
                <option value="INSTAGRAM">INSTAGRAM</option>
                <option value="THREADS">THREADS</option>
                <option value="TIKTOK">TIKTOK</option>
              </select>
            </label>

            <label>
              Access Token
              <input
                value={connectForm.accessToken}
                onChange={(event) => setConnectForm({ ...connectForm, accessToken: event.target.value })}
                placeholder="Required only for manual token entry"
              />
            </label>
          </div>

          <div className="two-column">
            <label>
              External Account ID
              <input value={connectForm.externalAccountId} onChange={(event) => setConnectForm({ ...connectForm, externalAccountId: event.target.value })} />
            </label>

            <label>
              Account Alias
              <input value={connectForm.accountAlias} onChange={(event) => setConnectForm({ ...connectForm, accountAlias: event.target.value })} />
            </label>
          </div>

          <label>
            Account Name
            <input value={connectForm.accountName} onChange={(event) => setConnectForm({ ...connectForm, accountName: event.target.value })} />
          </label>

          <label>
            Profile Picture URL
            <input value={connectForm.profilePictureUrl} onChange={(event) => setConnectForm({ ...connectForm, profilePictureUrl: event.target.value })} />
          </label>

          <div className="two-column">
            <label>
              Refresh Token
              <input value={connectForm.refreshToken} onChange={(event) => setConnectForm({ ...connectForm, refreshToken: event.target.value })} />
            </label>

            <label>
              Expires At
              <input type="datetime-local" value={connectForm.expiresAt} onChange={(event) => setConnectForm({ ...connectForm, expiresAt: event.target.value })} />
            </label>
          </div>

          <label>
            Scopes
            <textarea value={connectForm.scopes} onChange={(event) => setConnectForm({ ...connectForm, scopes: event.target.value })} rows={3} />
          </label>

          <label className="toggle-row">
            <input
              type="checkbox"
              checked={connectForm.autoPilot}
              onChange={(event) => setConnectForm({ ...connectForm, autoPilot: event.target.checked })}
            />
            Enable autopilot on connect
          </label>

          <div className="button-row">
            <button type="submit" disabled={connecting}>
              {connecting ? 'Connecting...' : 'Connect account'}
            </button>
          </div>
        </form>
      </section>

      <div className="dashboard-grid">
        <section className="dashboard-card dashboard-list-card">
          <div className="card-header">
            <div>
              <p className="section-label">Connected accounts</p>
              <h2>Choose an account</h2>
            </div>
            <button type="button" className="ghost-button" onClick={() => void loadAccounts()} disabled={loading}>
              Refresh
            </button>
          </div>

          {loading ? <p className="muted">Loading accounts...</p> : null}
          {!loading && accounts.length === 0 ? <p className="muted">No social accounts are connected yet.</p> : null}

          <div className="account-list">
            {accounts.map((account) => (
              <button
                key={account.id}
                type="button"
                className={`account-row ${account.id === selectedId ? 'active' : ''}`}
                onClick={() => setSelectedId(account.id)}
              >
                <div>
                  <strong>{account.accountName || account.accountAlias || account.id}</strong>
                  <span>{account.platform}</span>
                </div>
                <span className={account.autoPilot ? 'pill success' : 'pill muted-pill'}>
                  {account.autoPilot ? 'Autopilot on' : 'Autopilot off'}
                </span>
              </button>
            ))}
          </div>
        </section>

        <section className="dashboard-card dashboard-form-card">
          <div className="card-header">
            <div>
              <p className="section-label">Account details</p>
              <h2>{selectedAccount ? selectedAccount.accountName || selectedAccount.id : 'Select an account'}</h2>
            </div>
          </div>

          {selectedAccount ? (
            <form className="account-form" onSubmit={handleSubmit}>
              <label>
                Platform
                <select value={form.platform} onChange={(event) => setForm({ ...form, platform: event.target.value as Platform })}>
                  <option value="FACEBOOK">FACEBOOK</option>
                  <option value="INSTAGRAM">INSTAGRAM</option>
                  <option value="THREADS">THREADS</option>
                  <option value="TIKTOK">TIKTOK</option>
                </select>
              </label>

              <div className="two-column">
                <label>
                  External Account ID
                  <input value={form.externalAccountId} onChange={(event) => setForm({ ...form, externalAccountId: event.target.value })} />
                </label>

                <label>
                  Account Alias
                  <input value={form.accountAlias} onChange={(event) => setForm({ ...form, accountAlias: event.target.value })} />
                </label>
              </div>

              <label>
                Account Name
                <input value={form.accountName} onChange={(event) => setForm({ ...form, accountName: event.target.value })} />
              </label>

              <label>
                Profile Picture URL
                <input value={form.profilePictureUrl} onChange={(event) => setForm({ ...form, profilePictureUrl: event.target.value })} />
              </label>

              <label>
                Access Token
                <input
                  value={form.accessToken}
                  onChange={(event) => setForm({ ...form, accessToken: event.target.value })}
                  placeholder="Leave blank to keep existing token"
                />
              </label>

              <div className="two-column">
                <label>
                  Refresh Token
                  <input value={form.refreshToken} onChange={(event) => setForm({ ...form, refreshToken: event.target.value })} />
                </label>

                <label>
                  Expires At
                  <input type="datetime-local" value={form.expiresAt} onChange={(event) => setForm({ ...form, expiresAt: event.target.value })} />
                </label>
              </div>

              <label>
                Scopes
                <textarea value={form.scopes} onChange={(event) => setForm({ ...form, scopes: event.target.value })} rows={3} />
              </label>

              <label className="toggle-row">
                <input type="checkbox" checked={form.autoPilot} onChange={(event) => setForm({ ...form, autoPilot: event.target.checked })} />
                Enable autopilot for this account
              </label>

              {actionError ? <p className="error-text">{actionError}</p> : null}
              {actionMessage ? <p className="success-text">{actionMessage}</p> : null}

              <div className="button-row">
                <button type="submit" disabled={saving}>
                  {saving ? 'Saving...' : 'Save account'}
                </button>
                <button type="button" className="ghost-button" onClick={() => void toggleAutoPilot(!selectedAccount.autoPilot)} disabled={saving}>
                  Toggle autopilot
                </button>
                <button type="button" className="danger-button" onClick={() => void deleteAccount()} disabled={saving}>
                  Delete account
                </button>
              </div>
            </form>
          ) : (
            <p className="muted">Pick an account from the list to edit it.</p>
          )}
        </section>
      </div>
    </div>
  )
}
