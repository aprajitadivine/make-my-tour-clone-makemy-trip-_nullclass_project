import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import { ThemeProvider } from './context/ThemeContext'
import Navbar from './components/Navbar'
import Dashboard from './pages/Dashboard'
import Login from './pages/Login'
import Register from './pages/Register'
import SearchFlights from './pages/SearchFlights'
import SearchHotels from './pages/SearchHotels'
import BookingPage from './pages/BookingPage'
import MyBookings from './pages/MyBookings'
import TravelStories from './pages/TravelStories'
import RoutePlanner from './pages/RoutePlanner'
import AdminDashboard from './pages/AdminDashboard'

/** Guards routes that require authentication */
function PrivateRoute({ children }) {
  const { isAuthenticated } = useAuth()
  return isAuthenticated ? children : <Navigate to="/login" replace />
}

/** Guards routes that require ROLE_ADMIN */
function AdminRoute({ children }) {
  const { isAuthenticated, user } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  const isAdmin = user?.roles?.includes('ROLE_ADMIN')
  return isAdmin ? children : <Navigate to="/" replace />
}

function AppRoutes() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 dark:text-gray-100">
      <Navbar />
      <main>
        <Routes>
          {/* Public routes */}
          <Route path="/" element={<Dashboard />} />
          <Route
            path="/login"
            element={isAuthenticated ? <Navigate to="/" replace /> : <Login />}
          />
          <Route
            path="/register"
            element={isAuthenticated ? <Navigate to="/" replace /> : <Register />}
          />
          <Route path="/flights" element={<SearchFlights />} />
          <Route path="/hotels" element={<SearchHotels />} />
          <Route path="/stories" element={<TravelStories />} />
          <Route path="/route-planner" element={<RoutePlanner />} />

          {/* Protected routes */}
          <Route
            path="/book/:type/:id"
            element={
              <PrivateRoute>
                <BookingPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/my-bookings"
            element={
              <PrivateRoute>
                <MyBookings />
              </PrivateRoute>
            }
          />

          {/* Admin-only route */}
          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminDashboard />
              </AdminRoute>
            }
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}

function App() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <AuthProvider>
          <ToastProvider>
            <AppRoutes />
          </ToastProvider>
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  )
}

export default App
