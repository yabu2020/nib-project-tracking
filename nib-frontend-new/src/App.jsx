import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Projects from './pages/Projects';
import Tasks from './pages/Tasks';
import ProgressUpdates from './pages/ProgressUpdates';
import Reports from './pages/Reports';
import Milestones from './pages/Milestones';
import ApiLifecycle from './pages/ApiLifecycle';
import UserManagement from './pages/UserManagement';
import ActivityLogs from './pages/ActivityLogs';
import Notifications from './pages/Notifications';
import ResetPassword from './pages/ResetPassword';
import CompletedProjects from './components/CompletedProjects';

import './styles/App.css';


const ProtectedRoute = ({ children }) => {
  const { currentUser, loading } = useAuth();

  if (loading) {
    return <div>Loading authentication...</div>;
  }

  if (!currentUser) {
    return <Navigate to="/login" replace />;
  }

  return children;
};


const ManagerRoute = ({ children }) => {
  const { currentUser, loading } = useAuth();

  if (loading) {
    return <div>Loading authentication...</div>;
  }

  if (!currentUser) {
    return <Navigate to="/login" replace />;
  }

  const managerRoles = [
    'CEO',
    'DEPUTY_CHIEF',
    'DIRECTOR',
    'BUSINESS',
    'QUALITY_ASSURANCE',
    'PROJECT_MANAGER',
    'CORE_BANKING_MANAGER',
    'DIGITAL_BANKING_MANAGER'
  ];

  const isManager = managerRoles.includes(currentUser.role);

  if (!isManager) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
};

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          {/* Public routes - no login required */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/completed-projects" element={<CompletedProjects />} />
          {/* Protected routes - require login */}
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          } />

          <Route path="/projects" element={
            <ProtectedRoute>
              <Projects />
            </ProtectedRoute>
          } />

          <Route path="/tasks" element={
            <ProtectedRoute>
              <Tasks />
            </ProtectedRoute>
          } />

          <Route path="/progress-updates" element={
            <ProtectedRoute>
              <ProgressUpdates />
            </ProtectedRoute>
          } />

          <Route path="/reports" element={
            <ProtectedRoute>
              <Reports />
            </ProtectedRoute>
          } />

          <Route path="/milestones" element={
            <ProtectedRoute>
              <Milestones />
            </ProtectedRoute>
          } />

          <Route path="/apis" element={
            <ProtectedRoute>
              <ApiLifecycle />
            </ProtectedRoute>
          } />

          <Route path="/notifications" element={
            <ProtectedRoute>
              <Notifications />
            </ProtectedRoute>
          } />

          {/* Manager-only routes */}
          <Route path="/users" element={
            <ManagerRoute>
              <UserManagement />
            </ManagerRoute>
          } />

          <Route path="/activity-logs" element={
            <ManagerRoute>
              <ActivityLogs />
            </ManagerRoute>
          } />

          {/* Root path - redirect to dashboard if logged in, otherwise to login */}
          <Route path="/" element={
            <ProtectedRoute>
              <Navigate to="/dashboard" replace />
            </ProtectedRoute>
          } />

          {/* Catch-all route - redirect to dashboard or login */}
          <Route path="*" element={
            <ProtectedRoute>
              <Navigate to="/dashboard" replace />
            </ProtectedRoute>
          } />
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;