import { useState, useEffect } from 'react'
import { Star, X, Upload, Loader, ThumbsUp, Flag, ArrowUpDown } from 'lucide-react'
import { reviewApi } from '../services/api'

/**
 * Modal dialog for submitting a 1–5 star review.
 * Feature 2 – Review System.
 *
 * Props:
 *  - booking: the booking being reviewed
 *  - onClose: callback to close the modal
 *  - onSuccess: callback called after a successful submission
 *
 * Extra Feature 2 capabilities shown here:
 *  - Sort existing reviews by "most helpful"
 *  - Mark a review as helpful (👍 counter)
 *  - Flag inappropriate content
 */
export default function ReviewModal({ booking, onClose, onSuccess }) {
  const [rating, setRating] = useState(0)
  const [hoverRating, setHoverRating] = useState(0)
  const [comment, setComment] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // Existing reviews for this booking
  const [existingReviews, setExistingReviews] = useState([])
  const [sortBy, setSortBy] = useState('recent')   // 'recent' | 'helpful'
  const [helpfulLoading, setHelpfulLoading] = useState(null)
  const [flagging, setFlagging] = useState(null)
  const [flagged, setFlagged] = useState({})

  useEffect(() => {
    // Load reviews for the flight or hotel attached to this booking
    const entityId = booking.flight?.id || booking.hotel?.id
    const entityType = booking.flight ? 'flight' : 'hotel'
    if (!entityId) return

    reviewApi.getByEntity(entityType, entityId)
      .then(({ data }) => setExistingReviews(data))
      .catch(() => {})
  }, [booking])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (rating === 0) {
      setError('Please select a star rating')
      return
    }

    setLoading(true)
    setError(null)
    try {
      await reviewApi.create({
        bookingId: booking.id,
        rating,
        comment: comment.trim() || null,
        imageUrl: imageUrl.trim() || null,
      })
      onSuccess?.()
      onClose()
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to submit review. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const handleHelpful = async (reviewId) => {
    setHelpfulLoading(reviewId)
    try {
      await reviewApi.markHelpful(reviewId)
      setExistingReviews((prev) =>
        prev.map((r) =>
          r.id === reviewId ? { ...r, helpfulVotes: (r.helpfulVotes || 0) + 1 } : r
        )
      )
    } catch {
      // ignore
    } finally {
      setHelpfulLoading(null)
    }
  }

  const handleFlag = async (reviewId) => {
    setFlagging(reviewId)
    try {
      await reviewApi.flagReview(reviewId)
      setFlagged((prev) => ({ ...prev, [reviewId]: true }))
    } catch {
      // ignore
    } finally {
      setFlagging(null)
    }
  }

  const sortedReviews = [...existingReviews].sort((a, b) => {
    if (sortBy === 'helpful') return (b.helpfulVotes || 0) - (a.helpfulVotes || 0)
    return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
  })

  const starLabel = ['', 'Poor', 'Fair', 'Good', 'Very Good', 'Excellent']

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        {/* Modal header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-100 sticky top-0 bg-white z-10">
          <h2 className="text-xl font-bold text-gray-900">Write a Review</h2>
          <button
            onClick={onClose}
            className="p-2 rounded-lg hover:bg-gray-100 text-gray-500 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {/* Booking reference */}
          <div className="bg-gray-50 rounded-lg p-3 text-sm text-gray-600">
            Booking #{booking.id} ·{' '}
            {booking.flight?.name || booking.hotel?.name || 'Travel Booking'}
          </div>

          {/* Star rating selector */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Your Rating *
            </label>
            <div className="flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setRating(star)}
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(0)}
                  className="focus:outline-none transition-transform hover:scale-110"
                  aria-label={`Rate ${star} star${star > 1 ? 's' : ''}`}
                >
                  <Star
                    className={`w-8 h-8 transition-colors ${
                      star <= (hoverRating || rating)
                        ? 'text-yellow-400 fill-yellow-400'
                        : 'text-gray-300 fill-gray-300'
                    }`}
                  />
                </button>
              ))}
              {(hoverRating || rating) > 0 && (
                <span className="ml-2 text-sm font-medium text-gray-600">
                  {starLabel[hoverRating || rating]}
                </span>
              )}
            </div>
          </div>

          {/* Comment */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              Comment (optional)
            </label>
            <textarea
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              rows={4}
              maxLength={2000}
              placeholder="Share your experience – what did you love? What could be better?"
              className="input-field resize-none"
            />
            <p className="text-xs text-gray-400 mt-1 text-right">{comment.length}/2000</p>
          </div>

          {/* Image URL */}
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-2">
              <Upload className="w-4 h-4 inline mr-1" />
              Photo URL (optional)
            </label>
            <input
              type="url"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="https://example.com/my-travel-photo.jpg"
              className="input-field"
            />
          </div>

          {/* Image preview */}
          {imageUrl && (
            <div className="rounded-lg overflow-hidden border border-gray-200">
              <img
                src={imageUrl}
                alt="Review preview"
                className="w-full h-40 object-cover"
                onError={(e) => { e.target.style.display = 'none' }}
              />
            </div>
          )}

          {/* Error */}
          {error && (
            <p className="text-red-600 text-sm bg-red-50 px-3 py-2 rounded-lg">{error}</p>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary flex-1">
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="btn-primary flex-1 flex items-center justify-center gap-2"
            >
              {loading ? <Loader className="w-4 h-4 animate-spin" /> : null}
              {loading ? 'Submitting…' : 'Submit Review'}
            </button>
          </div>
        </form>

        {/* Existing reviews section – Feature 2 */}
        {sortedReviews.length > 0 && (
          <div className="border-t border-gray-100 px-6 pb-6">
            <div className="flex items-center justify-between mt-5 mb-3">
              <h3 className="font-semibold text-gray-800">
                Recent Reviews ({existingReviews.length})
              </h3>
              {/* Sort control */}
              <button
                onClick={() => setSortBy((s) => s === 'recent' ? 'helpful' : 'recent')}
                className="flex items-center gap-1.5 text-xs text-gray-500 hover:text-orange-500 border border-gray-200 rounded-lg px-2.5 py-1 transition-colors"
              >
                <ArrowUpDown className="w-3.5 h-3.5" />
                {sortBy === 'helpful' ? 'Most Helpful' : 'Most Recent'}
              </button>
            </div>

            <div className="space-y-4">
              {sortedReviews.map((review) => (
                <div key={review.id} className="bg-gray-50 rounded-xl p-4">
                  {/* Stars */}
                  <div className="flex items-center gap-1 mb-2">
                    {[1,2,3,4,5].map((s) => (
                      <Star key={s} className={`w-4 h-4 ${s <= review.rating ? 'text-yellow-400 fill-yellow-400' : 'text-gray-200 fill-gray-200'}`} />
                    ))}
                    <span className="ml-2 text-xs text-gray-400">
                      {review.createdAt ? new Date(review.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : ''}
                    </span>
                  </div>

                  {review.comment && (
                    <p className="text-sm text-gray-700 mb-3">{review.comment}</p>
                  )}

                  {review.imageUrl && (
                    <img
                      src={review.imageUrl}
                      alt="Review photo"
                      className="w-full h-28 object-cover rounded-lg mb-3"
                      onError={(e) => { e.target.style.display = 'none' }}
                    />
                  )}

                  {/* Helpful + Flag actions */}
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => handleHelpful(review.id)}
                      disabled={helpfulLoading === review.id}
                      className="flex items-center gap-1 text-xs text-gray-500 hover:text-green-600 transition-colors"
                    >
                      <ThumbsUp className="w-3.5 h-3.5" />
                      Helpful {review.helpfulVotes > 0 && `(${review.helpfulVotes})`}
                    </button>

                    {!flagged[review.id] ? (
                      <button
                        onClick={() => handleFlag(review.id)}
                        disabled={flagging === review.id}
                        className="flex items-center gap-1 text-xs text-gray-400 hover:text-red-500 transition-colors ml-auto"
                      >
                        <Flag className="w-3.5 h-3.5" />
                        Flag
                      </button>
                    ) : (
                      <span className="text-xs text-red-400 ml-auto">Flagged</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
