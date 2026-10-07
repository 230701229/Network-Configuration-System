import { apiClient } from './api';
import { Gns3ServerStatus, Gns3Project, Gns3Node, Device } from '../types';

export const gns3Api = {
  testConnection: async (serverUrl?: string): Promise<Gns3ServerStatus> => {
    const { data } = await apiClient.post<Gns3ServerStatus>('/gns3/test-connection', { serverUrl });
    return data;
  },

  getProjects: async (serverUrl?: string): Promise<Gns3Project[]> => {
    const { data } = await apiClient.get<Gns3Project[]>('/gns3/projects', {
      params: serverUrl ? { serverUrl } : {}
    });
    return data;
  },

  getNodes: async (projectId: string, serverUrl?: string): Promise<Gns3Node[]> => {
    const { data } = await apiClient.get<Gns3Node[]>(`/gns3/projects/${projectId}/nodes`, {
      params: serverUrl ? { serverUrl } : {}
    });
    return data;
  },

  importNode: async (payload: {
    name: string;
    ipAddress: string;
    managementProtocol: string;
    deviceType: string;
    port?: string;
    username?: string;
    password?: string;
  }): Promise<Device> => {
    const { data } = await apiClient.post<Device>('/gns3/import-node', payload);
    return data;
  },

  getConfigTemplates: async (hostname?: string, ip?: string): Promise<{
    netconfConfig: string;
    restconfConfig: string;
    compliantBaseline: string;
    driftInjectionScript: string;
  }> => {
    const { data } = await apiClient.get('/gns3/config-templates', {
      params: { hostname, ip }
    });
    return data;
  }
};
