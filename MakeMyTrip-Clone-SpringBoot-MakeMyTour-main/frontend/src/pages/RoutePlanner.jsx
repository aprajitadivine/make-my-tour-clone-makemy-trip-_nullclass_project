import { useState, useEffect, Component } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { MapPin, Plus, Trash2, Navigation, Clock, Route } from 'lucide-react'
import { MapContainer, TileLayer, Marker, Polyline, Popup } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import api from '../services/api'

// Fix default Leaflet marker icons (bundler path issue)
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

/** Error boundary to prevent a Leaflet crash from blanking the whole page */
class MapErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }
  static getDerivedStateFromError() {
    return { hasError: true }
  }
  render() {
    if (this.state.hasError) {
      return (
        <div className="h-full flex items-center justify-center text-gray-400 text-sm">
          Map could not be loaded. Please refresh the page.
        </div>
      )
    }
    return this.props.children
  }
}

/**
 * Interactive Route Planner page.
 *
 * - Users add waypoints (name, latitude, longitude)
 * - The Leaflet map (OpenStreetMap tiles – no API key required) renders
 *   markers and a polyline connecting them
 * - On save, the route is sent to POST /api/routes and the backend
 *   calculates total distance (Haversine) and traffic-aware ETA
 * - Saved routes are listed below the map
 *
 * Feature 2 – Interactive Route Planning.
 */
export default function RoutePlanner() {
  const { t } = useTranslation()
  const { isAuthenticated } = useAuth()
  const toast = useToast()

  const [waypoints, setWaypoints] = useState([
    { name: '', latitude: '', longitude: '', sequence: 0 },
  ])
  const [routeName, setRouteName] = useState('')
  const [trafficMultiplier, setTrafficMultiplier] = useState(1.0)
  const [savedRoutes, setSavedRoutes] = useState([])
  const [saving, setSaving] = useState(false)
  const [loadingRoutes, setLoadingRoutes] = useState(false)
  const [mapCenter, setMapCenter] = useState([20.5937, 78.9629]) // India center

  useEffect(() => {
    if (isAuthenticated) fetchMyRoutes()
  }, [isAuthenticated])

  const fetchMyRoutes = async () => {
    setLoadingRoutes(true)
    try {
      const { data } = await api.get('/routes/my')
      setSavedRoutes(data)
    } catch {
      // Non-critical
    } finally {
      setLoadingRoutes(false)
    }
  }

  const addWaypoint = () => {
    setWaypoints([...waypoints, { name: '', latitude: '', longitude: '', sequence: waypoints.length }])
  }

  const removeWaypoint = (index) => {
    if (waypoints.length <= 1) return
    const updated = waypoints.filter((_, i) => i !== index).map((wp, i) => ({ ...wp, sequence: i }))
    setWaypoints(updated)
  }

  const updateWaypoint = (index, field, value) => {
    const updated = [...waypoints]
    updated[index] = { ...updated[index], [field]: value }
    setWaypoints(updated)
  }

  const validWaypoints = waypoints.filter(
    (wp) => wp.name.trim() && !isNaN(parseFloat(wp.latitude)) && !isNaN(parseFloat(wp.longitude))
  )

  const mapPositions = validWaypoints.map((wp) => [parseFloat(wp.latitude), parseFloat(wp.longitude)])

  const handleSave = async () => {
    if (!routeName.trim()) {
      toast.error('Route name is required')
      return
    }
    if (validWaypoints.length < 1) {
      toast.error('At least one valid waypoint is required')
      return
    }
    if (!isAuthenticated) {
      toast.error('Please login to save routes')
      return
    }
    setSaving(true)
    try {
      await api.post('/routes', {
        name: routeName,
        waypoints: validWaypoints.map((wp, i) => ({
          name: wp.name,
          latitude: parseFloat(wp.latitude),
          longitude: parseFloat(wp.longitude),
          sequence: i,
        })),
        trafficMultiplier,
      })
      toast.success('Route saved successfully!')
      setRouteName('')
      setWaypoints([{ name: '', latitude: '', longitude: '', sequence: 0 }])
      setTrafficMultiplier(1.0)
      fetchMyRoutes()
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to save route')
    } finally {
      setSaving(false)
    }
  }

  const handleDeleteRoute = async (id) => {
    try {
      await api.delete(`/routes/${id}`)
      toast.success('Route deleted')
      setSavedRoutes(savedRoutes.filter((r) => r.id !== id))
    } catch {
      toast.error('Failed to delete route')
    }
  }

  const loadRouteOnMap = (route) => {
    if (route.waypoints && route.waypoints.length > 0) {
      const first = route.waypoints[0]
      setMapCenter([first.latitude, first.longitude])
    }
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <div className="flex items-center gap-3 mb-8">
        <Route className="w-8 h-8 text-orange-500" />
        <div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">{t('route.title')}</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-0.5">
            Plan your journey with interactive maps and traffic-aware ETAs
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Left: Waypoint form */}
        <div className="space-y-6">
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-6">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Build Your Route</h2>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Route Name *
              </label>
              <input
                type="text"
                value={routeName}
                onChange={(e) => setRouteName(e.target.value)}
                className="w-full border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
                placeholder="Mumbai → Goa Road Trip"
              />
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Traffic Multiplier (1.0 = normal, 1.5 = heavy traffic)
              </label>
              <input
                type="number"
                value={trafficMultiplier}
                onChange={(e) => setTrafficMultiplier(parseFloat(e.target.value) || 1.0)}
                min="1.0"
                max="3.0"
                step="0.1"
                className="w-32 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
              />
            </div>

            {/* Waypoints list */}
            <div className="space-y-3 mb-4">
              {waypoints.map((wp, index) => (
                <div key={index} className="border border-gray-200 dark:border-gray-600 rounded-lg p-3">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300 flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-orange-500" />
                      Stop {index + 1}
                    </span>
                    {waypoints.length > 1 && (
                      <button
                        onClick={() => removeWaypoint(index)}
                        className="text-red-400 hover:text-red-600 transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                  <input
                    type="text"
                    value={wp.name}
                    onChange={(e) => updateWaypoint(index, 'name', e.target.value)}
                    className="w-full border border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded px-2 py-1.5 text-sm mb-2 focus:outline-none focus:ring-1 focus:ring-orange-400"
                    placeholder={t('route.waypointName')}
                  />
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      type="number"
                      value={wp.latitude}
                      onChange={(e) => updateWaypoint(index, 'latitude', e.target.value)}
                      className="border border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-orange-400"
                      placeholder={t('route.latitude')}
                      step="0.0001"
                    />
                    <input
                      type="number"
                      value={wp.longitude}
                      onChange={(e) => updateWaypoint(index, 'longitude', e.target.value)}
                      className="border border-gray-200 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded px-2 py-1.5 text-sm focus:outline-none focus:ring-1 focus:ring-orange-400"
                      placeholder={t('route.longitude')}
                      step="0.0001"
                    />
                  </div>
                </div>
              ))}
            </div>

            <div className="flex gap-3">
              <button
                onClick={addWaypoint}
                className="flex items-center gap-1.5 text-orange-500 hover:text-orange-600 text-sm font-medium"
              >
                <Plus className="w-4 h-4" /> {t('route.addWaypoint')}
              </button>
            </div>

            {isAuthenticated ? (
              <button
                onClick={handleSave}
                disabled={saving || validWaypoints.length === 0}
                className="mt-4 w-full btn-primary disabled:opacity-50 flex items-center justify-center gap-2"
              >
                <Navigation className="w-4 h-4" />
                {saving ? t('common.loading') : t('route.calculate')}
              </button>
            ) : (
              <p className="mt-4 text-sm text-gray-500 dark:text-gray-400 text-center">
                Login to save routes and view history
              </p>
            )}
          </div>

          {/* Saved routes */}
          {isAuthenticated && savedRoutes.length > 0 && (
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 p-6">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">My Saved Routes</h2>
              {loadingRoutes ? (
                <p className="text-gray-400 text-sm">{t('common.loading')}</p>
              ) : (
                <div className="space-y-3">
                  {savedRoutes.map((route) => (
                    <div
                      key={route.id}
                      className="flex items-start justify-between gap-3 p-3 rounded-lg border border-gray-100 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
                    >
                      <div
                        className="flex-1 cursor-pointer"
                        onClick={() => loadRouteOnMap(route)}
                      >
                        <p className="font-medium text-gray-900 dark:text-gray-100 text-sm">{route.name}</p>
                        <div className="flex items-center gap-3 mt-1 text-xs text-gray-500 dark:text-gray-400">
                          <span className="flex items-center gap-0.5">
                            <MapPin className="w-3 h-3" /> {route.distanceKm} {t('route.km')}
                          </span>
                          <span className="flex items-center gap-0.5">
                            <Clock className="w-3 h-3" /> {route.baseEtaMinutes} {t('route.minutes')}
                          </span>
                          <span>{route.waypoints?.length || 0} stops</span>
                        </div>
                      </div>
                      <button
                        onClick={() => handleDeleteRoute(route.id)}
                        className="text-red-400 hover:text-red-600 transition-colors flex-shrink-0"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Right: Interactive map */}
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
          <div className="p-4 border-b border-gray-100 dark:border-gray-700">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
              Route Preview
            </h2>
            {validWaypoints.length >= 2 && (
              <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
                {validWaypoints.length} waypoints plotted
              </p>
            )}
          </div>
          <div className="h-[500px]">
            <MapErrorBoundary>
              <MapContainer
                center={mapCenter}
                zoom={5}
                className="h-full w-full"
                key={mapCenter.join(',')}
              >
                <TileLayer
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                />
                {mapPositions.map((pos, i) => (
                  <Marker key={i} position={pos}>
                    <Popup>{validWaypoints[i]?.name || `Stop ${i + 1}`}</Popup>
                  </Marker>
                ))}
                {mapPositions.length >= 2 && (
                  <Polyline positions={mapPositions} color="#f97316" weight={3} />
                )}
              </MapContainer>
            </MapErrorBoundary>
          </div>
        </div>
      </div>
    </div>
  )
}
