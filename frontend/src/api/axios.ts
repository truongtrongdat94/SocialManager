import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
})

const getAuthHeader = () => {
  const token = localStorage.getItem('token')
  if (token) {
    return `Bearer ${token}`
  }

  return null
}

// Attach the current auth header to every request
api.interceptors.request.use((config) => {
  const authHeader = getAuthHeader()
  if (authHeader) {
    config.headers.Authorization = authHeader
  }
  return config
})

// Redirect to login on 401
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !String(err.config?.url ?? '').includes('/api/auth/login')) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export default api
