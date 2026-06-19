import React, { useState } from 'react';
import { erpService } from '../services/api';
import { ThreeWayMatchResult, ThreeWayMatchRequest } from '../types';
import { Link2, Loader, CheckCircle, AlertCircle } from 'lucide-react';

export default function ThreeWayMatching() {
  const [formData, setFormData] = useState({
    poNumber: '',
    grnNumber: '',
    invoiceNumber: '',
    invoiceAmount: '',
    vendorGstin: '',
    invoiceDate: '',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<ThreeWayMatchResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setResult(null);

    // Validation
    if (!formData.poNumber || !formData.grnNumber || !formData.invoiceNumber || !formData.invoiceAmount || !formData.vendorGstin) {
      setError('Please fill in all required fields');
      return;
    }

    setIsLoading(true);
    try {
      const invoiceData: ThreeWayMatchRequest = {
        invoiceNumber: formData.invoiceNumber,
        invoiceAmount: parseFloat(formData.invoiceAmount),
        vendorGstin: formData.vendorGstin.toUpperCase(),
        invoiceDate: formData.invoiceDate,
      };

      const matchResult = await erpService.threeWayMatch(
        formData.poNumber,
        formData.grnNumber,
        invoiceData
      );
      setResult(matchResult);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to perform 3-way matching. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Form */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="mb-6 flex items-center gap-2">
          <Link2 size={24} className="text-blue-600" />
          <div>
            <h2 className="text-2xl font-bold text-gray-900">3-Way Matching</h2>
            <p className="text-sm text-gray-600">Validate PO, GR, and Invoice alignment</p>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
            <AlertCircle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                PO Number *
              </label>
              <input
                type="text"
                name="poNumber"
                value={formData.poNumber}
                onChange={handleChange}
                placeholder="e.g., PO-001"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                GRN Number *
              </label>
              <input
                type="text"
                name="grnNumber"
                value={formData.grnNumber}
                onChange={handleChange}
                placeholder="e.g., GRN-001"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Invoice Number *
            </label>
            <input
              type="text"
              name="invoiceNumber"
              value={formData.invoiceNumber}
              onChange={handleChange}
              placeholder="e.g., INV-2024-001"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Invoice Amount *
              </label>
              <input
                type="number"
                step="0.01"
                name="invoiceAmount"
                value={formData.invoiceAmount}
                onChange={handleChange}
                placeholder="0.00"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Invoice Date
              </label>
              <input
                type="date"
                name="invoiceDate"
                value={formData.invoiceDate}
                onChange={handleChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Vendor GSTIN *
            </label>
            <input
              type="text"
              name="vendorGstin"
              value={formData.vendorGstin}
              onChange={handleChange}
              placeholder="e.g., 33AAACW4514C1ZW"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {isLoading ? (
              <>
                <Loader size={20} className="animate-spin" />
                Processing...
              </>
            ) : (
              'Match Documents'
            )}
          </button>
        </form>
      </div>

      {/* Result */}
      {result && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <h3 className="text-lg font-bold text-gray-900 mb-4">Matching Result</h3>

          <div className={`p-4 rounded-lg mb-6 ${result.allMatch ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'}`}>
            <div className="flex items-center gap-2 mb-2">
              {result.allMatch ? (
                <CheckCircle size={24} className="text-green-600" />
              ) : (
                <AlertCircle size={24} className="text-red-600" />
              )}
              <p className={`text-lg font-bold ${result.allMatch ? 'text-green-900' : 'text-red-900'}`}>
                {result.allMatch ? 'All Checks Passed' : 'Matching Failed'}
              </p>
            </div>
            <p className={`text-sm ${result.allMatch ? 'text-green-700' : 'text-red-700'}`}>
              {result.allMatch
                ? 'All documents are aligned. Safe to proceed with payment.'
                : 'Discrepancies detected. Please review before processing.'}
            </p>
          </div>

          <div className="space-y-3">
            <CheckItem label="PO Match" status={result.poMatch} />
            <CheckItem label="GRN Match" status={result.grMatch} />
            <CheckItem label="PO-GRN Link" status={result.poGrLinkMatch} />
            <CheckItem label="Amount Match" status={result.amountMatch} />
            <CheckItem label="GSTIN Match" status={result.gstinMatch} />
          </div>

          {Object.keys(result.issues).length > 0 && (
            <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <h4 className="font-medium text-yellow-900 mb-2">Issues Found</h4>
              <ul className="space-y-1">
                {Object.entries(result.issues).map(([key, issue]) => (
                  <li key={key} className="text-sm text-yellow-800">• {issue}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

interface CheckItemProps {
  label: string;
  status: boolean;
}

function CheckItem({ label, status }: CheckItemProps) {
  return (
    <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
      <p className="text-sm font-medium text-gray-900">{label}</p>
      <span className={`text-lg font-bold ${status ? 'text-green-600' : 'text-red-600'}`}>
        {status ? '✓' : '✕'}
      </span>
    </div>
  );
}
