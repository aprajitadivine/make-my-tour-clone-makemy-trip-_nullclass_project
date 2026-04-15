import axios from 'axios'

/**
 * Axios instance pre-configured for the MakeMyTour Spring Boot API.
 * The JWT token is injected via a request interceptor so that every
 * authenticated request automatically includes the Authorization header.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

// ── Request Interceptor ─────────────────────────────────────────
// Attach the JWT token from localStorage to every outgoing request
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ── Response Interceptor ────────────────────────────────────────
// Redirect to login page on 401 Unauthorized; enrich error objects for callers
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    // Attach a human-readable message so callers can display it directly
    if (!error.response) {
      // No response at all → pure network / CORS / timeout error
      error.friendlyMessage = 'Network error — the server could not be reached.'
    } else {
      error.friendlyMessage = `Server error ${error.response.status}: ${
        error.response.data?.message || error.response.statusText || 'Unknown error'
      }`
    }
    return Promise.reject(error)
  }
)

// ── Auth Endpoints ───────────────────────────────────────────────
export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials),
  register: (data) => api.post('/auth/register', data),
}

// ── Flight Endpoints ─────────────────────────────────────────────
export const flightApi = {
  getAll: () => api.get('/flights'),
  getById: (id) => api.get(`/flights/${id}`),
  search: (origin, destination, date) =>
    api.get('/flights/search', { params: { origin, destination, date } }),
  getStatus: (id) => api.get(`/flights/${id}/status`),
  refreshStatus: (id) => api.put(`/flights/${id}/status/refresh`),
  getDynamicPrice: (id, bookingDate) =>
    api.get(`/flights/${id}/pricing`, { params: { bookingDate } }),
  getPriceHistory: (id, days = 14) =>
    api.get(`/flights/${id}/price-history`, { params: { days } }),
}

// ── Hotel Endpoints ──────────────────────────────────────────────
export const hotelApi = {
  getAll: () => api.get('/hotels'),
  getById: (id) => api.get(`/hotels/${id}`),
  search: (city, minStars = 1) =>
    api.get('/hotels/search', { params: { city, minStars } }),
  getDynamicPrice: (id, checkInDate) =>
    api.get(`/hotels/${id}/pricing`, { params: { checkInDate } }),
  getPriceHistory: (id, days = 14) =>
    api.get(`/hotels/${id}/price-history`, { params: { days } }),
}

// ── Booking Endpoints ────────────────────────────────────────────
export const bookingApi = {
  create: (data) => api.post('/bookings', data),
  getMyBookings: () => api.get('/bookings/my'),
  getById: (id) => api.get(`/bookings/${id}`),
  cancel: (id, cancelRequest) => api.delete(`/bookings/${id}/cancel`, { data: cancelRequest }),
  getRecommendation: () => api.get('/bookings/recommend'),
  submitRecommendationFeedback: (helpful, comment = '') =>
    api.post('/bookings/recommend/feedback', { helpful, comment }),
}

// ── Review Endpoints ─────────────────────────────────────────────
export const reviewApi = {
  create: (data) => api.post('/reviews', data),
  getMyReviews: () => api.get('/reviews/my'),
  getFlightReviews: (flightId, sortByHelpful = false) =>
    api.get(`/reviews/flight/${flightId}`, { params: { sort: sortByHelpful } }),
  getHotelReviews: (hotelId, sortByHelpful = false) =>
    api.get(`/reviews/hotel/${hotelId}`, { params: { sort: sortByHelpful } }),
  /** Convenience wrapper used by ReviewModal */
  getByEntity: (entityType, entityId, sortByHelpful = false) =>
    entityType === 'flight'
      ? api.get(`/reviews/flight/${entityId}`, { params: { sort: sortByHelpful } })
      : api.get(`/reviews/hotel/${entityId}`, { params: { sort: sortByHelpful } }),
  getFlightAverage: (flightId) => api.get(`/reviews/flight/${flightId}/average`),
  getHotelAverage: (hotelId) => api.get(`/reviews/hotel/${hotelId}/average`),
  update: (id, data) => api.put(`/reviews/${id}`, data),
  delete: (id) => api.delete(`/reviews/${id}`),
  markHelpful: (id) => api.post(`/reviews/${id}/helpful`),
  flag: (id) => api.post(`/reviews/${id}/flag`),
  /** Alias used by ReviewModal */
  flagReview: (id) => api.post(`/reviews/${id}/flag`),
  /** Admin: get all flagged reviews */
  getFlagged: () => api.get('/reviews/flagged'),
  /** Admin: restore a flagged review */
  unflag: (id) => api.post(`/reviews/${id}/unflag`),
}

// ── Story Endpoints ──────────────────────────────────────────────
export const storyApi = {
  getApproved: () => api.get('/stories'),
  getMine: () => api.get('/stories/my'),
  create: (data) => api.post('/stories', data),
  delete: (id) => api.delete(`/stories/${id}`),
  // Admin
  getPending: () => api.get('/stories/pending'),
  approve: (id) => api.post(`/stories/${id}/approve`),
  reject: (id, reason) => api.post(`/stories/${id}/reject`, { reason }),
}

// ── Payment Endpoints (Razorpay) ─────────────────────────────────
export const paymentApi = {
  /** Creates a Razorpay order on the backend and returns orderId + keyId */
  createOrder: (amount, receipt) => api.post('/payments/create-order', { amount, receipt }),
  /** Verifies the Razorpay signature and creates the booking */
  verify: (data) => api.post('/payments/verify', data),
}

export default api
