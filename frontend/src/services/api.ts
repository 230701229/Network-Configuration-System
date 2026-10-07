import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('nms_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('nms_token');
      localStorage.removeItem('nms_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
