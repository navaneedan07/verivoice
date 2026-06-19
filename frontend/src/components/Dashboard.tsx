import React from 'react';
import { AlertCircle, CheckCircle, TrendingUp, Database } from 'lucide-react';

interface StatsCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
}

function StatsCard({ title, value, icon, color }: StatsCardProps) {
  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-gray-600 mb-2">{title}</p>
          <p className="text-3xl font-bold text-gray-900">{value}</p>
        </div>
        <div className={`p-3 rounded-lg ${color}`}>
          {icon}
        </div>
      </div>
    </div>
  );
}

export default function Dashboard() {
  return (
    <div>
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Dashboard</h1>
        <p className="text-gray-600">Invoice verification and fraud detection overview</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <StatsCard
          title="Total Invoices"
          value="152"
          icon={<Database size={24} className="text-blue-600" />}
          color="bg-blue-50"
        />
        <StatsCard
          title="Verified"
          value="148"
          icon={<CheckCircle size={24} className="text-green-600" />}
          color="bg-green-50"
        />
        <StatsCard
          title="Flagged"
          value="4"
          icon={<AlertCircle size={24} className="text-red-600" />}
          color="bg-red-50"
        />
        <StatsCard
          title="Pending Review"
          value="2"
          icon={<TrendingUp size={24} className="text-yellow-600" />}
          color="bg-yellow-50"
        />
      </div>

      {/* Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <h2 className="text-lg font-bold text-gray-900 mb-4">Recent Verifications</h2>
          <div className="space-y-4">
            {[
              { id: 1, name: 'INV-2024-001', vendor: 'Tech Solutions Ltd', score: 95, status: 'Verified' },
              { id: 2, name: 'INV-2024-002', vendor: 'Global Enterprises', score: 78, status: 'Low Risk' },
              { id: 3, name: 'INV-2024-003', vendor: 'Premium Services', score: 92, status: 'Verified' },
              { id: 4, name: 'INV-2024-004', vendor: 'Unknown Vendor', score: 35, status: 'High Risk' },
            ].map((item) => (
              <div key={item.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                <div className="flex-1">
                  <p className="font-medium text-gray-900">{item.name}</p>
                  <p className="text-sm text-gray-600">{item.vendor}</p>
                </div>
                <div className="text-right">
                  <p className="font-bold text-gray-900">{item.score}/100</p>
                  <p className={`text-xs font-medium ${
                    item.status === 'Verified' ? 'text-green-600' :
                    item.status === 'Low Risk' ? 'text-blue-600' :
                    'text-red-600'
                  }`}>
                    {item.status}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Quick Start */}
        <div className="bg-gradient-to-br from-blue-50 to-blue-100 rounded-lg shadow-sm border border-blue-200 p-6">
          <h2 className="text-lg font-bold text-blue-900 mb-4">Quick Start</h2>
          <div className="space-y-3">
            <a href="/upload" className="block p-4 bg-white rounded-lg hover:shadow-md transition-shadow cursor-pointer">
              <p className="font-medium text-gray-900">📄 Upload Invoice</p>
              <p className="text-sm text-gray-600 mt-1">Verify a new document</p>
            </a>
            <a href="/vendors" className="block p-4 bg-white rounded-lg hover:shadow-md transition-shadow cursor-pointer">
              <p className="font-medium text-gray-900">🏢 View Vendors</p>
              <p className="text-sm text-gray-600 mt-1">Check vendor database</p>
            </a>
            <a href="/matching" className="block p-4 bg-white rounded-lg hover:shadow-md transition-shadow cursor-pointer">
              <p className="font-medium text-gray-900">🔗 3-Way Matching</p>
              <p className="text-sm text-gray-600 mt-1">Validate documents</p>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
