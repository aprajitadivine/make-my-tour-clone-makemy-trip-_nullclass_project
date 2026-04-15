import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Plane, Hotel, Calendar, TrendingUp, Loader, CheckCircle, Snowflake, BarChart2, CreditCard } from 'lucide-react'
import { flightApi, hotelApi, paymentApi } from '../services/api'
import SeatMap from '../components/SeatMap'
import { useToast } from '../context/ToastContext'
import { useAuth } from '../context/AuthContext'

/**
 * Booking page.
 *
 * Features:
 *  - Loads flight or hotel details based on URL params (type/id)
 *  - Shows the React SeatMap grid with premium seat support (Feature 4)
 *  - Calculates dynamic price (Feature 5)
 *  - Shows price history mini-chart for next 14 days (Feature 5)
 *  - Price freeze button: locks current price for 30 min in localStorage (Feature 5)
 *  - Opens Razorpay checkout after clicking "Proceed to Payment"
 *  - On successful payment, verifies signature and creates booking
 */
export default function BookingPage() {
  const { type, id } = useParams()          // type = 'flight' | 'hotel'
  const navigate = useNavigate()
  const toast = useToast()
  const { user } = useAuth()
  const isFlight = type === 'flight'

  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [booking, setBooking] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState(null)

  const [travelDate, setTravelDate] = useState('')
  const [returnDate, setReturnDate] = useState('')
  const [selectedSeat, setSelectedSeat] = useState(null)
  const [dynamicPrice, setDynamicPrice] = useState(null)

  // Feature 5 – price history
  const [priceHistory, setPriceHistory] = useState([])
  const [frozen, setFrozen] = useState(false)
  const [frozenPrice, setFrozenPrice] = useState(null)
  const freezeKey = `priceFreeze_${type}_${id}`

  useEffect(() => {
    const fetchProduct = isFlight ? flightApi.getById(id) : hotelApi.getById(id)
    fetchProduct
      .then(({ data }) => setProduct(data))
      .catch(() => setError('Product not found'))
      .finally(() => setLoading(false))

    // Load price history for the chart
    const historyApi = isFlight
      ? flightApi.getPriceHistory(id, 14)
      : hotelApi.getPriceHistory(id, 14)
    historyApi.then(({ data }) => setPriceHistory(data.priceHistory || [])).catch(() => {})

    // Restore any active price freeze
    const frozenData = JSON.parse(localStorage.getItem(freezeKey) || 'null')
    if (frozenData && frozenData.expiresAt > Date.now()) {
      setFrozen(true)
      setFrozenPrice(frozenData.price)
    } else {
      localStorage.removeItem(freezeKey)
    }
  }, [id, isFlight, freezeKey])

  // Fetch dynamic price when travel date changes
  useEffect(() => {
    if (!travelDate || !product) return

    const dateStr = new Date(travelDate).toISOString()
    const priceApi = isFlight
      ? flightApi.getDynamicPrice(id, dateStr)
      : hotelApi.getDynamicPrice(id, dateStr)

    priceApi
      .then(({ data }) => setDynamicPrice(data))
      .catch(() => setDynamicPrice(null))
  }, [travelDate, id, isFlight, product])

  /** Feature 5 – freeze current price for 30 minutes */
  const handlePriceFreeze = () => {
    const priceToFreeze = dynamicPrice?.dynamicPrice || product?.basePrice
    if (!priceToFreeze) return
    const expiresAt = Date.now() + 30 * 60 * 1000
    localStorage.setItem(freezeKey, JSON.stringify({ price: priceToFreeze, expiresAt }))
    setFrozen(true)
    setFrozenPrice(priceToFreeze)
    toast.success('Price frozen for 30 minutes! ❄️')
  }

  /** Opens the Razorpay checkout modal */
  const handlePayment = async () => {
    if (!travelDate) {
      setError('Please select a travel/check-in date')
      return
    }

    // Soft validation: warn if no seat/room selected, but allow proceeding
    if (!selectedSeat) {
      toast.warn(
        `No ${isFlight ? 'seat' : 'room'} selected — any available ${isFlight ? 'seat' : 'room'} will be assigned.`
      )
    }

    setBooking(true)
    setError(null)

    try {
      // 1. Ask backend to create a Razorpay order
      const receipt = `${type}-${id}`
      const { data: order } = await paymentApi.createOrder(totalPrice, receipt)

      const DATETIME_LOCAL_LENGTH = 16
      const formatDateTime = (dt) =>
        dt && dt.length === DATETIME_LOCAL_LENGTH ? dt + ':00' : dt

      // 2a. Demo/test mode – skip Razorpay widget and go straight to verify
      if (order.testMode) {
        toast.info('Demo mode: simulating payment (no real charge) 🧪')
        try {
          const { data: createdBooking } = await paymentApi.verify({
            razorpayPaymentId: `pay_demo_${Date.now()}`,
            razorpayOrderId:   order.orderId,
            razorpaySignature: 'demo_signature',
            [isFlight ? 'flightId' : 'hotelId']: Number(id),
            travelDate: formatDateTime(travelDate),
            returnDate: returnDate ? formatDateTime(returnDate) : null,
            seatId: selectedSeat,
          })
          localStorage.removeItem(freezeKey)
          setSuccess(true)
          toast.success(`Booking #${createdBooking.id} confirmed! 🎉`)
          setTimeout(() => navigate('/my-bookings'), 2500)
        } catch (err) {
          setError(err.response?.data?.error || 'Booking creation failed after payment.')
        } finally {
          setBooking(false)
        }
        return
      }

      // 2b. Real Razorpay mode – open the checkout widget
      if (!window.Razorpay) {
        setError('Payment gateway failed to load. Please refresh the page and try again.')
        setBooking(false)
        return
      }

      const options = {
        key: order.keyId,
        amount: order.amount,       // in paise
        currency: order.currency,
        name: 'MakeMyTour',
        description: `${isFlight ? 'Flight' : 'Hotel'} Booking – ${product?.name}`,
        order_id: order.orderId,
        prefill: {
          name: user?.username || '',
          email: user?.email || '',
        },
        theme: { color: '#f97316' },  // orange-500

        handler: async (response) => {
          // 3. Payment succeeded – verify signature and create booking
          try {
            const { data: createdBooking } = await paymentApi.verify({
              razorpayPaymentId: response.razorpay_payment_id,
              razorpayOrderId: response.razorpay_order_id,
              razorpaySignature: response.razorpay_signature,
              [isFlight ? 'flightId' : 'hotelId']: Number(id),
              travelDate: formatDateTime(travelDate),
              returnDate: returnDate ? formatDateTime(returnDate) : null,
              seatId: selectedSeat,
            })

            // Booking saved successfully
            localStorage.removeItem(freezeKey)
            setSuccess(true)
            toast.success(`Booking #${createdBooking.id} confirmed! 🎉`)
            setTimeout(() => navigate('/my-bookings'), 2500)
          } catch (err) {
            setError(err.response?.data?.error || 'Booking creation failed after payment.')
          } finally {
            setBooking(false)
          }
        },

        modal: {
          ondismiss: () => {
            setBooking(false)
            toast.error('Payment was cancelled.')
          },
        },
      }

      const rzp = new window.Razorpay(options)
      rzp.on('payment.failed', (resp) => {
        setBooking(false)
        setError(resp.error?.description || 'Payment failed. Please try again.')
      })
      rzp.open()
    } catch (err) {
      setError(err.response?.data?.error || 'Could not initiate payment. Please try again.')
      setBooking(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Loader className="w-8 h-8 text-orange-500 animate-spin" />
      </div>
    )
  }

  if (!product && !loading) {
    return (
      <div className="text-center py-16">
        <p className="text-red-500 text-xl">Product not found</p>
      </div>
    )
  }

  if (success) {
    return (
      <div className="max-w-md mx-auto px-4 py-16 text-center">
        <CheckCircle className="w-20 h-20 text-green-500 mx-auto mb-4" />
        <h2 className="text-2xl font-bold text-gray-900 mb-2">Booking Confirmed!</h2>
        <p className="text-gray-500">Redirecting to My Bookings…</p>
      </div>
    )
  }

  const finalPrice = frozen ? frozenPrice : (dynamicPrice?.dynamicPrice || product?.basePrice)
  const premiumSurcharge = selectedSeat && product?.premiumSeats?.includes(selectedSeat) ? 500 : 0
  const totalPrice = (finalPrice || 0) + premiumSurcharge

  // Compute price history chart metrics
  const maxPrice = priceHistory.reduce((m, p) => Math.max(m, p.price), 0) || 1

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 text-sm text-gray-500 mb-6">
        <span>Home</span>
        <span>/</span>
        <span>{isFlight ? 'Flights' : 'Hotels'}</span>
        <span>/</span>
        <span className="text-orange-500 font-semibold">Book</span>
      </div>

      <h1 className="text-2xl font-bold text-gray-900 mb-6 flex items-center gap-2">
        {isFlight ? <Plane className="w-6 h-6 text-orange-500" /> : <Hotel className="w-6 h-6 text-orange-500" />}
        {product?.name}
      </h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Details + Seat Map + Price History */}
        <div className="lg:col-span-2 space-y-6">
          {/* Product details */}
          <div className="card">
            <h2 className="font-bold text-gray-800 mb-3 text-lg">
              {isFlight ? 'Flight Details' : 'Hotel Details'}
            </h2>
            <dl className="grid grid-cols-2 gap-3 text-sm">
              {isFlight ? (
                <>
                  <dt className="text-gray-500">Flight No</dt>
                  <dd className="font-semibold">{product.flightNumber}</dd>
                  <dt className="text-gray-500">Route</dt>
                  <dd className="font-semibold">{product.origin} → {product.destination}</dd>
                  <dt className="text-gray-500">Departs</dt>
                  <dd className="font-semibold">
                    {new Date(product.departureTime).toLocaleString('en-IN')}
                  </dd>
                  <dt className="text-gray-500">Status</dt>
                  <dd>
                    <span className={`badge-${product.status?.toLowerCase().replace('_', '-')}`}>
                      {product.status?.replace('_', ' ')}
                    </span>
                    {product.status === 'DELAYED' && product.delayMinutes > 0 && (
                      <span className="ml-2 text-xs text-orange-600">
                        +{product.delayMinutes} min · {product.delayReason}
                      </span>
                    )}
                  </dd>
                </>
              ) : (
                <>
                  <dt className="text-gray-500">City</dt>
                  <dd className="font-semibold">{product.city}</dd>
                  <dt className="text-gray-500">Address</dt>
                  <dd className="font-semibold text-xs">{product.address}</dd>
                  <dt className="text-gray-500">Stars</dt>
                  <dd className="font-semibold">{'⭐'.repeat(product.starRating)}</dd>
                  <dt className="text-gray-500">Amenities</dt>
                  <dd className="text-xs">{product.amenities?.split(',').join(', ')}</dd>
                </>
              )}
            </dl>
          </div>

          {/* Seat/Room Grid – Feature 4 */}
          <div>
            <h2 className="font-bold text-gray-800 mb-3 text-lg flex items-center gap-2">
              {isFlight ? '✈ Select Your Seat' : '🏨 Select Your Room'}
            </h2>
            <SeatMap
              rows={isFlight ? product.seatRows : product.roomRows}
              cols={isFlight ? product.seatCols : product.roomCols}
              bookedSeats={isFlight ? (product.bookedSeats || []) : (product.bookedRooms || [])}
              premiumSeats={isFlight ? (product.premiumSeats || []) : []}
              onSeatSelect={setSelectedSeat}
              type={isFlight ? 'flight' : 'hotel'}
              productId={id}
            />
          </div>

          {/* Price History Chart – Feature 5 */}
          {priceHistory.length > 0 && (
            <div className="card">
              <h2 className="font-bold text-gray-800 mb-3 text-lg flex items-center gap-2">
                <BarChart2 className="w-5 h-5 text-orange-500" />
                Price Trend – Next 14 Days
              </h2>
              <div className="flex items-end gap-1 h-20 overflow-x-auto pb-1">
                {priceHistory.map((entry) => {
                  const heightPct = Math.max(10, Math.round((entry.price / maxPrice) * 100))
                  return (
                    <div
                      key={entry.date}
                      className="flex flex-col items-center gap-0.5 flex-1 min-w-[28px]"
                      title={`${entry.date}: ₹${entry.price?.toLocaleString('en-IN')}${entry.surgeDay ? ' ⚡ Surge' : ''}`}
                    >
                      <div
                        className={`w-full rounded-t ${entry.surgeDay ? 'bg-orange-400' : 'bg-blue-300'}`}
                        style={{ height: `${heightPct}%` }}
                      />
                      <span className="text-[9px] text-gray-400">
                        {entry.date?.slice(5)}
                      </span>
                    </div>
                  )
                })}
              </div>
              <div className="flex gap-4 mt-3 text-xs">
                <span className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded bg-blue-300 inline-block" /> Regular day
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="w-3 h-3 rounded bg-orange-400 inline-block" /> ⚡ Surge day (+20%)
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Right: Booking Summary */}
        <div className="space-y-4">
          <div className="card sticky top-20">
            <h2 className="font-bold text-gray-800 text-lg mb-4">Booking Summary</h2>

            {/* Travel Date */}
            <div className="mb-3">
              <label className="block text-sm font-semibold text-gray-600 mb-1.5 flex items-center gap-1">
                <Calendar className="w-4 h-4" />
                {isFlight ? 'Travel Date' : 'Check-in Date'} *
              </label>
              <input
                type="datetime-local"
                className="input-field text-sm"
                value={travelDate}
                onChange={(e) => setTravelDate(e.target.value)}
                min={new Date().toISOString().slice(0, 16)}
              />
            </div>

            {/* Return Date (hotels only) */}
            {!isFlight && (
              <div className="mb-3">
                <label className="block text-sm font-semibold text-gray-600 mb-1.5">
                  Check-out Date
                </label>
                <input
                  type="datetime-local"
                  className="input-field text-sm"
                  value={returnDate}
                  onChange={(e) => setReturnDate(e.target.value)}
                  min={travelDate || new Date().toISOString().slice(0, 16)}
                />
              </div>
            )}

            {/* Selected seat */}
            {selectedSeat && (
              <div className="bg-orange-50 border border-orange-200 rounded-lg p-2.5 mb-3 text-sm">
                <span className="text-orange-700 font-semibold">
                  {isFlight ? '💺 Seat' : '🚪 Room'}: {selectedSeat}
                  {product?.premiumSeats?.includes(selectedSeat) && (
                    <span className="ml-2 text-xs bg-yellow-100 text-yellow-700 px-1.5 py-0.5 rounded-full">
                      ⭐ Premium +₹500
                    </span>
                  )}
                </span>
              </div>
            )}

            {/* Dynamic pricing */}
            {!frozen && dynamicPrice && (
              <div className={`rounded-lg p-3 mb-3 text-sm ${dynamicPrice.surgeActive ? 'bg-orange-50 border border-orange-200' : 'bg-green-50 border border-green-200'}`}>
                <p className="flex items-center gap-1 font-semibold mb-1">
                  <TrendingUp className="w-4 h-4" />
                  {dynamicPrice.surgeActive ? '⚡ Surge Pricing (+20%)' : '✅ Regular Pricing'}
                </p>
                <p className="text-gray-600 text-xs">
                  Base ₹{dynamicPrice.basePrice?.toLocaleString('en-IN')} →{' '}
                  Final <strong>₹{dynamicPrice.dynamicPrice?.toLocaleString('en-IN')}</strong>
                </p>
              </div>
            )}

            {/* Frozen price badge – Feature 5 */}
            {frozen && frozenPrice && (
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-3 text-sm flex items-center gap-2">
                <Snowflake className="w-4 h-4 text-blue-500" />
                <span className="text-blue-700">
                  Price frozen at <strong>₹{frozenPrice?.toLocaleString('en-IN')}</strong>
                  <span className="block text-xs text-blue-500">Valid for 30 min</span>
                </span>
              </div>
            )}

            {/* Price Freeze button – Feature 5 */}
            {!frozen && (dynamicPrice || product?.basePrice) && (
              <button
                onClick={handlePriceFreeze}
                className="w-full flex items-center justify-center gap-2 text-sm text-blue-600 hover:text-blue-700 border border-blue-300 hover:border-blue-500 rounded-xl py-2 mb-3 transition-colors"
              >
                <Snowflake className="w-4 h-4" />
                Freeze this price for 30 min
              </button>
            )}

            {/* Price breakdown */}
            <div className="border-t border-gray-100 pt-3 mb-4">
              <div className="flex justify-between text-sm text-gray-500 mb-1">
                <span>Base price</span>
                <span>₹{product?.basePrice?.toLocaleString('en-IN')}</span>
              </div>
              {!frozen && dynamicPrice?.surgeActive && (
                <div className="flex justify-between text-sm text-orange-600 mb-1">
                  <span>Weekend surge</span>
                  <span>+20%</span>
                </div>
              )}
              {premiumSurcharge > 0 && (
                <div className="flex justify-between text-sm text-yellow-700 mb-1">
                  <span>⭐ Premium seat</span>
                  <span>+₹500</span>
                </div>
              )}
              <div className="flex justify-between font-bold text-gray-900 mt-2">
                <span>Total</span>
                <span className="text-orange-500 text-xl">
                  ₹{totalPrice?.toLocaleString('en-IN')}
                  {!isFlight && <span className="text-sm font-normal text-gray-400">/night</span>}
                </span>
              </div>
            </div>

            {/* Error */}
            {error && (
              <p className="text-red-600 text-sm bg-red-50 px-3 py-2 rounded-lg mb-3">{error}</p>
            )}

            {/* Pay button */}
            <button
              onClick={handlePayment}
              disabled={booking}
              className="btn-primary w-full py-3 flex items-center justify-center gap-2"
            >
              {booking
                ? <Loader className="w-4 h-4 animate-spin" />
                : <CreditCard className="w-4 h-4" />}
              {booking ? 'Processing…' : `Pay ₹${totalPrice?.toLocaleString('en-IN')}`}
            </button>

            <p className="text-xs text-gray-400 text-center mt-2">
              Secured by Razorpay · Free cancellation · Instant confirmation
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
