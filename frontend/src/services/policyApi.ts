import { apiClient } from './api';
import { Policy } from '../types';

export const policyApi = {
  getPolicies: async (): Promise<Policy[]> => {
    const { data } = await apiClient.get<Policy[]>('/policies');
    return data;
  },
  
  getPolicy: async (id: number): Promise<Policy> => {
    const { data } = await apiClient.get<Policy>(`/policies/${id}`);
    return data;
  },
  
  createPolicy: async (req: Partial<Policy>): Promise<Policy> => {
    const { data } = await apiClient.post<Policy>('/policies', req);
    return data;
  },
  
  updatePolicy: async (id: number, req: Partial<Policy>): Promise<Policy> => {
    const { data } = await apiClient.put<Policy>(`/policies/${id}`, req);
    return data;
  },
  
  deletePolicy: async (id: number): Promise<void> => {
    await apiClient.delete(`/policies/${id}`);
  }
};
