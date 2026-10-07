import React, { useState, useEffect } from 'react';
import { gns3Api } from '../services/gns3Api';
import { deviceApi } from '../services/deviceApi';
import { driftApi } from '../services/driftApi';
import { Gns3ServerStatus, Gns3Project, Gns3Node, Device } from '../types';
import { StatusBadge } from '../components/ui/StatusBadge';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { 
  Network, Server, CheckCircle2, XCircle, RefreshCw, 
  Copy, Check, Terminal, ExternalLink, ShieldAlert, 
  Play, Plus, ArrowRight, Zap, Info 
} from 'lucide-react';

export default function VirtualLabPage() {
  const [serverUrl, setServerUrl] = useState('http://localhost:3080');
  const [serverStatus, setServerStatus] = useState<Gns3ServerStatus | null>(null);
  const [testingConnection, setTestingConnection] = useState(false);
  const [projects, setProjects] = useState<Gns3Project[]>([]);
  const [selectedProject, setSelectedProject] = useState<string>('');
  const [nodes, setNodes] = useState<Gns3Node[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [loadingNodes, setLoadingNodes] = useState(false);
  
  // Config templates
  const [templates, setTemplates] = useState<{
    netconfConfig: string;
    restconfConfig: string;
    compliantBaseline: string;
    driftInjectionScript: string;
  } | null>(null);
  const [activeTemplateTab, setActiveTemplateTab] = useState<'netconf'|'restconf'|'baseline'|'drift'>('netconf');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  // Manual Add Virtual Device modal / form
  const [showAddModal, setShowAddModal] = useState(false);
  const [newDevice, setNewDevice] = useState({
    name: 'GNS3-CSR1000v-01',
    ipAddress: '192.168.122.101',
    managementProtocol: 'NETCONF',
    deviceType: 'ROUTER',
    port: '830',
    username: 'admin',
    password: 'Admin123!'
  });
  const [importing, setImporting] = useState(false);

  const loadInitialData = async () => {
    try {
      const [devList, tmpl] = await Promise.all([
        deviceApi.getAllDevices(),
        gns3Api.getConfigTemplates('Router-GNS3-01', '192.168.122.10')
      ]);
      setDevices(devList);
      setTemplates(tmpl);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    loadInitialData();
  }, []);

  const handleTestConnection = async () => {
    setTestingConnection(true);
    try {
      const status = await gns3Api.testConnection(serverUrl);
      setServerStatus(status);
      if (status.connected) {
        const projList = await gns3Api.getProjects(serverUrl);
        setProjects(projList);
        if (projList.length > 0) {
          setSelectedProject(projList[0].projectId);
          loadProjectNodes(projList[0].projectId);
        }
      }
    } catch (err) {
      setServerStatus({
        connected: false,
        url: serverUrl,
        message: 'Could not connect to GNS3 API at ' + serverUrl
      });
    } finally {
      setTestingConnection(false);
    }
  };

  const loadProjectNodes = async (projId: string) => {
    setLoadingNodes(true);
    try {
      const nodeList = await gns3Api.getNodes(projId, serverUrl);
      setNodes(nodeList);
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingNodes(false);
    }
  };

  const handleProjectChange = (projId: string) => {
    setSelectedProject(projId);
    loadProjectNodes(projId);
  };

  const handleImportNode = async (node: Gns3Node) => {
    setImporting(true);
    try {
      await gns3Api.importNode({
        name: node.name,
        ipAddress: '192.168.122.' + (100 + Math.floor(Math.random() * 50)),
        managementProtocol: 'NETCONF',
        deviceType: 'ROUTER',
        port: '830',
        username: 'admin',
        password: 'Admin123!'
      });
      alert(`Node "${node.name}" registered in NMS device inventory!`);
      const updated = await deviceApi.getAllDevices();
      setDevices(updated);
    } catch (err) {
      alert('Failed to import node');
    } finally {
      setImporting(false);
    }
  };

  const handleAddManualVirtualDevice = async (e: React.FormEvent) => {
    e.preventDefault();
    setImporting(true);
    try {
      await gns3Api.importNode(newDevice);
      alert(`Virtual device "${newDevice.name}" added to NMS inventory!`);
      setShowAddModal(false);
      const updated = await deviceApi.getAllDevices();
      setDevices(updated);
    } catch (err) {
      alert('Failed to add device');
    } finally {
      setImporting(false);
    }
  };

  const copyToClipboard = (text: string, key: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2500);
  };

  const virtualDevices = devices.filter(d => 
    d.managementProtocol === 'NETCONF' || 
    d.managementProtocol === 'RESTCONF' || 
    d.managementProtocol === 'SSH' ||
    d.hostname.toLowerCase().includes('gns3')
  );

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-blue-500/10 text-blue-400 rounded-xl border border-blue-500/20">
            <Network className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">Virtual Network Lab (GNS3)</h1>
            <p className="text-sm text-gray-400">
              Integrate simulated routers & switches via NETCONF (830), RESTCONF (443), and SSH (22)
            </p>
          </div>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="btn-primary flex items-center gap-2"
        >
          <Plus className="w-4 h-4" /> Register Virtual Device
        </button>
      </div>

      {/* GNS3 Server Connection Bar */}
      <div className="card p-5 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex-1">
            <label className="block text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1.5">
              GNS3 Server REST API Endpoint
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={serverUrl}
                onChange={(e) => setServerUrl(e.target.value)}
                placeholder="http://localhost:3080 or http://192.168.1.50:3080"
                className="flex-1 bg-gray-950 border border-gray-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500"
              />
              <button
                onClick={handleTestConnection}
                disabled={testingConnection}
                className="btn-primary flex items-center gap-2 whitespace-nowrap text-sm"
              >
                <RefreshCw className={`w-4 h-4 ${testingConnection ? 'animate-spin' : ''}`} />
                {testingConnection ? 'Testing...' : 'Test Connection'}
              </button>
            </div>
          </div>

          {/* Connection Status Pill */}
          <div className="flex items-center gap-3 pt-2 md:pt-4">
            {serverStatus ? (
              <div className={`px-4 py-2 rounded-xl border flex items-center gap-2 text-sm ${
                serverStatus.connected 
                  ? 'bg-emerald-950/60 border-emerald-500/30 text-emerald-300' 
                  : 'bg-red-950/60 border-red-500/30 text-red-300'
              }`}>
                {serverStatus.connected ? <CheckCircle2 className="w-5 h-5 text-emerald-400" /> : <XCircle className="w-5 h-5 text-red-400" />}
                <div>
                  <p className="font-semibold">{serverStatus.connected ? 'GNS3 Online' : 'GNS3 Offline'}</p>
                  <p className="text-xs opacity-80">{serverStatus.message}</p>
                </div>
              </div>
            ) : (
              <div className="px-4 py-2 bg-gray-900 border border-gray-800 rounded-xl text-gray-400 text-xs">
                Click 'Test Connection' to check your local or remote GNS3 server.
              </div>
            )}
          </div>
        </div>

        {/* Discovery section if connected */}
        {serverStatus?.connected && (
          <div className="pt-4 border-t border-gray-800 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <span className="text-xs text-gray-400 font-medium">Select GNS3 Project:</span>
                <select
                  value={selectedProject}
                  onChange={(e) => handleProjectChange(e.target.value)}
                  className="bg-gray-900 border border-gray-700 rounded-lg px-3 py-1.5 text-xs text-white"
                >
                  {projects.map(p => (
                    <option key={p.projectId} value={p.projectId}>{p.name} ({p.status})</option>
                  ))}
                </select>
              </div>
            </div>

            {loadingNodes ? (
              <div className="py-6 flex justify-center"><LoadingSpinner /></div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                {nodes.map(n => (
                  <div key={n.nodeId} className="bg-gray-950 border border-gray-800 p-3 rounded-xl flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-white text-sm">{n.name}</p>
                      <p className="text-xs text-gray-400">Type: {n.nodeType} • Port: {n.consolePort || 'N/A'}</p>
                    </div>
                    <button
                      onClick={() => handleImportNode(n)}
                      disabled={importing}
                      className="px-2.5 py-1 bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 rounded text-xs border border-blue-500/30 font-medium transition"
                    >
                      Import
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Registered Virtual Devices Inventory */}
      <div className="card space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Server className="w-5 h-5 text-blue-400" />
            <h3 className="font-semibold text-white">Registered Virtual Network Devices</h3>
          </div>
          <span className="text-xs text-gray-400">{virtualDevices.length} virtual devices active</span>
        </div>

        {virtualDevices.length === 0 ? (
          <div className="text-center py-8 text-gray-500 space-y-2">
            <p>No virtual network devices registered yet.</p>
            <p className="text-xs">Click "Register Virtual Device" above to add your GNS3 router or switch.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-950/60 border-b border-gray-800 text-xs text-gray-400 uppercase tracking-wider">
                <tr>
                  <th className="py-3 px-4">Hostname</th>
                  <th className="py-3 px-4">IP Address</th>
                  <th className="py-3 px-4">Protocol</th>
                  <th className="py-3 px-4">Port</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Last Seen</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-800/60">
                {virtualDevices.map(d => (
                  <tr key={d.id} className="hover:bg-gray-800/30 transition">
                    <td className="py-3 px-4 font-semibold text-white">{d.hostname}</td>
                    <td className="py-3 px-4 font-mono text-xs text-gray-300">{d.ipAddress}</td>
                    <td className="py-3 px-4">
                      <span className="font-mono text-xs px-2 py-0.5 rounded bg-blue-950 text-blue-300 border border-blue-800/50">
                        {d.managementProtocol}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-mono text-xs text-gray-400">
                      {d.netconfPort || d.restconfPort || 830}
                    </td>
                    <td className="py-3 px-4"><StatusBadge status={d.status} /></td>
                    <td className="py-3 px-4 text-xs text-gray-400">
                      {d.lastSeen ? new Date(d.lastSeen).toLocaleString() : 'Never'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Cisco IOS-XE Virtual Lab Configuration Generator */}
      <div className="card space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-gray-800">
          <div>
            <h3 className="font-semibold text-white flex items-center gap-2">
              <Terminal className="w-5 h-5 text-emerald-400" />
              Cisco IOS-XE / GNS3 Configuration Generator
            </h3>
            <p className="text-xs text-gray-400 mt-0.5">
              Copy and paste these pre-formatted commands directly into your GNS3 router console
            </p>
          </div>
        </div>

        {/* Template Tabs */}
        <div className="flex gap-2 border-b border-gray-800 pb-2">
          {[
            { id: 'netconf', label: '1. NETCONF & SSH Setup' },
            { id: 'restconf', label: '2. RESTCONF Setup' },
            { id: 'baseline', label: '3. Compliance Baseline' },
            { id: 'drift', label: '4. Drift Injection Test Script' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTemplateTab(tab.id as any)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
                activeTemplateTab === tab.id
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Code Box */}
        <div className="relative">
          <pre className="bg-gray-950 border border-gray-800/90 rounded-xl p-4 text-xs font-mono text-gray-300 overflow-x-auto whitespace-pre leading-relaxed">
            {activeTemplateTab === 'netconf' && templates?.netconfConfig}
            {activeTemplateTab === 'restconf' && templates?.restconfConfig}
            {activeTemplateTab === 'baseline' && templates?.compliantBaseline}
            {activeTemplateTab === 'drift' && templates?.driftInjectionScript}
          </pre>

          <button
            onClick={() => {
              const text = activeTemplateTab === 'netconf' ? templates?.netconfConfig
                : activeTemplateTab === 'restconf' ? templates?.restconfConfig
                : activeTemplateTab === 'baseline' ? templates?.compliantBaseline
                : templates?.driftInjectionScript;
              if (text) copyToClipboard(text, activeTemplateTab);
            }}
            className="absolute top-3 right-3 px-3 py-1.5 bg-gray-800/90 hover:bg-gray-700 text-gray-200 border border-gray-700 rounded-lg text-xs font-medium flex items-center gap-1.5 backdrop-blur-sm transition"
          >
            {copiedKey === activeTemplateTab ? (
              <>
                <Check className="w-3.5 h-3.5 text-emerald-400" /> Copied!
              </>
            ) : (
              <>
                <Copy className="w-3.5 h-3.5" /> Copy Code
              </>
            )}
          </button>
        </div>
      </div>

      {/* Add Virtual Device Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-sm">
          <div className="bg-gray-900 border border-gray-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl space-y-4">
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Server className="w-5 h-5 text-blue-400" />
              Register Virtual Network Device
            </h3>
            <p className="text-xs text-gray-400">
              Configure connection parameters for your GNS3 or simulated virtual node.
            </p>

            <form onSubmit={handleAddManualVirtualDevice} className="space-y-4 text-xs">
              <div>
                <label className="block text-gray-400 font-medium mb-1">Hostname</label>
                <input
                  type="text"
                  required
                  value={newDevice.name}
                  onChange={(e) => setNewDevice({ ...newDevice, name: e.target.value })}
                  className="w-full bg-gray-950 border border-gray-800 rounded-lg px-3 py-2 text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-gray-400 font-medium mb-1">IP Address</label>
                  <input
                    type="text"
                    required
                    value={newDevice.ipAddress}
                    onChange={(e) => setNewDevice({ ...newDevice, ipAddress: e.target.value })}
                    className="w-full bg-gray-950 border border-gray-800 rounded-lg px-3 py-2 text-white"
                  />
                </div>
                <div>
                  <label className="block text-gray-400 font-medium mb-1">Port</label>
                  <input
                    type="number"
                    value={newDevice.port}
                    onChange={(e) => setNewDevice({ ...newDevice, port: e.target.value })}
                    className="w-full bg-gray-950 border border-gray-800 rounded-lg px-3 py-2 text-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-gray-400 font-medium mb-1">Protocol</label>
                  <select
                    value={newDevice.managementProtocol}
                    onChange={(e) => {
                      const p = e.target.value;
                      const defaultPort = p === 'NETCONF' ? '830' : p === 'RESTCONF' ? '443' : p === 'SSH' ? '22' : '830';
                      setNewDevice({ ...newDevice, managementProtocol: p, port: defaultPort });
                    }}
                    className="w-full bg-gray-950 border border-gray-800 rounded-lg px-3 py-2 text-white"
                  >
                    <option value="NETCONF">NETCONF (port 830)</option>
                    <option value="RESTCONF">RESTCONF (port 443)</option>
                    <option value="SSH">SSH CLI (port 22)</option>
                    <option value="MOCK">MOCK (Simulated)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-gray-400 font-medium mb-1">Device Type</label>
                  <select
                    value={newDevice.deviceType}
                    onChange={(e) => setNewDevice({ ...newDevice, deviceType: e.target.value })}
                    className="w-full bg-gray-950 border border-gray-800 rounded-lg px-3 py-2 text-white"
                  >
                    <option value="ROUTER">ROUTER</option>
                    <option value="SWITCH">SWITCH</option>
                    <option value="FIREWALL">FIREWALL</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-gray-400 font-medium mb-1">Username</label>
                  <input
                    type="text"
                    value={newDevice.username}
                    onChange={(e) => setNewDevice({ ...newDevice, username: e.target.value })}
                    className="w-full bg-gray-950 border border-gray-800 rounded-lg px-3 py-2 text-white"
                  />
                </div>
                <div>
                  <label className="block text-gray-400 font-medium mb-1">Password</label>
                  <input
                    type="password"
                    value={newDevice.password}
                    onChange={(e) => setNewDevice({ ...newDevice, password: e.target.value })}
                    className="w-full bg-gray-950 border border-gray-800 rounded-lg px-3 py-2 text-white"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-gray-800">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="btn-secondary text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={importing}
                  className="btn-primary text-xs"
                >
                  {importing ? 'Registering...' : 'Save Device'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
