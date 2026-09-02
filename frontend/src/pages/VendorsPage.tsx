import { useState, useEffect } from 'react';
import type { Vendor } from '../types';
import { Database, Search, MapPin, CheckCircle } from 'lucide-react';
import { useMemo } from 'react';
import api, { getApiErrorMessage } from '../services/api';

export default function VendorsPage() {
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    (async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await api.get('/vendors');
        if (!cancelled) setVendors(res.data);
      } catch (error: unknown) {
        if (!cancelled) setError(getApiErrorMessage(error, 'Failed to load vendors'));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  const filteredVendors = useMemo(
    () =>
      vendors.filter(
        (vendor) =>
          (vendor.legalName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
          vendor.gstin.toLowerCase().includes(searchTerm.toLowerCase())
      ),
    [vendors, searchTerm]
  );


  return (
    <div>
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Vendor Database</h1>
        <p className="text-gray-600">Verified vendors and their GSTIN records</p>
      </div>

      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        {/* Search */}
        <div className="mb-6">
          <div className="relative">
            <Search size={20} className="absolute left-3 top-3 text-gray-400" />
            <input
              type="text"
              placeholder="Search by vendor name or GSTIN..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        {/* Error State */}
        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}

        {/* Vendors List */}
        {loading ? (
          <div className="text-center py-12">
            <div className="animate-spin h-8 w-8 border-4 border-blue-600 border-t-transparent rounded-full mx-auto mb-4" />
            <p className="text-gray-600">Loading vendors...</p>
          </div>
        ) : filteredVendors.length === 0 ? (
          <div className="text-center py-12">
            <Database size={48} className="mx-auto text-gray-400 mb-4" />
            <p className="text-gray-600">No vendors found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-4 font-semibold text-gray-900">Vendor Name</th>
                  <th className="text-left py-3 px-4 font-semibold text-gray-900">GSTIN</th>
                  <th className="text-left py-3 px-4 font-semibold text-gray-900">State</th>
                  <th className="text-left py-3 px-4 font-semibold text-gray-900">Status</th>
                  <th className="text-left py-3 px-4 font-semibold text-gray-900">Last Verified</th>
                </tr>
              </thead>
              <tbody>
                {filteredVendors.map((vendor) => (
                  <tr key={vendor.gstin} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                    <td className="py-4 px-4">
                      <div>
                        <p className="font-medium text-gray-900">{vendor.legalName || 'Name unavailable'}</p>
                        <p className="text-sm text-gray-600">{vendor.tradeName || '-'}</p>
                      </div>
                    </td>
                    <td className="py-4 px-4 font-mono text-sm text-gray-900">{vendor.gstin}</td>
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-1 text-gray-600">
                        <MapPin size={16} />
                        {vendor.state || 'Unknown'}
                      </div>
                    </td>
                    <td className="py-4 px-4">
                      <span className="inline-flex items-center gap-1 px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm font-medium">
                        <CheckCircle size={14} />
                        {vendor.status}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-sm text-gray-600">
                      {vendor.verifiedAt ? new Date(vendor.verifiedAt).toLocaleDateString('en-IN') : '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
