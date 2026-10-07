import React, { useEffect, useState } from 'react';
import { complianceApi } from '../services/complianceApi';
import { ComplianceScan } from '../types';
import { DataTable } from '../components/ui/DataTable';
import { StatusBadge } from '../components/ui/StatusBadge';
import { ComplianceScore } from '../components/ui/ComplianceScore';

export default function CompliancePage() {
  const [scans, setScans] = useState<ComplianceScan[]>([]);
  
  useEffect(() => {
    complianceApi.getComplianceResults().then(setScans);
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-white">Compliance Scans</h1>
      <div className="card">
        <DataTable
          data={scans}
          columns={[
            { header: 'Device', accessorKey: 'deviceHostname' },
            { header: 'Policy', accessorKey: 'policyName' },
            { header: 'Score', cell: (s) => <ComplianceScore score={s.complianceScore} size="sm" /> },
            { header: 'Status', cell: (s) => <StatusBadge status={s.complianceStatus} /> },
            { header: 'Date', cell: (s) => new Date(s.scannedAt).toLocaleString() }
          ]}
        />
      </div>
    </div>
  );
}
