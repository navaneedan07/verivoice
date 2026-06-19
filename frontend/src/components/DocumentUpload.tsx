import React, { useState, useRef } from 'react';
import { documentService } from '../services/api';
import { DocumentDto } from '../types';
import { Upload, Loader, CheckCircle, AlertCircle } from 'lucide-react';

interface DocumentUploadProps {
  onSuccess?: (document: DocumentDto) => void;
}

export default function DocumentUpload({ onSuccess }: DocumentUploadProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      handleFile(files[0]);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      handleFile(e.target.files[0]);
    }
  };

  const handleFile = async (file: File) => {
    setError(null);
    setSuccess(null);

    // Validate file type
    const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    if (!allowedTypes.includes(file.type)) {
      setError('Invalid file type. Please upload PDF, DOCX, PNG, or JPEG.');
      return;
    }

    // Validate file size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      setError('File size too large. Maximum 10MB allowed.');
      return;
    }

    setIsLoading(true);
    try {
      const result = await documentService.uploadDocument(file);
      setSuccess('Document uploaded and verified successfully!');
      if (onSuccess) {
        onSuccess(result);
      }
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to upload document. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-8">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900 mb-2">Upload Invoice</h2>
        <p className="text-gray-600">Upload a document for verification and fraud detection</p>
      </div>

      {/* Error Message */}
      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
          <AlertCircle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="font-medium text-red-900">Upload Failed</h3>
            <p className="text-sm text-red-700 mt-1">{error}</p>
          </div>
        </div>
      )}

      {/* Success Message */}
      {success && (
        <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start gap-3">
          <CheckCircle size={20} className="text-green-600 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="font-medium text-green-900">Success!</h3>
            <p className="text-sm text-green-700 mt-1">{success}</p>
          </div>
        </div>
      )}

      {/* Drag and Drop Area */}
      <div
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        className={`relative border-2 border-dashed rounded-lg p-12 text-center transition-colors ${
          dragActive
            ? 'border-blue-400 bg-blue-50'
            : 'border-gray-300 hover:border-gray-400 bg-gray-50 hover:bg-gray-100'
        }`}
      >
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileSelect}
          accept=".pdf,.docx,.png,.jpg,.jpeg"
          className="hidden"
          disabled={isLoading}
        />

        <div className="flex flex-col items-center gap-4">
          <div className={`p-4 rounded-lg ${dragActive ? 'bg-blue-100' : 'bg-gray-200'}`}>
            {isLoading ? (
              <Loader size={32} className="text-gray-600 animate-spin" />
            ) : (
              <Upload size={32} className={dragActive ? 'text-blue-600' : 'text-gray-600'} />
            )}
          </div>

          <div>
            <p className={`text-lg font-semibold ${dragActive ? 'text-blue-600' : 'text-gray-900'}`}>
              {isLoading ? 'Processing...' : 'Drag and drop your document'}
            </p>
            <p className="text-sm text-gray-600 mt-1">or click below to browse</p>
          </div>

          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={isLoading}
            className="mt-4 px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? 'Processing...' : 'Select File'}
          </button>
        </div>

        <p className="text-xs text-gray-500 mt-6">
          Supported formats: PDF, DOCX, PNG, JPEG (Max 10MB)
        </p>
      </div>
    </div>
  );
}
