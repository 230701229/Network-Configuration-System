import { apiClient } from './api';
import { DashboardSummary, DashboardTrends } from '../types';

export const dashboardApi = {
  getSummary: async (): Promise<DashboardSummary> => {
    const { data } = await apiClient.get<DashboardSummary>('/dashboard/summary');
    return data;
  },
  
  getTrends: async (): Promise<DashboardTrends> => {
    const { data } = await apiClient.get<DashboardTrends>('/dashboard/trends');
    return data;
  }
};
