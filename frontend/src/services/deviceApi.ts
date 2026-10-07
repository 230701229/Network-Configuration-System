import { apiClient } from './api';
import { Device, CreateDeviceRequest, NormalizedConfiguration, ComplianceScan, DriftEvent, TestConnectionResponse } from '../types';

export const deviceApi = {
  getDevices: async (): Promise<Device[]> => {
    const { data } = await apiClient.get<Device[]>('/devices');
    return data;
  },

  getAllDevices: async (): Promise<Device[]> => {
    const { data } = await apiClient.get<Device[]>('/devices');
    return data;
  },
  
  getDevice: async (id: number): Promise<Device> => {
    const { data } = await apiClient.get<Device>(`/devices/${id}`);
    return data;
  },
  
  createDevice: async (req: CreateDeviceRequest): Promise<Device> => {
    const { data } = await apiClient.post<Device>('/devices', req);
    return data;
  },
  
  updateDevice: async (id: number, req: Partial<CreateDeviceRequest>): Promise<Device> => {
    const { data } = await apiClient.put<Device>(`/devices/${id}`, req);
    return data;
  },
  
  deleteDevice: async (id: number): Promise<void> => {
    await apiClient.delete(`/devices/${id}`);
  },
  
  testConnection: async (id: number): Promise<TestConnectionResponse> => {
    const { data } = await apiClient.post<TestConnectionResponse>(`/devices/${id}/test`);
    return data;
  },
  
  retrieveConfiguration: async (id: number): Promise<NormalizedConfiguration> => {
    const { data } = await apiClient.post<NormalizedConfiguration>(`/devices/${id}/config`);
    return data;
  },
  
  scanDevice: async (id: number): Promise<ComplianceScan> => {
    const { data } = await apiClient.post<ComplianceScan>(`/devices/${id}/scan`);
    return data;
  },
  
  getDeviceCompliance: async (id: number): Promise<ComplianceScan[]> => {
    const { data } = await apiClient.get<ComplianceScan[]>(`/devices/${id}/compliance`);
    return data;
  },
  
  getDeviceDrift: async (id: number): Promise<DriftEvent[]> => {
    const { data } = await apiClient.get<DriftEvent[]>(`/devices/${id}/drift`);
    return data;
  },
  
  getDeviceHistory: async (id: number): Promise<ComplianceScan[]> => {
    const { data } = await apiClient.get<ComplianceScan[]>(`/devices/${id}/history`);
    return data;
  },
  
  simulateDrift: async (id: number): Promise<void> => {
    await apiClient.post(`/devices/${id}/simulate-drift`);
  },
  
  resetDrift: async (id: number): Promise<void> => {
    await apiClient.post(`/devices/${id}/reset-drift`);
  }
};
