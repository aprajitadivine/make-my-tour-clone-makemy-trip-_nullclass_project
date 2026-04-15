import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Plane, Search, Loader, AlertTriangle, RefreshCw } from 'lucide-react'
import { flightApi } from '../services/api'
import FlightCard from '../components/FlightCard'
import { fetchWithRetry } from '../utils/fetchWithRetry'

import CityDropdown from '../components/CityDropdown'


/**
 * Search page for flights.
 * Supports searching by origin, destination, and date.
 * Loads all flights by default and filters on search.
 */
export default function SearchFlights() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [origin, setOrigin] = useState(searchParams.get('origin') || '')
  const [destination, setDestination] = useState(searchParams.get('destination') || '')
  const [date, setDate] = useState(searchParams.get('date') || '')
  const [flights, setFlights] = useState([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const [error, setError] = useState(false)
  const [retryMsg, setRetryMsg] = useState(null)

  useEffect(() => {
    // Auto-search if URL params are present
    if (origin && destination) {
      performSearch()
    } else {
      loadAllFlights()
    }
  }, [])

  const loadAllFlights = () => {
    setLoading(true)
    setError(false)
    setRetryMsg(null)
    fetchWithRetry(() => flightApi.getAll(), {
      onRetry: (attempt, delayMs) => {
        const secs = Math.round(delayMs / 1000)
        setRetryMsg(`Backend is waking up… retrying in ${secs}s (attempt ${attempt}/3)`)
      },
    })
      .then(({ data }) => { setFlights(data); setRetryMsg(null) })
      .catch(() => { setError(true); setFlights([]) })
      .finally(() => setLoading(false))
  }

  const performSearch = async () => {
    if (!origin.trim() && !destination.trim()) {
      return loadAllFlights()
    }
    setLoading(true)
    setSearched(true)
    setError(false)
    try {
      // Build date string for API
      const searchDate = date
        ? new Date(date).toISOString()
        : new Date().toISOString()
      const { data } = await flightApi.search(origin, destination, searchDate)
      setFlights(data)
    } catch {
      setError(true)
      setFlights([])
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = (e) => {
    e.preventDefault()
    performSearch()
  }

  const handleBook = (flight) => {
    navigate(`/book/flight/${flight.id}`)
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-900 mb-6 flex items-center gap-2">
        <Plane className="w-8 h-8 text-orange-500" /> Search Flights
      </h1>

      {/* Search form */}
      <div className="card mb-8">
        <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-4">
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
          <span className="ml-3 text-gray-500">Loading flights…</span>
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
            onClick={searched ? performSearch : loadAllFlights}
            className="flex items-center gap-1.5 text-xs text-red-600 hover:text-red-800 border border-red-300 hover:border-red-500 rounded-lg px-2.5 py-1.5 transition-colors flex-shrink-0"
          >
            <RefreshCw className="w-3.5 h-3.5" /> Retry
          </button>
        </div>
      ) : flights.length > 0 ? (
        <>
          <p className="text-sm text-gray-500 mb-4">
            {searched ? `Found ${flights.length} flight(s)` : `${flights.length} available flights`}
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {flights.map((flight) => (
              <FlightCard key={flight.id} flight={flight} onBook={handleBook} />
            ))}
          </div>
        </>
      ) : (
        <div className="text-center py-16">
          <Plane className="w-16 h-16 text-gray-200 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-500">No flights found</h3>
          <p className="text-gray-400 mt-2">
            {searched
              ? 'Try different dates or destinations'
              : 'Start searching above'}
          </p>
        </div>
      )}
    </div>
  )
}
