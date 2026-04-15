import { useState, useRef, useEffect } from 'react'
import { MapPin, ChevronDown } from 'lucide-react'

/**
 * Searchable city dropdown for Indian cities.
 * Used for flight origin/destination and hotel city selection.
 */

const INDIAN_CITIES = [
  { name: 'Delhi', code: 'DEL', state: 'Delhi' },
  { name: 'Mumbai', code: 'BOM', state: 'Maharashtra' },
  { name: 'Bangalore', code: 'BLR', state: 'Karnataka' },
  { name: 'Chennai', code: 'MAA', state: 'Tamil Nadu' },
  { name: 'Kolkata', code: 'CCU', state: 'West Bengal' },
  { name: 'Hyderabad', code: 'HYD', state: 'Telangana' },
  { name: 'Ahmedabad', code: 'AMD', state: 'Gujarat' },
  { name: 'Pune', code: 'PNQ', state: 'Maharashtra' },
  { name: 'Goa', code: 'GOI', state: 'Goa' },
  { name: 'Jaipur', code: 'JAI', state: 'Rajasthan' },
  { name: 'Lucknow', code: 'LKO', state: 'Uttar Pradesh' },
  { name: 'Shimla', code: 'SLV', state: 'Himachal Pradesh' },
  { name: 'Cochin', code: 'COK', state: 'Kerala' },
  { name: 'Kottayam', code: 'CJT', state: 'Kerala' },
  { name: 'Chandigarh', code: 'IXC', state: 'Chandigarh' },
  { name: 'Varanasi', code: 'VNS', state: 'Uttar Pradesh' },
  { name: 'Amritsar', code: 'ATQ', state: 'Punjab' },
  { name: 'Udaipur', code: 'UDR', state: 'Rajasthan' },
  { name: 'Jodhpur', code: 'JDH', state: 'Rajasthan' },
  { name: 'Guwahati', code: 'GAU', state: 'Assam' },
  { name: 'Patna', code: 'PAT', state: 'Bihar' },
  { name: 'Bhopal', code: 'BHO', state: 'Madhya Pradesh' },
  { name: 'Indore', code: 'IDR', state: 'Madhya Pradesh' },
  { name: 'Srinagar', code: 'SXR', state: 'Jammu & Kashmir' },
  { name: 'Thiruvananthapuram', code: 'TRV', state: 'Kerala' },
  { name: 'Coimbatore', code: 'CJB', state: 'Tamil Nadu' },
  { name: 'Mangalore', code: 'IXE', state: 'Karnataka' },
  { name: 'Nagpur', code: 'NAG', state: 'Maharashtra' },
  { name: 'Visakhapatnam', code: 'VTZ', state: 'Andhra Pradesh' },
  { name: 'Ranchi', code: 'IXR', state: 'Jharkhand' },
  { name: 'Dehradun', code: 'DED', state: 'Uttarakhand' },
  { name: 'Agra', code: 'AGR', state: 'Uttar Pradesh' },
  { name: 'Mysore', code: 'MYQ', state: 'Karnataka' },
  { name: 'Pondicherry', code: 'PNY', state: 'Puducherry' },
  { name: 'Darjeeling', code: 'IXB', state: 'West Bengal' },
  { name: 'Madurai', code: 'IXM', state: 'Tamil Nadu' },
  { name: 'Leh', code: 'IXL', state: 'Ladakh' },
  { name: 'Port Blair', code: 'IXZ', state: 'Andaman & Nicobar' },
  { name: 'Raipur', code: 'RPR', state: 'Chhattisgarh' },
  { name: 'Bhubaneswar', code: 'BBI', state: 'Odisha' },
]

export default function CityDropdown({ value, onChange, placeholder = 'Select city', showAirportCode = false }) {
  const [isOpen, setIsOpen] = useState(false)
  const [search, setSearch] = useState(value || '')
  const wrapperRef = useRef(null)
  const inputRef = useRef(null)

  // Sync external value into search text
  useEffect(() => {
    setSearch(value || '')
  }, [value])

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
        setIsOpen(false)
        // Restore to the selected value if user typed something invalid
        if (search !== value) {
          setSearch(value || '')
        }
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [search, value])

  const filteredCities = INDIAN_CITIES.filter((city) => {
    const q = search.toLowerCase()
    return (
      city.name.toLowerCase().includes(q) ||
      city.code.toLowerCase().includes(q) ||
      city.state.toLowerCase().includes(q)
    )
  })

  const handleSelect = (city) => {
    setSearch(city.name)
    onChange(city.name)
    setIsOpen(false)
  }

  const handleInputChange = (e) => {
    setSearch(e.target.value)
    setIsOpen(true)
    // If the user clears the input, also clear the value
    if (e.target.value === '') {
      onChange('')
    }
  }

  const handleFocus = () => {
    setIsOpen(true)
    // Select all text on focus for easy replacement
    if (inputRef.current) {
      inputRef.current.select()
    }
  }

  return (
    <div ref={wrapperRef} className="relative">
      <div className="relative">
        <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
        <input
          ref={inputRef}
          type="text"
          className="input-field pl-9 pr-8"
          placeholder={placeholder}
          value={search}
          onChange={handleInputChange}
          onFocus={handleFocus}
          autoComplete="off"
        />
        <ChevronDown
          className={`absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
        />
      </div>

      {isOpen && (
        <ul className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-xl shadow-lg max-h-60 overflow-y-auto">
          {filteredCities.length > 0 ? (
            filteredCities.map((city) => (
              <li
                key={city.code}
                onClick={() => handleSelect(city)}
                className={`flex items-center gap-3 px-4 py-2.5 cursor-pointer hover:bg-orange-50 transition-colors ${
                  city.name === value ? 'bg-orange-50 text-orange-600 font-semibold' : 'text-gray-700'
                }`}
              >
                <MapPin className="w-4 h-4 text-orange-400 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <span className="font-medium">{city.name}</span>
                  {showAirportCode && (
                    <span className="ml-1.5 text-xs text-gray-400">({city.code})</span>
                  )}
                  <span className="block text-xs text-gray-400 truncate">{city.state}</span>
                </div>
              </li>
            ))
          ) : (
            <li className="px-4 py-3 text-sm text-gray-400 text-center">No cities found</li>
          )}
        </ul>
      )}
    </div>
  )
}

export { INDIAN_CITIES }
