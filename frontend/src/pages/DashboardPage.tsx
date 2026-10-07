import React, { useEffect, useState } from 'react';
import { dashboardApi } from '../services/dashboardApi';
import { DashboardSummary, DashboardTrends } from '../types';
import { StatCard } from '../components/ui/StatCard';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { Server, CheckCircle, AlertTriangle, AlertOctagon, Activity, Play } from 'lucide-react';
import { complianceApi } from '../services/complianceApi';
import { DataTable } from '../components/ui/DataTable';
import { ComplianceScore } from '../components/ui/ComplianceScore';
import { StatusBadge } from '../components/ui/StatusBadge';
import { Link, useNavigate } from 'react-router-dom';

import { subscribeToRealtimeEvents, StreamEvent } from '../services/sseService';

export default function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [trends, setTrends] = useState<DashboardTrends | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [scanning, setScanning] = useState(false);
  const [lastLiveEvent, setLastLiveEvent] = useState<string | null>(null);
  const navigate = useNavigate();

  const loadData = async () => {
    try {
      const [sum, trnd] = await Promise.all([dashboardApi.getSummary(), dashboardApi.getTrends()]);
      setSummary(sum);
      setTrends(trnd);
      setError('');
    } catch (err) {
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();

    // Subscribe to near real-time SSE stream
    const unsubscribe = subscribeToRealtimeEvents((event: StreamEvent) => {
      setLastLiveEvent(event.message || event.eventType);
      loadData();
      setTimeout(() => setLastLiveEvent(null), 8000);
    });

    // Fallback polling interval every 10 seconds
    const interval = setInterval(loadData, 10000);

    return () => {
      unsubscribe();
      clearInterval(interval);
    };
  }, []);

  const handleScanAll = async () => {
    setScanning(true);
    try {
      await complianceApi.scanAllDevices();
      await loadData();
    } catch (err) {
      alert('Scan failed');
    } finally {
      setScanning(false);
    }
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} onRetry={loadData} />;
  if (!summary || !trends) return null;

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between md:items-center gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-white">Dashboard</h1>
            <div className="flex items-center gap-2 bg-emerald-950/50 border border-emerald-500/30 px-3 py-1 rounded-full text-xs text-emerald-400 font-medium">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              Live Monitoring Active (5s)
            </div>
          </div>
          <p className="text-gray-400 text-sm mt-0.5">Real-time network compliance and configuration drift overview</p>
        </div>
        <button onClick={handleScanAll} disabled={scanning} className="btn-primary">
          <Play className="w-4 h-4" /> {scanning ? 'Scanning...' : 'Scan All Devices'}
        </button>
      </div>

      {lastLiveEvent && (
        <div className="bg-amber-950/40 border border-amber-500/30 text-amber-300 px-4 py-2.5 rounded-xl text-sm flex items-center justify-between animate-fadeIn">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
            <span><strong>Live Event:</strong> {lastLiveEvent}</span>
          </div>
          <span className="text-xs text-amber-400/80 font-mono">Just now</span>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <StatCard title="Total Devices" value={summary.totalDevices} icon={Server} color="blue" />
        <StatCard title="Compliant" value={summary.compliantDevices} icon={CheckCircle} color="green" />
        <StatCard title="Drift Detected" value={summary.devicesWithDrift} icon={AlertTriangle} color="yellow" />
        <StatCard title="Critical Violations" value={summary.criticalViolations} icon={AlertOctagon} color="red" />
        <StatCard title="Avg Score" value={`${Math.round(summary.averageComplianceScore)}%`} icon={Activity} color="purple" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <h2 className="text-lg font-semibold mb-4 text-white">Device Compliance Overview</h2>
          <DataTable
            data={trends.deviceComplianceList}
            columns={[
              { header: 'Device', accessorKey: 'hostname' },
              { header: 'Score', cell: (r) => <ComplianceScore score={r.latestScore} size="sm" /> },
              { header: 'Status', cell: (r) => <StatusBadge status={r.status} /> },
            ]}
            onRowClick={(item) => navigate(`/devices/${item.deviceId}`)}
          />
        </div>
        
        <div className="card">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-semibold text-white">Recent Alerts</h2>
            <Link to="/alerts" className="text-blue-400 text-sm hover:underline">View All</Link>
          </div>
          <div className="space-y-4">
            {summary.recentAlerts.length === 0 ? (
              <p className="text-gray-500 text-sm">No recent alerts</p>
            ) : (
              summary.recentAlerts.map(alert => (
                <div key={alert.id} className="p-3 bg-gray-900 rounded-lg flex gap-3 border border-gray-800">
                  <div className="mt-0.5"><StatusBadge status={alert.severity} /></div>
                  <div>
                    <p className="text-sm font-medium text-gray-200">{alert.title}</p>
                    <p className="text-xs text-gray-500 mt-1">
                      {alert.deviceHostname} • {new Date(alert.detectedAt).toLocaleString()}
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
