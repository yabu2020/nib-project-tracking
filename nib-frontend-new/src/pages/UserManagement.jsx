import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import { useNavigate } from 'react-router-dom';

const UserManagement = () => {
  const { currentUser, logout } = useAuth();
  const navigate = useNavigate();
  
  const [users, setUsers] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [filterRole, setFilterRole] = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterDepartment, setFilterDepartment] = useState('all');
  const [showEditModal, setShowEditModal] = useState(false);
  const [editFormData, setEditFormData] = useState({
    id: null,
    username: '',
    fullName: '',
    email: '',
    role: 'DEVELOPER',
    department: '',
    active: true
  });
  
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    fullName: '',
    email: '',
    role: 'DEVELOPER',
    department: ''
  });

  const [passwordData, setPasswordData] = useState({
    newPassword: '',
    confirmPassword: ''
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [usersRes, statsRes] = await Promise.all([
        api.get('/api/users', {
          headers: { 'X-User-Id': currentUser?.id }
        }),
        api.get('/api/users/stats', {
          headers: { 'X-User-Id': currentUser?.id }
        })
      ]);
      
      let usersData = usersRes.data;
      if (typeof usersData === 'string') {
        usersData = JSON.parse(usersData);
      }
      
      setUsers(Array.isArray(usersData) ? usersData : []);
      setStats(statsRes.data);
    } catch (error) {
      console.error('Error fetching ', error);
      setUsers([]);
      setStats(null);
    } finally {
      setLoading(false);
    }
  };

  const handleBackToDashboard = () => {
    navigate('/dashboard');
  };

  const handleInputChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handlePasswordChange = (e) => {
    setPasswordData({
      ...passwordData,
      [e.target.name]: e.target.value
    });
  };


  const handleEditInputChange = (e) => {
    setEditFormData({
      ...editFormData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await api.post('/api/users', formData, {
        headers: { 'X-User-Id': currentUser?.id }
      });
      const newUser = response.data;
      
      alert('User created successfully!');
      setShowModal(false);
      fetchData();
      setFormData({
        username: '',
        password: '',
        fullName: '',
        email: '',
        role: 'DEVELOPER',
        department: ''
      });
    } catch (error) {
      alert('Error creating user: ' + (error.response?.data?.error || 'Unknown error'));
    }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    try {
      const { id, ...updateData } = editFormData;
      await api.put(`/api/users/${id}`, updateData, {
        headers: { 'X-User-Id': currentUser?.id }
      });
      
      alert('User updated successfully!');
      setShowEditModal(false);
      fetchData();
    } catch (error) {
      alert('Error updating user: ' + (error.response?.data?.error || 'Unknown error'));
    }
  };

  
  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      alert('Passwords do not match!');
      return;
    }
    try {
      await api.put(`/api/users/${selectedUser.id}/password`, { 
        password: passwordData.newPassword 
      }, {
        headers: { 'X-User-Id': currentUser?.id }
      });
      
      alert('Password updated successfully!');
      setShowPasswordModal(false);
      setPasswordData({ newPassword: '', confirmPassword: '' });
      setSelectedUser(null);
    } catch (error) {
      alert('Error updating password: ' + (error.response?.data?.error || 'Unknown error'));
    }
  };


  const handleEditClick = (user) => {
    setEditFormData({
      id: user.id,
      username: user.username || '',
      fullName: user.fullName || '',
      email: user.email || '',
      role: user.role || 'DEVELOPER',
      department: user.department || '',
      active: user.active !== false
    });
    setShowEditModal(true);
  };

  const handleDeactivate = async (userId) => {
    if (!window.confirm('Are you sure you want to deactivate this user?')) return;
    try {
      await api.put(`/api/users/${userId}/deactivate`, {}, {
        headers: { 'X-User-Id': currentUser?.id }
      });
      fetchData();
      alert('User deactivated successfully!');
    } catch (error) {
      alert('Error deactivating user: ' + (error.response?.data?.error || 'Unknown error'));
    }
  };

  const handleReactivate = async (userId) => {
    try {
      await api.put(`/api/users/${userId}/reactivate`, {}, {
        headers: { 'X-User-Id': currentUser?.id }
      });
      fetchData();
      alert('User reactivated successfully!');
    } catch (error) {
      alert('Error reactivating user: ' + (error.response?.data?.error || 'Unknown error'));
    }
  };


  const handleDelete = async (userId) => {
    if (!window.confirm('Are you sure you want to permanently delete this user? This cannot be undone!')) return;
    try {
      await api.delete(`/api/users/${userId}`, {
        headers: { 'X-User-Id': currentUser?.id }
      });
      fetchData();
      alert('User deleted successfully!');
    } catch (error) {
      alert('Error deleting user: ' + (error.response?.data?.error || 'Unknown error'));
    }
  };

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  const getRoleColor = (role) => {
    const colors = {
      'CEO': '#003366',
      'DEPUTY_CHIEF': '#004532',
      'DIRECTOR': '#001234',
      'BUSINESS': '#004567',
      'QUALITY_ASSURANCE': '#137654',
      'DIGITAL_BANKING_MANAGER': '#0099ff',
      'PROJECT_MANAGER': '#6f42c1',
      'SENIOR_IT_OFFICER': '#28a745',
      'JUNIOR_IT_OFFICER': '#17a2b8',
      'IT_GRADUATE_TRAINEE': '#ffc107',
      'DEVELOPER': '#20c997'
    };
    return colors[role] || '#6c757d';
  };

 
  const filteredUsers = (Array.isArray(users) ? users : []).filter(u => {
    if (filterRole !== 'all' && u.role !== filterRole) return false;
    if (filterStatus !== 'all' && u.active !== (filterStatus === 'active')) return false;
    if (filterDepartment !== 'all' && u.department !== filterDepartment) return false;
    return true;
  });

  if (loading) {
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
        <div className="loading"><p>Loading users...</p></div>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      {/* Navigation */}
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
        {/* Header */}
        <div className="page-header">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <button 
              onClick={handleBackToDashboard}
              className="btn"
              style={{ 
                padding: '8px 15px', 
                background: 'linear-gradient(135deg, #8B4513 0%, #A0522D 100%)',
               color: '#FFD700',
               border: '2px solid #8B4513',
                color: 'white',
                border: 'none',
                borderRadius: '5px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '5px'
              }}
            >
               ← Back
            </button>
            <div>
              <h1>User Management</h1>
              <p>Manage system users and access control</p>
            </div>
            <button 
              className="btn btn-primary" 
              onClick={() => setShowModal(true)}
              style={{ padding: '12px 24px', fontSize: '14px' }}
            >
              + New User
            </button>
          </div>
        </div>

        {/* STATS */}
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-label">Total Users</div>
            <div className="stat-value">{stats?.totalUsers || 0}</div>
          </div>
          <div className="stat-card green">
            <div className="stat-label">Active Users</div>
            <div className="stat-value">{stats?.activeUsers || 0}</div>
          </div>
          <div className="stat-card red">
            <div className="stat-label">Inactive Users</div>
            <div className="stat-value">{stats?.inactiveUsers || 0}</div>
          </div>
        </div>

        {/* Filters */}
        <div className="content-card" style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: '150px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Filter by Role
              </label>
              <select 
                value={filterRole}
                onChange={(e) => setFilterRole(e.target.value)}
                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              >
                <option value="all">All Roles</option>
                <option value="CEO">CEO</option>
                <option value="DEPUTY_CHIEF">Deputy Chief</option>
                <option value="DIRECTOR">Director</option>
                <option value="BUSINESS">Business</option>
                <option value="QUALITY_ASSURANCE">Quality Assurance</option>
                <option value="DIGITAL_BANKING_MANAGER">Technical Digital Banking Manager</option>
                <option value="PROJECT_MANAGER">Project Manager</option>
                <option value="SENIOR_IT_OFFICER">Senior IT Officer</option>
                <option value="JUNIOR_IT_OFFICER">Junior IT Officer</option>
                <option value="IT_GRADUATE_TRAINEE">IT Graduate Trainee</option>
                <option value="DEVELOPER">Developer</option>
              </select>
            </div>
            
            <div style={{ flex: 1, minWidth: '150px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Filter by Status
              </label>
              <select 
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              >
                <option value="all">All Status</option>
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>

            <div style={{ flex: '0 0 auto' }}>
              <button 
                className="btn btn-primary"
                onClick={() => { setFilterRole('all'); setFilterStatus('all'); setFilterDepartment('all'); }}
                style={{ marginTop: '28px' }}
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>

        {/* Users Table */}
        <div className="content-card">
          <h2>All Users ({filteredUsers.length})</h2>
          
          {filteredUsers.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
              <p style={{ fontSize: '16px', marginBottom: '10px' }}>No users found</p>
              <p style={{ fontSize: '14px' }}>Click "+ New User" to add your first user</p>
            </div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Full Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td style={{ fontWeight: '600' }}>{user.username}</td>
                    <td>{user.fullName}</td>
                    <td>{user.email}</td>
                    <td>
                      <span 
                        className="badge"
                        style={{
                          backgroundColor: getRoleColor(user.role),
                          color: 'white',
                          fontSize: '11px',
                          padding: '5px 10px'
                        }}
                      >
                        {user.role?.replace(/_/g, ' ')}
                      </span>
                    </td>
                    
                    <td>
                      <span className={`badge ${user.active ? 'badge-green' : 'badge-red'}`}>
                        {user.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                   <td>
                      <div style={{ display: 'flex', gap: '5px' }}>
                        {/* Edit Button */}
                        <button 
                          className="btn btn-primary"
                          style={{ 
                            padding: '5px 10px', 
                            fontSize: '11px', 
                            backgroundColor: '#17a2b8',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer'
                          }}
                          onClick={() => handleEditClick(user)}
                          title="Edit User"
                        >
                          ✏️ 
                        </button>
                        
                        {/* Reset Password Button */}
                        <button 
                          className="btn btn-primary"
                          style={{ 
                            padding: '5px 10px', 
                            fontSize: '11px',
                            backgroundColor: '#007bff',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer'
                          }}
                          onClick={() => { setSelectedUser(user); setShowPasswordModal(true); }}
                          title="Reset Password"
                        >
                          🔑 
                        </button>
                        
                        {/* Deactivate/Reactivate Button */}
                        {user.active ? (
                          <button 
                            className="btn"
                            style={{ 
                              padding: '5px 10px', 
                              fontSize: '11px', 
                              backgroundColor: '#ffc107',
                              color: '#000',
                              border: 'none',
                              borderRadius: '4px',
                              cursor: 'pointer'
                            }}
                            onClick={() => handleDeactivate(user.id)}
                            title="Deactivate"
                          >
                            ⏸️ 
                          </button>
                        ) : (
                          <button 
                            className="btn"
                            style={{ 
                              padding: '5px 10px', 
                              fontSize: '11px', 
                              backgroundColor: '#28a745', 
                              color: 'white',
                              border: 'none',
                              borderRadius: '4px',
                              cursor: 'pointer'
                            }}
                            onClick={() => handleReactivate(user.id)}
                            title="Reactivate"
                          >
                            ▶️ 
                          </button>
                        )}
                        
                        {/* Delete Button */}
                        <button 
                          className="btn btn-danger"
                          style={{ 
                            padding: '5px 10px', 
                            fontSize: '11px',
                            backgroundColor: '#dc3545',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer'
                          }}
                          onClick={() => handleDelete(user.id)}
                          title="Delete"
                        >
                          🗑️ Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Create User Modal */}
      {showModal && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white',
            padding: '30px',
            borderRadius: '10px',
            width: '100%',
            maxWidth: '500px',
            maxHeight: '90vh',
            overflow: 'auto'
          }}>
            <h2 style={{ marginBottom: '20px', color: '#003366' }}>Create New User</h2>
            
            <form onSubmit={handleSubmit}>
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Username *</label>
                <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Password *</label>
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  required
                  minLength="6"
                />
              </div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Full Name *</label>
                <input
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Email *</label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Role *</label>
                <select
                  name="role"
                  value={formData.role}
                  onChange={handleInputChange}
                  required
                  style={{
                    padding: '12px 15px',
                    border: '1px solid #ddd',
                    borderRadius: '5px',
                    fontSize: '14px',
                    width: '100%'
                  }}
                >
                  <option value="CEO">CEO</option>
                <option value="DEPUTY_CHIEF">Deputy Chief</option>
                <option value="DIRECTOR">Director</option>
                <option value="BUSINESS">Business</option>
                <option value="QUALITY_ASSURANCE">Quality Assurance</option>
                  <option value="DIGITAL_BANKING_MANAGER">Technical Digital Banking Manager</option>
                  <option value="PROJECT_MANAGER">Project Manager</option>
                  <option value="SENIOR_IT_OFFICER">Senior IT Officer</option>
                  <option value="JUNIOR_IT_OFFICER">Junior IT Officer</option>
                  <option value="IT_GRADUATE_TRAINEE">IT Graduate Trainee</option>
                  <option value="DEVELOPER">Developer</option>
                </select>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                  Create User
                </button>
                <button 
                  type="button" 
                  className="btn"
                  style={{ flex: 1, backgroundColor: '#6c757d', color: 'white' }}
                  onClick={() => setShowModal(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reset Password Modal */}
      {showPasswordModal && selectedUser && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white',
            padding: '30px',
            borderRadius: '10px',
            width: '100%',
            maxWidth: '400px'
          }}>
            <h2 style={{ marginBottom: '20px', color: '#003366' }}>Reset Password</h2>
            <p style={{ marginBottom: '20px', color: '#666' }}>
              User: <strong>{selectedUser.username}</strong>
            </p>
            
            <form onSubmit={handlePasswordSubmit}>
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>New Password *</label>
                <input
                  type="password"
                  name="newPassword"
                  value={passwordData.newPassword}
                  onChange={handlePasswordChange}
                  required
                  minLength="6"
                />
              </div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Confirm Password *</label>
                <input
                  type="password"
                  name="confirmPassword"
                  value={passwordData.confirmPassword}
                  onChange={handlePasswordChange}
                  required
                  minLength="6"
                />
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                  Update Password
                </button>
                <button 
                  type="button" 
                  className="btn"
                  style={{ flex: 1, backgroundColor: '#6c757d', color: 'white' }}
                  onClick={() => { 
                    setShowPasswordModal(false); 
                    setSelectedUser(null); 
                    setPasswordData({ newPassword: '', confirmPassword: '' }); 
                  }}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit User Modal */}
      {showEditModal && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white',
            padding: '30px',
            borderRadius: '10px',
            width: '100%',
            maxWidth: '500px',
            maxHeight: '90vh',
            overflow: 'auto'
          }}>
            <h2 style={{ marginBottom: '20px', color: '#003366' }}>Edit User</h2>
            
            <form onSubmit={handleEditSubmit}>
              {/* Username (disabled - not editable) */}
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Username</label>
                <input
                  type="text"
                  value={editFormData.username}
                  disabled
                  style={{
                    padding: '12px 15px',
                    border: '1px solid #ddd',
                    borderRadius: '5px',
                    fontSize: '14px',
                    width: '100%',
                    backgroundColor: '#f8f9fa',
                    cursor: 'not-allowed'
                  }}
                />
                <small style={{ color: '#666', fontSize: '11px' }}>Username cannot be changed</small>
              </div>

              {/* Full Name */}
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Full Name *</label>
                <input
                  type="text"
                  name="fullName"
                  value={editFormData.fullName}
                  onChange={handleEditInputChange}
                  required
                  style={{
                    padding: '12px 15px',
                    border: '1px solid #ddd',
                    borderRadius: '5px',
                    fontSize: '14px',
                    width: '100%'
                  }}
                />
              </div>

              {/* Email */}
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Email *</label>
                <input
                  type="email"
                  name="email"
                  value={editFormData.email}
                  onChange={handleEditInputChange}
                  required
                  style={{
                    padding: '12px 15px',
                    border: '1px solid #ddd',
                    borderRadius: '5px',
                    fontSize: '14px',
                    width: '100%'
                  }}
                />
              </div>

              {/* Role */}
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Role *</label>
                <select
                  name="role"
                  value={editFormData.role}
                  onChange={handleEditInputChange}
                  required
                  style={{
                    padding: '12px 15px',
                    border: '1px solid #ddd',
                    borderRadius: '5px',
                    fontSize: '14px',
                    width: '100%'
                  }}
                >
                  <option value="CEO">CEO</option>
                   <option value="DEPUTY_CHIEF">Deputy Chief</option>
                <option value="DIRECTOR">Director</option>
                <option value="BUSINESS">Business</option>
                <option value="QUALITY_ASSURANCE">Quality Assurance</option>
                  <option value="DIGITAL_BANKING_MANAGER">Technical Digital Banking Manager</option>
                  <option value="PROJECT_MANAGER">Project Manager</option>
                  <option value="SENIOR_IT_OFFICER">Senior IT Officer</option>
                  <option value="JUNIOR_IT_OFFICER">Junior IT Officer</option>
                  <option value="IT_GRADUATE_TRAINEE">IT Graduate Trainee</option>
                  <option value="DEVELOPER">Developer</option>
                </select>
              </div>

              {/* Active Status Toggle */}
              <div className="form-group" style={{ marginBottom: '20px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    name="active"
                    checked={editFormData.active}
                    onChange={(e) => setEditFormData({ ...editFormData, active: e.target.checked })}
                    style={{ width: '18px', height: '18px' }}
                  />
                  <span style={{ fontSize: '14px', fontWeight: '500' }}>User is Active</span>
                </label>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                  Save Changes
                </button>
                <button 
                  type="button" 
                  className="btn"
                  style={{ flex: 1, backgroundColor: '#6c757d', color: 'white' }}
                  onClick={() => setShowEditModal(false)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default UserManagement;