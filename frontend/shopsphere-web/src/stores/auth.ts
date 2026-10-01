import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authService, type LoginPayload, type RegisterPayload } from '../services/authService'
import { readToken, writeToken, normalizeError, UNAUTHORIZED_EVENT } from '../services/http'
import type { User, UserRole } from '../types'

const USER_KEY = 'shopsphere.user'

function loadUser(): User | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

/**
 * Single source of truth for authentication. The token and the cached user are kept in sync, and a
 * 401 from anywhere in the app clears both through the UNAUTHORIZED_EVENT broadcast so the UI can
 * never end up showing a signed-in shell with no credential.
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(loadUser())
  const busy = ref(false)
  const error = ref('')

  const token = computed(() => readToken())
  const isAuthenticated = computed(() => Boolean(token.value && user.value))
  const role = computed<UserRole | null>(() => user.value?.role ?? null)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const displayName = computed(() =>
    user.value ? `${user.value.firstName} ${user.value.lastName}`.trim() : '',
  )
  const initials = computed(() =>
    user.value ? `${user.value.firstName.charAt(0)}${user.value.lastName.charAt(0)}`.toUpperCase() : '',
  )

  function applySession(accessToken: string, nextUser: User): void {
    writeToken(accessToken)
    user.value = nextUser
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
    error.value = ''
  }

  async function login(payload: LoginPayload): Promise<boolean> {
    busy.value = true
    error.value = ''
    try {
      const response = await authService.login(payload)
      applySession(response.accessToken, response.user)
      return true
    } catch (cause) {
      error.value = normalizeError(cause).message
      return false
    } finally {
      busy.value = false
    }
  }

  async function register(payload: RegisterPayload): Promise<boolean> {
    busy.value = true
    error.value = ''
    try {
      const response = await authService.register(payload)
      applySession(response.accessToken, response.user)
      return true
    } catch (cause) {
      error.value = normalizeError(cause).message
      return false
    } finally {
      busy.value = false
    }
  }

  /** Re-reads the profile so a role change or a revoked account is reflected without a reload. */
  async function refresh(): Promise<void> {
    if (!token.value) return
    try {
      const fresh = await authService.me()
      user.value = fresh
      localStorage.setItem(USER_KEY, JSON.stringify(fresh))
    } catch {
      signOut()
    }
  }

  function signOut(): void {
    writeToken(null)
    user.value = null
    localStorage.removeItem(USER_KEY)
    error.value = ''
  }

  window.addEventListener(UNAUTHORIZED_EVENT, signOut)

  return {
    user,
    busy,
    error,
    token,
    isAuthenticated,
    role,
    isAdmin,
    displayName,
    initials,
    login,
    register,
    refresh,
    signOut,
  }
})
