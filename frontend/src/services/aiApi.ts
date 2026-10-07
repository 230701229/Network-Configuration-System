import { apiClient } from './api';
import { AIAnalysis } from '../types';

export const aiApi = {
  getAnalyses: async (): Promise<AIAnalysis[]> => {
    const { data } = await apiClient.get<AIAnalysis[]>('/ai/analyses');
    return data;
  },
  
  getDeviceAnalyses: async (deviceId: number): Promise<AIAnalysis[]> => {
    const { data } = await apiClient.get<AIAnalysis[]>(`/ai/device/${deviceId}`);
    return data;
  }
};
