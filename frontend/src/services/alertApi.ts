import { apiClient } from './api';
import { Alert } from '../types';

export const alertApi = {
  getAlerts: async (): Promise<Alert[]> => {
    const { data } = await apiClient.get<Alert[]>('/alerts');
    return data;
  },
  
  acknowledgeAlert: async (id: number): Promise<Alert> => {
    const { data } = await apiClient.post<Alert>(`/alerts/${id}/acknowledge`);
    return data;
  }
};
