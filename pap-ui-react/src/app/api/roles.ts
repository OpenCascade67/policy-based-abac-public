import { apiRequest } from './client';
import type { RoleDto } from './types';

export const rolesApi = {
  getAll: () => apiRequest<RoleDto[]>('/api/roles'),
  create: (data: { name: string; description: string }) =>
    apiRequest<RoleDto>('/api/roles', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, data: { name: string; description: string }) =>
    apiRequest<RoleDto>(`/api/roles/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: string) => apiRequest<void>(`/api/roles/${id}`, { method: 'DELETE' }),
};
