import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import { useNavigate } from 'react-router-dom'; 
import Select from "react-select";
const Milestones = () => {
  const { currentUser, logout } = useAuth();
  const [milestones, setMilestones] = useState([]);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [filterProject, setFilterProject] = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');
  const navigate = useNavigate(); 
  
  const [formData, setFormData] = useState({
    milestoneName: '',
    description: '',
    projectId: '',
    targetDate: '',
    completionPercentage: 0
  });

 useEffect(() => {
  fetchData();  
}, []);

useEffect(() => {
  if (!Array.isArray(projects)) {
    setProjects([]);
  }
}, [projects]);

const fetchData = async () => {
  console.log('=== FETCHING DATA ===');
  try {
    console.log('Fetching milestones...');
    const milestonesRes = await api.get('/api/milestones'); 
    console.log('Milestones response:', milestonesRes);
    console.log('Milestones data:', milestonesRes.data);

    console.log('Fetching projects...');
    const projectsRes = await api.get('/api/projects');
    console.log('Projects response:', projectsRes);
    console.log('Projects data:', projectsRes.data);

 
    const milestonesData = Array.isArray(milestonesRes.data) ? milestonesRes.data : [];
    const projectsData = Array.isArray(projectsRes.data) ? projectsRes.data : [];

    setMilestones(milestonesData);
    setProjects(projectsData);

    console.log('State updated - Milestones:', milestonesData);
    console.log('State updated - Projects:', projectsData);

  } catch (error) {
    console.error('❌ Error fetching data:', error);
    console.error('Error response:', error.response);
    console.error('Error message:', error.message);

    setMilestones([]);
    setProjects([]);
  } finally {
    console.log('✅ Setting loading to false');
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

const handleSubmit = async (e) => {
  e.preventDefault();
  try {
    
    await api.post('/api/milestones', formData);
    alert('Milestone created successfully!');
    setShowModal(false);
    fetchData();
    setFormData({
      milestoneName: '',
      description: '',
      projectId: '',
      targetDate: '',
      completionPercentage: 0
    });
  } catch (error) {
    alert('Error creating milestone: ' + (error.response?.data?.error || 'Unknown error'));
  }
};

const handleStatusChange = async (milestoneId, newStatus) => {
  try {
    
    const milestone = milestones.find(m => m.id === milestoneId);
    const currentProgress = milestone?.completionPercentage || 0;

   
    let progressValue = currentProgress;

    switch (newStatus) {
      case 'PLANNED':
        progressValue = 0;
        break;
      case 'IN_PROGRESS':
        if (milestone?.status === 'PLANNED') {
          progressValue = 25;
        }
        break;
      case 'DELAYED':
        progressValue = currentProgress > 0 ? currentProgress : 25;
        break;
      case 'COMPLETED':
        progressValue = 100;
        break;
      default:
        progressValue = currentProgress;
    }

    
    await api.put(`/api/milestones/${milestoneId}/status`, { status: newStatus });

    
    if (progressValue !== currentProgress) {
      await api.put(`/api/milestones/${milestoneId}/progress`, { completionPercentage: progressValue });
    }

    fetchData();
  } catch (error) {
    alert('Error updating milestone: ' + (error.response?.data?.error || 'Unknown error'));
  }
};

const handleProgressChange = async (milestoneId, newProgress) => {
  try {
  
    await api.put(`/api/milestones/${milestoneId}/progress`, { completionPercentage: parseInt(newProgress) });
    fetchData();
  } catch (error) {
    console.error('Error updating progress:', error);
  }
};

const handleDelete = async (milestoneId) => {
  if (!window.confirm('Are you sure you want to delete this milestone?')) return;

  try {
   
    await api.delete(`/api/milestones/${milestoneId}`);
    fetchData();
    alert('Milestone deleted successfully!');
  } catch (error) {
    alert('Error deleting milestone: ' + (error.response?.data?.error || 'Unknown error'));
  }
};
const canCreateMilestone = () => {
  const restrictedRoles = ['QUALITY_ASSURANCE', 'BUSINESS'];
  if (restrictedRoles.includes(currentUser?.role)) {
    return false;
  }
  return isManager() || isExecutive() || isTechnicalStaff();
};

const canDeleteMilestone = () => {
  const restrictedRoles = ['QUALITY_ASSURANCE', 'BUSINESS'];
  if (restrictedRoles.includes(currentUser?.role)) {
    return false;
  }
  return isManager() || isExecutive();
};
const isManager = () => {
  const managerRoles = ['DIGITAL_BANKING_MANAGER', 'PROJECT_MANAGER', 'CORE_BANKING_MANAGER'];
  return managerRoles.includes(currentUser?.role);
};

const isExecutive = () => {
  const executiveRoles = ['CEO', 'DEPUTY_CHIEF', 'DIRECTOR'];
  return executiveRoles.includes(currentUser?.role);
};

const isTechnicalStaff = () => {
  const techRoles = ['DEVELOPER', 'SENIOR_IT_OFFICER', 'JUNIOR_IT_OFFICER', 'IT_GRADUATE_TRAINEE'];
  return techRoles.includes(currentUser?.role);
};

const handleLogout = () => {
  logout();
  window.location.href = '/login';
};

  const getStatusColor = (status) => {
    switch (status) {
      case 'COMPLETED': return '#28a745';
      case 'IN_PROGRESS': return '#007bff';
      case 'PLANNED': return '#6c757d';
      case 'DELAYED': return '#dc3545';
      default: return '#6c757d';
    }
  };

  const isOverdue = (targetDate, status) => {
    if (status === 'COMPLETED') return false;
    if (!targetDate) return false;
    return new Date(targetDate) < new Date();
  };

 
  const filteredMilestones = (Array.isArray(milestones) ? milestones : []).filter(m => {
    if (filterProject !== 'all' && m.project?.id !== parseInt(filterProject)) return false;
    if (filterStatus !== 'all' && m.status !== filterStatus) return false;
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
        <div className="loading"><p>Loading milestones...</p></div>
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
       {/* Header */}
<div className="page-header" style={{
  position: 'relative',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: '20px 0'
}}>
  {/* Back Button - Absolute Left */}
  <button 
    onClick={handleBackToDashboard}
    className="btn"
    style={{ 
      padding: '8px 15px', 
      background: 'linear-gradient(135deg, #8B4513 0%, #A0522D 100%)',
      color: '#FFD700',
      border: '2px solid #8B4513',
      borderRadius: '5px',
      cursor: 'pointer',
      display: 'flex',
      alignItems: 'center',
      gap: '5px',
      position: 'absolute',
      left: 0
    }}
  >
    ← Back
  </button>
  
  {/* Centered Title and Subtitle */}
  <div style={{
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center'
  }}>
    <h1 style={{ 
      margin: 0, 
      fontSize: '28px', 
      fontWeight: '700', 
      color: '#8B4513',
      textAlign: 'center'
    }}>
      Project Milestones
    </h1>
    <p style={{ 
      margin: '5px 0 0 0', 
      fontSize: '14px',
      color: '#666',
      textAlign: 'center'
    }}>
      Track key deliverables and deadlines
    </p>
  </div>
  
  {/* New Milestone Button - Absolute Right */}
  {canCreateMilestone() && (
    <button 
      className="btn btn-primary" 
      onClick={() => setShowModal(true)}
      style={{ 
        padding: '12px 24px', 
        fontSize: '14px',
        position: 'absolute',
        right: 0
      }}
    >
      + New Milestone
    </button>
  )}
</div>
        

        {/* Summary Cards */}
        <div className="stats-grid" style={{ marginBottom: '20px' }}>
          <div className="stat-card">
            <div className="stat-label">Total Milestones</div>
            <div className="stat-value">{filteredMilestones.length}</div>
          </div>
          <div className="stat-card green">
            <div className="stat-label">Completed</div>
            <div className="stat-value">
              {filteredMilestones.filter(m => m.status === 'COMPLETED').length}
            </div>
          </div>
          <div className="stat-card amber">
            <div className="stat-label">In Progress</div>
            <div className="stat-value">
              {filteredMilestones.filter(m => m.status === 'IN_PROGRESS').length}
            </div>
          </div>
          <div className="stat-card red">
            <div className="stat-label">Overdue</div>
            <div className="stat-value">
              {filteredMilestones.filter(m => isOverdue(m.targetDate, m.status)).length}
            </div>
          </div>
        </div>

        {/* Filters */}
        <div className="content-card" style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: '200px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Filter by Project
              </label>
            <Select
  options={[
    { value: "all", label: "All Projects" },
    ...(Array.isArray(projects)
      ? projects.map(p => ({
          value: p.id,
          label: p.projectName
        }))
      : [])
  ]}
  value={
    filterProject === "all"
      ? { value: "all", label: "All Projects" }
      : projects
          .map(p => ({ value: p.id, label: p.projectName }))
          .find(o => o.value === parseInt(filterProject)) || null
  }
  onChange={(option) => setFilterProject(option.value)}
  placeholder="Search project..."
  isSearchable
/>
            </div>
            
            <div style={{ flex: 1, minWidth: '200px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Filter by Status
              </label>
              <select 
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              >
                <option value="all">All Status</option>
                <option value="PLANNED">Planned</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="COMPLETED">Completed</option>
                <option value="DELAYED">Delayed</option>
              </select>
            </div>

            <div style={{ flex: '0 0 auto' }}>
              <button 
                className="btn btn-primary"
                onClick={() => { setFilterProject('all'); setFilterStatus('all'); }}
                style={{ marginTop: '28px' }}
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>

        {/* Milestones Table */}
        <div className="content-card">
          <h2>All Milestones ({filteredMilestones.length})</h2>
          
          {filteredMilestones.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
              <p style={{ fontSize: '16px', marginBottom: '10px' }}>No milestones found</p>
              <p style={{ fontSize: '14px' }}>Click "New Milestone" to create your first milestone</p>
            </div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Milestone</th>
                  <th>Project</th>
                  <th>Target Date</th>
                  <th>Progress</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredMilestones.map((milestone) => (
                  <tr key={milestone.id}>
                    <td>
                      <div style={{ fontWeight: '600' }}>{milestone.milestoneName}</div>
                      {milestone.description && (
                        <div style={{ fontSize: '12px', color: '#666', marginTop: '3px' }}>
                          {milestone.description.substring(0, 50)}{milestone.description.length > 50 ? '...' : ''}
                        </div>
                      )}
                    </td>
                    <td>{milestone.project?.projectName || 'N/A'}</td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                        <span>{milestone.targetDate || 'Not set'}</span>
                        {isOverdue(milestone.targetDate, milestone.status) && (
                          <span className="badge badge-red" style={{ fontSize: '10px', padding: '3px 8px' }}>
                            Overdue
                          </span>
                        )}
                      </div>
                    </td>
                    <td style={{ width: '200px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <input
                          type="range"
                          min="0"
                          max="100"
                          value={milestone.completionPercentage || 0}
                          onChange={(e) => handleProgressChange(milestone.id, e.target.value)}
                          style={{ flex: 1, cursor: 'pointer' }}
                        />
                        <span style={{ fontSize: '12px', minWidth: '35px', fontWeight: '600' }}>
                          {milestone.completionPercentage || 0}%
                        </span>
                      </div>
                    </td>
                    <td>
                      <select
                        value={milestone.status}
                        onChange={(e) => handleStatusChange(milestone.id, e.target.value)}
                        style={{
                          padding: '5px 10px',
                          borderRadius: '5px',
                          border: '1px solid #ddd',
                          backgroundColor: getStatusColor(milestone.status),
                          color: milestone.status === 'IN_PROGRESS' ? '#fff' : '#000',
                          fontSize: '12px',
                          fontWeight: '600',
                          cursor: 'pointer'
                        }}
                      >
                        <option value="PLANNED">Planned</option>
                        <option value="IN_PROGRESS">In Progress</option>
                        <option value="COMPLETED">Completed</option>
                        <option value="DELAYED">Delayed</option>
                      </select>
                    </td>
                   <td>
  {canDeleteMilestone() && (
    <button 
      className="btn btn-danger"
      style={{ padding: '5px 10px', fontSize: '12px' }}
      onClick={() => handleDelete(milestone.id)}
    >
      Delete
    </button>
  )}
  {!canDeleteMilestone() && (
    <span style={{ fontSize: '12px', color: '#999', fontStyle: 'italic' }}>
      View only
    </span>
  )}
</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Create Milestone Modal */}
      {showModal && canCreateMilestone() && (
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
            <h2 style={{ marginBottom: '20px', color: '#003366' }}>Create New Milestone</h2>
            
            <form onSubmit={handleSubmit}>
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Milestone Name *</label>
                <input
                  type="text"
                  name="milestoneName"
                  value={formData.milestoneName}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Description</label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  rows="3"
                  style={{
                    padding: '12px 15px',
                    border: '1px solid #ddd',
                    borderRadius: '5px',
                    fontSize: '14px',
                    width: '100%',
                    resize: 'vertical'
                  }}
                />
              </div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Project *</label>
          <Select
  options={
    Array.isArray(projects)
      ? projects.map(p => ({
          value: p.id,
          label: p.projectName
        }))
      : []
  }
  value={
    projects
      .map(p => ({ value: p.id, label: p.projectName }))
      .find(o => o.value === parseInt(formData.projectId)) || null
  }
  onChange={(option) =>
    setFormData(prev => ({
      ...prev,
      projectId: option ? option.value : ""
    }))
  }
  placeholder="Search project..."
  isSearchable
/>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '15px' }}>
                <div className="form-group">
                  <label>Target Date *</label>
                  <input
                    type="date"
                    name="targetDate"
                    value={formData.targetDate}
                    onChange={handleInputChange}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Initial Progress</label>
                  <select
                    name="completionPercentage"
                    value={formData.completionPercentage}
                    onChange={handleInputChange}
                    style={{
                      padding: '12px 15px',
                      border: '1px solid #ddd',
                      borderRadius: '5px',
                      fontSize: '14px',
                      width: '100%'
                    }}
                  >
                    <option value="0">0% - Not Started</option>
                    <option value="25">25% - Just Started</option>
                    <option value="50">50% - Halfway</option>
                    <option value="75">75% - Almost Done</option>
                    <option value="100">100% - Completed</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                  Create Milestone
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
    </div>
  );
};

export default Milestones;