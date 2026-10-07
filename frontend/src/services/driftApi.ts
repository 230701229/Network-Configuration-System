import { apiClient } from './api';
import { DriftEvent, DriftStatus, ConfigurationDiffData } from '../types';

export const driftApi = {
  getAllDrift: async (deviceId?: number, status?: string): Promise<DriftEvent[]> => {
    const params: Record<string, any> = {};
    if (deviceId) params.deviceId = deviceId;
    if (status && status !== 'ALL') params.status = status;
    const { data } = await apiClient.get<DriftEvent[]>('/drift', { params });
    return data;
  },

  getDeviceDrift: async (deviceId: number): Promise<DriftEvent[]> => {
    const { data } = await apiClient.get<DriftEvent[]>(`/drift/device/${deviceId}`);
    return data;
  },
  
  updateDriftStatus: async (id: number, status: DriftStatus): Promise<DriftEvent> => {
    const { data } = await apiClient.put<DriftEvent>(`/drift/${id}/status`, { status });
    return data;
  },

  detectAllDrift: async (): Promise<DriftEvent[]> => {
    const { data } = await apiClient.post<DriftEvent[]>('/drift/detect-all');
    return data;
  },

  detectDeviceDrift: async (deviceId: number): Promise<DriftEvent[]> => {
    const { data } = await apiClient.post<DriftEvent[]>(`/drift/device/${deviceId}/detect`);
    return data;
  },

  getDeviceDiff: async (deviceId: number): Promise<ConfigurationDiffData> => {
    const { data } = await apiClient.get<ConfigurationDiffData>(`/drift/device/${deviceId}/diff`);
    return data;
  },

  setBaseline: async (deviceId: number): Promise<{ message: string }> => {
    const { data } = await apiClient.post<{ message: string }>(`/drift/device/${deviceId}/set-baseline`);
    return data;
  },

  simulateDrift: async (deviceId: number, param: string = 'telnet', value: string = 'true'): Promise<{ message: string; driftsDetected: string }> => {
    const { data } = await apiClient.post<{ message: string; driftsDetected: string }>(
      `/drift/device/${deviceId}/simulate`,
      null,
      { params: { param, value } }
    );
    return data;
  },

  resetDrift: async (deviceId: number): Promise<{ message: string }> => {
    const { data } = await apiClient.post<{ message: string }>(`/drift/device/${deviceId}/reset-drift`);
    return data;
  }
};
