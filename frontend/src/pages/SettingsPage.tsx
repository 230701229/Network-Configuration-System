import React from 'react';

export default function SettingsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-white">System Settings</h1>
      <div className="card space-y-4">
        <div>
          <h3 className="font-semibold text-white">Scheduled Scans</h3>
          <p className="text-sm text-gray-400">Configure auto-scan interval.</p>
        </div>
        <hr className="border-gray-800" />
        <div>
          <h3 className="font-semibold text-white">AI Services</h3>
          <p className="text-sm text-gray-400">Configure AI endpoints and models.</p>
        </div>
      </div>
    </div>
  );
}
