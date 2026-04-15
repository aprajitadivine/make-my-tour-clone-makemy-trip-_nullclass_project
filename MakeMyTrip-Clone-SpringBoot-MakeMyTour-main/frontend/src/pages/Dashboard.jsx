import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Plane, Hotel, MapPin, Sparkles, TrendingUp, Star, ArrowRight, Search, ThumbsUp, ThumbsDown, HelpCircle, X, Loader, AlertTriangle, RefreshCw } from 'lucide-react'
import { flightApi, hotelApi, bookingApi } from '../services/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

import { fetchWithRetry } from '../utils/fetchWithRetry'

import CityDropdown from '../components/CityDropdown'


/**
 * Main Dashboard – the landing page of MakeMyTour.
 *
 * Displays:
 *  - Hero search bar (flights / hotels)
 *  - Featured flights and hotels (with loading skeletons, error banners, and retry)
 *  - AI destination recommendation with "Why this?" tooltip + feedback (Feature 6)
 *  - Dynamic pricing banner (Feature 5)
 */

/** Placeholder skeleton card shown while data is loading */
function SkeletonCard() {
  return (
    <div className="card border border-gray-100 animate-pulse">
      <div className="flex items-center justify-between mb-3">
        <div className="h-4 bg-gray-200 rounded w-24" />
        <div className="h-5 bg-gray-200 rounded-full w-16" />
      </div>
      <div className="h-3 bg-gray-200 rounded w-40 mb-2" />
      <div className="h-3 bg-gray-200 rounded w-28 mb-4" />
      <div className="flex items-center justify-between">
        <div className="h-6 bg-gray-200 rounded w-20" />
        <div className="h-8 bg-gray-200 rounded-lg w-16" />
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()

  const [activeTab, setActiveTab] = useState('flights')
  const [origin, setOrigin] = useState('')
  const [destination, setDestination] = useState('')
  const [city, setCity] = useState('')
  const [date, setDate] = useState('')

  const [featuredFlights, setFeaturedFlights] = useState([])
  const [featuredHotels, setFeaturedHotels] = useState([])
  const [flightLoading, setFlightLoading] = useState(true)
  const [hotelLoading, setHotelLoading] = useState(true)
  const [flightError, setFlightError] = useState(null)
  const [hotelError, setHotelError] = useState(null)
  const [flightRetryMsg, setFlightRetryMsg] = useState(null)
  const [hotelRetryMsg, setHotelRetryMsg] = useState(null)

  const [recommendation, setRecommendation] = useState(null)
  const [recReason, setRecReason] = useState(null)
  const [loadingRec, setLoadingRec] = useState(false)
  const [showReason, setShowReason] = useState(false)
  const [feedbackSent, setFeedbackSent] = useState(false)

  const loadFeaturedFlights = () => {
    setFlightLoading(true)
    setFlightError(null)
    setFlightRetryMsg(null)
    fetchWithRetry(() => flightApi.getAll(), {
      onRetry: (attempt, delayMs) => {
        const secs = Math.round(delayMs / 1000)
        setFlightRetryMsg(`Backend is waking up… retrying in ${secs}s (attempt ${attempt}/3)`)
      },
    })
      .then(({ data }) => { setFeaturedFlights((Array.isArray(data) ? data : []).slice(0, 3)); setFlightRetryMsg(null) })
      .catch((err) => {
        const status = err.response?.status
        const isNetwork = !err.response
        setFlightError(
          isNetwork
            ? 'Network error — cannot reach the backend. It may still be starting up (cold start takes ~60 s on the free tier).'
            : `Server error ${status} — please try again shortly.`
        )
        setFlightRetryMsg(null)
      })
      .finally(() => setFlightLoading(false))
  }

  const loadFeaturedHotels = () => {
    setHotelLoading(true)
    setHotelError(null)
    setHotelRetryMsg(null)
    fetchWithRetry(() => hotelApi.getAll(), {
      onRetry: (attempt, delayMs) => {
        const secs = Math.round(delayMs / 1000)
        setHotelRetryMsg(`Backend is waking up… retrying in ${secs}s (attempt ${attempt}/3)`)
      },
    })
      .then(({ data }) => { setFeaturedHotels((Array.isArray(data) ? data : []).slice(0, 3)); setHotelRetryMsg(null) })
      .catch((err) => {
        const status = err.response?.status
        const isNetwork = !err.response
        setHotelError(
          isNetwork
            ? 'Network error — cannot reach the backend. It may still be starting up (cold start takes ~60 s on the free tier).'
            : `Server error ${status} — please try again shortly.`
        )
        setHotelRetryMsg(null)
      })
      .finally(() => setHotelLoading(false))
  }

  useEffect(() => {
    loadFeaturedFlights()
    loadFeaturedHotels()
  }, [])

  useEffect(() => {
    if (isAuthenticated) {
      setLoadingRec(true)
      bookingApi.getRecommendation()
        .then(({ data }) => {
          setRecommendation(data.recommendation)
          setRecReason(data.reason)
        })
        .catch(() => {
          setRecommendation(null)
          setRecReason(null)
        })
        .finally(() => setLoadingRec(false))
    }
  }, [isAuthenticated])

  const handleSearch = (e) => {
    e.preventDefault()
    if (activeTab === 'flights') {
      navigate(`/flights?origin=${origin}&destination=${destination}&date=${date}`)
    } else {
      navigate(`/hotels?city=${city}`)
    }
  }

  /** Feature 6 – thumbs feedback loop */
  const handleFeedback = async (helpful) => {
    if (feedbackSent) return
    try {
      await bookingApi.submitRecommendationFeedback(helpful)
      setFeedbackSent(true)
      toast.success(helpful
        ? "Thanks! We'll keep personalising your recommendations. 👍"
        : "Thanks for the feedback! We'll improve your next recommendation. 👎")
    } catch {
      toast.error('Could not submit feedback. Please try again.')
    }
  }

  const statusBadge = {
    ON_TIME: 'badge-on-time',
    DELAYED: 'badge-delayed',
    CANCELLED: 'badge-cancelled',
  }

  const formatPrice = (price) =>
    `₹${price?.toLocaleString('en-IN') || '--'}`

  const formatTime = (dateStr) => {
    if (!dateStr) return '--'
    return new Date(dateStr).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })
  }

  return (
    <div className="min-h-screen">
      {/* ── Hero Section ─────────────────────────────── */}
      <section className="bg-gradient-to-br from-orange-500 via-orange-400 to-amber-300 text-white py-16 px-4">
        <div className="max-w-4xl mx-auto text-center mb-10">
          <h1 className="text-4xl md:text-5xl font-extrabold mb-4 drop-shadow">
            ✈ Travel Smarter with <span className="underline decoration-wavy">MakeMyTour</span>
          </h1>
          <p className="text-orange-100 text-lg">
            Book flights & hotels with real-time status, dynamic pricing, and AI recommendations
          </p>
        </div>

        {/* Search Widget */}
        <div className="max-w-3xl mx-auto bg-white rounded-2xl shadow-2xl p-6">
          {/* Tabs */}
          <div className="flex gap-2 mb-5">
            <button
              onClick={() => setActiveTab('flights')}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-semibold text-sm transition-colors ${
                activeTab === 'flights'
                  ? 'bg-orange-500 text-white shadow'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <Plane className="w-4 h-4" /> Flights
            </button>
            <button
              onClick={() => setActiveTab('hotels')}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-xl font-semibold text-sm transition-colors ${
                activeTab === 'hotels'
                  ? 'bg-orange-500 text-white shadow'
                  : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <Hotel className="w-4 h-4" /> Hotels
            </button>
          </div>

          <form onSubmit={handleSearch}>
            {activeTab === 'flights' ? (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <CityDropdown
                  value={origin}
                  onChange={setOrigin}
                  placeholder="From city"
                  showAirportCode
                />
                <CityDropdown
                  value={destination}
                  onChange={setDestination}
                  placeholder="To city"
                  showAirportCode
                />
                <input
                  type="date"
                  className="input-field"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  min={new Date().toISOString().split('T')[0]}
                />
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <CityDropdown
                  value={city}
                  onChange={setCity}
                  placeholder="Select city"
                />
                <input
                  type="date"
                  className="input-field"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  min={new Date().toISOString().split('T')[0]}
                />
              </div>
            )}
            <button type="submit" className="btn-primary w-full mt-4 py-3 flex items-center justify-center gap-2">
              <Search className="w-5 h-5" />
              Search {activeTab === 'flights' ? 'Flights' : 'Hotels'}
            </button>
          </form>
        </div>
      </section>

      {/* ── AI Recommendation Banner (Feature 6) ───────── */}
      {isAuthenticated && (
        <section className="max-w-7xl mx-auto px-4 pt-8">
          <div className="bg-gradient-to-r from-purple-600 to-indigo-600 text-white rounded-2xl p-6">
            <div className="flex items-start gap-4">
              <Sparkles className="w-8 h-8 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="font-bold text-lg mb-1">🤖 AI Recommendation For You</p>
                {loadingRec ? (
                  <p className="text-purple-200 text-sm animate-pulse">Analysing your travel history…</p>
                ) : (
                  <p className="text-purple-100">
                    {recommendation || 'Book your first trip to get personalised recommendations!'}
                  </p>
                )}
              </div>
              {/* "Why this?" button */}
              {recommendation && recReason && (
                <button
                  onClick={() => setShowReason((s) => !s)}
                  className="flex items-center gap-1.5 text-xs text-purple-200 hover:text-white border border-purple-400 hover:border-white rounded-lg px-2.5 py-1.5 transition-colors flex-shrink-0"
                  title="Why this recommendation?"
                >
                  <HelpCircle className="w-4 h-4" />
                  Why this?
                </button>
              )}
            </div>

            {/* Expandable "Why this?" reason (Feature 6) */}
            {showReason && recReason && (
              <div className="mt-4 bg-white/10 rounded-xl p-4 text-purple-100 text-sm relative">
                <button
                  onClick={() => setShowReason(false)}
                  className="absolute top-3 right-3 text-purple-300 hover:text-white"
                >
                  <X className="w-4 h-4" />
                </button>
                <p className="font-semibold text-white mb-1">Why we recommended this:</p>
                <p>{recReason}</p>
              </div>
            )}

            {/* Thumbs up/down feedback (Feature 6) */}
            {recommendation && !feedbackSent && (
              <div className="mt-4 flex items-center gap-3">
                <p className="text-purple-200 text-xs">Was this helpful?</p>
                <button
                  onClick={() => handleFeedback(true)}
                  className="flex items-center gap-1 text-xs text-purple-200 hover:text-white border border-purple-400 hover:border-white rounded-lg px-2.5 py-1 transition-colors"
                >
                  <ThumbsUp className="w-3.5 h-3.5" /> Yes
                </button>
                <button
                  onClick={() => handleFeedback(false)}
                  className="flex items-center gap-1 text-xs text-purple-200 hover:text-white border border-purple-400 hover:border-white rounded-lg px-2.5 py-1 transition-colors"
                >
                  <ThumbsDown className="w-3.5 h-3.5" /> No
                </button>
              </div>
            )}
            {feedbackSent && (
              <p className="mt-3 text-purple-200 text-xs">✓ Feedback recorded — thank you!</p>
            )}
          </div>
        </section>
      )}

      {/* ── Dynamic Pricing Notice ───────────────────── */}
      <section className="max-w-7xl mx-auto px-4 pt-6">
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-center gap-3">
          <TrendingUp className="w-5 h-5 text-amber-600 flex-shrink-0" />
          <p className="text-amber-800 text-sm">
            <strong>Dynamic Pricing Active:</strong> Prices may surge by 20% on weekends and public holidays.
            Book on weekdays to get the best rates!
          </p>
        </div>
      </section>

      {/* ── Featured Flights ─────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Plane className="w-6 h-6 text-orange-500" /> Popular Flights
          </h2>
          <Link to="/flights" className="text-orange-500 hover:text-orange-600 font-medium flex items-center gap-1">
            View all <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        {flightRetryMsg ? (
          <div className="flex items-center gap-3 bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm text-amber-800">
            <Loader className="w-5 h-5 flex-shrink-0 animate-spin text-amber-600" />
            <p>{flightRetryMsg}</p>
          </div>
        ) : flightError ? (
          <div className="flex items-start gap-3 bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
            <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
            <div className="flex-1">
              <p className="font-semibold">Could not load flights</p>
              <p className="text-red-600 text-xs mt-0.5">{flightError}</p>
            </div>
            <button
              onClick={loadFeaturedFlights}
              className="flex items-center gap-1 text-xs text-red-600 hover:text-red-800 border border-red-300 hover:border-red-500 rounded-lg px-2.5 py-1.5 transition-colors"
            >
              <RefreshCw className="w-3.5 h-3.5" /> Retry
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {flightLoading
              ? Array.from({ length: 3 }).map((_, i) => <SkeletonCard key={i} />)
              : featuredFlights.map((flight) => (
                  <div key={flight.id} className="card border border-gray-100 hover:border-orange-200">
                    <div className="flex items-center justify-between mb-3">
                      <span className="font-bold text-gray-800">{flight.flightNumber}</span>
                      <span className={statusBadge[flight.status] || 'badge-on-time'}>
                        {flight.status?.replace('_', ' ')}
                      </span>
                    </div>
                    <p className="text-gray-600 text-sm mb-2">
                      {flight.origin} → {flight.destination}
                    </p>
                    <p className="text-xs text-gray-400 mb-3">Departs {formatTime(flight.departureTime)}</p>
                    <div className="flex items-center justify-between">
                      <span className="text-xl font-bold text-orange-500">{formatPrice(flight.basePrice)}</span>
                      <Link to={`/book/flight/${flight.id}`} className="btn-primary text-sm">
                        Book
                      </Link>
                    </div>
                  </div>
                ))}
          </div>
        )}
      </section>

      {/* ── Featured Hotels ──────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 pb-12">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Hotel className="w-6 h-6 text-orange-500" /> Top Hotels
          </h2>
          <Link to="/hotels" className="text-orange-500 hover:text-orange-600 font-medium flex items-center gap-1">
            View all <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        {hotelRetryMsg ? (
          <div className="flex items-center gap-3 bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm text-amber-800">
            <Loader className="w-5 h-5 flex-shrink-0 animate-spin text-amber-600" />
            <p>{hotelRetryMsg}</p>
          </div>
        ) : hotelError ? (
          <div className="flex items-start gap-3 bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700">
            <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
            <div className="flex-1">
              <p className="font-semibold">Could not load hotels</p>
              <p className="text-red-600 text-xs mt-0.5">{hotelError}</p>
            </div>
            <button
              onClick={loadFeaturedHotels}
              className="flex items-center gap-1 text-xs text-red-600 hover:text-red-800 border border-red-300 hover:border-red-500 rounded-lg px-2.5 py-1.5 transition-colors"
            >
              <RefreshCw className="w-3.5 h-3.5" /> Retry
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {hotelLoading
              ? Array.from({ length: 3 }).map((_, i) => <SkeletonCard key={i} />)
              : featuredHotels.map((hotel) => (
                  <div key={hotel.id} className="card border border-gray-100 hover:border-orange-200">
                    <div className="flex items-center justify-between mb-2">
                      <h3 className="font-bold text-gray-800 line-clamp-1">{hotel.name}</h3>
                      <div className="flex items-center gap-0.5">
                        {Array.from({ length: hotel.starRating }).map((_, i) => (
                          <Star key={i} className="w-3.5 h-3.5 text-yellow-400 fill-yellow-400" />
                        ))}
                      </div>
                    </div>
                    <p className="text-gray-500 text-sm flex items-center gap-1 mb-2">
                      <MapPin className="w-3.5 h-3.5" /> {hotel.city}
                    </p>
                    <p className="text-xs text-gray-400 line-clamp-2 mb-3">{hotel.description}</p>
                    <div className="flex items-center justify-between">
                      <div>
                        <span className="text-xl font-bold text-orange-500">{formatPrice(hotel.basePrice)}</span>
                        <span className="text-xs text-gray-400">/night</span>
                      </div>
                      <Link to={`/book/hotel/${hotel.id}`} className="btn-primary text-sm">
                        Book
                      </Link>
                    </div>
                  </div>
                ))}
          </div>
        )}
      </section>
    </div>
  )
}
