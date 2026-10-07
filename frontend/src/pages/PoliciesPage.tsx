import React, { useEffect, useState } from 'react';
import { policyApi } from '../services/policyApi';
import { Policy } from '../types';
import { DataTable } from '../components/ui/DataTable';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { Plus, Shield } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function PoliciesPage() {
  const [policies, setPolicies] = useState<Policy[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const loadPolicies = async () => {
    try {
      const data = await policyApi.getPolicies();
      setPolicies(data);
    } catch (err) {
      setError('Failed to load policies');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadPolicies(); }, []);

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} onRetry={loadPolicies} />;

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-white">Compliance Policies</h1>
          <p className="text-gray-400">Manage configuration rules and baselines</p>
        </div>
        <button className="btn-primary"><Plus className="w-4 h-4" /> Create Policy</button>
      </div>

      <div className="card">
        <DataTable
          data={policies}
          columns={[
            { header: 'Policy Name', accessorKey: 'policyName' },
            { header: 'Device Type', accessorKey: 'deviceType' },
            { header: 'Vendor', cell: (p) => p.vendor || 'All' },
            { header: 'Version', accessorKey: 'version' },
            { header: 'Rules', cell: (p) => p.rules?.length || 0 },
            { header: 'Status', cell: (p) => p.isActive ? <span className="text-green-500">Active</span> : <span className="text-gray-500">Inactive</span> }
          ]}
          onRowClick={(p) => navigate(`/policies/${p.id}`)}
        />
      </div>
    </div>
  );
}
