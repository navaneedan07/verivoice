export interface ExtractedData {
  vendorName?: string;
  gstNumber?: string;
  invoiceNumber?: string;
  purchaseOrderNumber?: string;
  invoiceDate?: string;
  customerName?: string;
  taxAmount?: number;
  totalAmount?: number;
  subtotal?: number;
  cgstAmount?: number;
  sgstAmount?: number;
  igstAmount?: number;
  gstRate?: number;
  currency?: string;
  paymentMethod?: string;
  hsnSac?: string;
  qrCode?: string;
  irn?: string;
  confidenceScore?: number;
}
export interface VerificationCheck {
  layer: string;
  code: string;
  status: 'PASSED' | 'FAILED' | 'NOT_PERFORMED';
  message: string;
  detail?: string;
}
export interface DocumentDto {
  id: string;
  fileName: string;
  status: 'PROCESSING' | 'APPROVED' | 'NEEDS_REVIEW' | 'FLAGGED';
  riskScore: number;
  verificationScore: number;
  extractedData: ExtractedData;
  verificationChecks: VerificationCheck[];
  anomalies: string[];
  uploadDate?: string;
}

export interface Vendor {
  gstin: string;
  legalName: string;
  tradeName?: string;
  state?: string;
  status: string;
  verifiedAt?: string;
}

export interface DashboardStats {
  totalInvoices: number;
  verified: number;
  flagged: number;
  pendingReview: number;
  recentVerifications: DocumentDto[];
}
