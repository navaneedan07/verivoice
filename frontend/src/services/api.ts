import axios from 'axios';
import type { DocumentDto, DashboardStats } from '../types';

const configuredApiUrl = String(import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');
const API_BASE_URL = configuredApiUrl.endsWith('/api')
  ? configuredApiUrl
  : `${configuredApiUrl}/api`;

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
    const stats = response.data ?? {};
    return {
      totalInvoices: Number(stats.totalInvoices) || 0,
      verified: Number(stats.verified) || 0,
      flagged: Number(stats.flagged) || 0,
      pendingReview: Number(stats.pendingReview) || 0,
      recentVerifications: Array.isArray(stats.recentVerifications)
        ? stats.recentVerifications
        : [],
    };
  },
};

export default api;
