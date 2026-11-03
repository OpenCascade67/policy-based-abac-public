import { apiRequest, BASE_URL } from './client';
import type { PolicyDto, PagedResult, PolicyDefinitionDto } from './types';

export const policiesApi = {
  getAll: (page = 0, pageSize = 10, search?: string) =>
    apiRequest<PagedResult<PolicyDto>>(
      `/api/policies?page=${page}&pageSize=${pageSize}${search ? `&search=${encodeURIComponent(search)}` : ''}`
    ),
  getById: (id: string) => apiRequest<PolicyDto>(`/api/policies/${id}`),
  create: (data: { name: string; description: string; policyDefinition: PolicyDefinitionDto }) =>
    apiRequest<PolicyDto>('/api/policies', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, data: { name: string; description: string; policyDefinition: PolicyDefinitionDto }) =>
    apiRequest<PolicyDto>(`/api/policies/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: string) => apiRequest<void>(`/api/policies/${id}`, { method: 'DELETE' }),
  exportJson: (id: string) => apiRequest<string>(`/api/policies/${id}/export/json`, { responseType: 'text' }),
  exportXml: (id: string) => apiRequest<string>(`/api/policies/${id}/export/xml`, { responseType: 'text' }),
};
