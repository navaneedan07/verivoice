export const getRiskLevel = (score: number): { label: string; color: string; bgColor: string } => {
  if (score >= 90) return { label: 'VERIFIED', color: 'text-green-700', bgColor: 'bg-green-50' };
  if (score >= 70) return { label: 'LOW RISK', color: 'text-blue-700', bgColor: 'bg-blue-50' };
  if (score >= 40) return { label: 'REVIEW REQUIRED', color: 'text-yellow-700', bgColor: 'bg-yellow-50' };
  return { label: 'HIGH RISK', color: 'text-red-700', bgColor: 'bg-red-50' };
};
export const formatCurrency = (amount: number | undefined): string => {
  if (!amount) return '₹0.00';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
  }).format(amount);
};
export const formatDate = (dateString: string | undefined): string => {
  if (!dateString) return '-';
  return new Date(dateString).toLocaleDateString('en-IN');
};

export const getStatusBadge = (status: string): { label: string; color: string } => {
  const statuses: Record<string, { label: string; color: string }> = {
    APPROVED: { label: '✓ Approved', color: 'bg-green-100 text-green-800' },
    NEEDS_REVIEW: { label: '⚠ Review Needed', color: 'bg-yellow-100 text-yellow-800' },
    FLAGGED: { label: '✕ Flagged', color: 'bg-red-100 text-red-800' },
    PROCESSING: { label: '⟳ Processing', color: 'bg-blue-100 text-blue-800' },
  };
  return statuses[status] || { label: status, color: 'bg-gray-100 text-gray-800' };
};
