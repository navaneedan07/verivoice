import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, CheckCircle, TrendingUp, Database, Upload, Building2, AlertTriangle, RefreshCw } from 'lucide-react';
import { dashboardService, getApiErrorMessage } from '../services/api';
import type { DashboardStats } from '../types';
import { getRiskLevel, getStatusBadge } from '../utils/helpers';

interface StatsCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
  isLoading?: boolean;
}

function StatsCard({ title, value, icon, color, isLoading }: StatsCardProps) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 transition-all duration-300 hover:shadow-md hover:border-blue-200">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-500 mb-2">{title}</p>
          {isLoading ? (
            <div className="h-9 w-20 bg-gray-200 rounded animate-pulse" />
          ) : (
            <p className="text-3xl font-bold text-gray-900 transition-all duration-500">{value}</p>
          )}
        </div>
        <div className={`p-3 rounded-xl ${color} transition-transform duration-300 group-hover:scale-110`}>
          {icon}
        </div>
      </div>
    </div>
  );
}

export default function Dashboard() {
  const [data, setData] = useState<DashboardStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const loadStats = async (initialLoad = false) => {
      try {
        if (initialLoad) setIsLoading(true);
        else setIsRefreshing(true);
        setError(null);
        const stats = await dashboardService.getStats();
        if (!cancelled) {
          setData(stats);
          setLastUpdated(new Date());
        }
      } catch (err: unknown) {
        if (!cancelled) {
          setError(getApiErrorMessage(err, 'Failed to load dashboard stats'));
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
          setIsRefreshing(false);
        }
      }
    };

    loadStats(true);
    const refreshTimer = window.setInterval(() => loadStats(), 15000);
    const handleDocumentProcessed = () => loadStats();
    window.addEventListener('verivoice:document-processed', handleDocumentProcessed);

    return () => {
      cancelled = true;
      window.clearInterval(refreshTimer);
      window.removeEventListener('verivoice:document-processed', handleDocumentProcessed);
    };
  }, []);

  const stats = data
    ? {
        total: data.totalInvoices,
        verified: data.verified,
        flagged: data.flagged,
        pending: data.pendingReview,
      }
    : { total: 0, verified: 0, flagged: 0, pending: 0 };

  const recentItems = data?.recentVerifications ?? [];

  return (
    <div>
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Dashboard</h1>
        <div className="flex items-center gap-2 text-gray-500">
          <p>Invoice verification and compliance overview</p>
          <span aria-live="polite" className="inline-flex items-center gap-1 text-xs text-gray-400">
            <RefreshCw size={13} className={isRefreshing ? 'animate-spin' : ''} />
            {lastUpdated ? `Updated ${lastUpdated.toLocaleTimeString()}` : 'Updating'}
          </span>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl flex items-start gap-3">
          <AlertTriangle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <h3 className="font-semibold text-red-900">Failed to load dashboard</h3>
            <p className="text-sm text-red-700 mt-1">{error}</p>
          </div>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <StatsCard
          title="Total Invoices"
          value={stats.total}
          icon={<Database size={24} className="text-blue-600" />}
          color="bg-blue-50"
          isLoading={isLoading}
        />
        <StatsCard
          title="Verified"
          value={stats.verified}
          icon={<CheckCircle size={24} className="text-green-600" />}
          color="bg-green-50"
          isLoading={isLoading}
        />
        <StatsCard
          title="Flagged"
          value={stats.flagged}
          icon={<AlertCircle size={24} className="text-red-600" />}
          color="bg-red-50"
          isLoading={isLoading}
        />
        <StatsCard
          title="Pending Review"
          value={stats.pending}
          icon={<TrendingUp size={24} className="text-yellow-600" />}
          color="bg-yellow-50"
          isLoading={isLoading}
        />
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Verifications */}
        <div className="lg:col-span-2 bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 className="text-lg font-bold text-gray-900 mb-4">Recent Verifications</h2>
          {isLoading ? (
            <div className="space-y-3">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                  <div className="flex-1 space-y-2">
                    <div className="h-4 w-32 bg-gray-200 rounded animate-pulse" />
                    <div className="h-3 w-24 bg-gray-200 rounded animate-pulse" />
                  </div>
                  <div className="text-right space-y-2">
                    <div className="h-4 w-16 bg-gray-200 rounded animate-pulse" />
                    <div className="h-3 w-12 bg-gray-200 rounded animate-pulse ml-auto" />
                  </div>
                </div>
              ))}
            </div>
          ) : recentItems.length === 0 ? (
            <div className="text-center py-12">
              <Database size={40} className="mx-auto text-gray-300 mb-3" />
              <p className="text-gray-500">No verifications yet</p>
            </div>
          ) : (
            <div className="space-y-3">
              {recentItems.map((item, idx) => {
                const score = item.verificationScore ?? 0;
                const riskLevel = getRiskLevel(score);
                const statusBadge = getStatusBadge(item.status);
                const vendorName = item.extractedData?.vendorName || 'Unknown Vendor';
                return (
                  <div
                    key={item.id}
                    className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-all duration-200 hover:translate-x-1"
                    style={{ animationDelay: `${idx * 80}ms` }}
                  >
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-gray-900 truncate">{item.fileName}</p>
                      <p className="text-sm text-gray-500 truncate">{vendorName}</p>
                    </div>
                    <div className="text-right ml-4 flex-shrink-0">
                      <div className="flex items-center gap-2">
                        <div className="h-2 w-16 rounded-full bg-gray-200 overflow-hidden">
                          <div
                            className={`h-full rounded-full ${
                              score >= 90 ? 'bg-green-500' : score >= 70 ? 'bg-blue-500' : 'bg-red-500'
                            }`}
                            style={{ width: `${score}%` }}
                          />
                        </div>
                        <p className="font-bold text-gray-900 text-sm">{score}</p>
                      </div>
                      <p className={`text-xs font-semibold mt-1 ${riskLevel.color}`}>
                        {statusBadge.label}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Quick Start */}
        <div className="bg-gradient-to-br from-blue-600 to-blue-700 rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-bold text-white mb-1">Quick Start</h2>
          <p className="text-sm text-blue-100 mb-5">Get started with common tasks</p>
          <div className="space-y-3">
            <Link
              to="/upload"
              className="flex items-center gap-4 p-4 bg-white/10 backdrop-blur-sm rounded-xl hover:bg-white/20 transition-all duration-200 group"
            >
              <div className="p-2.5 bg-white/20 rounded-lg group-hover:scale-110 transition-transform">
                <Upload size={20} className="text-white" />
              </div>
              <div>
                <p className="font-semibold text-white">Upload Invoice</p>
                <p className="text-xs text-blue-100">Verify a new document</p>
              </div>
            </Link>
            <Link
              to="/vendors"
              className="flex items-center gap-4 p-4 bg-white/10 backdrop-blur-sm rounded-xl hover:bg-white/20 transition-all duration-200 group"
            >
              <div className="p-2.5 bg-white/20 rounded-lg group-hover:scale-110 transition-transform">
                <Building2 size={20} className="text-white" />
              </div>
              <div>
                <p className="font-semibold text-white">View Vendors</p>
                <p className="text-xs text-blue-100">Check vendor database</p>
              </div>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
