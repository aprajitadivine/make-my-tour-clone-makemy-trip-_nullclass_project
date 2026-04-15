import { useState } from 'react'
import { MapPin, Star, Wifi, Dumbbell, Waves, Coffee, TrendingUp } from 'lucide-react'
import { hotelApi } from '../services/api'

/**
 * Hotel listing card with star rating display and dynamic pricing check.
 */
export default function HotelCard({ hotel, onBook }) {
  const [dynamicPrice, setDynamicPrice] = useState(null)

  const handleCheckPrice = async () => {
    try {
      const today = new Date().toISOString().slice(0, 19)
      const { data } = await hotelApi.getDynamicPrice(hotel.id, today)
      setDynamicPrice(data)
    } catch (err) {
      console.error('Price check failed:', err)
    }
  }

  const amenityIcons = {
    WiFi: <Wifi className="w-3 h-3" />,
    Pool: <Waves className="w-3 h-3" />,
    Gym: <Dumbbell className="w-3 h-3" />,
    Restaurant: <Coffee className="w-3 h-3" />,
  }

  const amenities = hotel.amenities?.split(',') || []

  return (
    <div className="card border border-gray-100">
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div>
          <h3 className="font-bold text-gray-900 text-lg leading-tight">{hotel.name}</h3>
          <div className="flex items-center gap-1 mt-1">
            {Array.from({ length: 5 }).map((_, i) => (
              <Star
                key={i}
                className={`w-4 h-4 ${i < hotel.starRating ? 'text-yellow-400 fill-yellow-400' : 'text-gray-200 fill-gray-200'}`}
              />
            ))}
          </div>
        </div>
        <span className="text-xs bg-blue-50 text-blue-700 font-semibold px-2 py-1 rounded-full">
          {hotel.category?.replace('_', ' ')}
        </span>
      </div>

      {/* Location */}
      <p className="flex items-center gap-1 text-sm text-gray-500 mb-3">
        <MapPin className="w-4 h-4 text-orange-400 flex-shrink-0" />
        {hotel.address}
      </p>

      {/* Description */}
      <p className="text-gray-600 text-sm mb-4 line-clamp-2">{hotel.description}</p>

      {/* Amenities */}
      {amenities.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-4">
          {amenities.slice(0, 5).map((amenity) => (
            <span
              key={amenity}
              className="flex items-center gap-1 text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded-full"
            >
              {amenityIcons[amenity] || null}
              {amenity}
            </span>
          ))}
          {amenities.length > 5 && (
            <span className="text-xs text-gray-400">+{amenities.length - 5} more</span>
          )}
        </div>
      )}

      {/* Dynamic Pricing Info */}
      {dynamicPrice && (
        <div className={`rounded-lg p-3 mb-4 text-sm ${dynamicPrice.surgeActive ? 'bg-orange-50 border border-orange-200' : 'bg-green-50 border border-green-200'}`}>
          <div className="flex items-center gap-1 font-semibold mb-1">
            <TrendingUp className={`w-4 h-4 ${dynamicPrice.surgeActive ? 'text-orange-500' : 'text-green-500'}`} />
            {dynamicPrice.surgeActive ? (
              <span className="text-orange-700">Weekend Surge (+20%)</span>
            ) : (
              <span className="text-green-700">Regular Pricing</span>
            )}
          </div>
          <p className="text-gray-600">
            Base: ₹{dynamicPrice.basePrice?.toLocaleString('en-IN')} →
            Final: <strong>₹{dynamicPrice.dynamicPrice?.toLocaleString('en-IN')}</strong>/night
          </p>
        </div>
      )}

      {/* Rooms & Actions */}
      <div className="flex items-center justify-between pt-4 border-t border-gray-100">
        <div>
          <p className="text-sm text-gray-500">per night</p>
          <p className="text-2xl font-bold text-orange-500">
            ₹{hotel.basePrice?.toLocaleString('en-IN')}
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
            onClick={() => onBook(hotel)}
            className="btn-primary text-sm"
          >
            Book Now
          </button>
        </div>
      </div>
    </div>
  )
}
