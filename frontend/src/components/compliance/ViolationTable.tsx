import React from 'react';
import { ComplianceResult } from '../../types';
import { DataTable } from '../ui/DataTable';
import { StatusBadge } from '../ui/StatusBadge';

interface ViolationTableProps {
  violations: ComplianceResult[];
}

export const ViolationTable: React.FC<ViolationTableProps> = ({ violations }) => {
  return (
    <DataTable
      data={violations}
      emptyMessage="No violations found"
      columns={[
        { header: 'Parameter', accessorKey: 'parameter' },
        { header: 'Expected', accessorKey: 'expectedValue' },
        { header: 'Actual', accessorKey: 'actualValue' },
        { header: 'Severity', cell: (r) => <StatusBadge status={r.severity} /> },
        { header: 'Description', accessorKey: 'description' },
        { header: 'Remediation', accessorKey: 'remediation' }
      ]}
    />
  );
};
