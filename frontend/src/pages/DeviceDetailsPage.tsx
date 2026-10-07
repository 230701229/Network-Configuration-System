import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { deviceApi } from '../services/deviceApi';
import { driftApi } from '../services/driftApi';
import { Device, ComplianceScan, DriftEvent, AIAnalysis, ConfigurationDiffData, DriftStatus } from '../types';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { StatusBadge } from '../components/ui/StatusBadge';
import { ComplianceScore } from '../components/ui/ComplianceScore';
import { ConfigurationDiff } from '../components/compliance/ConfigurationDiff';
import { ViolationTable } from '../components/compliance/ViolationTable';
import { aiApi } from '../services/aiApi';
import { 
  Play, RefreshCw, Activity, Sparkles, Shield, AlertTriangle, 
  Check, Eye, ShieldAlert, ArrowRight, X 
} from 'lucide-react';

export default function DeviceDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const deviceId = Number(id);
  const [device, setDevice] = useState<Device | null>(null);
  const [scans, setScans] = useState<ComplianceScan[]>([]);
  const [drift, setDrift] = useState<DriftEvent[]>([]);
  const [aiAnalysis, setAiAnalysis] = useState<AIAnalysis[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState<'overview'|'compliance'|'violations'|'ai'|'drift'>('overview');
  const [actionLoading, setActionLoading] = useState(false);
  const [diffData, setDiffData] = useState<ConfigurationDiffData | null>(null);
  const [diffModalOpen, setDiffModalOpen] = useState(false);
  const [notification, setNotification] = useState<string | null>(null);

  const loadData = async () => {
    try {
      setLoading(true);
      const [dev, scn, drf, ai] = await Promise.all([
        deviceApi.getDevice(deviceId),
        deviceApi.getDeviceHistory(deviceId),
        driftApi.getDeviceDrift(deviceId),
        aiApi.getDeviceAnalyses(deviceId)
      ]);
      setDevice(dev);
      setScans(scn);
      setDrift(drf);
      setAiAnalysis(ai);
      setError('');
    } catch (err) {
      setError('Failed to load device details');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (deviceId) loadData();
  }, [deviceId]);

  const handleScan = async () => {
    setActionLoading(true);
    try {
      await deviceApi.scanDevice(deviceId);
      await loadData();
      showNotification('Compliance scan completed successfully');
    } catch (err) {
      alert('Scan failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDetectDrift = async () => {
    setActionLoading(true);
    try {
      const newDrifts = await driftApi.detectDeviceDrift(deviceId);
      await loadData();
      showNotification(
        newDrifts.length > 0 
          ? `Drift check finished: ${newDrifts.length} parameter deviation(s) detected!`
          : 'Drift check finished: Device is in full sync with approved baseline.'
      );
    } catch (err) {
      alert('Drift check failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdateDriftStatus = async (driftId: number, status: DriftStatus) => {
    try {
      await driftApi.updateDriftStatus(driftId, status);
      await loadData();
      showNotification(`Drift event marked as ${status}`);
    } catch (err) {
      alert('Failed to update drift status');
    }
  };

  const handleViewDiff = async () => {
    setActionLoading(true);
    try {
      const data = await driftApi.getDeviceDiff(deviceId);
      setDiffData(data);
      setDiffModalOpen(true);
    } catch (err) {
      alert('Failed to load configuration diff');
    } finally {
      setActionLoading(false);
    }
  };

  const handleSetBaseline = async () => {
    try {
      await driftApi.setBaseline(deviceId);
      showNotification('Current configuration accepted as new baseline! Open drifts resolved.');
      setDiffModalOpen(false);
      await loadData();
    } catch (err) {
      alert('Failed to set baseline');
    }
  };

  const showNotification = (msg: string) => {
    setNotification(msg);
    setTimeout(() => setNotification(null), 4000);
  };

  if (loading) return <div className="py-12 flex justify-center"><LoadingSpinner /></div>;
  if (error || !device) return <ErrorMessage message={error} onRetry={loadData} />;

  const latestScan = scans.length > 0 ? scans[0] : null;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <h1 className="text-2xl font-bold text-white">{device.hostname}</h1>
            <StatusBadge status={device.status} />
          </div>
          <p className="text-gray-400 text-sm">
            {device.ipAddress} • {device.deviceType} • Protocol: <span className="text-blue-400 font-mono">{device.managementProtocol}</span>
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <button 
            onClick={handleDetectDrift} 
            disabled={actionLoading} 
            className="px-3 py-2 bg-amber-600/20 hover:bg-amber-600/30 text-amber-300 border border-amber-500/30 rounded-lg text-sm font-medium flex items-center gap-2 transition"
          >
            <AlertTriangle className="w-4 h-4 text-amber-400" /> Check Drift Now
          </button>
          <button 
            onClick={handleScan} 
            disabled={actionLoading} 
            className="btn-primary flex items-center gap-2"
          >
            <Play className="w-4 h-4" /> Run Compliance Scan
          </button>
          <button 
            onClick={loadData} 
            disabled={actionLoading} 
            className="btn-secondary"
            title="Refresh"
          >
            <RefreshCw className={`w-4 h-4 ${actionLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Notification Toast */}
      {notification && (
        <div className="p-3.5 bg-blue-950/60 border border-blue-500/30 rounded-lg text-sm text-blue-200 flex items-center justify-between">
          <span>{notification}</span>
          <button onClick={() => setNotification(null)} className="text-gray-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-4 border-b border-gray-800 overflow-x-auto">
        {[
          { id: 'overview', label: 'Overview', icon: Activity },
          { id: 'compliance', label: 'Compliance', icon: Shield },
          { id: 'violations', label: 'Violations', icon: AlertTriangle },
          { id: 'drift', label: `Drift Events (${drift.filter(d => d.status === 'OPEN').length})`, icon: RefreshCw },
          { id: 'ai', label: 'AI Analysis', icon: Sparkles }
        ].map(t => (
          <button
            key={t.id}
            onClick={() => setActiveTab(t.id as any)}
            className={`flex items-center gap-2 px-4 py-3 border-b-2 font-medium text-sm transition-colors whitespace-nowrap ${
              activeTab === t.id ? 'border-blue-500 text-blue-400' : 'border-transparent text-gray-400 hover:text-gray-200'
            }`}
          >
            <t.icon className="w-4 h-4" /> {t.label}
          </button>
        ))}
      </div>

      {/* Tab Panels */}
      <div className="min-h-[400px]">
        {activeTab === 'overview' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="card">
              <h3 className="text-lg font-semibold mb-4 text-white">Device Information</h3>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between py-1 border-b border-gray-800/60">
                  <span className="text-gray-400">Management Protocol</span>
                  <span className="text-white font-mono">{device.managementProtocol}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-gray-800/60">
                  <span className="text-gray-400">Port</span>
                  <span className="text-white font-mono">{device.netconfPort || device.restconfPort || 830}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-gray-800/60">
                  <span className="text-gray-400">Last Seen</span>
                  <span className="text-white">{device.lastSeen ? new Date(device.lastSeen).toLocaleString() : 'Never'}</span>
                </div>
                <div className="flex justify-between py-1 border-b border-gray-800/60">
                  <span className="text-gray-400">Applied Policy</span>
                  <span className="text-white">{device.appliedPolicyName || 'Default Enterprise Policy'}</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-gray-400">Description</span>
                  <span className="text-gray-300">{device.description || 'No description provided'}</span>
                </div>
              </div>
            </div>

            <div className="card flex flex-col items-center justify-center">
              <h3 className="text-lg font-semibold mb-6 text-white w-full text-left">Latest Compliance</h3>
              {latestScan ? (
                <div className="text-center">
                  <ComplianceScore score={latestScan.complianceScore} size="lg" />
                  <div className="mt-4"><StatusBadge status={latestScan.complianceStatus} /></div>
                  <p className="text-xs text-gray-500 mt-2">Scanned: {new Date(latestScan.scannedAt).toLocaleString()}</p>
                </div>
              ) : (
                <p className="text-gray-500">No scans performed yet.</p>
              )}
            </div>
          </div>
        )}

        {activeTab === 'compliance' && latestScan && (
          <div className="card">
            <h3 className="text-lg font-semibold mb-4 text-white">Rule Evaluation Matrix</h3>
            <ConfigurationDiff results={latestScan.results} />
          </div>
        )}

        {activeTab === 'violations' && latestScan && (
          <div className="card">
            <h3 className="text-lg font-semibold mb-4 text-white">Policy Violations</h3>
            <ViolationTable violations={latestScan.results.filter(r => !r.passed)} />
          </div>
        )}

        {activeTab === 'drift' && (
          <div className="card space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-gray-800">
              <div>
                <h3 className="text-lg font-semibold text-white">Device Configuration Drift History</h3>
                <p className="text-xs text-gray-400 mt-0.5">
                  Tracks changes between running configuration and the golden baseline
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handleViewDiff}
                  className="px-3 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded-lg text-xs font-medium border border-gray-700 flex items-center gap-1.5 transition"
                >
                  <Eye className="w-3.5 h-3.5 text-blue-400" /> View Baseline Diff
                </button>
                <button
                  onClick={handleSetBaseline}
                  className="px-3 py-1.5 bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 rounded-lg text-xs font-medium border border-emerald-500/30 flex items-center gap-1.5 transition"
                >
                  <Check className="w-3.5 h-3.5" /> Accept Current as Baseline
                </button>
              </div>
            </div>

            {drift.length === 0 ? (
              <div className="text-center py-12 space-y-2">
                <Shield className="w-8 h-8 text-emerald-400 mx-auto" />
                <p className="text-sm font-semibold text-white">Zero Configuration Drift</p>
                <p className="text-xs text-gray-400 max-w-sm mx-auto">
                  Running configuration on {device.hostname} exactly matches the baseline.
                </p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-gray-950/60 border-b border-gray-800 text-xs text-gray-400 uppercase tracking-wider">
                    <tr>
                      <th className="py-2.5 px-3">Parameter</th>
                      <th className="py-2.5 px-3">Baseline Expected</th>
                      <th className="py-2.5 px-3">Running Actual</th>
                      <th className="py-2.5 px-3">Severity</th>
                      <th className="py-2.5 px-3">Status</th>
                      <th className="py-2.5 px-3">Detected At</th>
                      <th className="py-2.5 px-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-800/60">
                    {drift.map(d => (
                      <tr key={d.id} className="hover:bg-gray-800/30 transition">
                        <td className="py-2.5 px-3 font-mono text-xs text-blue-300 font-semibold">
                          {d.parameter}
                        </td>
                        <td className="py-2.5 px-3 font-mono text-xs text-emerald-400">
                          {d.expectedValue}
                        </td>
                        <td className="py-2.5 px-3 font-mono text-xs text-red-400 font-bold">
                          {d.actualValue || '<absent>'}
                        </td>
                        <td className="py-2.5 px-3"><StatusBadge status={d.severity} /></td>
                        <td className="py-2.5 px-3"><StatusBadge status={d.status} /></td>
                        <td className="py-2.5 px-3 text-xs text-gray-400">
                          {new Date(d.detectedAt).toLocaleString()}
                        </td>
                        <td className="py-2.5 px-3 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            {d.status === 'OPEN' && (
                              <button
                                onClick={() => handleUpdateDriftStatus(d.id, 'ACKNOWLEDGED')}
                                className="px-2 py-0.5 bg-amber-500/10 text-amber-300 hover:bg-amber-500/20 rounded text-xs border border-amber-500/20"
                              >
                                Ack
                              </button>
                            )}
                            {d.status !== 'RESOLVED' && (
                              <button
                                onClick={() => handleUpdateDriftStatus(d.id, 'RESOLVED')}
                                className="px-2 py-0.5 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20 rounded text-xs border border-emerald-500/20"
                              >
                                Resolve
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === 'ai' && (
          <div className="space-y-4">
            {aiAnalysis.length > 0 ? aiAnalysis.map(ai => (
              <div key={ai.id} className="card">
                <div className="flex justify-between items-start mb-4">
                  <h3 className="text-lg font-semibold text-white">AI Analysis Report</h3>
                  <StatusBadge status={ai.priority} />
                </div>
                <p className="text-gray-300 mb-4">{ai.summary}</p>
                <div className="bg-gray-900/50 p-4 rounded-lg mb-4 text-sm text-gray-400 whitespace-pre-wrap">{ai.analysis}</div>
                <h4 className="font-medium text-white mb-2">Recommendations:</h4>
                <ul className="list-disc list-inside text-sm text-gray-300 space-y-1">
                  {ai.recommendations.map((r, i) => <li key={i}>{r}</li>)}
                </ul>
              </div>
            )) : <div className="card text-center py-8 text-gray-500">No AI analysis available for this device.</div>}
          </div>
        )}
      </div>

      {/* Diff Modal */}
      {diffModalOpen && diffData && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-sm">
          <div className="bg-gray-900 border border-gray-800 rounded-2xl w-full max-w-2xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden">
            <div className="p-5 border-b border-gray-800 flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <Activity className="w-4 h-4 text-blue-400" />
                  Baseline vs Running Diff: {device.hostname}
                </h3>
              </div>
              <button onClick={() => setDiffModalOpen(false)} className="text-gray-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 overflow-y-auto space-y-4 flex-1 text-xs">
              {diffData.changes.length === 0 ? (
                <div className="text-center py-8 text-emerald-400">
                  <Check className="w-8 h-8 mx-auto mb-2" />
                  <p className="font-semibold text-sm">Perfect Match</p>
                  <p className="text-gray-400 mt-1">No deviations from baseline detected.</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {diffData.changes.map((ch, idx) => (
                    <div key={idx} className="bg-gray-950 p-3 rounded-lg border border-gray-800 flex items-center justify-between">
                      <div>
                        <span className="font-mono font-semibold text-white bg-gray-800 px-2 py-0.5 rounded">
                          {ch.parameter}
                        </span>
                        {ch.remediation && (
                          <p className="text-amber-400 text-[11px] mt-1">{ch.remediation}</p>
                        )}
                      </div>
                      <div className="flex items-center gap-2 font-mono">
                        <span className="text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded">
                          - {ch.baselineValue}
                        </span>
                        <ArrowRight className="w-3 h-3 text-gray-500" />
                        <span className="text-red-400 bg-red-950/60 px-2 py-0.5 rounded font-bold">
                          + {ch.currentValue}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="p-4 border-t border-gray-800 flex justify-end gap-2">
              {diffData.hasDrift && (
                <button onClick={handleSetBaseline} className="btn-primary text-xs flex items-center gap-1.5">
                  <Check className="w-3.5 h-3.5" /> Accept as Baseline
                </button>
              )}
              <button onClick={() => setDiffModalOpen(false)} className="btn-secondary text-xs">
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
