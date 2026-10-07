import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import AppLayout from './layouts/AppLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import DevicesPage from './pages/DevicesPage';
import DeviceDetailsPage from './pages/DeviceDetailsPage';
import PoliciesPage from './pages/PoliciesPage';
import PolicyDetailsPage from './pages/PolicyDetailsPage';
import CompliancePage from './pages/CompliancePage';
import DriftPage from './pages/DriftPage';
import HistoryPage from './pages/HistoryPage';
import AIInsightsPage from './pages/AIInsightsPage';
import AlertsPage from './pages/AlertsPage';
import YangModelsPage from './pages/YangModelsPage';
import SettingsPage from './pages/SettingsPage';
import VirtualLabPage from './pages/VirtualLabPage';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="devices" element={<DevicesPage />} />
            <Route path="devices/:id" element={<DeviceDetailsPage />} />
            <Route path="virtual-lab" element={<VirtualLabPage />} />
            <Route path="policies" element={<PoliciesPage />} />
            <Route path="policies/:id" element={<PolicyDetailsPage />} />
            <Route path="compliance" element={<CompliancePage />} />
            <Route path="drift" element={<DriftPage />} />
            <Route path="history" element={<HistoryPage />} />
            <Route path="ai-insights" element={<AIInsightsPage />} />
            <Route path="alerts" element={<AlertsPage />} />
            <Route path="yang-models" element={<YangModelsPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
