import React from 'react';
import ThreeWayMatching from '../components/ThreeWayMatching';

export default function MatchingPage() {
  return (
    <div>
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">3-Way Matching</h1>
        <p className="text-gray-600">Verify alignment between Purchase Order, Goods Receipt, and Invoice</p>
      </div>
      <ThreeWayMatching />
    </div>
  );
}
