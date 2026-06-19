import axios from 'axios';
import type { DocumentDto, ThreeWayMatchRequest, ThreeWayMatchResult } from '../types';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Document Services
export const documentService = {
  uploadDocument: async (file: File): Promise<DocumentDto> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  getDocumentById: async (docId: string): Promise<DocumentDto> => {
    const response = await api.get(`/documents/${docId}`);
    return response.data;
  },

  getDocumentFile: async (docId: string) => {
    const response = await api.get(`/documents/${docId}/file`, {
      responseType: 'blob',
    });
    return response.data;
  },
};

// ERP Services
export const erpService = {
  createPurchaseOrder: async (order: any) => {
    const response = await api.post('/erp/purchase-orders', order);
    return response.data;
  },

  listPurchaseOrders: async () => {
    const response = await api.get('/erp/purchase-orders');
    return response.data;
  },

  createGoodsReceipt: async (receipt: any) => {
    const response = await api.post('/erp/goods-receipts', receipt);
    return response.data;
  },

  listGoodsReceipts: async () => {
    const response = await api.get('/erp/goods-receipts');
    return response.data;
  },

  threeWayMatch: async (poNumber: string, grnNumber: string, invoiceData: ThreeWayMatchRequest): Promise<ThreeWayMatchResult> => {
    const response = await api.post('/erp/matching/validate', invoiceData, {
      params: { poNumber, grnNumber },
    });
    return response.data;
  },
};

export default api;
