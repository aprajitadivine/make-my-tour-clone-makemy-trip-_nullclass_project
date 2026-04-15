import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useToast } from '../context/ToastContext'
import { X, Star } from 'lucide-react'
import axios from 'axios'

/**
 * DetailedReviewModal – Multi-dimensional rating component.
 *
 * Renders three 1–5 star sliders for:
 *   - Punctuality  (weight: 40%)
 *   - Cleanliness  (weight: 30%)
 *   - Amenities    (weight: 30%)
 *
 * On submit, calls POST /api/detailed-reviews with all three scores.
 * The backend calculates a weighted average with time-decay.
 *
 * Feature 6 – Rate & Review System (RRS).
 */
export default function DetailedReviewModal({ bookingId, onClose, onSuccess }) {
  const { t } = useTranslation()
  const toast = useToast()

  const [scores, setScores] = useState({ punctualityScore: 0, cleanlinessScore: 0, amenitiesScore: 0 })
  const [comment, setComment] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const setScore = (field, value) => setScores({ ...scores, [field]: value })

  const weightedPreview =
    scores.punctualityScore > 0 && scores.cleanlinessScore > 0 && scores.amenitiesScore > 0
      ? (
          0.4 * scores.punctualityScore +
          0.3 * scores.cleanlinessScore +
          0.3 * scores.amenitiesScore
        ).toFixed(2)
      : null

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (scores.punctualityScore === 0 || scores.cleanlinessScore === 0 || scores.amenitiesScore === 0) {
      toast.error('Please rate all three dimensions')
      return
    }
    const token = localStorage.getItem('token')
    if (!token) {
      toast.error('Please login to submit a review')
      return
    }
    setSubmitting(true)
    try {
      await axios.post(
        '/api/detailed-reviews',
        { bookingId, ...scores, comment },
        { headers: { Authorization: `Bearer ${token}` } }
      )
      toast.success('Review submitted successfully!')
      onSuccess?.()
      onClose()
    } catch (err) {
      toast.error(err.response?.data?.error || 'Failed to submit review')
    } finally {
      setSubmitting(false)
    }
  }

  const StarRating = ({ field, label, weight }) => {
    const value = scores[field]
    return (
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
            {label}
            <span className="ml-1 text-xs text-gray-400">({weight}% weight)</span>
          </label>
          {value > 0 && (
            <span className="text-sm font-semibold text-orange-500">{value}/5</span>
          )}
        </div>
        <div className="flex gap-1">
          {[1, 2, 3, 4, 5].map((star) => (
            <button
              key={star}
              type="button"
              onClick={() => setScore(field, star)}
              className="focus:outline-none transition-transform hover:scale-110"
              aria-label={`${star} star`}
            >
              <Star
                className={`w-8 h-8 transition-colors ${
                  star <= value
                    ? 'fill-orange-400 text-orange-400'
                    : 'text-gray-300 dark:text-gray-600'
                }`}
              />
            </button>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 px-4">
      <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl w-full max-w-md">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">{t('review.title')}</h2>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-5">
          {/* Three dimensional scores */}
          <StarRating field="punctualityScore" label={t('review.punctuality')} weight={40} />
          <StarRating field="cleanlinessScore" label={t('review.cleanliness')} weight={30} />
          <StarRating field="amenitiesScore" label={t('review.amenities')} weight={30} />

          {/* Live weighted score preview */}
          {weightedPreview && (
            <div className="bg-orange-50 dark:bg-orange-900/20 rounded-lg px-4 py-3 flex items-center justify-between">
              <span className="text-sm text-gray-600 dark:text-gray-400">{t('review.score')}</span>
              <span className="text-2xl font-bold text-orange-500">{weightedPreview}</span>
            </div>
          )}

          {/* Comment */}
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              {t('review.comment')} (optional)
            </label>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              rows={3}
              className="w-full border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-orange-400"
              placeholder="Share your experience..."
            />
          </div>

          <div className="flex gap-3">
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 btn-primary disabled:opacity-50"
            >
              {submitting ? t('common.loading') : t('review.submit')}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              {t('common.cancel')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
