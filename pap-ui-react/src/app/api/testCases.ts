import { apiRequest } from './client';
import type { TestCaseDto, TestResultDto, PagedResult } from './types';

export interface RunTestCaseResponse {
  decision: TestResultDto['decision'];
  matchedPolicies: string[];
  executedAt: string;
}

export const testCasesApi = {
  getAll: (page = 0, pageSize = 20) =>
    apiRequest<PagedResult<TestCaseDto>>(`/api/test-cases?page=${page}&pageSize=${pageSize}`),
  create: (data: { name: string; description: string; policyId?: string; scenarioId?: string; inputData: string }) =>
    apiRequest<TestCaseDto>('/api/test-cases', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: string, data: Partial<TestCaseDto>) =>
    apiRequest<TestCaseDto>(`/api/test-cases/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: string) => apiRequest<void>(`/api/test-cases/${id}`, { method: 'DELETE' }),
  run: (id: string, inputData?: string) =>
    apiRequest<RunTestCaseResponse>(`/api/test-cases/${id}/run`, { 
      method: 'POST', 
      body: JSON.stringify({ inputData }) 
    }),
};
