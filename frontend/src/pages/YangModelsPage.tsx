import React, { useEffect, useState } from 'react';
import { yangApi } from '../services/yangApi';
import { YangModel } from '../types';
import { DataTable } from '../components/ui/DataTable';

export default function YangModelsPage() {
  const [models, setModels] = useState<YangModel[]>([]);

  useEffect(() => {
    yangApi.getYangModels().then(setModels);
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-white">YANG Models</h1>
      <div className="card">
        <DataTable
          data={models}
          columns={[
            { header: 'Model Name', accessorKey: 'modelName' },
            { header: 'Namespace', accessorKey: 'namespace' },
            { header: 'Device Type', accessorKey: 'deviceType' },
            { header: 'Valid', cell: (m) => m.isValid ? 'Yes' : 'No' }
          ]}
        />
      </div>
    </div>
  );
}
