import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import { useNavigate } from 'react-router-dom';
import Select from 'react-select';
const ActivityLogs = () => {
  const { currentUser, logout } = useAuth();
  const navigate = useNavigate();
  
  const [logs, setLogs] = useState([]);
  const [allUsers, setAllUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    userId: '',
    action: '',
    entityType: '',
    startDate: '',
    endDate: ''
  });
  const [pagination, setPagination] = useState({
    page: 0,
    size: 50,
    totalPages: 1,
    totalElements: 0
  });


  const actionLabels = {
    'LOGIN': '🔐 Login',
    'LOGOUT': '🚪 Logout',
    'PROJECT_CREATED': '📁 Project Created',
    'PROJECT_UPDATED': '✏️ Project Updated',
    'PROJECT_DELETED': '🗑️ Project Deleted',
   'PROJECT_APPROVED': '✅ Project Approved',        
  'PROJECT_REJECTED': '❌ Project Rejected', 
    'TASK_CREATED': '✅ Task Created',
    'TASK_UPDATED': '✏️ Task Updated',
    'TASK_DELETED': '🗑️ Task Deleted',
    'FILE_UPLOADED': '📤 File Uploaded',
    'FILE_DELETED': '🗑️ File Deleted',
    'PASSWORD_CHANGED': '🔑 Password Changed',
    'USER_CREATED': '👤 User Created',
    'USER_UPDATED': '✏️ User Updated',
    'USER_DELETED': '🗑️ User Deleted',
    'USER_DEACTIVATED': '⏸️ User Deactivated',
    'USER_REACTIVATED': '▶️ User Reactivated',

    
  };

  useEffect(() => {
    fetchLogs();
    fetchUsers();
  }, [filters, pagination.page]);

  const fetchUsers = async () => {
    try {
      console.log('📥 Fetching users for dropdown...');
      const response = await api.get('/api/users');
      console.log('✅ Users fetched:', response.data.length);
      setAllUsers(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('❌ Error fetching users:', error);
      setAllUsers([]);
    }
  };

  const fetchLogs = async () => {
    try {
      console.log('📥 Fetching activity logs...');
      
      const params = {
        page: pagination.page,
        size: pagination.size,
        ...(filters.userId && { userId: filters.userId }),
        ...(filters.action && { action: filters.action }),
        ...(filters.entityType && { entityType: filters.entityType }),
        ...(filters.startDate && { startDate: filters.startDate }),
        ...(filters.endDate && { endDate: filters.endDate })
      };
      
      console.log('API params:', params);
      
    const response = await api.get('/api/activity-logs', { params });

console.log('✅ Logs response:', response.data);


const logsData = response.data.content || response.data.logs || [];
setLogs(logsData);

console.log('📊 Logs extracted:', logsData.length);

setPagination(prev => ({
  ...prev,
  totalPages: response.data.totalPages || 1,
  totalElements: response.data.totalElements || 0,
  page: response.data.number || 0  
}));
    } catch (error) {
      console.error('❌ Error fetching logs:', error);
      setLogs([]);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (e) => {
    setFilters({
      ...filters,
      [e.target.name]: e.target.value
    });
    setPagination(prev => ({ ...prev, page: 0 }));
  };

  const handleExport = async () => {
    try {
      const params = {
        ...(filters.userId && { userId: filters.userId }),
        ...(filters.action && { action: filters.action }),
        ...(filters.entityType && { entityType: filters.entityType }),
        ...(filters.startDate && { startDate: filters.startDate }),
        ...(filters.endDate && { endDate: filters.endDate })
      };
      
      const response = await api.get('/api/activity-logs/export', { 
        params,
        responseType: 'blob'
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `activity-logs-${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      
    } catch (error) {
      console.error('❌ Error exporting logs:', error);
      alert('Failed to export logs');
    }
  };

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  const getActionDisplay = (action) => {
    return actionLabels[action] || action.replace(/_/g, ' ');
  };
const getActionBadgeStyle = (action) => {
  switch(action) {
    case 'PROJECT_APPROVED':
      return { backgroundColor: '#d4edda', color: '#155724' };
    case 'PROJECT_REJECTED':
      return { backgroundColor: '#f8d7da', color: '#721c24' };
    default:
      return { backgroundColor: '#e7f3ff', color: '#003366' };
  }
};
  if (loading && logs.length === 0) {
    return (
      <div className="dashboard-container">
        <nav className="navbar">
          <div className="navbar-brand">NIB IT Project Tracking
            <h4>ንብ ኢንተርናሽናል ባንክ</h4>
          </div>
          <div className="navbar-user">
            <span>Welcome, {currentUser?.fullName}</span>
            <button onClick={handleLogout} className="logout-btn">Logout</button>
          </div>
        </nav>
        <div className="loading"><p>Loading activity logs...</p></div>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
     <nav className="navbar">
  <div className="navbar-brand" style={{ 
    display: 'flex', 
    alignItems: 'center', 
    gap: '12px' 
  }}>
    {/* Logo Image */}
    <img 
      src="/nibicon.jpeg" 
      alt="NIB Logo" 
      style={{
        width: '40px',
        height: '40px',
        objectFit: 'contain',
        borderRadius: '6px'
      }}
    />
    
    {/* Text */}
    <div>
      <div style={{ 
        fontSize: '20px', 
        fontWeight: 'bold',
      }}>
        NIB IT Project Tracking
      </div>
      <div style={{ 
        fontSize: '13px',
        opacity: '0.9',
        marginTop: '2px'
      }}>
        ንብ ኢንተርናሽናል ባንክ
      </div>
    </div>
  </div>

  <div className="navbar-user">
    <span>Welcome, {currentUser?.fullName || currentUser?.username}</span>
    <button onClick={handleLogout} className="logout-btn">Logout</button>
  </div>
</nav>

      <div className="main-content">
        <div className="page-header">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <button 
              onClick={() => navigate('/dashboard')}
              className="btn"
              style={{ 
                padding: '8px 15px', 
                background: 'linear-gradient(135deg, #8B4513 0%, #A0522D 100%)',
               color: '#FFD700',
               border: '2px solid #8B4513',
                color: 'white',
                border: 'none',
                borderRadius: '5px',
                cursor: 'pointer'
              }}
            >
              ← Back
            </button>
            <div style={{ textAlign: 'center' }}>
              <h1>Activity Logs</h1>
              <p>Audit trail of user actions</p>
            </div>
            <button 
              onClick={handleExport}
              className="btn btn-primary"
              style={{ padding: '10px 20px' }}
            >
              📥 Export CSV
            </button>
          </div>
        </div>

        {/* Filters */}
        <div className="content-card" style={{ marginBottom: '20px' }}>
          <h3 style={{ marginBottom: '15px' }}>🔍 Filters</h3>
          <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
         
<div style={{ flex: 1, minWidth: '150px' }}>
  <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
    User
  </label>
  <Select
    options={allUsers.map(user => ({
      value: user.id,
      label: `${user.fullName} (${user.username}) - ${user.role?.replace(/_/g, ' ')}`
    }))}
    value={allUsers
      .filter(user => user.id === filters.userId)
      .map(user => ({
        value: user.id,
        label: `${user.fullName} (${user.username}) - ${user.role?.replace(/_/g, ' ')}`
      }))}
    onChange={(selectedOption) => {
      setFilters(prev => ({
        ...prev,
        userId: selectedOption ? selectedOption.value : ''
      }));
      setPagination(prev => ({ ...prev, page: 0 }));
    }}
    isClearable
    placeholder="All Users"
    styles={{
      control: (base) => ({ ...base, borderRadius: '5px', padding: '2px' }),
      menu: (base) => ({ ...base, zIndex: 9999 })
    }}
  />
</div>

{/* Action Filter with react-select */}
<div style={{ flex: 1, minWidth: '150px' }}>
  <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
    Action
  </label>
  <Select
    options={Object.entries(actionLabels).map(([key, label]) => ({
      value: key,
      label
    }))}
    value={Object.entries(actionLabels)
      .filter(([key]) => key === filters.action)
      .map(([key, label]) => ({ value: key, label }))}
    onChange={(selectedOption) => {
      setFilters(prev => ({
        ...prev,
        action: selectedOption ? selectedOption.value : ''
      }));
      setPagination(prev => ({ ...prev, page: 0 }));
    }}
    isClearable
    placeholder="All Actions"
    styles={{
      control: (base) => ({ ...base, borderRadius: '5px', padding: '2px' }),
      menu: (base) => ({ ...base, zIndex: 9999 })
    }}
  />
            </div>
            
            <div style={{ flex: 1, minWidth: '150px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Entity Type
              </label>
              <select 
                name="entityType"
                value={filters.entityType}
                onChange={handleFilterChange}
                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              >
                <option value="">All Types</option>
                <option value="Project">Project</option>
                <option value="Task">Task</option>
                <option value="Attachment">Attachment</option>
                <option value="User">User</option>
                <option value="ProgressUpdate">Progress Update</option> {/* ← ADDED THIS LINE */}
              </select>
            </div>
            
            <div style={{ flex: 1, minWidth: '150px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Start Date
              </label>
              <input
                type="date"
                name="startDate"
                value={filters.startDate}
                onChange={handleFilterChange}
                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              />
            </div>
            
            <div style={{ flex: 1, minWidth: '150px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                End Date
              </label>
              <input
                type="date"
                name="endDate"
                value={filters.endDate}
                onChange={handleFilterChange}
                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              />
            </div>
            
            <div style={{ flex: '0 0 auto', alignSelf: 'flex-end' }}>
              <button 
                onClick={() => {
                  setFilters({ userId: '', action: '', entityType: '', startDate: '', endDate: '' });
                  setPagination(prev => ({ ...prev, page: 0 }));
                }}
                className="btn"
                style={{ backgroundColor: '#6c757d', color: 'white', padding: '10px 20px' }}
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>

        {/* Logs Table - unchanged */}
        <div className="content-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
            <h2>Activity Logs ({pagination.totalElements})</h2>
            <span style={{ fontSize: '14px', color: '#666' }}>
             Page {pagination.page + 1} of {pagination.totalPages}
            </span>
          </div>
          
          {logs.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
              <p style={{ fontSize: '16px' }}>No activity logs found</p>
              <p style={{ fontSize: '14px' }}>
                {loading ? 'Loading...' : 'Try adjusting your filters or perform some actions to generate logs'}
              </p>
              <div style={{ marginTop: '20px', padding: '15px', backgroundColor: '#f8f9fa', borderRadius: '5px', fontSize: '13px' }}>
                <strong>💡 Tip:</strong> Activity logs are generated when you:
                <ul style={{ marginTop: '10px', textAlign: 'left', paddingLeft: '20px' }}>
                  <li>Login/Logout</li>
                  <li>Create/Update/Delete Projects</li>
                  <li>Create/Update/Delete Tasks</li>
                  <li>Upload/Delete Files</li>
                  <li>Change Passwords</li>
                  <li>Create/Update Users</li>
                  
                </ul>
              </div>
            </div>
          ) : (
            <>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>User</th>
                    <th>Action</th>
                    <th>Entity</th>
                    <th>Details</th>
                    <th>IP Address</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((log) => (
                    <tr key={log.id}>
                      <td style={{ fontSize: '13px', whiteSpace: 'nowrap' }}>
                        {formatDate(log.timestamp)}
                      </td>
                      <td style={{ fontWeight: '600' }}>
                        {log.user?.username || log.user?.fullName || 'Unknown'}
                      </td>
            <td>
                        <span style={{ 
                          padding: '3px 8px', 
                          borderRadius: '4px', 
                          backgroundColor: '#e7f3ff',
                          color: '#003366',
                          fontSize: '12px',
                          fontWeight: '500'
                        }}>
                          {getActionDisplay(log.action)}
                        </span>
                      </td>
                      <td>
                        {log.entityType && log.entityId ? (
                          <span style={{ fontSize: '13px' }}>
                            {log.entityType} #{log.entityId}
                          </span>
                        ) : (
                          <span style={{ color: '#999', fontSize: '13px' }}>—</span>
                        )}
                      </td>
                      <td style={{ fontSize: '13px', maxWidth: '300px', wordBreak: 'break-word' }}>
                        {log.details || '—'}
                      </td>
                      <td style={{ fontSize: '12px', color: '#666' }}>
                        {log.ipAddress || '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              
              {/* Pagination */}
              {pagination.totalPages > 1 && (
                <div style={{ display: 'flex', justifyContent: 'center', gap: '10px', marginTop: '20px' }}>
                  <button
                    onClick={() => setPagination(prev => ({ ...prev, page: Math.max(0, prev.page - 1) }))}
                    disabled={pagination.page === 0}
                    className="btn"
                    style={{ 
                      padding: '8px 15px',
                      backgroundColor: pagination.page === 0 ? '#ccc' : '#003366',
                      color: 'white',
                      border: 'none',
                      borderRadius: '5px',
                      cursor: pagination.page === 0 ? 'not-allowed' : 'pointer'
                    }}
                  >
                    ← Previous
                  </button>
                  
                  <span style={{ padding: '8px 15px', fontSize: '14px' }}>
                    Page {pagination.page + 1} of {pagination.totalPages}
                  </span>
                  
                  <button
                    onClick={() => setPagination(prev => ({ ...prev, page: Math.min(pagination.totalPages - 1, prev.page + 1) }))}
                    disabled={pagination.page >= pagination.totalPages - 1}
                    className="btn"
                    style={{ 
                      padding: '8px 15px',
                      backgroundColor: pagination.page >= pagination.totalPages - 1 ? '#ccc' : '#003366',
                      color: 'white',
                      border: 'none',
                      borderRadius: '5px',
                      cursor: pagination.page >= pagination.totalPages - 1 ? 'not-allowed' : 'pointer'
                    }}
                  >
                    Next →
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default ActivityLogs;