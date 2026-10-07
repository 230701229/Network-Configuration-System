import React from 'react';
import { clsx } from 'clsx';

interface ComplianceScoreProps {
  score: number;
  size?: 'sm' | 'md' | 'lg';
}

export const ComplianceScore: React.FC<ComplianceScoreProps> = ({ score, size = 'md' }) => {
  let colorClass = 'text-green-500';
  let bgClass = 'bg-green-500';
  if (score < 50) {
    colorClass = 'text-red-500';
    bgClass = 'bg-red-500';
  } else if (score < 80) {
    colorClass = 'text-yellow-500';
    bgClass = 'bg-yellow-500';
  }

  const sizeClasses = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-12 h-12 text-sm',
    lg: 'w-24 h-24 text-2xl'
  };

  const strokeWidth = size === 'lg' ? 4 : 2;
  const radius = size === 'lg' ? 44 : (size === 'md' ? 22 : 14);
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (score / 100) * circumference;

  return (
    <div className={clsx('relative inline-flex items-center justify-center', sizeClasses[size])}>
      <svg className="transform -rotate-90 w-full h-full">
        <circle
          cx="50%"
          cy="50%"
          r={radius}
          stroke="currentColor"
          strokeWidth={strokeWidth}
          fill="transparent"
          className="text-gray-700"
        />
        <circle
          cx="50%"
          cy="50%"
          r={radius}
          stroke="currentColor"
          strokeWidth={strokeWidth}
          fill="transparent"
          strokeDasharray={circumference}
          strokeDashoffset={strokeDashoffset}
          className={clsx('transition-all duration-1000 ease-in-out', colorClass)}
        />
      </svg>
      <span className={clsx('absolute font-bold text-gray-100')}>{score}%</span>
    </div>
  );
};
