import React, { useEffect, useState } from 'react';
import { aiApi } from '../services/aiApi';
import { AIAnalysis } from '../types';
import { StatusBadge } from '../components/ui/StatusBadge';
import { Sparkles } from 'lucide-react';

export default function AIInsightsPage() {
  const [analyses, setAnalyses] = useState<AIAnalysis[]>([]);

  useEffect(() => {
    aiApi.getAnalyses().then(setAnalyses);
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="bg-purple-500/20 p-2 rounded-lg"><Sparkles className="w-6 h-6 text-purple-400" /></div>
        <div>
          <h1 className="text-2xl font-bold text-white">AI Insights</h1>
          <p className="text-gray-400 text-sm">AI provides explanations only. Compliance is determined by the rule engine.</p>
        </div>
      </div>
      
      <div className="grid gap-4">
        {analyses.map(ai => (
          <div key={ai.id} className="card">
            <div className="flex justify-between mb-4">
              <h3 className="font-bold text-lg text-white">Analysis #{ai.id}</h3>
              <StatusBadge status={ai.priority} />
            </div>
            <p className="text-gray-300">{ai.summary}</p>
          </div>
        ))}
        {analyses.length === 0 && <div className="text-gray-500 text-center py-8">No AI insights available.</div>}
      </div>
    </div>
  );
}
