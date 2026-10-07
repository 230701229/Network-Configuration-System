import React from 'react';
import { LoadingSpinner } from './LoadingSpinner';

export interface ColumnDef<T> {
  header: string;
  accessorKey?: keyof T;
  cell?: (item: T) => React.ReactNode;
}

interface DataTableProps<T> {
  data: T[];
  columns: ColumnDef<T>[];
  loading?: boolean;
  emptyMessage?: string;
  onRowClick?: (item: T) => void;
}

export function DataTable<T>({ data, columns, loading, emptyMessage = 'No data available', onRowClick }: DataTableProps<T>) {
  if (loading) return <LoadingSpinner />;
  
  if (!data || data.length === 0) {
    return (
      <div className="p-8 text-center text-gray-500 border border-gray-800 rounded-lg bg-gray-900/50">
        {emptyMessage}
      </div>
    );
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-800">
      <table className="w-full text-sm text-left">
        <thead className="text-xs text-gray-400 bg-gray-800/50 uppercase border-b border-gray-800">
          <tr>
            {columns.map((col, i) => (
              <th key={i} className="px-6 py-3 font-medium">{col.header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((item, i) => (
            <tr 
              key={i} 
              onClick={() => onRowClick?.(item)}
              className={`bg-gray-900 border-b border-gray-800 hover:bg-gray-800/50 transition-colors ${onRowClick ? 'cursor-pointer' : ''}`}
            >
              {columns.map((col, j) => (
                <td key={j} className="px-6 py-4">
                  {col.cell ? col.cell(item) : String(item[col.accessorKey as keyof T] || '')}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
