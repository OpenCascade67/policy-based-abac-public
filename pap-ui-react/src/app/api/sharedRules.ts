import { apiRequest } from './client';
import type { SharedRuleDto, PolicyTargetDto, PolicyConditionDto } from './types';

export const sharedRulesApi = {
  getAll: () => apiRequest<SharedRuleDto[]>('/api/shared-rules'),
  getById: (id: string) => apiRequest<SharedRuleDto>(`/api/shared-rules/${id}`),
  create: (data: { name: string; description: string; category: string; effect: string; target?: PolicyTargetDto; condition?: PolicyConditionDto }) =>
    apiRequest<SharedRuleDto>('/api/shared-rules', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, data: { name: string; description: string; category: string; effect: string; target?: PolicyTargetDto; condition?: PolicyConditionDto }) =>
    apiRequest<SharedRuleDto>(`/api/shared-rules/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: string) => apiRequest<void>(`/api/shared-rules/${id}`, { method: 'DELETE' }),
};
