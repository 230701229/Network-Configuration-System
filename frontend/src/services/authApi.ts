import { apiClient } from './api';
import { LoginRequest, LoginResponse } from '../types';

export const authApi = {
  login: async (req: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/auth/login', req);
    if (response.data.token) {
      localStorage.setItem('nms_token', response.data.token);
      localStorage.setItem('nms_user', JSON.stringify({
        username: response.data.username,
        role: response.data.role
      }));
    }
    return response.data;
  },
  
  logout: () => {
    localStorage.removeItem('nms_token');
    localStorage.removeItem('nms_user');
  },
  
  getCurrentUser: () => {
    const userStr = localStorage.getItem('nms_user');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  }
};
