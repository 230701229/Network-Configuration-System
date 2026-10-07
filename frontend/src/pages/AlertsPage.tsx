import React, { useEffect, useState } from 'react';
import { alertApi } from '../services/alertApi';
import { Alert } from '../types';
import { DataTable } from '../components/ui/DataTable';
import { StatusBadge } from '../components/ui/StatusBadge';

export default function AlertsPage() {
  const [alerts, setAlerts] = useState<Alert[]>([]);

  useEffect(() => {
    alertApi.getAlerts().then(setAlerts);
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-white">System Alerts</h1>
      <div className="card">
        <DataTable
          data={alerts}
          columns={[
            { header: 'Title', accessorKey: 'title' },
            { header: 'Device', accessorKey: 'deviceHostname' },
            { header: 'Severity', cell: (a) => <StatusBadge status={a.severity} /> },
            { header: 'Status', cell: (a) => <StatusBadge status={a.status} /> },
            { header: 'Date', cell: (a) => new Date(a.detectedAt).toLocaleString() }
          ]}
        />
      </div>
    </div>
  );
}
