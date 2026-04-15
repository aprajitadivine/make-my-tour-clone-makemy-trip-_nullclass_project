import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import { useTranslation } from 'react-i18next'
import { Plane, Hotel, BookOpen, LogOut, LogIn, UserPlus, Menu, X, Moon, Sun, Globe, Map, BookMarked, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import api from '../services/api'
import { STORAGE_KEY_LANGUAGE } from '../constants'

/**
 * Responsive navigation bar.
 * Shows different links for authenticated vs. unauthenticated users.
 * Collapses to a hamburger menu on mobile (< md).
 *
 * Includes:
 *  - Dark mode toggle (Feature 5)
 *  - Language selector (Feature 3)
 *  - Travel Stories and Route Planner links (Features 2 & 4)
 */
export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const isAdmin = user?.roles?.includes('ROLE_ADMIN')
  const { isDark, toggleTheme } = useTheme()
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  /** Switches the UI language and persists the preference */
  const changeLanguage = async (lang) => {
    i18n.changeLanguage(lang)
    localStorage.setItem(STORAGE_KEY_LANGUAGE, lang)
    try {
      await api.patch('/users/me/language', { language: lang })
    } catch {
      // Non-critical – localStorage already persists the preference
    }
  }

  return (
    <nav className="bg-white dark:bg-gray-800 shadow-sm sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 font-bold text-xl text-orange-500">
            <Plane className="w-6 h-6" />
            <span>MakeMyTour</span>
          </Link>

          {/* Desktop nav links */}
          <div className="hidden md:flex items-center gap-6">
            <Link
              to="/flights"
              className="flex items-center gap-1.5 text-gray-600 dark:text-gray-300 hover:text-orange-500 transition-colors font-medium"
            >
              <Plane className="w-4 h-4" />
              {t('nav.flights')}
            </Link>
            <Link
              to="/hotels"
              className="flex items-center gap-1.5 text-gray-600 dark:text-gray-300 hover:text-orange-500 transition-colors font-medium"
            >
              <Hotel className="w-4 h-4" />
              {t('nav.hotels')}
            </Link>
            <Link
              to="/stories"
              className="flex items-center gap-1.5 text-gray-600 dark:text-gray-300 hover:text-orange-500 transition-colors font-medium"
            >
              <BookMarked className="w-4 h-4" />
              {t('nav.stories')}
            </Link>
            <Link
              to="/route-planner"
              className="flex items-center gap-1.5 text-gray-600 dark:text-gray-300 hover:text-orange-500 transition-colors font-medium"
            >
              <Map className="w-4 h-4" />
              {t('nav.routePlanner')}
            </Link>

            {isAuthenticated ? (
              <>
                <Link
                  to="/my-bookings"
                  className="flex items-center gap-1.5 text-gray-600 dark:text-gray-300 hover:text-orange-500 transition-colors font-medium"
                >
                  <BookOpen className="w-4 h-4" />
                  {t('nav.myBookings')}
                </Link>
                {isAdmin && (
                  <Link
                    to="/admin"
                    className="flex items-center gap-1.5 text-purple-600 dark:text-purple-400 hover:text-purple-700 transition-colors font-medium"
                  >
                    <ShieldCheck className="w-4 h-4" />
                    Admin
                  </Link>
                )}
                <div className="flex items-center gap-3 ml-4 pl-4 border-l border-gray-200 dark:border-gray-600">
                  <span className="text-sm text-gray-500 dark:text-gray-400">
                    Hi, <span className="font-semibold text-gray-800 dark:text-gray-100">{user?.username}</span>
                  </span>
                  <button
                    onClick={handleLogout}
                    className="flex items-center gap-1.5 text-red-500 hover:text-red-600 font-medium transition-colors"
                  >
                    <LogOut className="w-4 h-4" />
                    {t('nav.logout')}
                  </button>
                </div>
              </>
            ) : (
              <div className="flex items-center gap-3 ml-4">
                <Link
                  to="/login"
                  className="flex items-center gap-1.5 text-gray-600 dark:text-gray-300 hover:text-orange-500 font-medium transition-colors"
                >
                  <LogIn className="w-4 h-4" />
                  {t('nav.login')}
                </Link>
                <Link to="/register" className="btn-primary flex items-center gap-1.5">
                  <UserPlus className="w-4 h-4" />
                  {t('nav.register')}
                </Link>
              </div>
            )}

            {/* Language selector (Feature 3) */}
            <div className="flex items-center gap-1 ml-2">
              <Globe className="w-4 h-4 text-gray-500 dark:text-gray-400" />
              <select
                value={i18n.language}
                onChange={(e) => changeLanguage(e.target.value)}
                className="text-sm bg-transparent text-gray-600 dark:text-gray-300 border-none outline-none cursor-pointer"
                aria-label={t('language.select')}
              >
                <option value="en">EN</option>
                <option value="hi">हि</option>
                <option value="fr">FR</option>
              </select>
            </div>

            {/* Dark mode toggle (Feature 5) */}
            <button
              onClick={toggleTheme}
              aria-label={isDark ? t('theme.light') : t('theme.dark')}
              className="p-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
            >
              {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>
          </div>

          {/* Mobile hamburger */}
          <div className="md:hidden flex items-center gap-2">
            {/* Dark mode toggle on mobile */}
            <button
              onClick={toggleTheme}
              aria-label={isDark ? t('theme.light') : t('theme.dark')}
              className="p-2 rounded-lg text-gray-600 dark:text-gray-300"
            >
              {isDark ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
            </button>
            <button
              className="p-2 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
              onClick={() => setMenuOpen(!menuOpen)}
              aria-label="Toggle menu"
            >
              {menuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {menuOpen && (
        <div className="md:hidden bg-white dark:bg-gray-800 border-t border-gray-100 dark:border-gray-700 px-4 py-4 space-y-3">
          <Link
            to="/flights"
            className="flex items-center gap-2 text-gray-700 dark:text-gray-300 py-2"
            onClick={() => setMenuOpen(false)}
          >
            <Plane className="w-4 h-4 text-orange-500" /> {t('nav.flights')}
          </Link>
          <Link
            to="/hotels"
            className="flex items-center gap-2 text-gray-700 dark:text-gray-300 py-2"
            onClick={() => setMenuOpen(false)}
          >
            <Hotel className="w-4 h-4 text-orange-500" /> {t('nav.hotels')}
          </Link>
          <Link
            to="/stories"
            className="flex items-center gap-2 text-gray-700 dark:text-gray-300 py-2"
            onClick={() => setMenuOpen(false)}
          >
            <BookMarked className="w-4 h-4 text-orange-500" /> {t('nav.stories')}
          </Link>
          <Link
            to="/route-planner"
            className="flex items-center gap-2 text-gray-700 dark:text-gray-300 py-2"
            onClick={() => setMenuOpen(false)}
          >
            <Map className="w-4 h-4 text-orange-500" /> {t('nav.routePlanner')}
          </Link>
          {isAuthenticated ? (
            <>
              <Link
                to="/my-bookings"
                className="flex items-center gap-2 text-gray-700 dark:text-gray-300 py-2"
                onClick={() => setMenuOpen(false)}
              >
                <BookOpen className="w-4 h-4 text-orange-500" /> {t('nav.myBookings')}
              </Link>
              {isAdmin && (
                <Link
                  to="/admin"
                  className="flex items-center gap-2 text-purple-600 dark:text-purple-400 py-2"
                  onClick={() => setMenuOpen(false)}
                >
                  <ShieldCheck className="w-4 h-4" /> Admin
                </Link>
              )}
              <button
                onClick={() => { handleLogout(); setMenuOpen(false) }}
                className="flex items-center gap-2 text-red-500 py-2 w-full"
              >
                <LogOut className="w-4 h-4" /> {t('nav.logout')}
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="flex items-center gap-2 text-gray-700 dark:text-gray-300 py-2"
                onClick={() => setMenuOpen(false)}
              >
                <LogIn className="w-4 h-4 text-orange-500" /> {t('nav.login')}
              </Link>
              <Link
                to="/register"
                className="btn-primary block text-center"
                onClick={() => setMenuOpen(false)}
              >
                {t('nav.register')}
              </Link>
            </>
          )}
          {/* Language selector on mobile */}
          <div className="flex items-center gap-2 py-2">
            <Globe className="w-4 h-4 text-gray-500" />
            <select
              value={i18n.language}
              onChange={(e) => { changeLanguage(e.target.value); setMenuOpen(false) }}
              className="text-sm bg-transparent text-gray-600 dark:text-gray-300 border border-gray-300 dark:border-gray-600 rounded px-1"
            >
              <option value="en">English</option>
              <option value="hi">हिंदी</option>
              <option value="fr">Français</option>
            </select>
          </div>
        </div>
      )}
    </nav>
  )
}
