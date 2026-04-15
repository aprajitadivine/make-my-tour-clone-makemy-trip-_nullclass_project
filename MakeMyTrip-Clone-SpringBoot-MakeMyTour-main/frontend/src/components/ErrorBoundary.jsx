import { Component } from 'react'

/**
 * React Error Boundary — catches any unhandled render/lifecycle errors that
 * would otherwise produce a completely blank page.
 *
 * Wrap the top-level <App> with this component so that:
 *  1. The user always sees a friendly "Something went wrong" message instead
 *     of a white screen.
 *  2. The full error details are printed to the console for debugging.
 *
 * Usage:
 *   <ErrorBoundary>
 *     <App />
 *   </ErrorBoundary>
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, info) {
    console.error('[ErrorBoundary] Uncaught error:', error, info)
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
          <div className="max-w-md w-full bg-white rounded-2xl shadow-lg p-8 text-center">
            <div className="text-5xl mb-4">✈️</div>
            <h1 className="text-2xl font-bold text-gray-800 mb-2">Something went wrong</h1>
            <p className="text-gray-500 text-sm mb-6">
              An unexpected error occurred. Please reload the page — if the problem
              persists, try clearing your browser cache.
            </p>
            {this.state.error && (
              <details className="text-left mb-6 bg-gray-50 rounded-lg p-3">
                <summary className="text-xs text-gray-400 cursor-pointer select-none">
                  Error details
                </summary>
                <pre className="text-xs text-red-600 mt-2 whitespace-pre-wrap break-all">
                  {this.state.error.toString()}
                </pre>
              </details>
            )}
            <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
              <button
                onClick={this.handleReload}
                className="bg-orange-500 hover:bg-orange-600 text-white font-semibold py-2 px-6 rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-orange-400 focus:ring-offset-2"
              >
                Reload Page
              </button>
              <a
                href="/"
                className="bg-white hover:bg-gray-50 text-gray-800 font-semibold py-2 px-6 rounded-lg border border-gray-300 transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-gray-300 focus:ring-offset-2"
              >
                Go to Home
              </a>
            </div>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
