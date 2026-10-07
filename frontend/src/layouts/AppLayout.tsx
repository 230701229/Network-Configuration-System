import React from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  LayoutDashboard, Server, Shield, Code, CheckCircle, 
  AlertTriangle, History, Sparkles, Bell, Settings, LogOut, Activity, Network 
} from 'lucide-react';
import { clsx } from 'clsx';

export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = [
    { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/devices', icon: Server, label: 'Devices' },
    { to: '/virtual-lab', icon: Network, label: 'Virtual Lab (GNS3)' },
    { to: '/policies', icon: Shield, label: 'Policies' },
    { to: '/yang-models', icon: Code, label: 'YANG Models' },
    { to: '/compliance', icon: CheckCircle, label: 'Compliance' },
    { to: '/drift', icon: AlertTriangle, label: 'Drift Events' },
    { to: '/history', icon: History, label: 'History' },
    { to: '/ai-insights', icon: Sparkles, label: 'AI Insights' },
    { to: '/alerts', icon: Bell, label: 'Alerts' },
    { to: '/settings', icon: Settings, label: 'Settings' }
  ];

  return (
    <div className="min-h-screen flex bg-gray-950 text-gray-100">
      {/* Sidebar */}
      <aside className="w-64 bg-gray-900 border-r border-gray-800 flex flex-col hidden md:flex">
        <div className="p-6 flex items-center gap-3 border-b border-gray-800">
          <div className="bg-blue-600 p-2 rounded-lg">
            <Activity className="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 className="font-bold text-lg leading-tight">NetCopilot</h1>
            <p className="text-xs text-blue-400">Compliance & Drift</p>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto p-4 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => clsx('sidebar-link', isActive && 'active')}
            >
              <item.icon className="w-5 h-5" />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-gray-800">
          <div className="flex items-center gap-3 mb-4 px-2">
            <div className="w-8 h-8 bg-gray-800 rounded-full flex items-center justify-center text-sm font-bold text-gray-300">
              {user?.username?.[0]?.toUpperCase()}
            </div>
            <div>
              <p className="text-sm font-medium">{user?.username}</p>
              <p className="text-xs text-gray-500">{user?.role}</p>
            </div>
          </div>
          <button onClick={handleLogout} className="w-full sidebar-link text-red-400 hover:text-red-300 hover:bg-red-500/10">
            <LogOut className="w-5 h-5" />
            Logout
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <div className="flex-1 overflow-y-auto p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
