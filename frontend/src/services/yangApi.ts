import { apiClient } from './api';
import { YangModel } from '../types';

export const yangApi = {
  getYangModels: async (): Promise<YangModel[]> => {
    const { data } = await apiClient.get<YangModel[]>('/yang');
    return data;
  },
  
  createYangModel: async (req: Partial<YangModel>): Promise<YangModel> => {
    const { data } = await apiClient.post<YangModel>('/yang', req);
    return data;
  },
  
  deleteYangModel: async (id: number): Promise<void> => {
    await apiClient.delete(`/yang/${id}`);
  },
  
  validateYangModel: async (id: number): Promise<{ isValid: boolean, error?: string }> => {
    const { data } = await apiClient.post<{ isValid: boolean, error?: string }>(`/yang/${id}/validate`);
    return data;
  }
};
