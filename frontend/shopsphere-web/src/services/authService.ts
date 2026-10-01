import { http } from './http'
import type { AuthResponse, User } from '../types'

export interface RegisterPayload {
  firstName: string
  lastName: string
  email: string
  password: string
}

export interface LoginPayload {
  email: string
  password: string
}

export const authService = {
  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const { data } = await http.post<AuthResponse>('/api/auth/register', payload)
    return data
  },

  async login(payload: LoginPayload): Promise<AuthResponse> {
    const { data } = await http.post<AuthResponse>('/api/auth/login', payload)
    return data
  },

  async me(): Promise<User> {
    const { data } = await http.get<User>('/api/auth/me')
    return data
  },
}
