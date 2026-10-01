import axios, { AxiosError, type AxiosInstance } from 'axios'

const TOKEN_KEY = 'shopsphere.token'

export function readToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function writeToken(token: string | null): void {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

/**
 * A single axios instance for the whole app.
 *
 * The gateway rejects a request whose token is missing, expired or tampered with with a 401, and
 * returns 403 when a valid token lacks the required role. Both cases mean the stored credential is
 * useless, so the token is dropped and the app returns to a signed-out state instead of retrying
 * with a token that cannot work.
 */
export const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

export const UNAUTHORIZED_EVENT = 'shopsphere:unauthorized'

http.interceptors.request.use((config) => {
  const token = readToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      writeToken(null)
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
    }
    return Promise.reject(normalizeError(error))
  },
)

export interface FriendlyError {
  status: number | null
  title: string
  message: string
  isNetworkError: boolean
}

const STATUS_MESSAGES: Record<number, string> = {
  400: 'That request could not be processed. Please check the details and try again.',
  401: 'Please sign in to continue.',
  403: 'You do not have permission to perform this action.',
  404: 'We could not find what you were looking for.',
  409: 'That action conflicts with the current state. Please refresh and try again.',
  422: 'Some of the details provided are not valid.',
  500: 'Something went wrong on our side. Please try again in a moment.',
  503: 'The service is temporarily unavailable. Please try again shortly.',
}

/**
 * Converts an axios failure into a message safe to show a shopper. Backend problem-detail bodies
 * are used when present because they are more specific, but a generic message is always the
 * fallback so the UI never leaks a stack trace or raw exception name.
 */
export function normalizeError(error: unknown): FriendlyError {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<{ detail?: string; message?: string }>
    const status = axiosError.response?.status ?? null

    if (!axiosError.response) {
      return {
        status: null,
        title: 'Connection problem',
        message: 'Unable to reach ShopSphere. Check your connection and try again.',
        isNetworkError: true,
      }
    }

    const fromBody = axiosError.response.data?.detail ?? axiosError.response.data?.message
    const fallback = status === null ? undefined : STATUS_MESSAGES[status]
    return {
      status,
      title: statusTitle(status),
      message: fromBody ?? fallback ?? 'Something went wrong. Please try again.',
      isNetworkError: false,
    }
  }

  return {
    status: null,
    title: 'Something went wrong',
    message: 'An unexpected problem occurred. Please try again.',
    isNetworkError: false,
  }
}

function statusTitle(status: number | null): string {
  switch (status) {
    case 401:
      return 'Sign in required'
    case 403:
      return 'Not allowed'
    case 404:
      return 'Not found'
    default:
      return status && status >= 500 ? 'Server error' : 'Request failed'
  }
}
