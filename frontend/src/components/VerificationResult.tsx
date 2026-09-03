import type { DocumentDto, VerificationCheck } from '../types';
import { getRiskLevel, formatCurrency, formatDate, getStatusBadge } from '../utils/helpers';
import { AlertCircle, CheckCircle, XCircle, HelpCircle, Shield, BarChart3, FileText, Hash, DollarSign } from 'lucide-react';

interface VerificationResultProps {
  document: DocumentDto;
}

// ─── Status icon helper ───────────────────────────────────
const statusIcon = (status: string, size = 18) => {
  switch (status) {
    case 'PASSED':
      return <CheckCircle size={size} className="text-green-600" />;
    case 'FAILED':
      return <XCircle size={size} className="text-red-600" />;
    default:
      return <HelpCircle size={size} className="text-gray-400" />;
  }
};

// ─── Score bar color scales ────────────────────────────────
const getScoreBarColor = (score: number, isRisk = false): string => {
  if (isRisk) {
    if (score >= 90) return '#ef4444'; // high risk → red
    if (score >= 70) return '#f97316'; // elevated → orange
    if (score >= 40) return '#eab308'; // moderate → yellow
    return '#22c55e';                // low → green
  }
  if (score >= 90) return '#22c55e'; // excellent → green
  if (score >= 70) return '#3b82f6'; // good → blue
  if (score >= 40) return '#eab308'; // fair → yellow
  return '#ef4444';                  // poor → red
};

const getScoreLabel = (score: number, isRisk = false): string => {
  if (isRisk) {
    if (score >= 90) return 'Critical';
    if (score >= 70) return 'Elevated';
    if (score >= 40) return 'Moderate';
    return 'Low';
  }
  if (score >= 90) return 'Excellent';
  if (score >= 70) return 'Good';
  if (score >= 40) return 'Fair';
  return 'Poor';
};

// ─── Score card sub-component ─────────────────────────────
function ScoreCard({ label, score, isRisk }: { label: string; score: number; isRisk?: boolean }) {
  const barColor = getScoreBarColor(score, isRisk);
  const scoreLabel = getScoreLabel(score, isRisk);
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-gray-500">{label}</p>
        <span
          className="text-xs font-semibold px-2 py-0.5 rounded-full"
          style={{ backgroundColor: barColor + '18', color: barColor }}
        >
          {scoreLabel}
        </span>
      </div>
      <div className="mt-2 flex items-end gap-2">
        <span className="text-3xl font-bold text-gray-900">{Math.round(score)}</span>
        <span className="text-gray-400 mb-1 text-sm">/100</span>
      </div>
      <div className="mt-3">
        <div className="h-2.5 rounded-full bg-gray-100 overflow-hidden">
          <div
            className="h-full rounded-full transition-all duration-700"
            style={{ width: `${Math.max(0, Math.min(100, score))}%`, background: barColor }}
          />
        </div>
      </div>
    </div>
  );
}

// ─── Anomaly item sub-component ───────────────────────────
function AnomalyItem({ anomaly }: { anomaly: string }) {
  // Split "key: message" format from backend
  const colonIdx = anomaly.indexOf(': ');
  const hasKey = colonIdx > 0 && colonIdx < 60;
  const key = hasKey ? anomaly.slice(0, colonIdx) : null;
  const message = hasKey ? anomaly.slice(colonIdx + 2) : anomaly;

  const isDuplicate =
    key?.toLowerCase().includes('duplicate') || key?.toLowerCase().includes('unique');
  const isTax =
    key?.toLowerCase().includes('gst') || key?.toLowerCase().includes('tax') || key?.toLowerCase().includes('reconcil');

  return (
    <li className="flex items-start gap-3 p-3 rounded-lg bg-white/80 border border-yellow-100">
      <div
        className={`p-1.5 rounded-full flex-shrink-0 mt-0.5 ${
          isDuplicate ? 'bg-red-50' : isTax ? 'bg-orange-50' : 'bg-yellow-50'
        }`}
      >
        {isDuplicate ? (
          <AlertCircle size={14} className="text-red-500" />
        ) : isTax ? (
          <XCircle size={14} className="text-orange-500" />
        ) : (
          <AlertCircle size={14} className="text-yellow-500" />
        )}
      </div>
      <div className="flex-1 min-w-0">
        {key && <p className="text-xs font-semibold text-yellow-900 uppercase tracking-wider">{key}</p>}
        <p className="text-sm text-yellow-800 mt-0.5">{message}</p>
      </div>
    </li>
  );
}

// ─── Main component ───────────────────────────────────────
export default function VerificationResult({ document }: VerificationResultProps) {
  const d = document; // short alias to avoid shadowing the global `document`
  const verificationChecks = Array.isArray(d.verificationChecks) ? d.verificationChecks : [];

  const riskLevel = getRiskLevel(d.verificationScore);
  const statusBadge = getStatusBadge(d.status);

  const groupedChecks = verificationChecks.reduce(
    (acc, check) => {
      if (!acc[check.layer]) acc[check.layer] = [];
      acc[check.layer].push(check);
      return acc;
    },
    {} as Record<string, VerificationCheck[]>,
  );

  const totalPassed = verificationChecks.filter((c) => c.status === 'PASSED').length;
  const totalFailed = verificationChecks.filter((c) => c.status === 'FAILED').length;
  const totalSkipped = verificationChecks.filter((c) => c.status === 'NOT_PERFORMED').length;

  const criticalCodes = ['GST_STATUS', 'SIGNATURE_VALID', 'PAYLOAD_MATCH'];
  const criticalChecks = criticalCodes
    .map((code) => verificationChecks.find((c) => c.code === code))
    .filter(Boolean) as VerificationCheck[];

  // ── Render ──────────────────────────────────────────────
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
      {/* ── Header ───────────────────────────────────────── */}
      <div className={`p-6 border-b border-gray-200 ${riskLevel.bgColor}`}>
        <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
          <div className="min-w-0">
            <div className="flex items-center gap-3 mb-3">
              <Shield size={24} className="text-gray-700 flex-shrink-0" />
              <h2 className="text-2xl font-bold text-gray-900 truncate">{d.fileName}</h2>
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${statusBadge.color}`}>
                {statusBadge.label}
              </span>
              <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-semibold ${riskLevel.bgColor} ${riskLevel.color}`}>
                {riskLevel.label}
              </span>
            </div>
          </div>

          {/* Score cards */}
          <div className="grid grid-cols-2 gap-3 w-full md:w-80">
            <ScoreCard label="Verification" score={d.verificationScore} />
            <ScoreCard label="Risk" score={d.riskScore} isRisk />
          </div>
        </div>

        {/* Summary ribbon – uses consistent "skipped" terminology */}
        <div className="mt-4 flex items-center gap-4 text-sm text-gray-600">
          <span className="flex items-center gap-1.5">
            <CheckCircle size={15} className="text-green-500" />
            {totalPassed} passed
          </span>
          <span className="flex items-center gap-1.5">
            <XCircle size={15} className="text-red-500" />
            {totalFailed} failed
          </span>
          <span className="flex items-center gap-1.5">
            <HelpCircle size={15} className="text-gray-400" />
            {totalSkipped} skipped
          </span>
        </div>
      </div>

      {/* ── Extracted Data ───────────────────────────────── */}
      <div className="p-6 border-b border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <FileText size={20} className="text-blue-600" />
          Extracted Information
        </h3>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          {/* Vendor */}
          <div className="lg:col-span-1 rounded-xl border border-gray-200 bg-white p-4">
            <h4 className="text-sm font-semibold uppercase tracking-wider text-gray-500 mb-3">
              Vendor
            </h4>
            <div className="space-y-3">
              <div>
                <p className="text-xs text-gray-500">Vendor Name</p>
                <p className="text-base font-medium text-gray-900">{d.extractedData.vendorName || '-'}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500">GSTIN</p>
                <p className="text-base font-mono font-medium text-gray-900">{d.extractedData.gstNumber || '-'}</p>
              </div>
            </div>
          </div>

          {/* Invoice */}
          <div className="lg:col-span-1 rounded-xl border border-gray-200 bg-white p-4">
            <h4 className="text-sm font-semibold uppercase tracking-wider text-gray-500 mb-3 flex items-center gap-1">
              <Hash size={14} /> Invoice
            </h4>
            <div className="space-y-3">
              <div>
                <p className="text-xs text-gray-500">Invoice Number</p>
                <p className="text-base font-medium text-gray-900">{d.extractedData.invoiceNumber || '-'}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500">Invoice Date</p>
                <p className="text-base font-medium text-gray-900">{formatDate(d.extractedData.invoiceDate)}</p>
              </div>
            </div>
          </div>

          {/* Amounts */}
          <div className="lg:col-span-1 rounded-xl border border-gray-200 bg-white p-4">
            <h4 className="text-sm font-semibold uppercase tracking-wider text-gray-500 mb-3 flex items-center gap-1">
              <DollarSign size={14} /> Amounts
            </h4>
            <div className="space-y-3">
              <div>
                <p className="text-xs text-gray-500">Total Amount</p>
                <p className="text-base font-medium text-gray-900">{formatCurrency(d.extractedData.totalAmount)}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500">Tax Amount</p>
                <p className="text-base font-medium text-gray-900">{formatCurrency(d.extractedData.taxAmount)}</p>
              </div>
            </div>

            <div className="mt-4 pt-4 border-t border-gray-100">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <p className="text-xs text-gray-500">CGST</p>
                  <p className="text-sm font-medium text-gray-900">{formatCurrency(d.extractedData.cgstAmount)}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-500">SGST</p>
                  <p className="text-sm font-medium text-gray-900">{formatCurrency(d.extractedData.sgstAmount)}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-500">IGST</p>
                  <p className="text-sm font-medium text-gray-900">{formatCurrency(d.extractedData.igstAmount)}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-500">GST Rate</p>
                  <p className="text-sm font-medium text-gray-900">
                    {d.extractedData.gstRate != null ? `${d.extractedData.gstRate}%` : '-'}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Metadata row */}
        <div className="mt-4 grid grid-cols-2 md:grid-cols-4 gap-3">
          {[
            { label: 'Currency', value: d.extractedData.currency || 'INR' },
            {
              label: 'HSN/SAC',
              value: d.extractedData.hsnSac || '-',
            },
            { label: 'IRN', value: d.extractedData.irn || '-' },
          ].map(({ label, value }) => (
            <div key={label} className="rounded-lg border border-gray-100 bg-gray-50/50 p-3">
              <p className="text-xs text-gray-500">{label}</p>
              <p className="text-sm font-semibold text-gray-900 mt-0.5 truncate">{value}</p>
            </div>
          ))}
        </div>
      </div>

      {/* ── Critical Checks ──────────────────────────────── */}
      {criticalChecks.length > 0 && (
        <div className="p-6 border-b border-gray-200 bg-gradient-to-b from-gray-50 to-white">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <BarChart3 size={20} className="text-blue-600" />
            Critical Checks
          </h3>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {criticalChecks.map((check) => {
              const isPassed = check.status === 'PASSED';
              const isFailed = check.status === 'FAILED';
              return (
                <div
                  key={check.code}
                  className={`rounded-xl border p-4 transition-shadow hover:shadow-sm ${
                    isPassed
                      ? 'border-green-200 bg-green-50/50'
                      : isFailed
                      ? 'border-red-200 bg-red-50/50'
                      : 'border-gray-200 bg-white'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-3">
                      {statusIcon(check.status, 20)}
                      <div>
                        <p className="text-sm font-semibold text-gray-900">{check.code}</p>
                        <p
                          className={`text-xs mt-0.5 ${isPassed ? 'text-green-700' : isFailed ? 'text-red-700' : 'text-gray-500'}`}
                        >
                          {check.message}
                        </p>
                      </div>
                    </div>
                    <span
                      className={`flex-shrink-0 px-2.5 py-1 rounded-full text-xs font-semibold ${
                        isPassed
                          ? 'bg-green-100 text-green-800'
                          : isFailed
                          ? 'bg-red-100 text-red-800'
                          : 'bg-gray-100 text-gray-600'
                      }`}
                    >
                      {check.status === 'PASSED' ? 'PASSED' : check.status === 'FAILED' ? 'FAILED' : 'N/A'}
                    </span>
                  </div>
                  {check.detail && <p className="mt-2 text-xs text-gray-500 ml-9">{check.detail}</p>}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ── Verification Checks ──────────────────────────── */}
      <div className="p-6 border-b border-gray-200">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
          <Shield size={20} className="text-blue-600" />
          Verification Checks
        </h3>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {Object.entries(groupedChecks).map(([layer, checks]) => {
            const failedCount = checks.filter((c) => c.status === 'FAILED').length;
            const passedCount = checks.filter((c) => c.status === 'PASSED').length;
            const skippedCount = checks.filter((c) => c.status === 'NOT_PERFORMED').length;

            return (
              <div key={layer} className="rounded-xl border border-gray-200 bg-white overflow-hidden">
                {/* Layer header */}
                <div className="flex items-center justify-between gap-3 px-4 py-3 border-b border-gray-100 bg-gray-50/80">
                  <div>
                    <h4 className="font-semibold text-gray-900 text-sm">{layer}</h4>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {passedCount > 0 && <span className="text-green-600">{passedCount} passed</span>}
                      {failedCount > 0 && <span className="text-red-600 ml-2">{failedCount} failed</span>}
                      {skippedCount > 0 && <span className="text-gray-400 ml-2">{skippedCount} skipped</span>}
                    </p>
                  </div>
                  <span
                    className={`flex-shrink-0 px-2.5 py-1 rounded-full text-xs font-semibold ${
                      failedCount > 0
                        ? 'bg-red-50 text-red-700'
                        : passedCount > 0
                        ? 'bg-green-50 text-green-700'
                        : 'bg-gray-100 text-gray-600'
                    }`}
                  >
                    {failedCount > 0 ? 'Attention' : 'Healthy'}
                  </span>
                </div>

                {/* Check items */}
                <div className="divide-y divide-gray-50">
                  {checks.map((check, idx) => (
                    <div
                      key={idx}
                      className={`px-4 py-3 transition-colors ${
                        check.status === 'FAILED'
                          ? 'bg-red-50/30'
                          : check.status === 'PASSED'
                          ? 'bg-green-50/20'
                          : ''
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        <div className="flex-shrink-0 mt-0.5">{statusIcon(check.status, 16)}</div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between gap-3">
                            <p className="text-sm font-semibold text-gray-900">{check.code}</p>
                            <span
                              className={`flex-shrink-0 text-xs font-semibold px-2 py-0.5 rounded-full ${
                                check.status === 'PASSED'
                                  ? 'text-green-700 bg-green-50'
                                  : check.status === 'FAILED'
                                  ? 'text-red-700 bg-red-50'
                                  : 'text-gray-500 bg-gray-100'
                              }`}
                            >
                              {check.status === 'NOT_PERFORMED' ? 'SKIPPED' : check.status}
                            </span>
                          </div>
                          <p className="text-sm text-gray-600 mt-0.5">{check.message}</p>
                          {check.detail && <p className="text-xs text-gray-400 mt-0.5">{check.detail}</p>}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* ── Anomalies ────────────────────────────────────── */}
      {d.anomalies && d.anomalies.length > 0 && (
        <div className="p-6 bg-gradient-to-b from-yellow-50 to-white">
          <h3 className="text-lg font-semibold text-yellow-900 mb-4 flex items-center gap-2">
            <AlertCircle size={20} />
            Detected Anomalies
          </h3>
          <ul className="space-y-2">
            {d.anomalies.map((anomaly, idx) => (
              <AnomalyItem key={idx} anomaly={anomaly} />
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
