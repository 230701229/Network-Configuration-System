import { apiClient } from './api';
import { ComplianceScan } from '../types';

export const complianceApi = {
  scanAllDevices: async (): Promise<{ scanned: number, successful: number }> => {
    const { data } = await apiClient.post<{ scanned: number, successful: number }>('/compliance/scan-all');
    return data;
  },
  
  getComplianceResults: async (): Promise<ComplianceScan[]> => {
    const { data } = await apiClient.get<ComplianceScan[]>('/compliance');
    return data;
  },
  
  getScan: async (scanId: number): Promise<ComplianceScan> => {
    const { data } = await apiClient.get<ComplianceScan>(`/compliance/${scanId}`);
    return data;
  }
};
