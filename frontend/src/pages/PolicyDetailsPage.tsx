import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { policyApi } from '../services/policyApi';
import { Policy } from '../types';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { DataTable } from '../components/ui/DataTable';
import { StatusBadge } from '../components/ui/StatusBadge';

export default function PolicyDetailsPage() {
  const { id } = useParams();
  const [policy, setPolicy] = useState<Policy | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    policyApi.getPolicy(Number(id))
      .then(setPolicy)
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <LoadingSpinner />;
  if (!policy) return <ErrorMessage message="Policy not found" />;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-white">{policy.policyName}</h1>
      <div className="card">
        <h2 className="text-lg font-semibold mb-4">Rules</h2>
        <DataTable
          data={policy.rules}
          columns={[
            { header: 'Parameter', accessorKey: 'parameter' },
            { header: 'Operator', accessorKey: 'operator' },
            { header: 'Expected Value', accessorKey: 'expectedValue' },
            { header: 'Severity', cell: (r) => <StatusBadge status={r.severity} /> },
            { header: 'Remediation', accessorKey: 'remediation' }
          ]}
        />
      </div>
    </div>
  );
}
