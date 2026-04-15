import { createContext, useContext, useState, useCallback } from 'react'

/**
 * ToastContext – lightweight in-app notification system.
 *
 * Usage:
 *   const toast = useToast()
 *   toast.success('Booking confirmed!')
 *   toast.error('Something went wrong.')
 *   toast.info('Flight status updated.')
 */
const ToastContext = createContext(null)

let nextId = 1

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const show = useCallback((message, type = 'info', duration = 4000) => {
    const id = nextId++
    setToasts((prev) => [...prev, { id, message, type }])
    if (duration > 0) {
      setTimeout(() => dismiss(id), duration)
    }
    return id
  }, [dismiss])

  const toast = {
    success: (msg, duration) => show(msg, 'success', duration),
    error:   (msg, duration) => show(msg, 'error',   duration),
    info:    (msg, duration) => show(msg, 'info',    duration),
    warn:    (msg, duration) => show(msg, 'warn',    duration),
    dismiss,
  }

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <ToastContainer toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>')
  return ctx
}

/* ── Internal toast list ─────────────────────────────────────── */

const styles = {
  success: {
    bar:  'bg-green-500',
    icon: '✅',
    text: 'text-green-800',
    bg:   'bg-green-50 border-green-200',
  },
  error: {
    bar:  'bg-red-500',
    icon: '❌',
    text: 'text-red-800',
    bg:   'bg-red-50 border-red-200',
  },
  info: {
    bar:  'bg-blue-500',
    icon: 'ℹ️',
    text: 'text-blue-800',
    bg:   'bg-blue-50 border-blue-200',
  },
  warn: {
    bar:  'bg-orange-500',
    icon: '⚠️',
    text: 'text-orange-800',
    bg:   'bg-orange-50 border-orange-200',
  },
}

function ToastContainer({ toasts, onDismiss }) {
  if (toasts.length === 0) return null

  return (
    <div
      className="fixed bottom-4 right-4 z-[9999] flex flex-col gap-2 pointer-events-none"
      aria-live="polite"
      aria-atomic="false"
    >
      {toasts.map((t) => {
        const s = styles[t.type] || styles.info
        return (
          <div
            key={t.id}
            role="alert"
            className={`pointer-events-auto flex items-start gap-3 rounded-xl border shadow-lg px-4 py-3 w-80 max-w-xs animate-slide-in ${s.bg}`}
          >
            <span className="text-lg leading-none mt-0.5" aria-hidden="true">{s.icon}</span>
            <p className={`flex-1 text-sm font-medium ${s.text}`}>{t.message}</p>
            <button
              onClick={() => onDismiss(t.id)}
              aria-label="Dismiss notification"
              className={`shrink-0 text-lg leading-none opacity-50 hover:opacity-100 transition-opacity ${s.text}`}
            >
              ×
            </button>
          </div>
        )
      })}
    </div>
  )
}
