// Base API client with JWT Bearer auth and automatic token refresh

export const BASE_URL = (import.meta.env.VITE_API_URL as string | undefined) ?? 'http://localhost:5200';

// In-memory token store (survives re-renders, cleared on tab close)
let _accessToken: string | null = sessionStorage.getItem('pap_access_token');
let _refreshToken: string | null = sessionStorage.getItem('pap_refresh_token');

export function setTokens(access: string, refresh: string) {
  _accessToken = access;
  _refreshToken = refresh;
  sessionStorage.setItem('pap_access_token', access);
  sessionStorage.setItem('pap_refresh_token', refresh);
}

export function clearTokens() {
  _accessToken = null;
  _refreshToken = null;
  sessionStorage.removeItem('pap_access_token');
  sessionStorage.removeItem('pap_refresh_token');
}

export function getAccessToken() { return _accessToken; }
export function getRefreshToken() { return _refreshToken; }

export class ApiError extends Error {
  constructor(public status: number, message: string, public details?: unknown) {
    super(message);
    this.name = 'ApiError';
  }
}

let isRefreshing = false;
type RefreshSubscriber = (token: string) => void;
let refreshSubscribers: RefreshSubscriber[] = [];

function onRefreshed(token: string) {
  refreshSubscribers.forEach(cb => cb(token));
  refreshSubscribers = [];
}

async function tryRefreshTokens(): Promise<string | null> {
  if (!_refreshToken) return null;
  if (isRefreshing) {
    return new Promise(resolve => refreshSubscribers.push(resolve));
  }
  isRefreshing = true;
  try {
    const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: _refreshToken }),
    });
    if (!res.ok) { clearTokens(); return null; }
    const data = await res.json();
    setTokens(data.accessToken, data.refreshToken);
    onRefreshed(data.accessToken);
    return data.accessToken;
  } catch {
    clearTokens();
    return null;
  } finally {
    isRefreshing = false;
  }
}

export async function apiRequest<T>(
  path: string, 
  options: RequestInit & { responseType?: 'json' | 'text' | 'blob' } = {}
): Promise<T> {
  const { responseType = 'json', ...fetchOptions } = options;

  const makeRequest = async (token: string | null): Promise<Response> => {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(fetchOptions.headers as Record<string, string> ?? {}),
    };
    return fetch(`${BASE_URL}${path}`, { ...fetchOptions, headers });
  };

  let response = await makeRequest(_accessToken);

  if (response.status === 401) {
    const newToken = await tryRefreshTokens();
    if (newToken) {
      response = await makeRequest(newToken);
    } else {
      clearTokens();
      window.location.href = '/login';
      throw new ApiError(401, 'Session expired. Please log in again.');
    }
  }

  if (response.status === 204) return undefined as T;
  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as Record<string, unknown>;
    throw new ApiError(response.status, (error.title as string) ?? 'An error occurred', error);
  }

  if (responseType === 'text') return response.text() as unknown as T;
  if (responseType === 'blob') return response.blob() as unknown as T;
  return response.json() as Promise<T>;
}
