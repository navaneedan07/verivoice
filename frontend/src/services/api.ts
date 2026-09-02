import axios from 'axios';
import type { DocumentDto, DashboardStats } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL;

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message || error.response?.data?.error;
    if (typeof message === 'string' && message.trim()) return message;
    if (error.message) return error.message;
  }
  return error instanceof Error && error.message ? error.message : fallback;
};

// Document Services
export const documentService = {
  uploadDocument: async (file: File): Promise<DocumentDto> => {
    const formData = new FormData();
    formData.append('file', file);
    // Backend route is POST /api/documents/upload
    const response = await api.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },
};

// Dashboard Services
export const dashboardService = {
  getStats: async (): Promise<DashboardStats> => {
    const response = await api.get('/dashboard/stats', {
      headers: { 'Cache-Control': 'no-cache' },
      params: { at: Date.now() },
    });
    return response.data;
  },
};

export default api;
