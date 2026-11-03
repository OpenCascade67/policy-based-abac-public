import { apiRequest } from './client';
import type { DomainDto } from './types';

export const domainsApi = {
  getAll: () => apiRequest<DomainDto[]>('/api/domains'),
  getById: (id: string) => apiRequest<DomainDto>(`/api/domains/${id}`),
  create: (data: { name: string; description: string; status: string }) =>
    apiRequest<DomainDto>('/api/domains', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, data: { name: string; description: string; status: string }) =>
    apiRequest<DomainDto>(`/api/domains/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: string) => apiRequest<void>(`/api/domains/${id}`, { method: 'DELETE' }),
};
