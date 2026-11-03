import { apiRequest } from './client';
import type { AppSettingsDto } from './types';

export const settingsApi = {
  get: () => apiRequest<AppSettingsDto>('/api/settings'),
  update: (data: { theme: string; colorScheme: string }) =>
    apiRequest<AppSettingsDto>('/api/settings', { method: 'PUT', body: JSON.stringify(data) }),
};
