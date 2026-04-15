import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import en from './locales/en/translation.json'
import hi from './locales/hi/translation.json'
import fr from './locales/fr/translation.json'
import { STORAGE_KEY_LANGUAGE } from './constants'

/**
 * i18next configuration for Multi-Language Support (Feature 3).
 *
 * Dual-layer persistence strategy:
 *  1. Reads initial language from localStorage (instant, no network round-trip)
 *  2. When user is authenticated, language preference is also synced to
 *     the MongoDB/H2 User profile via PATCH /api/users/me/language
 *     (handled by the LanguageSelector component in Navbar.jsx)
 */
i18n
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      hi: { translation: hi },
      fr: { translation: fr },
    },
    lng: localStorage.getItem(STORAGE_KEY_LANGUAGE) || 'en',
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false, // React already escapes values
    },
  })

export default i18n
