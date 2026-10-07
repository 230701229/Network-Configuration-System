import React from 'react';
import { Device } from '../../types';
import { StatusBadge } from '../ui/StatusBadge';
import { Server, Activity, Shield } from 'lucide-react';
import { Link } from 'react-router-dom';
import { deviceApi } from '../../services/deviceApi';
import { ComplianceScore } from '../ui/ComplianceScore';

interface DeviceCardProps {
  device: Device;
}

export const DeviceCard: React.FC<DeviceCardProps> = ({ device }) => {
  const [score, setScore] = React.useState<number | null>(null);

  React.useEffect(() => {
    deviceApi.getDeviceHistory(device.id).then(history => {
      if (history && history.length > 0) {
        setScore(history[0].complianceScore);
      }
    }).catch(() => {});
  }, [device.id]);

  return (
    <Link to={`/devices/${device.id}`} className="block card hover:border-blue-500/50 transition-colors">
      <div className="flex justify-between items-start mb-4">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-gray-800 rounded-lg">
            <Server className="w-6 h-6 text-blue-400" />
          </div>
          <div>
            <h3 className="text-lg font-semibold text-white">{device.hostname}</h3>
            <p className="text-sm text-gray-400">{device.ipAddress}</p>
          </div>
        </div>
        <StatusBadge status={device.status} />
      </div>
      
      <div className="grid grid-cols-2 gap-4 mb-4">
        <div className="bg-gray-900/50 p-3 rounded-lg">
          <p className="text-xs text-gray-500 mb-1">Device Type</p>
          <p className="text-sm text-gray-200">{device.deviceType}</p>
        </div>
        <div className="bg-gray-900/50 p-3 rounded-lg">
          <p className="text-xs text-gray-500 mb-1">Protocol</p>
          <p className="text-sm text-gray-200">{device.managementProtocol}</p>
        </div>
      </div>

      <div className="flex items-center justify-between pt-4 border-t border-gray-700/50">
        <div className="flex items-center gap-2">
          <Shield className="w-4 h-4 text-gray-400" />
          <span className="text-sm text-gray-400">Compliance</span>
        </div>
        {score !== null ? <ComplianceScore score={score} size="sm" /> : <span className="text-sm text-gray-500">No scans</span>}
      </div>
    </Link>
  );
};
