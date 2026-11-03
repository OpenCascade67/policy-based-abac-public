import { apiRequest } from './client';
import type { AuthResponse, AppSettingsDto } from './types';

export const authApi = {
  login: (username: string, password: string) =>
    apiRequest<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  sendOtp: (email: string) =>
    apiRequest<{ message: string; code: string }>('/api/auth/otp/send', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  verifyOtp: (email: string, code: string) =>
    apiRequest<AuthResponse>('/api/auth/otp/verify', {
      method: 'POST',
      body: JSON.stringify({ email, code }),
    }),

  refresh: (refreshToken: string) =>
    apiRequest<AuthResponse>('/api/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),

  switchDomain: (domainId: string) =>
    apiRequest<AuthResponse>('/api/auth/switch-domain', {
      method: 'POST',
      body: JSON.stringify({ domainId }),
    }),

  logout: (refreshToken: string) =>
    apiRequest<void>('/api/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),
};
