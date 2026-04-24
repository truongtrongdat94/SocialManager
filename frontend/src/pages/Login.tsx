import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

export default function Login() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('devuser')
  const [password, setPassword] = useState('devpass123')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setIsSubmitting(true)

    try {
      const response = await api.post('/api/auth/login', { username, password })
      localStorage.setItem('token', response.data.token)
      localStorage.setItem('username', response.data.username)
      setError('')
      navigate('/dashboard')
    } catch (requestError) {
      setError('Invalid username or password')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="auth-shell">
      <form className="auth-card" onSubmit={handleSubmit}>
        <p className="eyebrow">Social Manager</p>
        <h1>Sign in</h1>
        <p className="muted">Use your local dev credentials to enter the dashboard.</p>

        <label>
          Username
          <input value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" />
        </label>

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
          />
        </label>

        {error ? <p className="error-text">{error}</p> : null}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Signing in...' : 'Continue'}
        </button>
      </form>
    </div>
  )
}
