import { apiRequest } from './client';
import type { UserDto, PagedResult } from './types';

export const usersApi = {
  getAll: (page = 0, pageSize = 10, search?: string) =>
    apiRequest<PagedResult<UserDto>>(
      `/api/users?page=${page}&pageSize=${pageSize}${search ? `&search=${encodeURIComponent(search)}` : ''}`
    ),

  getById: (id: string) => apiRequest<UserDto>(`/api/users/${id}`),

  create: (data: { username: string; password: string; status: string; domainRoles: { domainId: string; roles: string[] }[] }) =>
    apiRequest<UserDto>('/api/users', { method: 'POST', body: JSON.stringify(data) }),

  update: (id: string, data: { password?: string; status: string; domainRoles: { domainId: string; roles: string[] }[] }) =>
    apiRequest<UserDto>(`/api/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),

  delete: (id: string) => apiRequest<void>(`/api/users/${id}`, { method: 'DELETE' }),

  setDefaultDomain: (id: string, domainId: string) =>
    apiRequest<void>(`/api/users/${id}/default-domain`, {
      method: 'PUT',
      body: JSON.stringify({ domainId }),
    }),
};
