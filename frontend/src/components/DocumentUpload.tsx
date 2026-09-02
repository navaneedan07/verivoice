import { useState, useRef, type DragEvent, type ChangeEvent } from 'react';
import { documentService, getApiErrorMessage } from '../services/api';
import type { DocumentDto } from '../types';
import { Upload, Loader, CheckCircle, AlertCircle, FileText } from 'lucide-react';

interface DocumentUploadProps {
  onSuccess?: (document: DocumentDto) => void;
}

export default function DocumentUpload({ onSuccess }: DocumentUploadProps) {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDrag = (e: DragEvent) => {
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
      validateAndUpload(files[0]);
    }
  };

  const handleFileSelect = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      validateAndUpload(e.target.files[0]);
    }
  };

  const validateAndUpload = (file: File) => {
    setError(null);
    setSuccess(null);
    setSelectedFile(file);

    // Validate file type
    const allowedTypes = [
      'application/pdf',
      'image/jpeg',
      'image/png',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'text/plain',
    ];
    if (!allowedTypes.includes(file.type)) {
      setError('Invalid file type. Please upload PDF, DOCX, TXT, PNG, or JPEG.');
      setSelectedFile(null);
      return;
    }

    // Validate file size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      setError('File size too large. Maximum 10MB allowed.');
      setSelectedFile(null);
      return;
    }

    handleUpload(file);
  };

  const handleUpload = async (file: File) => {
    setIsLoading(true);
    try {
      const result = await documentService.uploadDocument(file);
      setSuccess('Document uploaded and verified successfully!');
      window.dispatchEvent(new Event('verivoice:document-processed'));
      if (onSuccess) {
        onSuccess(result);
      }
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Failed to upload document. Please try again.'));
      setSelectedFile(null);
    } finally {
      setIsLoading(false);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 transition-all">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900 mb-2">Upload Invoice</h2>
        <p className="text-gray-500">Upload a document for verification and compliance checks</p>
      </div>

      {/* Error Message */}
      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-xl flex items-start gap-3">
          <AlertCircle size={20} className="text-red-600 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="font-semibold text-red-900">Upload Failed</h3>
            <p className="text-sm text-red-700 mt-1">{error}</p>
          </div>
        </div>
      )}

      {/* Success Message */}
      {success && (
        <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-xl flex items-start gap-3">
          <CheckCircle size={20} className="text-green-600 flex-shrink-0 mt-0.5" />
          <div>
            <h3 className="font-semibold text-green-900">Success!</h3>
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
        onClick={() => !isLoading && !selectedFile && fileInputRef.current?.click()}
        className={`relative border-2 border-dashed rounded-xl p-12 text-center transition-all duration-300 ${
          dragActive
            ? 'border-blue-400 bg-blue-50 scale-[1.01]'
            : 'border-gray-300 hover:border-blue-400 bg-gray-50 hover:bg-gray-100'
        } ${isLoading ? 'pointer-events-none opacity-70' : 'cursor-pointer'}`}
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
          <div
            className={`p-4 rounded-xl transition-all duration-300 ${
              dragActive
                ? 'bg-blue-100 scale-110'
                : isLoading
                ? 'bg-blue-100'
                : selectedFile
                ? 'bg-green-100'
                : 'bg-gray-200'
            }`}
          >
            {isLoading ? (
              <div className="relative">
                <Loader size={36} className="text-blue-600 animate-spin" />
              </div>
            ) : selectedFile ? (
              <FileText size={36} className="text-green-600" />
            ) : (
              <Upload size={36} className={dragActive ? 'text-blue-600' : 'text-gray-500'} />
            )}
          </div>

          {isLoading ? (
            <div className="text-center">
              <p className="text-lg font-semibold text-blue-600">Processing Document</p>
              <p className="text-sm text-gray-500 mt-1">Extracting and verifying data...</p>
              <div className="mt-4 w-48 h-2 bg-gray-200 rounded-full overflow-hidden mx-auto">
                <div className="h-full bg-blue-600 rounded-full animate-pulse" style={{ width: '60%' }} />
              </div>
            </div>
          ) : selectedFile ? (
            <div className="text-center">
              <div className="flex items-center gap-2 justify-center">
                <FileText size={18} className="text-blue-600" />
                <p className="text-lg font-semibold text-gray-900">{selectedFile.name}</p>
              </div>
              <p className="text-sm text-gray-500 mt-1">{formatFileSize(selectedFile.size)}</p>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  fileInputRef.current?.click();
                }}
                className="mt-2 text-sm text-blue-600 hover:text-blue-700 font-medium hover:underline"
              >
                Change file
              </button>
            </div>
          ) : (
            <div>
              <p className={`text-lg font-semibold ${dragActive ? 'text-blue-600' : 'text-gray-900'}`}>
                {dragActive ? 'Drop your file here' : 'Drag & drop your document'}
              </p>
              <p className="text-sm text-gray-500 mt-1">or click to browse</p>
            </div>
          )}

          {!isLoading && !selectedFile && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                fileInputRef.current?.click();
              }}
              className="mt-2 px-6 py-2.5 bg-blue-600 text-white rounded-xl font-semibold hover:bg-blue-700 transition-all duration-200 hover:shadow-lg active:scale-95"
            >
              Select File
            </button>
          )}
        </div>

        <p className="text-xs text-gray-400 mt-6">
          Supported formats: PDF, DOCX, TXT, PNG, JPEG (Max 10MB)
        </p>
      </div>
    </div>
  );
}
