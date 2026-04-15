import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Hotel, Search, Loader, AlertTriangle, RefreshCw } from 'lucide-react'
import { hotelApi } from '../services/api'
import HotelCard from '../components/HotelCard'

import { fetchWithRetry } from '../utils/fetchWithRetry'

import CityDropdown from '../components/CityDropdown'


/**
 * Hotel search page.
 */
export default function SearchHotels() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [city, setCity] = useState(searchParams.get('city') || '')
  const [minStars, setMinStars] = useState(1)
  const [hotels, setHotels] = useState([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const [error, setError] = useState(false)
  const [retryMsg, setRetryMsg] = useState(null)

  useEffect(() => {
    if (city) {
      performSearch()
    } else {
      loadAllHotels()
    }
  }, [])

  const loadAllHotels = () => {
    setLoading(true)
    setError(false)
    setRetryMsg(null)
    fetchWithRetry(() => hotelApi.getAll(), {
      onRetry: (attempt, delayMs) => {
        const secs = Math.round(delayMs / 1000)
        setRetryMsg(`Backend is waking up… retrying in ${secs}s (attempt ${attempt}/3)`)
      },
    })
      .then(({ data }) => { setHotels(data); setRetryMsg(null) })
      .catch(() => { setError(true); setHotels([]) })
      .finally(() => setLoading(false))
  }

  const performSearch = async () => {
    if (!city.trim()) return loadAllHotels()
    setLoading(true)
    setSearched(true)
    setError(false)
    try {
      const { data } = await hotelApi.search(city, minStars)
      setHotels(data)
    } catch {
      setError(true)
      setHotels([])
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = (e) => {
    e.preventDefault()
    performSearch()
  }

  const handleBook = (hotel) => {
    navigate(`/book/hotel/${hotel.id}`)
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-900 mb-6 flex items-center gap-2">
        <Hotel className="w-8 h-8 text-orange-500" /> Search Hotels
      </h1>

      {/* Search form */}
      <div className="card mb-8">
        <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <CityDropdown
            value={city}
            onChange={setCity}
            placeholder="Select city"
          />
          <select
            className="input-field"
            value={minStars}
            onChange={(e) => setMinStars(Number(e.target.value))}
          >
            <option value={1}>All Star Ratings</option>
            <option value={3}>3+ Stars</option>
            <option value={4}>4+ Stars</option>
            <option value={5}>5 Stars Only</option>
          </select>
          <button
            type="submit"
            className="btn-primary flex items-center justify-center gap-2"
          >
            <Search className="w-4 h-4" /> Search
          </button>
        </form>
      </div>

      {/* Results */}
      {retryMsg ? (
        <div className="flex items-center gap-3 bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm text-amber-800">
          <Loader className="w-5 h-5 flex-shrink-0 animate-spin text-amber-600" />
          <p>{retryMsg}</p>
        </div>
      ) : loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader className="w-8 h-8 text-orange-500 animate-spin" />
          <span className="ml-3 text-gray-500">Loading hotels…</span>
        </div>
      ) : error ? (
        <div className="flex items-start gap-3 bg-red-50 border border-red-200 rounded-xl p-5 text-sm text-red-700">
          <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="font-semibold">Could not connect to the server</p>
            <p className="text-red-600 text-xs mt-1">
              Make sure the backend is running on port 8080. On the free Render plan, the first
              request after a period of inactivity can take up to 30 seconds.
            </p>
          </div>
          <button
            onClick={searched ? performSearch : loadAllHotels}
            className="flex items-center gap-1.5 text-xs text-red-600 hover:text-red-800 border border-red-300 hover:border-red-500 rounded-lg px-2.5 py-1.5 transition-colors flex-shrink-0"
          >
            <RefreshCw className="w-3.5 h-3.5" /> Retry
          </button>
        </div>
      ) : hotels.length > 0 ? (
        <>
          <p className="text-sm text-gray-500 mb-4">
            {searched ? `Found ${hotels.length} hotel(s)` : `${hotels.length} available hotels`}
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {hotels.map((hotel) => (
              <HotelCard key={hotel.id} hotel={hotel} onBook={handleBook} />
            ))}
          </div>
        </>
      ) : (
        <div className="text-center py-16">
          <Hotel className="w-16 h-16 text-gray-200 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-500">No hotels found</h3>
          <p className="text-gray-400 mt-2">
            {searched ? 'Try a different city' : 'Start searching above'}
          </p>
        </div>
      )}
    </div>
  )
}
