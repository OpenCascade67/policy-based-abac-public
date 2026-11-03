import { apiRequest } from './client';
import type { ScenarioDto } from './types';

export const scenariosApi = {
  getAll: () => apiRequest<ScenarioDto[]>('/api/scenarios'),
  create: (data: { name: string; description: string; value: string }) =>
    apiRequest<ScenarioDto>('/api/scenarios', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, data: { name: string; description: string; value: string }) =>
    apiRequest<ScenarioDto>(`/api/scenarios/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: string) => apiRequest<void>(`/api/scenarios/${id}`, { method: 'DELETE' }),
};
