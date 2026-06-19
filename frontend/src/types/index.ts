export interface ExtractedData {
  vendorName?: string;
  gstNumber?: string;
  invoiceNumber?: string;
  invoiceDate?: string;
  customerName?: string;
  taxAmount?: number;
  totalAmount?: number;
  currency?: string;
  hsnSacCodes?: string[];
  lineItems?: LineItem[];
  qrCode?: string;
}

export interface LineItem {
  description?: string;
  quantity?: number;
  unitPrice?: number;
  amount?: number;
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
  fraudDetected: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface VerificationResult {
  status: 'VERIFIED' | 'LOW_RISK' | 'REVIEW_REQUIRED' | 'HIGH_RISK';
  score: number;
  details: string[];
}

export interface Vendor {
  gstin: string;
  legalName: string;
  tradeName?: string;
  state?: string;
  status: string;
  verifiedAt?: string;
}

export interface ThreeWayMatchRequest {
  invoiceNumber: string;
  invoiceAmount: number;
  vendorGstin: string;
  invoiceDate: string;
}

export interface ThreeWayMatchResult {
  poMatch: boolean;
  grMatch: boolean;
  poGrLinkMatch: boolean;
  amountMatch: boolean;
  gstinMatch: boolean;
  allMatch: boolean;
  issues: Record<string, string>;
}
