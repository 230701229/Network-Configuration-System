import React, { useEffect, useState } from 'react';
import { driftApi } from '../services/driftApi';
import { deviceApi } from '../services/deviceApi';
import { DriftEvent, DriftStatus, Device, ConfigurationDiffData } from '../types';
import { StatusBadge } from '../components/ui/StatusBadge';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { 
  AlertTriangle, RefreshCw, CheckCircle2, ShieldAlert, 
  Activity, Eye, Play, Sparkles, Filter, Check, X, ArrowRight
} from 'lucide-react';

import { subscribeToRealtimeEvents, StreamEvent } from '../services/sseService';

export default function DriftPage() {
  const [drifts, setDrifts] = useState<DriftEvent[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<number | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [loading, setLoading] = useState(true);
  const [scanning, setScanning] = useState(false);
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'info' | 'error' } | null>(null);

  // Diff Modal state
  const [diffModalOpen, setDiffModalOpen] = useState(false);
  const [diffData, setDiffData] = useState<ConfigurationDiffData | null>(null);
  const [diffLoading, setDiffLoading] = useState(false);

  // Simulation panel
  const [simulatingDevice, setSimulatingDevice] = useState<number | undefined>(undefined);
  const [simParam, setSimParam] = useState('telnet');
  const [simValue, setSimValue] = useState('true');
  const [showSimPanel, setShowSimPanel] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const [driftList, deviceList] = await Promise.all([
        driftApi.getAllDrift(selectedDevice, statusFilter),
        deviceApi.getAllDevices()
      ]);
      setDrifts(driftList);
      setDevices(deviceList);
      if (deviceList.length > 0 && !simulatingDevice) {
        setSimulatingDevice(deviceList[0].id);
      }
    } catch (err) {
      console.error('Failed to load drift data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();

    // Subscribe to real-time drift events
    const unsubscribe = subscribeToRealtimeEvents((event: StreamEvent) => {
      if (event.eventType === 'DRIFT_DETECTED') {
        setNotification({
          message: event.message || 'New configuration drift detected in real time!',
          type: 'info'
        });
        loadData();
      } else if (event.eventType === 'DRIFT_RESOLVED' || event.eventType === 'SCAN_COMPLETED') {
        loadData();
      }
    });

    // Fallback polling interval every 10s
    const interval = setInterval(loadData, 10000);

    return () => {
      unsubscribe();
      clearInterval(interval);
    };
  }, [selectedDevice, statusFilter]);

  const handleScanAllDrift = async () => {
    setScanning(true);
    try {
      const newDrifts = await driftApi.detectAllDrift();
      await loadData();
      setNotification({
        message: newDrifts.length > 0 
          ? `Drift scan complete! ${newDrifts.length} new drift event(s) detected.`
          : 'Drift scan complete. All configurations match their baselines.',
        type: newDrifts.length > 0 ? 'info' : 'success'
      });
    } catch (err) {
      setNotification({ message: 'Error scanning network for drift', type: 'error' });
    } finally {
      setScanning(false);
      setTimeout(() => setNotification(null), 5000);
    }
  };

  const handleUpdateStatus = async (id: number, status: DriftStatus) => {
    try {
      await driftApi.updateDriftStatus(id, status);
      await loadData();
      setNotification({ message: `Drift event marked as ${status}`, type: 'success' });
      setTimeout(() => setNotification(null), 3000);
    } catch (err) {
      alert('Failed to update status');
    }
  };

  const handleViewDiff = async (deviceId: number) => {
    setDiffLoading(true);
    setDiffModalOpen(true);
    try {
      const data = await driftApi.getDeviceDiff(deviceId);
      setDiffData(data);
    } catch (err) {
      console.error('Failed to fetch diff', err);
    } finally {
      setDiffLoading(false);
    }
  };

  const handleSetBaseline = async (deviceId: number) => {
    try {
      await driftApi.setBaseline(deviceId);
      setNotification({ message: 'Current running configuration set as new baseline! Open drifts resolved.', type: 'success' });
      setDiffModalOpen(false);
      await loadData();
      setTimeout(() => setNotification(null), 4000);
    } catch (err) {
      alert('Failed to set baseline');
    }
  };

  const handleSimulate = async () => {
    if (!simulatingDevice) return;
    try {
      const res = await driftApi.simulateDrift(simulatingDevice, simParam, simValue);
      await loadData();
      setNotification({
        message: `Simulation applied: '${simParam}' set to '${simValue}'. System detected ${res.driftsDetected} drift event(s)!`,
        type: 'info'
      });
      setTimeout(() => setNotification(null), 6000);
    } catch (err) {
      alert('Simulation failed');
    }
  };

  const handleResetSimulation = async () => {
    if (!simulatingDevice) return;
    try {
      await driftApi.resetDrift(simulatingDevice);
      await loadData();
      setNotification({ message: 'Simulation reset. Configurations restored to original state.', type: 'success' });
      setTimeout(() => setNotification(null), 4000);
    } catch (err) {
      alert('Reset failed');
    }
  };

  const totalCount = drifts.length;
  const openCount = drifts.filter(d => d.status === 'OPEN').length;
  const criticalCount = drifts.filter(d => d.status === 'OPEN' && (d.severity === 'CRITICAL' || d.severity === 'HIGH')).length;
  const resolvedCount = drifts.filter(d => d.status === 'RESOLVED').length;

  return (
    <div className="space-y-6">
      {/* Top Banner & Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2 bg-amber-500/10 text-amber-400 rounded-lg border border-amber-500/20">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">Configuration Drift Control Center</h1>
              <p className="text-sm text-gray-400">
                Continuous monitoring for unauthorized or unexpected configuration deviations
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowSimPanel(!showSimPanel)}
            className="px-3 py-2 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded-lg text-sm font-medium border border-gray-700 flex items-center gap-2 transition"
          >
            <Sparkles className="w-4 h-4 text-purple-400" />
            {showSimPanel ? 'Hide Simulator' : 'Test Drift Simulation'}
          </button>

          <button
            onClick={handleScanAllDrift}
            disabled={scanning}
            className="btn-primary flex items-center gap-2"
          >
            <RefreshCw className={`w-4 h-4 ${scanning ? 'animate-spin' : ''}`} />
            {scanning ? 'Detecting Drift...' : 'Scan Network for Drift'}
          </button>
        </div>
      </div>

      {/* Notification Toast */}
      {notification && (
        <div className={`p-4 rounded-lg flex items-center justify-between text-sm ${
          notification.type === 'success' 
            ? 'bg-emerald-950/60 border border-emerald-500/40 text-emerald-200' 
            : notification.type === 'error'
            ? 'bg-red-950/60 border border-red-500/40 text-red-200'
            : 'bg-amber-950/60 border border-amber-500/40 text-amber-200'
        }`}>
          <div className="flex items-center gap-2">
            <Activity className="w-4 h-4" />
            <span>{notification.message}</span>
          </div>
          <button onClick={() => setNotification(null)} className="text-gray-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="card flex items-center gap-4">
          <div className="p-3 bg-blue-500/10 text-blue-400 rounded-xl">
            <Activity className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs text-gray-400 font-medium uppercase tracking-wider">Total Drifts Tracked</p>
            <p className="text-2xl font-bold text-white mt-1">{totalCount}</p>
          </div>
        </div>

        <div className="card flex items-center gap-4">
          <div className="p-3 bg-amber-500/10 text-amber-400 rounded-xl">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs text-gray-400 font-medium uppercase tracking-wider">Open Drift Events</p>
            <p className="text-2xl font-bold text-amber-400 mt-1">{openCount}</p>
          </div>
        </div>

        <div className="card flex items-center gap-4">
          <div className="p-3 bg-red-500/10 text-red-400 rounded-xl">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs text-gray-400 font-medium uppercase tracking-wider">Critical / High Risk</p>
            <p className="text-2xl font-bold text-red-400 mt-1">{criticalCount}</p>
          </div>
        </div>

        <div className="card flex items-center gap-4">
          <div className="p-3 bg-emerald-500/10 text-emerald-400 rounded-xl">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs text-gray-400 font-medium uppercase tracking-wider">Resolved Drifts</p>
            <p className="text-2xl font-bold text-emerald-400 mt-1">{resolvedCount}</p>
          </div>
        </div>
      </div>

      {/* Interactive Drift Simulation Panel (Collapsible) */}
      {showSimPanel && (
        <div className="card bg-gray-900/90 border border-purple-500/30 p-5 rounded-xl space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-purple-400" />
              <h3 className="font-semibold text-white">Interactive Drift Simulation Lab</h3>
            </div>
            <span className="text-xs px-2 py-1 bg-purple-500/20 text-purple-300 rounded font-mono">
              Live Testing Mode
            </span>
          </div>
          <p className="text-xs text-gray-400">
            Inject configuration drift into a virtual router to verify that the automated detection engine immediately catches unauthorized changes.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1">Target Device</label>
              <select
                value={simulatingDevice}
                onChange={(e) => setSimulatingDevice(Number(e.target.value))}
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-gray-200"
              >
                {devices.map(d => (
                  <option key={d.id} value={d.id}>{d.hostname} ({d.ipAddress})</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1">Parameter to Drift</label>
              <select
                value={simParam}
                onChange={(e) => setSimParam(e.target.value)}
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-gray-200"
              >
                <option value="telnet">telnet (Enable insecure Telnet)</option>
                <option value="ntp">ntp (Disable NTP sync)</option>
                <option value="password_encryption">password_encryption (Turn off encryption)</option>
                <option value="http">http (Enable HTTP server)</option>
                <option value="logging">logging (Disable Syslog logging)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1">New Drifted Value</label>
              <select
                value={simValue}
                onChange={(e) => setSimValue(e.target.value)}
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-gray-200"
              >
                <option value="true">true</option>
                <option value="false">false</option>
              </select>
            </div>

            <div className="flex gap-2">
              <button
                onClick={handleSimulate}
                className="flex-1 px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm font-medium flex items-center justify-center gap-1 transition"
              >
                <Play className="w-4 h-4" /> Inject Drift
              </button>
              <button
                onClick={handleResetSimulation}
                className="px-3 py-2 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded-lg text-sm font-medium border border-gray-700 transition"
                title="Reset simulation overrides"
              >
                Reset
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Filter Toolbar */}
      <div className="card p-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Status Tabs */}
        <div className="flex items-center gap-1 bg-gray-950 p-1 rounded-lg border border-gray-800">
          {['ALL', 'OPEN', 'ACKNOWLEDGED', 'RESOLVED'].map(st => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition ${
                statusFilter === st
                  ? 'bg-blue-600 text-white shadow'
                  : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800/60'
              }`}
            >
              {st} {st === 'OPEN' && openCount > 0 ? `(${openCount})` : ''}
            </button>
          ))}
        </div>

        {/* Device Dropdown */}
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-gray-400" />
          <span className="text-xs text-gray-400">Device:</span>
          <select
            value={selectedDevice || ''}
            onChange={(e) => setSelectedDevice(e.target.value ? Number(e.target.value) : undefined)}
            className="bg-gray-900 border border-gray-700 rounded-lg px-3 py-1.5 text-xs text-gray-200 focus:outline-none focus:border-blue-500"
          >
            <option value="">All Devices</option>
            {devices.map(d => (
              <option key={d.id} value={d.id}>{d.hostname}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Drift Events Table */}
      <div className="card overflow-hidden">
        {loading ? (
          <div className="py-12 flex justify-center"><LoadingSpinner /></div>
        ) : drifts.length === 0 ? (
          <div className="py-16 text-center space-y-3">
            <div className="inline-flex p-3 rounded-full bg-emerald-500/10 text-emerald-400">
              <CheckCircle2 className="w-8 h-8" />
            </div>
            <h3 className="text-base font-semibold text-white">No Drift Detected</h3>
            <p className="text-sm text-gray-400 max-w-md mx-auto">
              All live network device configurations are currently identical to their approved baselines.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-950/60 border-b border-gray-800 text-xs text-gray-400 uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Device</th>
                  <th className="py-3 px-4">Parameter</th>
                  <th className="py-3 px-4">Baseline Expected</th>
                  <th className="py-3 px-4">Live Running</th>
                  <th className="py-3 px-4">Severity</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Detected At</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-800/60">
                {drifts.map((d) => (
                  <tr key={d.id} className="hover:bg-gray-800/30 transition">
                    <td className="py-3 px-4 font-medium text-white flex items-center gap-2">
                      <span className="w-2 h-2 rounded-full bg-blue-400" />
                      {d.deviceHostname || `Device #${d.deviceId}`}
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-mono text-xs bg-gray-800/80 px-2 py-1 rounded text-blue-300">
                        {d.parameter}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-mono text-xs text-emerald-400">
                      {d.expectedValue}
                    </td>
                    <td className="py-3 px-4 font-mono text-xs text-red-400 font-semibold">
                      {d.actualValue || '<removed>'}
                    </td>
                    <td className="py-3 px-4">
                      <StatusBadge status={d.severity} />
                    </td>
                    <td className="py-3 px-4">
                      <StatusBadge status={d.status} />
                    </td>
                    <td className="py-3 px-4 text-xs text-gray-400">
                      {new Date(d.detectedAt).toLocaleString()}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => handleViewDiff(d.deviceId)}
                          className="p-1.5 text-gray-400 hover:text-blue-400 hover:bg-gray-800 rounded transition"
                          title="View Configuration Diff"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                        {d.status === 'OPEN' && (
                          <button
                            onClick={() => handleUpdateStatus(d.id, 'ACKNOWLEDGED')}
                            className="px-2 py-1 bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 rounded text-xs border border-amber-500/20 transition"
                            title="Acknowledge Drift"
                          >
                            Ack
                          </button>
                        )}
                        {d.status !== 'RESOLVED' && (
                          <button
                            onClick={() => handleUpdateStatus(d.id, 'RESOLVED')}
                            className="px-2 py-1 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 rounded text-xs border border-emerald-500/20 transition flex items-center gap-1"
                            title="Mark as Resolved"
                          >
                            <Check className="w-3 h-3" /> Resolve
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

      {/* Configuration Diff Modal */}
      {diffModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-sm">
          <div className="bg-gray-900 border border-gray-800 rounded-2xl w-full max-w-3xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden">
            <div className="p-5 border-b border-gray-800 flex items-center justify-between">
              <div>
                <h3 className="text-lg font-bold text-white flex items-center gap-2">
                  <Activity className="w-5 h-5 text-blue-400" />
                  Configuration Baseline Diff
                </h3>
                <p className="text-xs text-gray-400 mt-0.5">
                  Device: <span className="text-white font-medium">{diffData?.hostname || 'Device'}</span>
                </p>
              </div>
              <button
                onClick={() => setDiffModalOpen(false)}
                className="text-gray-400 hover:text-white p-1 rounded-lg hover:bg-gray-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 overflow-y-auto space-y-6 flex-1">
              {diffLoading ? (
                <div className="py-12 flex justify-center"><LoadingSpinner /></div>
              ) : diffData ? (
                <>
                  <div className="grid grid-cols-2 gap-4 text-xs bg-gray-950 p-4 rounded-xl border border-gray-800">
                    <div>
                      <p className="text-gray-500 uppercase font-semibold">Approved Baseline</p>
                      <p className="text-white mt-1">
                        {diffData.baselineTime ? new Date(diffData.baselineTime).toLocaleString() : 'N/A'}
                      </p>
                      <p className="text-gray-500 font-mono text-[11px] truncate mt-0.5">Hash: {diffData.baselineHash || 'N/A'}</p>
                    </div>
                    <div>
                      <p className="text-gray-500 uppercase font-semibold">Current Running Config</p>
                      <p className="text-white mt-1">
                        {diffData.currentTime ? new Date(diffData.currentTime).toLocaleString() : 'N/A'}
                      </p>
                      <p className="text-gray-500 font-mono text-[11px] truncate mt-0.5">Hash: {diffData.currentHash || 'N/A'}</p>
                    </div>
                  </div>

                  {diffData.changes.length === 0 ? (
                    <div className="p-8 text-center text-emerald-400 bg-emerald-950/20 border border-emerald-500/20 rounded-xl">
                      <CheckCircle2 className="w-8 h-8 mx-auto mb-2" />
                      <p className="font-medium text-sm">Exact Match</p>
                      <p className="text-xs text-gray-400 mt-1">
                        Current running configuration is 100% synchronized with the approved baseline.
                      </p>
                    </div>
                  ) : (
                    <div>
                      <h4 className="text-sm font-semibold text-white mb-3 flex items-center justify-between">
                        <span>Differences Detected ({diffData.changes.length})</span>
                      </h4>
                      <div className="space-y-2">
                        {diffData.changes.map((ch, idx) => (
                          <div
                            key={idx}
                            className="bg-gray-950 border border-gray-800/80 rounded-xl p-3 text-xs flex flex-col md:flex-row md:items-center justify-between gap-3"
                          >
                            <div className="space-y-1">
                              <div className="flex items-center gap-2">
                                <span className="font-mono font-semibold text-white bg-gray-800 px-2 py-0.5 rounded">
                                  {ch.parameter}
                                </span>
                                <StatusBadge status={ch.severity} />
                                <span className="text-gray-500 text-[11px]">({ch.changeType})</span>
                              </div>
                              {ch.remediation && (
                                <p className="text-amber-400 text-[11px] font-mono mt-1">
                                  Remediation: {ch.remediation}
                                </p>
                              )}
                            </div>

                            <div className="flex items-center gap-3 font-mono">
                              <span className="px-2.5 py-1 rounded bg-emerald-950/80 text-emerald-300 border border-emerald-700/40">
                                - {ch.baselineValue}
                              </span>
                              <ArrowRight className="w-3.5 h-3.5 text-gray-500" />
                              <span className="px-2.5 py-1 rounded bg-red-950/80 text-red-300 border border-red-700/40 font-bold">
                                + {ch.currentValue}
                              </span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              ) : (
                <p className="text-center text-gray-500">No snapshot history found for this device.</p>
              )}
            </div>

            <div className="p-4 border-t border-gray-800 bg-gray-950/60 flex items-center justify-between">
              <span className="text-xs text-gray-400">
                Accepting changes updates the golden baseline.
              </span>
              <div className="flex gap-2">
                {diffData && diffData.hasDrift && (
                  <button
                    onClick={() => handleSetBaseline(diffData.deviceId)}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 transition"
                  >
                    <Check className="w-3.5 h-3.5" /> Accept as New Baseline
                  </button>
                )}
                <button
                  onClick={() => setDiffModalOpen(false)}
                  className="btn-secondary text-xs"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
