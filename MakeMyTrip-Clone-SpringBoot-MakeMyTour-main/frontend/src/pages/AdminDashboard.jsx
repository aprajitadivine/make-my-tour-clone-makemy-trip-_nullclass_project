import { useState, useEffect } from 'react'
import DOMPurify from 'dompurify'
import {
  ShieldCheck, BookMarked, Star, CheckCircle, XCircle, Flag,
  Loader, AlertTriangle, RefreshCw, ChevronDown, ChevronUp,
} from 'lucide-react'
import { storyApi, reviewApi } from '../services/api'
import { useToast } from '../context/ToastContext'

/**
 * Admin Dashboard – content moderation panel.
 *
 * Sections:
 *  1. Pending Travel Stories – approve or reject with a reason
 *  2. Flagged Reviews        – unflag (restore) or delete
 */
export default function AdminDashboard() {
  const toast = useToast()

  // ── Stories ──────────────────────────────────────────────────────
  const [pendingStories, setPendingStories] = useState([])
  const [storiesLoading, setStoriesLoading] = useState(true)
  const [actingStory, setActingStory] = useState(null)   // id of story being acted on
  const [rejectReasons, setRejectReasons] = useState({}) // { [id]: string }
  const [showRejectBox, setShowRejectBox] = useState({}) // { [id]: bool }

  // ── Flagged Reviews ───────────────────────────────────────────────
  const [flaggedReviews, setFlaggedReviews] = useState([])
  const [reviewsLoading, setReviewsLoading] = useState(true)
  const [actingReview, setActingReview] = useState(null)

  useEffect(() => {
    loadPendingStories()
    loadFlaggedReviews()
  }, [])

  const loadPendingStories = async () => {
    setStoriesLoading(true)
    try {
      const { data } = await storyApi.getPending()
      setPendingStories(data)
    } catch {
      toast.error('Failed to load pending stories')
    } finally {
      setStoriesLoading(false)
    }
  }

  const loadFlaggedReviews = async () => {
    setReviewsLoading(true)
    try {
      const { data } = await reviewApi.getFlagged()
      setFlaggedReviews(data)
    } catch {
      toast.error('Failed to load flagged reviews')
    } finally {
      setReviewsLoading(false)
    }
  }

  // ── Story actions ─────────────────────────────────────────────────

  const handleApprove = async (id) => {
    setActingStory(id)
    try {
      await storyApi.approve(id)
      setPendingStories((prev) => prev.filter((s) => s.id !== id))
      toast.success('Story approved and published! ✅')
    } catch {
      toast.error('Failed to approve story')
    } finally {
      setActingStory(null)
    }
  }

  const handleReject = async (id) => {
    setActingStory(id)
    try {
      await storyApi.reject(id, rejectReasons[id] || '')
      setPendingStories((prev) => prev.filter((s) => s.id !== id))
      toast.success('Story rejected.')
    } catch {
      toast.error('Failed to reject story')
    } finally {
      setActingStory(null)
      setShowRejectBox((prev) => ({ ...prev, [id]: false }))
    }
  }

  const toggleRejectBox = (id) =>
    setShowRejectBox((prev) => ({ ...prev, [id]: !prev[id] }))

  // ── Review actions ────────────────────────────────────────────────

  const handleUnflag = async (id) => {
    setActingReview(id)
    try {
      await reviewApi.unflag(id)
      setFlaggedReviews((prev) => prev.filter((r) => r.id !== id))
      toast.success('Review restored to public feed.')
    } catch {
      toast.error('Failed to unflag review')
    } finally {
      setActingReview(null)
    }
  }

  const handleDeleteReview = async (id) => {
    if (!window.confirm('Permanently delete this review?')) return
    setActingReview(id)
    try {
      await reviewApi.delete(id)
      setFlaggedReviews((prev) => prev.filter((r) => r.id !== id))
      toast.success('Review deleted.')
    } catch {
      toast.error('Failed to delete review')
    } finally {
      setActingReview(null)
    }
  }

  return (
    <div className="max-w-5xl mx-auto px-4 py-8 space-y-10">
      {/* Header */}
      <div className="flex items-center gap-3">
        <ShieldCheck className="w-8 h-8 text-orange-500" />
        <div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Admin Dashboard</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">Content moderation panel</p>
        </div>
      </div>

      {/* ── Pending Stories ── */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">
            <BookMarked className="w-5 h-5 text-orange-500" />
            Pending Travel Stories
            {pendingStories.length > 0 && (
              <span className="ml-2 bg-orange-100 text-orange-700 text-xs font-semibold px-2 py-0.5 rounded-full">
                {pendingStories.length}
              </span>
            )}
          </h2>
          <button
            onClick={loadPendingStories}
            className="text-sm text-gray-500 hover:text-orange-500 flex items-center gap-1"
          >
            <RefreshCw className="w-4 h-4" /> Refresh
          </button>
        </div>

        {storiesLoading ? (
          <div className="flex items-center gap-2 text-gray-500">
            <Loader className="w-5 h-5 animate-spin" /> Loading…
          </div>
        ) : pendingStories.length === 0 ? (
          <div className="card text-center py-10 text-gray-400">
            <CheckCircle className="w-10 h-10 mx-auto mb-2 text-green-400" />
            No stories pending review — all caught up!
          </div>
        ) : (
          <div className="space-y-4">
            {pendingStories.map((story) => (
              <div key={story.id} className="card border border-orange-100 dark:border-orange-900">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-gray-900 dark:text-gray-100 truncate">{story.title}</p>
                    <p className="text-xs text-gray-400 mb-1">
                      by <span className="font-medium">{story.author?.username || 'unknown'}</span>
                      {' · '}{story.destination}
                      {' · '}{new Date(story.createdAt).toLocaleDateString('en-IN')}
                    </p>
                    {/* Strip HTML for preview using DOMPurify – allow no tags, plain text only */}
                    <p className="text-sm text-gray-600 dark:text-gray-300 line-clamp-2">
                      {DOMPurify.sanitize(story.content || '', { ALLOWED_TAGS: [] })}
                    </p>
                  </div>
                  {story.coverImageUrl && (
                    <img
                      src={story.coverImageUrl}
                      alt={story.title}
                      className="w-20 h-16 object-cover rounded-lg flex-shrink-0"
                      onError={(e) => { e.target.style.display = 'none' }}
                    />
                  )}
                </div>

                {/* Actions */}
                <div className="flex flex-wrap items-center gap-2 mt-4 pt-3 border-t border-gray-100 dark:border-gray-700">
                  <button
                    onClick={() => handleApprove(story.id)}
                    disabled={actingStory === story.id}
                    className="flex items-center gap-1.5 bg-green-500 hover:bg-green-600 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
                  >
                    {actingStory === story.id
                      ? <Loader className="w-4 h-4 animate-spin" />
                      : <CheckCircle className="w-4 h-4" />}
                    Approve
                  </button>

                  <button
                    onClick={() => toggleRejectBox(story.id)}
                    className="flex items-center gap-1.5 bg-red-100 hover:bg-red-200 text-red-600 text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
                  >
                    <XCircle className="w-4 h-4" />
                    Reject
                    {showRejectBox[story.id]
                      ? <ChevronUp className="w-3.5 h-3.5" />
                      : <ChevronDown className="w-3.5 h-3.5" />}
                  </button>
                </div>

                {/* Reject reason input */}
                {showRejectBox[story.id] && (
                  <div className="mt-3 space-y-2">
                    <input
                      type="text"
                      className="input-field text-sm"
                      placeholder="Reason for rejection (optional)"
                      value={rejectReasons[story.id] || ''}
                      onChange={(e) =>
                        setRejectReasons((prev) => ({ ...prev, [story.id]: e.target.value }))
                      }
                    />
                    <button
                      onClick={() => handleReject(story.id)}
                      disabled={actingStory === story.id}
                      className="w-full bg-red-500 hover:bg-red-600 text-white text-sm font-semibold py-2 rounded-lg transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
                    >
                      {actingStory === story.id && <Loader className="w-4 h-4 animate-spin" />}
                      Confirm Rejection
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      {/* ── Flagged Reviews ── */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">
            <Flag className="w-5 h-5 text-red-500" />
            Flagged Reviews
            {flaggedReviews.length > 0 && (
              <span className="ml-2 bg-red-100 text-red-700 text-xs font-semibold px-2 py-0.5 rounded-full">
                {flaggedReviews.length}
              </span>
            )}
          </h2>
          <button
            onClick={loadFlaggedReviews}
            className="text-sm text-gray-500 hover:text-orange-500 flex items-center gap-1"
          >
            <RefreshCw className="w-4 h-4" /> Refresh
          </button>
        </div>

        {reviewsLoading ? (
          <div className="flex items-center gap-2 text-gray-500">
            <Loader className="w-5 h-5 animate-spin" /> Loading…
          </div>
        ) : flaggedReviews.length === 0 ? (
          <div className="card text-center py-10 text-gray-400">
            <CheckCircle className="w-10 h-10 mx-auto mb-2 text-green-400" />
            No flagged reviews — community is behaving!
          </div>
        ) : (
          <div className="space-y-4">
            {flaggedReviews.map((review) => (
              <div key={review.id} className="card border border-red-100 dark:border-red-900">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-semibold bg-red-100 text-red-600 px-2 py-0.5 rounded-full flex items-center gap-1">
                        <Flag className="w-3 h-3" /> Flagged
                      </span>
                      <span className="text-xs text-gray-400">Review #{review.id}</span>
                    </div>
                    {/* Stars */}
                    <div className="flex items-center gap-0.5 mb-1">
                      {[1, 2, 3, 4, 5].map((s) => (
                        <Star
                          key={s}
                          className={`w-4 h-4 ${s <= review.rating ? 'fill-orange-400 text-orange-400' : 'text-gray-300'}`}
                        />
                      ))}
                      <span className="ml-2 text-xs text-gray-400">
                        by {review.user?.username || 'unknown'}
                      </span>
                    </div>
                    {review.comment && (
                      <p className="text-sm text-gray-600 dark:text-gray-300">{review.comment}</p>
                    )}
                    {review.imageUrl && (
                      <img
                        src={review.imageUrl}
                        alt="Review"
                        className="mt-2 h-24 rounded object-cover"
                        onError={(e) => { e.target.style.display = 'none' }}
                      />
                    )}
                  </div>
                </div>

                <div className="flex gap-2 mt-4 pt-3 border-t border-gray-100 dark:border-gray-700">
                  <button
                    onClick={() => handleUnflag(review.id)}
                    disabled={actingReview === review.id}
                    className="flex items-center gap-1.5 bg-green-100 hover:bg-green-200 text-green-700 text-sm font-semibold px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
                  >
                    {actingReview === review.id
                      ? <Loader className="w-4 h-4 animate-spin" />
                      : <CheckCircle className="w-4 h-4" />}
                    Restore
                  </button>
                  <button
                    onClick={() => handleDeleteReview(review.id)}
                    disabled={actingReview === review.id}
                    className="flex items-center gap-1.5 bg-red-100 hover:bg-red-200 text-red-600 text-sm font-semibold px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
                  >
                    <XCircle className="w-4 h-4" />
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Info box */}
      <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-xl p-4 flex gap-3 text-sm text-blue-700 dark:text-blue-300">
        <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
        <div>
          <p className="font-semibold mb-1">Admin credentials (development only)</p>
          <p>Username: <code className="bg-blue-100 dark:bg-blue-900 px-1 rounded">admin</code> · Password: <code className="bg-blue-100 dark:bg-blue-900 px-1 rounded">admin123</code></p>
        </div>
      </div>
    </div>
  )
}
