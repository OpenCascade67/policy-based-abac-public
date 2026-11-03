import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api/auth';
import { setTokens, clearTokens, getRefreshToken } from '../api/client';
import type { UserSummaryDto } from '../api/types';

interface AuthState {
  isAuthenticated: boolean;
  user: UserSummaryDto | null;
  currentDomainId: string | null;
}

interface AuthContextType {
  auth: AuthState;
  login: (username: string, password: string) => Promise<void>;
  loginWithOtp: (email: string, code: string) => Promise<void>;
  sendOtp: (email: string) => Promise<string>;
  logout: () => Promise<void>;
  switchDomain: (domainId: string) => Promise<void>;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const SESSION_KEY = 'pap_auth_state';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<AuthState>(() => {
    try {
      const stored = sessionStorage.getItem(SESSION_KEY);
      if (stored) return JSON.parse(stored) as AuthState;
    } catch { /* ignore */ }
    return { isAuthenticated: false, user: null, currentDomainId: null };
  });
  const [isLoading, setIsLoading] = useState(false);

  const applyAuth = useCallback((response: Awaited<ReturnType<typeof authApi.login>>) => {
    setTokens(response.accessToken, response.refreshToken);
    const newState: AuthState = {
      isAuthenticated: true,
      user: response.user,
      currentDomainId: response.currentDomainId ?? null,
    };
    setAuth(newState);
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(newState));
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    setIsLoading(true);
    try {
      const response = await authApi.login(username, password);
      applyAuth(response);
    } finally {
      setIsLoading(false);
    }
  }, [applyAuth]);

  const sendOtp = useCallback(async (email: string): Promise<string> => {
    const response = await authApi.sendOtp(email);
    return response.code; // dev-only: real apps send via email
  }, []);

  const loginWithOtp = useCallback(async (email: string, code: string) => {
    setIsLoading(true);
    try {
      const response = await authApi.verifyOtp(email, code);
      applyAuth(response);
    } finally {
      setIsLoading(false);
    }
  }, [applyAuth]);

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    try {
      if (refreshToken) await authApi.logout(refreshToken);
    } catch { /* ignore logout errors */ }
    clearTokens();
    sessionStorage.removeItem(SESSION_KEY);
    setAuth({ isAuthenticated: false, user: null, currentDomainId: null });
  }, []);

  const switchDomain = useCallback(async (domainId: string) => {
    const response = await authApi.switchDomain(domainId);
    applyAuth(response);
  }, [applyAuth]);

  return (
    <AuthContext.Provider value={{ auth, login, loginWithOtp, sendOtp, logout, switchDomain, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
