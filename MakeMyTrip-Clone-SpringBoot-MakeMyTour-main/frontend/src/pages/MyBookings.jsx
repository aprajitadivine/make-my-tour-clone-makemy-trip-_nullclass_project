import { useState, useEffect } from 'react'
import { BookOpen, Plane, Hotel, X, Star, Loader, Sparkles, ChevronDown, RefreshCw, BarChart2 } from 'lucide-react'
import { bookingApi } from '../services/api'
import ReviewModal from '../components/ReviewModal'
import DetailedReviewModal from '../components/DetailedReviewModal'
import { useToast } from '../context/ToastContext'

/**
 * My Bookings page.
 *
 * Features shown:
 *  - Feature 1: Cancel booking with reason dropdown + refund policy breakdown + refund status tracker
 *  - Feature 2: Write a review after booking (ReviewModal)
 *  - Feature 6: AI recommendation banner
 */

const CANCEL_REASONS = [
  'Change of plans',
  'Found a better deal',
  'Medical emergency',
  'Work / Personal obligation',
  'Duplicate booking',
  'Other',
]

export default function MyBookings() {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [cancelling, setCancelling] = useState(null)
  const [selectedBookingForReview, setSelectedBookingForReview] = useState(null)
  const [selectedBookingForDetailedReview, setSelectedBookingForDetailedReview] = useState(null)
  const [recommendation, setRecommendation] = useState(null)

  // Cancellation dialog state
  const [cancelDialogId, setCancelDialogId] = useState(null)
  const [cancelReason, setCancelReason] = useState('')
  const [refundResult, setRefundResult] = useState(null)   // holds last refund breakdown
  const toast = useToast()

  useEffect(() => {
    loadBookings()
    loadRecommendation()
  }, [])

  const loadBookings = async () => {
    setLoading(true)
    try {
      const { data } = await bookingApi.getMyBookings()
      setBookings(data)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const loadRecommendation = async () => {
    try {
      const { data } = await bookingApi.getRecommendation()
      setRecommendation(data.recommendation)
    } catch {
      // ignore
    }
  }

  /** Step 1: open the reason dialog */
  const openCancelDialog = (bookingId) => {
    setCancelDialogId(bookingId)
    setCancelReason('')
    setRefundResult(null)
  }

  /** Step 2: user confirms in the dialog */
  const handleCancel = async () => {
    if (!cancelReason) {
      toast.error('Please select a cancellation reason')
      return
    }

    const bookingId = cancelDialogId
    setCancelling(bookingId)
    try {
      const { data } = await bookingApi.cancel(bookingId, { reason: cancelReason })
      setRefundResult(data)
      // Update the local list immediately
      setBookings((prev) =>
        prev.map((b) =>
          b.id === bookingId
            ? { ...b, status: 'CANCELLED', refundStatus: 'PENDING_REFUND' }
            : b
        )
      )
      toast.success(
        `Booking #${bookingId} cancelled. ₹${data.refundAmount?.toLocaleString('en-IN')} refund initiated.`
      )
    } catch (err) {
      toast.error(err.response?.data?.error || 'Cancellation failed')
      setCancelDialogId(null)
    } finally {
      setCancelling(null)
    }
  }

  const closeCancelDialog = () => {
    setCancelDialogId(null)
    setCancelReason('')
    setRefundResult(null)
  }

  const statusColor = {
    CONFIRMED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-red-100 text-red-700',
    REFUNDED: 'bg-blue-100 text-blue-700',
    PENDING: 'bg-yellow-100 text-yellow-700',
  }

  const refundStatusColor = {
    PENDING_REFUND: 'bg-yellow-50 border-yellow-200 text-yellow-700',
    REFUNDED: 'bg-blue-50 border-blue-200 text-blue-700',
  }

  const formatDate = (dateStr) => {
    if (!dateStr) return '--'
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: 'numeric', month: 'short', year: 'numeric',
    })
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Loader className="w-8 h-8 text-orange-500 animate-spin" />
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold text-gray-900 mb-6 flex items-center gap-2">
        <BookOpen className="w-8 h-8 text-orange-500" /> My Bookings
      </h1>

      {/* AI Recommendation banner – Feature 6 */}
      {recommendation && (
        <div className="bg-gradient-to-r from-purple-600 to-indigo-600 text-white rounded-2xl p-5 mb-6 flex items-start gap-3">
          <Sparkles className="w-6 h-6 flex-shrink-0 mt-0.5" />
          <div>
            <p className="font-bold mb-0.5">🤖 Your Next Destination</p>
            <p className="text-purple-100 text-sm">{recommendation}</p>
          </div>
        </div>
      )}

      {/* Bookings list */}
      {bookings.length === 0 ? (
        <div className="text-center py-16">
          <BookOpen className="w-16 h-16 text-gray-200 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-500">No bookings yet</h3>
          <p className="text-gray-400 mt-2">Start exploring flights and hotels!</p>
        </div>
      ) : (
        <div className="space-y-4">
          {bookings.map((booking) => (
            <div key={booking.id} className="card border border-gray-100">
              {/* Header */}
              <div className="flex items-start justify-between mb-4">
                <div className="flex items-center gap-2">
                  {booking.flight ? (
                    <Plane className="w-5 h-5 text-orange-500" />
                  ) : (
                    <Hotel className="w-5 h-5 text-orange-500" />
                  )}
                  <div>
                    <p className="font-bold text-gray-900">
                      {booking.flight?.name || booking.hotel?.name || 'Booking #' + booking.id}
                    </p>
                    <p className="text-xs text-gray-400">Booking ID: #{booking.id}</p>
                  </div>
                </div>
                <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${statusColor[booking.status] || 'bg-gray-100 text-gray-600'}`}>
                  {booking.status}
                </span>
              </div>

              {/* Details */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm mb-4">
                <div>
                  <p className="text-gray-400 text-xs">Booked on</p>
                  <p className="font-medium">{formatDate(booking.bookingDate)}</p>
                </div>
                <div>
                  <p className="text-gray-400 text-xs">{booking.flight ? 'Departure' : 'Check-in'}</p>
                  <p className="font-medium">{formatDate(booking.travelDate)}</p>
                </div>
                {booking.seatId && (
                  <div>
                    <p className="text-gray-400 text-xs">{booking.flight ? 'Seat' : 'Room'}</p>
                    <p className="font-medium">{booking.seatId}</p>
                  </div>
                )}
                <div>
                  <p className="text-gray-400 text-xs">Total Paid</p>
                  <p className="font-bold text-orange-500">₹{booking.totalPrice?.toLocaleString('en-IN')}</p>
                </div>
              </div>

              {/* Flight route */}
              {booking.flight && (
                <p className="text-sm text-gray-500 mb-3">
                  {booking.flight.origin} → {booking.flight.destination} · {booking.flight.flightNumber}
                </p>
              )}

              {/* Refund status tracker – Feature 1 */}
              {booking.refundStatus && refundStatusColor[booking.refundStatus] && (
                <div className={`rounded-lg border px-3 py-2 mb-3 text-xs flex items-center gap-2 ${refundStatusColor[booking.refundStatus]}`}>
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>
                    {booking.refundStatus === 'PENDING_REFUND'
                      ? '⏳ Refund in progress – typically processed within 5–7 business days'
                      : '✅ Refund successfully credited to your account'}
                  </span>
                </div>
              )}

              {/* Cancellation reason if already cancelled */}
              {booking.status === 'CANCELLED' && booking.cancellationReason && (
                <p className="text-xs text-gray-400 mb-3">
                  Cancellation reason: <em>{booking.cancellationReason}</em>
                </p>
              )}

              {/* Actions */}
              <div className="flex gap-2 pt-3 border-t border-gray-100">
                {booking.status === 'CONFIRMED' && (
                  <button
                    onClick={() => openCancelDialog(booking.id)}
                    className="flex items-center gap-1.5 text-sm text-red-500 hover:text-red-600 font-medium"
                  >
                    <X className="w-4 h-4" />
                    Cancel Booking
                  </button>
                )}

                <button
                  onClick={() => setSelectedBookingForReview(booking)}
                  className="flex items-center gap-1.5 text-sm text-blue-500 hover:text-blue-600 font-medium ml-auto"
                >
                  <Star className="w-4 h-4" /> Write Review
                </button>
                <button
                  onClick={() => setSelectedBookingForDetailedReview(booking)}
                  className="flex items-center gap-1.5 text-sm text-purple-500 hover:text-purple-600 font-medium"
                >
                  <BarChart2 className="w-4 h-4" /> Detailed Review
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ── Cancellation Dialog – Feature 1 ───────────────────── */}
      {cancelDialogId && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between p-6 border-b border-gray-100">
              <h2 className="text-xl font-bold text-gray-900">Cancel Booking #{cancelDialogId}</h2>
              <button onClick={closeCancelDialog} className="p-2 rounded-lg hover:bg-gray-100 text-gray-500">
                <X className="w-5 h-5" />
              </button>
            </div>

            {refundResult ? (
              /* Refund breakdown after cancellation */
              <div className="p-6 space-y-4">
                <div className={`rounded-xl p-4 text-center ${refundResult.refundPercent === 100 ? 'bg-green-50 border border-green-200' : 'bg-orange-50 border border-orange-200'}`}>
                  <p className="text-3xl font-bold text-green-600 mb-1">
                    ₹{refundResult.refundAmount?.toLocaleString('en-IN')}
                  </p>
                  <p className="text-sm font-semibold text-gray-700">{refundResult.refundPercent}% Refund</p>
                  <p className="text-xs text-gray-500 mt-1">{refundResult.refundPolicy}</p>
                </div>

                {/* Refund progress steps */}
                <div className="space-y-2 text-sm">
                  {[
                    { label: 'Cancellation received', done: true },
                    { label: 'Refund approved', done: true },
                    { label: 'Refund processing (5–7 business days)', done: false },
                    { label: 'Credited to original payment method', done: false },
                  ].map((step, i) => (
                    <div key={i} className="flex items-center gap-2">
                      <span className={`w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 ${step.done ? 'bg-green-500 text-white' : 'bg-gray-200 text-gray-400'}`}>
                        {step.done ? '✓' : i + 1}
                      </span>
                      <span className={step.done ? 'text-gray-800' : 'text-gray-400'}>{step.label}</span>
                    </div>
                  ))}
                </div>

                <button onClick={closeCancelDialog} className="btn-primary w-full">
                  Done
                </button>
              </div>
            ) : (
              /* Reason selection + confirmation */
              <div className="p-6 space-y-4">
                <p className="text-sm text-gray-600">
                  Please select a reason for cancelling. This helps us improve our services.
                </p>

                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2 flex items-center gap-1">
                    <ChevronDown className="w-4 h-4" />
                    Cancellation Reason *
                  </label>
                  <select
                    value={cancelReason}
                    onChange={(e) => setCancelReason(e.target.value)}
                    className="input-field"
                  >
                    <option value="">-- Select a reason --</option>
                    {CANCEL_REASONS.map((r) => (
                      <option key={r} value={r}>{r}</option>
                    ))}
                  </select>
                </div>

                {/* Refund policy preview */}
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-xs text-blue-700 space-y-1">
                  <p className="font-semibold">Refund Policy:</p>
                  <p>• Cancelled ≥ 24 hours before departure → <strong>100% refund</strong></p>
                  <p>• Cancelled &lt; 24 hours before departure → <strong>50% refund</strong></p>
                  <p>• After departure → <strong>No refund</strong></p>
                </div>

                <div className="flex gap-3 pt-2">
                  <button type="button" onClick={closeCancelDialog} className="btn-secondary flex-1">
                    Keep Booking
                  </button>
                  <button
                    onClick={handleCancel}
                    disabled={cancelling === cancelDialogId}
                    className="flex-1 bg-red-500 hover:bg-red-600 text-white font-semibold py-2.5 px-4 rounded-xl transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    {cancelling === cancelDialogId && <Loader className="w-4 h-4 animate-spin" />}
                    Confirm Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Review Modal */}
      {selectedBookingForReview && (
        <ReviewModal
          booking={selectedBookingForReview}
          onClose={() => setSelectedBookingForReview(null)}
          onSuccess={() => {
            setSelectedBookingForReview(null)
            toast.success('Review submitted successfully! 🎉')
          }}
        />
      )}

      {/* Detailed Review Modal */}
      {selectedBookingForDetailedReview && (
        <DetailedReviewModal
          bookingId={selectedBookingForDetailedReview.id}
          onClose={() => setSelectedBookingForDetailedReview(null)}
          onSuccess={() => {
            setSelectedBookingForDetailedReview(null)
          }}
        />
      )}
    </div>
  )
}
