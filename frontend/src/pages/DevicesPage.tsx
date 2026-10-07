import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { deviceApi } from '../services/deviceApi';
import { Device, DeviceType, ManagementProtocol, DeviceStatus } from '../types';
import { DataTable } from '../components/ui/DataTable';
import { StatusBadge } from '../components/ui/StatusBadge';
import { LoadingSpinner } from '../components/ui/LoadingSpinner';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { Modal } from '../components/ui/Modal';
import { Plus, Search, Filter } from 'lucide-react';

export default function DevicesPage() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const navigate = useNavigate();

  const loadDevices = async () => {
    try {
      setLoading(true);
      const data = await deviceApi.getDevices();
      setDevices(data);
      setError('');
    } catch (err) {
      setError('Failed to load devices');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDevices();
  }, []);

  const filteredDevices = devices.filter(d => 
    d.hostname.toLowerCase().includes(search.toLowerCase()) || 
    d.ipAddress.includes(search)
  );

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-white">Network Devices</h1>
          <p className="text-gray-400">Manage and monitor network inventory</p>
        </div>
        <button onClick={() => setIsAddModalOpen(true)} className="btn-primary">
          <Plus className="w-4 h-4" /> Add Device
        </button>
      </div>

      <div className="card">
        <div className="flex gap-4 mb-6">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500" />
            <input
              type="text"
              placeholder="Search by hostname or IP..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input pl-10"
            />
          </div>
          <button className="btn-secondary">
            <Filter className="w-4 h-4" /> Filter
          </button>
        </div>

        {loading ? <LoadingSpinner /> : error ? <ErrorMessage message={error} onRetry={loadDevices} /> : (
          <DataTable
            data={filteredDevices}
            columns={[
              { header: 'Hostname', accessorKey: 'hostname' },
              { header: 'IP Address', accessorKey: 'ipAddress' },
              { header: 'Type', accessorKey: 'deviceType' },
              { header: 'Vendor', cell: (d) => d.vendor || '-' },
              { header: 'Protocol', accessorKey: 'managementProtocol' },
              { header: 'Status', cell: (d) => <StatusBadge status={d.status} /> },
            ]}
            onRowClick={(device) => navigate(`/devices/${device.id}`)}
          />
        )}
      </div>

      <Modal isOpen={isAddModalOpen} onClose={() => setIsAddModalOpen(false)} title="Add New Device">
        <AddDeviceForm onSuccess={() => { setIsAddModalOpen(false); loadDevices(); }} />
      </Modal>
    </div>
  );
}

function AddDeviceForm({ onSuccess }: { onSuccess: () => void }) {
  const [formData, setFormData] = useState({
    hostname: '', ipAddress: '', deviceType: 'ROUTER' as DeviceType,
    managementProtocol: 'NETCONF' as ManagementProtocol, username: '', password: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await deviceApi.createDevice(formData);
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to add device');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && <div className="text-red-500 text-sm">{error}</div>}
      <div className="grid grid-cols-2 gap-4">
        <div><label className="label">Hostname</label><input required className="input" value={formData.hostname} onChange={e => setFormData({...formData, hostname: e.target.value})} /></div>
        <div><label className="label">IP Address</label><input required className="input" value={formData.ipAddress} onChange={e => setFormData({...formData, ipAddress: e.target.value})} /></div>
        <div><label className="label">Type</label><select className="input" value={formData.deviceType} onChange={e => setFormData({...formData, deviceType: e.target.value as DeviceType})}><option value="ROUTER">Router</option><option value="SWITCH">Switch</option><option value="FIREWALL">Firewall</option></select></div>
        <div><label className="label">Protocol</label><select className="input" value={formData.managementProtocol} onChange={e => setFormData({...formData, managementProtocol: e.target.value as ManagementProtocol})}><option value="NETCONF">NETCONF</option><option value="RESTCONF">RESTCONF</option><option value="MOCK">MOCK</option></select></div>
        <div><label className="label">Username</label><input className="input" value={formData.username} onChange={e => setFormData({...formData, username: e.target.value})} /></div>
        <div><label className="label">Password</label><input type="password" className="input" value={formData.password} onChange={e => setFormData({...formData, password: e.target.value})} /></div>
      </div>
      <div className="flex justify-end pt-4"><button type="submit" disabled={loading} className="btn-primary">{loading ? 'Adding...' : 'Add Device'}</button></div>
    </form>
  );
}
