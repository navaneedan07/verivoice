import { useState } from 'react';
import DocumentUpload from '../components/DocumentUpload';
import VerificationResult from '../components/VerificationResult';
import type { DocumentDto } from '../types';

export default function UploadPage() {
  const [document, setDocument] = useState<DocumentDto | null>(null);

  return (
    <div className="space-y-6">
      {!document ? (
        <DocumentUpload onSuccess={setDocument} />
      ) : (
        <div>
          <button
            onClick={() => setDocument(null)}
            className="mb-6 px-4 py-2 text-blue-600 hover:text-blue-700 font-medium"
          >
            ← Back to Upload
          </button>
          <VerificationResult document={document} />
        </div>
      )}
    </div>
  );
}
