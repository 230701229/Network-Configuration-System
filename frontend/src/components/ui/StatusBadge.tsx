import React from 'react';
import { CheckCircle, AlertTriangle, XCircle, AlertCircle, HelpCircle, Activity, Power } from 'lucide-react';
import { clsx } from 'clsx';

interface StatusBadgeProps {
  status: string;
  type?: 'status' | 'compliance' | 'severity' | 'alert';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, type = 'status' }) => {
  let colorClass = 'bg-gray-100 text-gray-800';
  let Icon = HelpCircle;

  switch (status) {
    // Device Status
    case 'ONLINE':
      colorClass = 'bg-green-100 text-green-800';
      Icon = Activity;
      break;
    case 'OFFLINE':
      colorClass = 'bg-red-100 text-red-800';
      Icon = Power;
      break;
    case 'UNREACHABLE':
      colorClass = 'bg-orange-100 text-orange-800';
      Icon = AlertCircle;
      break;
      
    // Compliance Status
    case 'COMPLIANT':
      colorClass = 'bg-green-100 text-green-800';
      Icon = CheckCircle;
      break;
    case 'MINOR_DRIFT':
      colorClass = 'bg-yellow-100 text-yellow-800';
      Icon = AlertTriangle;
      break;
    case 'MAJOR_DRIFT':
      colorClass = 'bg-orange-100 text-orange-800';
      Icon = AlertTriangle;
      break;
    case 'CRITICAL_NON_COMPLIANT':
      colorClass = 'bg-red-100 text-red-800';
      Icon = XCircle;
      break;

    // Severity
    case 'CRITICAL':
      colorClass = 'bg-red-100 text-red-800';
      Icon = XCircle;
      break;
    case 'HIGH':
      colorClass = 'bg-orange-100 text-orange-800';
      Icon = AlertTriangle;
      break;
    case 'MEDIUM':
      colorClass = 'bg-yellow-100 text-yellow-800';
      Icon = AlertCircle;
      break;
    case 'LOW':
    case 'INFO':
      colorClass = 'bg-blue-100 text-blue-800';
      Icon = HelpCircle;
      break;
      
    // Drift/Alert Status
    case 'OPEN':
      colorClass = 'bg-red-100 text-red-800';
      Icon = AlertCircle;
      break;
    case 'ACKNOWLEDGED':
      colorClass = 'bg-yellow-100 text-yellow-800';
      Icon = AlertTriangle;
      break;
    case 'RESOLVED':
      colorClass = 'bg-green-100 text-green-800';
      Icon = CheckCircle;
      break;
  }

  return (
    <span className={clsx('inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium', colorClass)}>
      <Icon className="w-3.5 h-3.5" />
      {status.replace(/_/g, ' ')}
    </span>
  );
};
