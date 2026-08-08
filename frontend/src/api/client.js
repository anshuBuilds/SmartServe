import axios from 'axios'
export const TOKEN_KEY = 'smartserve.staff.token'
export const USER_KEY = 'smartserve.staff.user'
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 15000,
  headers: {'Content-Type': 'application/json'},
})
api.interceptors.request.use(config => {
  if (!config.url?.startsWith('/guest/')) {
    const token = sessionStorage.getItem(TOKEN_KEY)
    if (token) config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
api.interceptors.response.use(
  response => {
    const body = response.data
    return body && Object.hasOwn(body, 'data') ? body.data : body
  },
  error => {
    const status = error.response?.status
    const body = error.response?.data || {}
    if (status === 401) {
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(USER_KEY)
      if (!location.pathname.startsWith('/login')) location.assign('/login')
    }
    return Promise.reject({
      status,
      message: body.message || error.message || 'Request failed',
      fieldErrors: body.validationErrors || {},
    })
  },
)
export const authApi = {
  login: data => api.post('/auth/login', data),
  me: () => api.get('/users/me'),
}
export const guestApi = {
  session: token => api.get(`/guest/session/${token}`),
  menu: token => api.get(`/guest/menu/${token}`),
  order: (token, data) => api.post(`/guest/orders/${token}`, data),
  track: trackingToken => api.get(`/guest/orders/${trackingToken}`),
  verifyPayment: (trackingToken, data) =>
    api.post(`/guest/orders/${trackingToken}/payment/verify`, data),
}
