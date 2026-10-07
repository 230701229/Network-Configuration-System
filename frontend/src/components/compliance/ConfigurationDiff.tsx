import React from 'react';
import { ComplianceResult } from '../../types';
import { Check, X, ArrowRight } from 'lucide-react';
import { clsx } from 'clsx';

interface ConfigurationDiffProps {
  results: ComplianceResult[];
}

export const ConfigurationDiff: React.FC<ConfigurationDiffProps> = ({ results }) => {
  return (
    <div className="overflow-x-auto rounded-lg border border-gray-800">
      <table className="w-full text-sm text-left">
        <thead className="text-xs text-gray-400 bg-gray-800/50 uppercase border-b border-gray-800">
          <tr>
            <th className="px-6 py-3 font-medium">Parameter</th>
            <th className="px-6 py-3 font-medium">Expected Value</th>
            <th className="px-6 py-3 font-medium">Actual Value</th>
            <th className="px-6 py-3 font-medium">Configuration Transition</th>
            <th className="px-6 py-3 font-medium">Status</th>
          </tr>
        </thead>
        <tbody className="font-mono text-xs">
          {results.map((res) => (
            <tr key={res.id} className={clsx('border-b border-gray-800', !res.passed && 'bg-red-950/20')}>
              <td className="px-6 py-4 font-semibold text-white">{res.parameter}</td>
              <td className="px-6 py-4 text-emerald-400">{res.expectedValue}</td>
              <td className={clsx('px-6 py-4', res.passed ? 'text-gray-300' : 'text-red-400 font-semibold')}>
                {res.actualValue || 'N/A'}
              </td>
              <td className="px-6 py-4">
                {res.passed ? (
                  <span className="text-gray-500 text-[11px]">Synchronized</span>
                ) : (
                  <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded bg-red-950/60 border border-red-500/30 text-red-300 text-xs font-semibold">
                    <span>{res.expectedValue}</span>
                    <ArrowRight className="w-3 h-3 text-red-400" />
                    <span className="text-red-400 underline">{res.actualValue || '<absent>'}</span>
                  </span>
                )}
              </td>
              <td className="px-6 py-4">
                {res.passed ? (
                  <span className="flex items-center text-emerald-400 gap-1 font-semibold"><Check className="w-4 h-4"/> PASS</span>
                ) : (
                  <span className="flex items-center text-red-400 gap-1 font-bold"><X className="w-4 h-4"/> DRIFT / FAIL</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
