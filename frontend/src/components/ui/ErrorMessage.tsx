import React from 'react';
import { AlertCircle, RefreshCw } from 'lucide-react';

interface ErrorMessageProps {
  message: string;
  onRetry?: () => void;
}

export const ErrorMessage: React.FC<ErrorMessageProps> = ({ message, onRetry }) => (
  <div className="bg-red-500/10 border border-red-500/20 rounded-lg p-4 flex flex-col items-center justify-center text-center space-y-3">
    <AlertCircle className="w-8 h-8 text-red-500" />
    <p className="text-red-400 font-medium">{message}</p>
    {onRetry && (
      <button onClick={onRetry} className="btn-secondary mt-2">
        <RefreshCw className="w-4 h-4" /> Retry
      </button>
    )}
  </div>
);
