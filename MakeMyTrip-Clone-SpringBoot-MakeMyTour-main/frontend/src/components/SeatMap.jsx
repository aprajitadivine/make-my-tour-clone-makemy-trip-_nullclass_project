import { useState, useEffect, useRef } from 'react'

/**
 * Feature 4 – Seat/Room Grid
 *
 * Renders a 2D clickable grid of seats (for flights) or rooms (for hotels).
 * - Green  : available  → clickable
 * - Red    : booked     → disabled
 * - Orange : selected   → currently chosen by the user
 * - Gold   : premium    → available premium seat (first class / extra-legroom)
 *
 * The selected seatId is passed up to the parent via onSeatSelect(seatId).
 *
 * For flights: seat IDs are generated as "{row}{colLetter}" e.g. "1A", "3C"
 * For hotels:  room IDs are generated as "{floor}{roomNumber}" e.g. "101", "203"
 *
 * Premium seats are highlighted and include an upsell badge (+₹500).
 * The user's last selected seat is saved in localStorage so it can be
 * restored if they navigate away and come back.
 */
export default function SeatMap({
  rows,
  cols,
  bookedSeats = [],
  premiumSeats = [],
  onSeatSelect,
  type = 'flight',
  productId,
}) {
  const storageKey = productId ? `selectedSeat_${type}_${productId}` : null

  const [selectedSeat, setSelectedSeat] = useState(() => {
    if (storageKey) {
      return localStorage.getItem(storageKey) || null
    }
    return null
  })

  // Restore preference on mount and propagate it up
  // Capture the callback in a ref so the mount-only effect doesn't need it as a dep
  const onSeatSelectRef = useRef(onSeatSelect)
  useEffect(() => { onSeatSelectRef.current = onSeatSelect })

  // Restore saved preference on mount only
  useEffect(() => {
    if (selectedSeat) {
      onSeatSelectRef.current(selectedSeat)
    }
  }, [selectedSeat]) // selectedSeat is stable (initialised from localStorage)

  // Column labels: A-F for flights, 1-N for hotels
  const colLabels = type === 'flight'
    ? ['A', 'B', 'C', 'D', 'E', 'F'].slice(0, cols)
    : Array.from({ length: cols }, (_, i) => String(i + 1).padStart(2, '0'))

  /** Generates a seat/room ID from row and column indices */
  const getSeatId = (rowIndex, colIndex) =>
    `${rowIndex + 1}${colLabels[colIndex]}`   // "3A" or "101"

  const handleSelect = (seatId) => {
    const newSelection = selectedSeat === seatId ? null : seatId
    setSelectedSeat(newSelection)
    onSeatSelect(newSelection)
    if (storageKey) {
      if (newSelection) {
        localStorage.setItem(storageKey, newSelection)
      } else {
        localStorage.removeItem(storageKey)
      }
    }
  }

  const getSeatClass = (seatId) => {
    if (selectedSeat === seatId) return 'seat-selected'
    if (bookedSeats.includes(seatId)) return 'seat-booked'
    if (premiumSeats.includes(seatId)) return 'seat-premium'
    return 'seat-available'
  }

  const isPremium = (seatId) => premiumSeats.includes(seatId)

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-4 overflow-x-auto">
      {/* Legend */}
      <div className="flex gap-4 mb-4 text-xs flex-wrap">
        <span className="flex items-center gap-1.5">
          <span className="w-4 h-4 rounded bg-green-200 border-2 border-green-400 inline-block" />
          Available
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-4 h-4 rounded bg-red-100 border-2 border-red-300 inline-block" />
          Booked
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-4 h-4 rounded bg-orange-400 border-2 border-orange-600 inline-block" />
          Selected
        </span>
        {premiumSeats.length > 0 && (
          <span className="flex items-center gap-1.5">
            <span className="w-4 h-4 rounded bg-yellow-300 border-2 border-yellow-500 inline-block" />
            Premium (+₹500)
          </span>
        )}
      </div>

      {/* Column headers */}
      {type === 'flight' && (
        <div
          className="grid gap-1 mb-2"
          style={{ gridTemplateColumns: `2rem repeat(${cols}, 1fr)` }}
        >
          <div />
          {colLabels.map((label) => (
            <div key={label} className="text-center text-xs font-bold text-gray-400">
              {label}
            </div>
          ))}
        </div>
      )}

      {/* Seat/room grid */}
      <div className="space-y-1">
        {Array.from({ length: rows }).map((_, rowIndex) => (
          <div
            key={rowIndex}
            className="grid gap-1 items-center"
            style={{ gridTemplateColumns: `2rem repeat(${cols}, 1fr)` }}
          >
            {/* Row number */}
            <span className="text-xs text-gray-400 text-right pr-1">
              {type === 'flight' ? rowIndex + 1 : `F${rowIndex + 1}`}
            </span>

            {Array.from({ length: cols }).map((_, colIndex) => {
              const seatId = getSeatId(rowIndex, colIndex)
              const isBooked = bookedSeats.includes(seatId)
              const premium = isPremium(seatId)

              return (
                <button
                  key={seatId}
                  className={`${getSeatClass(seatId)} h-8 text-xs font-medium flex items-center justify-center min-w-[2rem] transition-all duration-150`}
                  onClick={() => !isBooked && handleSelect(seatId)}
                  disabled={isBooked}
                  title={
                    isBooked
                      ? `${seatId} – Already booked`
                      : premium
                        ? `${seatId} – Premium seat (+₹500 extra legroom)`
                        : `${seatId} – Click to select`
                  }
                  aria-label={`Seat ${seatId} ${isBooked ? 'unavailable' : premium ? 'premium' : 'available'}`}
                >
                  {type === 'hotel' ? seatId : seatId.replace(/^\d+/, '')}
                </button>
              )
            })}
          </div>
        ))}
      </div>

      {/* Selected seat summary */}
      {selectedSeat && (
        <div className={`mt-4 p-3 rounded-lg text-sm border ${isPremium(selectedSeat) ? 'bg-yellow-50 border-yellow-200' : 'bg-orange-50 border-orange-200'}`}>
          <p className={`font-semibold ${isPremium(selectedSeat) ? 'text-yellow-800' : 'text-orange-800'}`}>
            {isPremium(selectedSeat) ? '⭐ Premium ' : ''}
            {type === 'flight' ? '✈ Seat' : '🏨 Room'} <strong>{selectedSeat}</strong> selected
            {isPremium(selectedSeat) && (
              <span className="ml-2 text-xs font-normal bg-yellow-200 text-yellow-700 px-1.5 py-0.5 rounded-full">
                +₹500 extra legroom
              </span>
            )}
          </p>
          <p className={`text-xs mt-0.5 ${isPremium(selectedSeat) ? 'text-yellow-600' : 'text-orange-600'}`}>
            Your preference is saved automatically.
          </p>
        </div>
      )}
    </div>
  )
}
