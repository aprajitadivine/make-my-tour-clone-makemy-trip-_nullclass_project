import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { BookMarked, Plus, CheckCircle, Clock, XCircle, MapPin } from 'lucide-react'
import DOMPurify from 'dompurify'
import api from '../services/api'

/**
 * Travel Stories community page.
 *
 * Public users see the approved stories feed.
 * Authenticated users can submit new stories (starts PENDING)
 * and view their own stories with moderation status badges.
 *
 * Moderation pipeline: PENDING → APPROVED | REJECTED
 * Feature 4 – User-Generated Content & Community.
 */
export default function TravelStories() {
  const { t } = useTranslation()
  const { isAuthenticated } = useAuth()
  const toast = useToast()

  const [stories, setStories] = useState([])
  const [myStories, setMyStories] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [activeTab, setActiveTab] = useState('feed') // 'feed' | 'mine'
  const [form, setForm] = useState({
    title: '',
    content: '',
    destination: '',
    coverImageUrl: '',
  })

  useEffect(() => {
    fetchApprovedStories()
    if (isAuthenticated) fetchMyStories()
  }, [isAuthenticated])

  const fetchApprovedStories = async () => {
    try {
      const { data } = await api.get('/stories')
      setStories(data)
    } catch {
      // Keep empty list on error
    } finally {
      setLoading(false)
    }
  }

  const fetchMyStories = async () => {
    try {
      const { data } = await api.get('/stories/my')
      setMyStories(data)
    } catch {
      // Keep empty list on error
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.title.trim() || !form.content.trim()) {
      toast.error('Title and content are required')
      return
    }
    setSubmitting(true)
    try {
      await api.post('/stories', form)
      toast.success('Story submitted! Awaiting moderation approval.')
      setForm({ title: '', content: '', destination: '', coverImageUrl: '' })
      setShowForm(false)
      fetchMyStories()
    } catch (err) {
      toast.error(err.response?.data?.error || 'Submission failed')
    } finally {
      setSubmitting(false)
    }
  }

  const StatusBadge = ({ status }) => {
    const config = {
      PENDING: { icon: Clock, color: 'bg-yellow-100 text-yellow-700', label: t('stories.pending') },
      APPROVED: { icon: CheckCircle, color: 'bg-green-100 text-green-700', label: t('stories.approved') },
      REJECTED: { icon: XCircle, color: 'bg-red-100 text-red-700', label: t('stories.rejected') },
    }
    const { icon: Icon, color, label } = config[status] || config.PENDING
    return (
      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${color}`}>
        <Icon className="w-3 h-3" /> {label}
      </span>
    )
  }

  const StoryCard = ({ story, showStatus = false }) => (
    <article className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
      {story.coverImageUrl && (
        <img
          src={story.coverImageUrl}
          alt={story.title}
          className="w-full h-48 object-cover"
          onError={(e) => { e.target.style.display = 'none' }}
        />
      )}
      <div className="p-5">
        <div className="flex items-start justify-between gap-2 mb-2">
          <h3 className="font-semibold text-gray-900 dark:text-gray-100 text-lg leading-tight">
            {story.title}
          </h3>
          {showStatus && <StatusBadge status={story.status} />}
        </div>
        {story.destination && (
          <p className="flex items-center gap-1 text-sm text-orange-500 mb-2">
            <MapPin className="w-3.5 h-3.5" /> {story.destination}
          </p>
        )}
        <p className="text-gray-600 dark:text-gray-400 text-sm line-clamp-3"
           dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(story.content) }} />
        <p className="text-xs text-gray-400 dark:text-gray-500 mt-3">
          {new Date(story.createdAt).toLocaleDateString()}
        </p>
      </div>
    </article>
  )

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-3">
          <BookMarked className="w-8 h-8 text-orange-500" />
          <div>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">{t('stories.title')}</h1>
            <p className="text-gray-500 dark:text-gray-400 mt-0.5">Community travel experiences</p>
          </div>
        </div>
        {isAuthenticated && (
          <button
            onClick={() => setShowForm(!showForm)}
            className="btn-primary flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            {t('stories.share')}
          </button>
        )}
      </div>

      {/* Story submission form */}
      {showForm && isAuthenticated && (
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-md border border-gray-200 dark:border-gray-700 p-6 mb-8">
          <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100 mb-4">{t('stories.share')}</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {t('stories.storyTitle')} *
              </label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                maxLength={200}
                className="w-full border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
                placeholder="My amazing trip to Goa..."
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {t('stories.destination')}
              </label>
              <input
                type="text"
                value={form.destination}
                onChange={(e) => setForm({ ...form, destination: e.target.value })}
                className="w-full border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
                placeholder="Goa, Manali, Rajasthan..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {t('stories.coverImage')}
              </label>
              <input
                type="url"
                value={form.coverImageUrl}
                onChange={(e) => setForm({ ...form, coverImageUrl: e.target.value })}
                className="w-full border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400"
                placeholder="https://..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                {t('stories.content')} *
              </label>
              {/* Rich-text area – accepts HTML content from the editor */}
              <textarea
                value={form.content}
                onChange={(e) => setForm({ ...form, content: e.target.value })}
                rows={8}
                className="w-full border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-orange-400 font-mono text-sm"
                placeholder="Share your experience... (HTML is supported)"
                required
              />
              <p className="text-xs text-gray-400 mt-1">HTML tags like &lt;b&gt;, &lt;i&gt;, &lt;p&gt; are supported</p>
            </div>
            <div className="flex gap-3">
              <button
                type="submit"
                disabled={submitting}
                className="btn-primary flex items-center gap-2 disabled:opacity-50"
              >
                {submitting ? t('common.loading') : t('stories.submit')}
              </button>
              <button
                type="button"
                onClick={() => setShowForm(false)}
                className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
              >
                {t('common.cancel')}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Tabs for authenticated users */}
      {isAuthenticated && (
        <div className="flex gap-1 mb-6 border-b border-gray-200 dark:border-gray-700">
          <button
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              activeTab === 'feed'
                ? 'border-orange-500 text-orange-600 dark:text-orange-400'
                : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
            }`}
            onClick={() => setActiveTab('feed')}
          >
            Community Feed ({stories.length})
          </button>
          <button
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              activeTab === 'mine'
                ? 'border-orange-500 text-orange-600 dark:text-orange-400'
                : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
            }`}
            onClick={() => setActiveTab('mine')}
          >
            My Stories ({myStories.length})
          </button>
        </div>
      )}

      {/* Stories grid */}
      {loading ? (
        <div className="text-center py-12 text-gray-400">{t('common.loading')}</div>
      ) : activeTab === 'feed' ? (
        stories.length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <BookMarked className="w-12 h-12 mx-auto mb-3 opacity-30" />
            <p>No approved stories yet. Be the first to share!</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {stories.map((story) => (
              <StoryCard key={story.id} story={story} />
            ))}
          </div>
        )
      ) : (
        myStories.length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <BookMarked className="w-12 h-12 mx-auto mb-3 opacity-30" />
            <p>You haven&apos;t shared any stories yet.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {myStories.map((story) => (
              <StoryCard key={story.id} story={story} showStatus />
            ))}
          </div>
        )
      )}
    </div>
  )
}
