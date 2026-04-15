import { useState } from 'react'
import { Plane, Clock, MapPin, Users, RefreshCw, TrendingUp, AlertTriangle } from 'lucide-react'
import { flightApi } from '../services/api'
import { useToast } from '../context/ToastContext'

/**
 * Flight listing card.
 * Shows flight details, live status badge, and dynamic pricing.
 * The "Refresh Status" button triggers Feature 3 (Live Status Mock).
 *
 * Feature 3 additions:
 *  - Delay minutes and reason shown when DELAYED
 *  - Estimated arrival updated with delay offset
 *  - Toast push-notification when status changes
 */
export default function FlightCard({ flight, onBook }) {
  const toast = useToast()
  const [liveData, setLiveData] = useState({
    status: flight.status,
    delayMinutes: flight.delayMinutes || 0,
    delayReason: flight.delayReason || null,
    estimatedArrival: flight.arrivalTime,
  })
  const [refreshing, setRefreshing] = useState(false)
  const [dynamicPrice, setDynamicPrice] = useState(null)

  const statusBadge = {
    ON_TIME: 'badge-on-time',
    DELAYED: 'badge-delayed',
    CANCELLED: 'badge-cancelled',
  }

  const statusLabel = {
    ON_TIME: '✈ On Time',
    DELAYED: '⏰ Delayed',
    CANCELLED: '✕ Cancelled',
  }

  /** Feature 3 – Refresh live flight status with push notification */
  const handleRefreshStatus = async () => {
    setRefreshing(true)
    try {
      const { data } = await flightApi.refreshStatus(flight.id)
      const prev = liveData.status

      setLiveData({
        status: data.status,
        delayMinutes: data.delayMinutes || 0,
        delayReason: data.delayReason || null,
        estimatedArrival: data.estimatedArrival || flight.arrivalTime,
      })

      // Push toast notification when status changes
      if (data.status !== prev) {
        if (data.status === 'DELAYED') {
          toast.warn(
            `${flight.flightNumber} is now DELAYED by ${data.delayMinutes} min${data.delayReason ? ` — ${data.delayReason}` : ''}`,
            6000
          )
        } else if (data.status === 'ON_TIME') {
          toast.success(`${flight.flightNumber} is back ON TIME! ✈`)
        } else if (data.status === 'CANCELLED') {
          toast.error(`${flight.flightNumber} has been CANCELLED.`)
        }
      } else {
        toast.info(`Status refreshed: ${statusLabel[data.status] || data.status}`)
      }
    } catch (err) {
      console.error('Status refresh failed:', err)
    } finally {
      setRefreshing(false)
    }
  }

  /** Feature 5 – Fetch dynamic price for today */
  const handleCheckPrice = async () => {
    try {
      const today = new Date().toISOString().slice(0, 19)
      const { data } = await flightApi.getDynamicPrice(flight.id, today)
      setDynamicPrice(data)
    } catch (err) {
      console.error('Price check failed:', err)
    }
  }

  const formatTime = (dateStr) => {
    if (!dateStr) return '--'
    return new Date(dateStr).toLocaleTimeString('en-IN', {
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return '--'
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
    })
  }

  const { status, delayMinutes, delayReason, estimatedArrival } = liveData

  return (
    <div className="card border border-gray-100">
      {/* Header row */}
      <div className="flex items-start justify-between mb-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Plane className="w-4 h-4 text-orange-500" />
            <span className="font-bold text-gray-900">{flight.flightNumber}</span>
            <span className="text-gray-400 text-sm">{flight.name}</span>
          </div>
          <span className={statusBadge[status] || 'badge-on-time'}>
            {statusLabel[status] || status}
          </span>
          {/* Feature 3 – delay info inline */}
          {status === 'DELAYED' && delayMinutes > 0 && (
            <span className="ml-2 text-xs text-orange-600 font-medium">
              +{delayMinutes} min
            </span>
          )}
        </div>
        <button
          onClick={handleRefreshStatus}
          disabled={refreshing}
          className="p-2 rounded-lg hover:bg-gray-100 transition-colors text-gray-500 hover:text-orange-500"
          title="Refresh live status"
        >
          <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Feature 3 – Delay details banner */}
      {status === 'DELAYED' && (delayMinutes > 0 || delayReason) && (
        <div className="bg-orange-50 border border-orange-200 rounded-lg px-3 py-2 mb-4 flex items-start gap-2 text-sm">
          <AlertTriangle className="w-4 h-4 text-orange-500 flex-shrink-0 mt-0.5" />
          <div>
            {delayReason && <p className="text-orange-700 font-medium">{delayReason}</p>}
            {estimatedArrival && (
              <p className="text-orange-600 text-xs">
                <Clock className="w-3 h-3 inline mr-0.5" />
                Estimated arrival: {formatTime(estimatedArrival)}
              </p>
            )}
          </div>
        </div>
      )}

      {/* Route & Time */}
      <div className="flex items-center gap-3 mb-4">
        <div className="text-center">
          <p className="text-2xl font-bold text-gray-900">{formatTime(flight.departureTime)}</p>
          <p className="text-xs text-gray-500">{formatDate(flight.departureTime)}</p>
          <p className="text-sm font-semibold text-gray-700 flex items-center gap-1">
            <MapPin className="w-3 h-3" /> {flight.origin}
          </p>
        </div>

        <div className="flex-1 flex flex-col items-center">
          <div className="w-full border-t-2 border-dashed border-gray-300 relative">
            <Plane className="w-4 h-4 text-orange-400 absolute -top-2.5 left-1/2 -translate-x-1/2" />
          </div>
          <span className="text-xs text-gray-400 mt-1">Non-stop</span>
        </div>

        <div className="text-center">
          <p className={`text-2xl font-bold ${status === 'DELAYED' ? 'text-orange-500 line-through opacity-60' : 'text-gray-900'}`}>
            {formatTime(flight.arrivalTime)}
          </p>
          {status === 'DELAYED' && estimatedArrival && (
            <p className="text-lg font-bold text-orange-600">
              {formatTime(estimatedArrival)}
            </p>
          )}
          <p className="text-xs text-gray-500">{formatDate(flight.arrivalTime)}</p>
          <p className="text-sm font-semibold text-gray-700 flex items-center gap-1">
            <MapPin className="w-3 h-3" /> {flight.destination}
          </p>
        </div>
      </div>

      {/* Seats info */}
      <div className="flex items-center gap-2 text-sm text-gray-500 mb-4">
        <Users className="w-4 h-4" />
        <span>
          {flight.seatRows * flight.seatCols - (flight.bookedSeats?.length || 0)} seats available
        </span>
        <span className="mx-1">·</span>
        <span className="text-gray-400 text-xs">{flight.category?.replace('_', ' ')}</span>
        {flight.premiumSeats?.length > 0 && (
          <>
            <span className="mx-1">·</span>
            <span className="text-yellow-600 text-xs">⭐ {flight.premiumSeats.length} premium seats</span>
          </>
        )}
      </div>

      {/* Dynamic Pricing Info */}
      {dynamicPrice && (
        <div className={`rounded-lg p-3 mb-4 text-sm ${dynamicPrice.surgeActive ? 'bg-orange-50 border border-orange-200' : 'bg-green-50 border border-green-200'}`}>
          <div className="flex items-center gap-1 font-semibold mb-1">
            <TrendingUp className={`w-4 h-4 ${dynamicPrice.surgeActive ? 'text-orange-500' : 'text-green-500'}`} />
            {dynamicPrice.surgeActive ? (
              <span className="text-orange-700">Weekend Surge Active (+20%)</span>
            ) : (
              <span className="text-green-700">Regular Pricing</span>
            )}
          </div>
          <p className="text-gray-600">
            Base: ₹{dynamicPrice.basePrice?.toLocaleString('en-IN')} →
            Final: <strong>₹{dynamicPrice.dynamicPrice?.toLocaleString('en-IN')}</strong>
          </p>
        </div>
      )}

      {/* Action buttons */}
      <div className="flex items-center justify-between pt-4 border-t border-gray-100">
        <div>
          <p className="text-sm text-gray-500">Base price</p>
          <p className="text-2xl font-bold text-orange-500">
            ₹{flight.basePrice?.toLocaleString('en-IN')}
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleCheckPrice}
            className="btn-secondary text-sm px-3 py-2 flex items-center gap-1"
          >
            <TrendingUp className="w-4 h-4" /> Price
          </button>
          <button
            onClick={() => onBook(flight)}
            disabled={status === 'CANCELLED'}
            className="btn-primary text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Book Now
          </button>
        </div>
      </div>
    </div>
  )
}
