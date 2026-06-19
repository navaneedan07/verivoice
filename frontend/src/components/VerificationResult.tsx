import React from 'react';
import { DocumentDto, VerificationCheck } from '../types';
import { getRiskLevel, formatCurrency, formatDate, getStatusBadge, getCheckIcon } from '../utils/helpers';
import { AlertCircle, CheckCircle, Clock } from 'lucide-react';

interface VerificationResultProps {
  document: DocumentDto;
}

export default function VerificationResult({ document }: VerificationResultProps) {
  const riskLevel = getRiskLevel(document.verificationScore);
  const statusBadge = getStatusBadge(document.status);

  const groupedChecks = document.verificationChecks.reduce((acc, check) => {
    if (!acc[check.layer]) acc[check.layer] = [];
    acc[check.layer].push(check);
    return acc;
  }, {} as Record<string, VerificationCheck[]>);

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200">
      {/* Header */}
      <div className={`p-6 border-b border-gray-200 ${riskLevel.bgColor}`}>
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">{document.fileName}</h2>
            <div className="flex items-center gap-4 flex-wrap">
              <span className={`inline-block px-3 py-1 rounded-full text-sm font-medium ${statusBadge.color}`}>
                {statusBadge.label}
              </span>
              <span className={`text-sm font-medium ${riskLevel.color}`}>{riskLevel.label}</span>
              {document.fraudDetected && (
                <span className="text-sm font-medium text-red-700 flex items-center gap-1">
                  <AlertCircle size={16} />
                  Fraud Detected
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Risk Score */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-gray-600 mb-1">Verification Score</p>
            <div className="flex items-end gap-2">
              <span className="text-3xl font-bold text-gray-900">{Math.round(document.verificationScore)}</span>
              <span className="text-gray-600 mb-1">/100</span>
            </div>
          </div>
          <div>
            <p className="text-sm text-gray-600 mb-1">Risk Score</p>
            <div className="flex items-end gap-2">
              <span className="text-3xl font-bold text-gray-900">{Math.round(document.riskScore)}</span>
              <span className="text-gray-600 mb-1">/100</span>
            </div>
          </div>
        </div>
      </div>

      {/* Extracted Data */}
      <div className="p-6 border-b border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Extracted Information</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <p className="text-sm text-gray-500">Vendor Name</p>
            <p className="text-base font-medium text-gray-900">{document.extractedData.vendorName || '-'}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">GSTIN</p>
            <p className="text-base font-medium text-gray-900">{document.extractedData.gstNumber || '-'}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Invoice Number</p>
            <p className="text-base font-medium text-gray-900">{document.extractedData.invoiceNumber || '-'}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Invoice Date</p>
            <p className="text-base font-medium text-gray-900">{formatDate(document.extractedData.invoiceDate)}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Total Amount</p>
            <p className="text-base font-medium text-gray-900">{formatCurrency(document.extractedData.totalAmount)}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Tax Amount</p>
            <p className="text-base font-medium text-gray-900">{formatCurrency(document.extractedData.taxAmount)}</p>
          </div>
        </div>
      </div>

      {/* Verification Checks */}
      <div className="p-6 border-b border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Verification Checks</h3>
        <div className="space-y-4">
          {Object.entries(groupedChecks).map(([layer, checks]) => (
            <div key={layer}>
              <h4 className="font-medium text-gray-900 mb-2 text-sm">{layer}</h4>
              <div className="space-y-2 ml-4">
                {checks.map((check, idx) => (
                  <div key={idx} className="flex items-start gap-3">
                    <span
                      className={`text-lg font-bold mt-0.5 ${
                        check.status === 'PASSED'
                          ? 'text-green-600'
                          : check.status === 'FAILED'
                          ? 'text-red-600'
                          : 'text-gray-400'
                      }`}
                    >
                      {getCheckIcon(check.status)}
                    </span>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-gray-900">{check.code}</p>
                      <p className="text-sm text-gray-600">{check.message}</p>
                      {check.detail && <p className="text-xs text-gray-500 mt-1">{check.detail}</p>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Anomalies */}
      {document.anomalies && document.anomalies.length > 0 && (
        <div className="p-6 bg-yellow-50 border-t border-yellow-200">
          <h3 className="text-lg font-semibold text-yellow-900 mb-3 flex items-center gap-2">
            <AlertCircle size={20} />
            Detected Anomalies
          </h3>
          <ul className="space-y-2">
            {document.anomalies.map((anomaly, idx) => (
              <li key={idx} className="text-sm text-yellow-800 flex items-start gap-2">
                <span className="text-yellow-600 mt-1">•</span>
                <span>{anomaly}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
