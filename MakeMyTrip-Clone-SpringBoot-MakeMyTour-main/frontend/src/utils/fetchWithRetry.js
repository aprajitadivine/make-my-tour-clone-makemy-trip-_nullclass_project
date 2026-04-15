/**
 * Calls apiFn() and retries up to `maxRetries` times with exponential back-off.
 * onRetry(attempt, delayMs) is called before each retry so the UI can display a
 * live countdown — essential for Render free-tier cold starts (≈30–60 s warm-up).
 *
 * Retry schedule (default): 10 s → 20 s → 30 s  (≈ 60 s total coverage)
 */
export async function fetchWithRetry(
  apiFn,
  { maxRetries = 3, delays = [10000, 20000, 30000], onRetry } = {}
) {
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await apiFn()
    } catch (err) {
      if (attempt === maxRetries) throw err
      const delayMs = delays[attempt] ?? delays[delays.length - 1]
      if (onRetry) onRetry(attempt + 1, delayMs)
      await new Promise((resolve) => setTimeout(resolve, delayMs))
    }
  }
}
