import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import { useNavigate } from 'react-router-dom'; 
import Select from "react-select";
const ApiLifecycle = () => {
  const { currentUser, logout } = useAuth();
  const [apis, setApis] = useState([]);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [filterStage, setFilterStage] = useState('all');
  const [filterProject, setFilterProject] = useState('all');
const navigate = useNavigate(); 
  
  const [formData, setFormData] = useState({
    apiName: '',
    apiVersion: '',
    description: '',
    endpoint: '',
    documentationUrl: '',
    projectId: '',
    ownerId: ''
  });

 useEffect(() => {
  fetchData();
}, []);

useEffect(() => {
  if (!Array.isArray(projects)) {
    setProjects([]);
  }
}, [projects]);

const isTechnicalStaff = () => {
  const techRoles = ['DEVELOPER', 'SENIOR_IT_OFFICER', 'JUNIOR_IT_OFFICER', 'IT_GRADUATE_TRAINEE'];
  return techRoles.includes(currentUser?.role);
};
const isManager = () => {
  const managerRoles = [
    'DIGITAL_BANKING_MANAGER', 'PROJECT_MANAGER', 'CORE_BANKING_MANAGER'
  ];
  return managerRoles.includes(currentUser?.role);
};

const isExecutive = () => {
  const executiveRoles = ['CEO', 'DEPUTY_CHIEF', 'DIRECTOR'];
  return executiveRoles.includes(currentUser?.role);
};
const canCreateApi = () => {
 
  if (currentUser?.role === 'QUALITY_ASSURANCE' || currentUser?.role === 'BUSINESS') {
    return false;
  }
  
 
  return isManager() ||  isTechnicalStaff();
};
const handleBackToDashboard = () => {
  navigate('/dashboard');
 
};

const fetchData = async () => {
  try {
    console.log('🔗 Fetching API lifecycle data...');
    console.log('User ID:', currentUser?.id);
    console.log('User Role:', currentUser?.role);

   
    const [apisRes, projectsRes] = await Promise.all([
      api.get('/api/apis'),       
      api.get('/api/projects')    
    ]);

    let apisData = apisRes.data;
    if (typeof apisData === 'string') {
      apisData = JSON.parse(apisData);
    }

    let projectsData = projectsRes.data;
    if (typeof projectsData === 'string') {
      projectsData = JSON.parse(projectsData);
    }

    let filteredApis = Array.isArray(apisData) ? apisData : [];

    if (isTechnicalStaff() && currentUser?.id) {
      console.log('User is technical staff - filtering APIs by assigned projects...');

      const tasksRes = await api.get('/api/tasks', {
        params: {
          userId: currentUser.id,
          userRole: currentUser.role
        }
      });

      let tasksData = tasksRes.data;
      if (typeof tasksData === 'string') {
        tasksData = JSON.parse(tasksData);
      }

      console.log('User has', tasksData.length, 'tasks');

     
      const assignedProjectIds = [...new Set(
        tasksData.map(task => task.project?.id).filter(Boolean)
      )];

      console.log('Assigned project IDs:', assignedProjectIds);

      filteredApis = filteredApis.filter(apiItem =>
        apiItem.project?.id && assignedProjectIds.includes(apiItem.project.id)
      );

      console.log('Filtered to', filteredApis.length, 'APIs for user');
    } else {
      console.log('User is manager - showing all', filteredApis.length, 'APIs');
    }

    setApis(filteredApis);
    setProjects(Array.isArray(projectsData) ? projectsData : []);

  } catch (error) {
    console.error('❌ Error fetching API lifecycle data:', error);
    console.error('Error response:', error.response);
    setApis([]);
    setProjects([]);
  } finally {
    setLoading(false);
  }
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
    const requestData = {
      ...formData,
      ownerId: currentUser?.id,
     
      endpoint: formData.endpoint?.trim() || null,
      documentationUrl: formData.documentationUrl?.trim() || null
    };

   
    if (!requestData.endpoint) delete requestData.endpoint;
    if (!requestData.documentationUrl) delete requestData.documentationUrl;
    
    await api.post('/api/apis', requestData);

    alert('API created successfully!');
    setShowModal(false);
    fetchData();

    setFormData({
      apiName: '',
      apiVersion: '',
      description: '',
      endpoint: '',
      documentationUrl: '',
      projectId: '',
      ownerId: ''
    });
  } catch (error) {
    alert('Error creating API: ' + (error.response?.data?.error || 'Unknown error'));
  }
};

const handleStageChange = async (apiId, newStage) => {
  try {
  
    await api.put(`/api/apis/${apiId}/stage`, { stage: newStage });
    fetchData();
    alert('API stage updated!');
  } catch (error) {
    alert('Error updating API: ' + (error.response?.data?.error || 'Unknown error'));
  }
};

const handleStatusChange = async (apiId, newStatus) => {
  try {
  
    await api.put(`/api/apis/${apiId}/status`, { status: newStatus });
    fetchData();
  } catch (error) {
    console.error('Error updating status:', error);
  }
};

const handleDelete = async (apiId) => {
  if (!window.confirm('Are you sure you want to delete this API?')) return;

  try {
 
    await api.delete(`/api/apis/${apiId}`);
    fetchData();
    alert('API deleted successfully!');
  } catch (error) {
    alert('Error deleting API: ' + (error.response?.data?.error || 'Unknown error'));
  }
};

const handleLogout = () => {
  logout();
  window.location.href = '/login';
};

  const getStageColor = (stage) => {
    const colors = {
      'DOCUMENTATION': '#6c757d',
      'DEVELOPMENT': '#007bff',
      'TESTING': '#ffc107',
      'SANDBOXING': '#17a2b8',
      'UAT': '#6f42c1',
      'PRODUCTION': '#28a745'
    };
    return colors[stage] || '#6c757d';
  };

  const getStatusColor = (status) => {
    const colors = {
      'ACTIVE': '#28a745',
      'DEPRECATED': '#ffc107',
      'RETIRED': '#dc3545',
      'DRAFT': '#6c757d'
    };
    return colors[status] || '#6c757d';
  };
const canDeleteApi = () => {
  
  if (currentUser?.role === 'QUALITY_ASSURANCE') return false;
  return isManager() || isTechnicalStaff();
};
 
  const filteredApis = (Array.isArray(apis) ? apis : []).filter(a => {
    if (filterStage !== 'all' && a.lifecycleStage !== filterStage) return false;
    if (filterProject !== 'all' && a.project?.id !== parseInt(filterProject)) return false;
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
        <div className="loading"><p>Loading APIs...</p></div>
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
      {/* Header - Card Style */}
{/* Header - Matches Tasks/Dashboard Style */}
{/* Header - Centered Title */}
<div style={{ 
  position: 'relative',
  display: 'flex', 
  alignItems: 'center',
  marginBottom: '30px',
  padding: '20px 0'
}}>
  {/* Back Button - Absolute Left */}
  <button 
    onClick={handleBackToDashboard}
    className="btn btn-secondary"
    style={{ 
      padding: '8px 15px',
      position: 'absolute',
      background: 'linear-gradient(135deg, #8B4513 0%, #A0522D 100%)',
               color: '#FFD700',
               border: '2px solid #8B4513', 
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
      API Lifecycle Tracking
    </h1>
    <p style={{ 
      margin: '5px 0 0 0', 
      fontSize: '14px',
      color: '#666',
      textAlign: 'center'
    }}>
      Monitor APIs through development stages 
    </p>
  </div>
  
  {/* New API Button - Absolute Right */}
 {/* New API Button - Only show if user can create APIs */}
{canCreateApi() && (
  <button 
    className="btn btn-primary" 
    onClick={() => setShowModal(true)}
    style={{ 
      padding: '10px 20px',
      fontSize: '14px',
      position: 'absolute',
      right: 0
    }}
  >
    + New API
  </button>
)}
</div>

        {/* Lifecycle Pipeline Visualization */}
        <div className="content-card" style={{ marginBottom: '20px' }}>
          <h2>API Development Pipeline</h2>
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            padding: '20px 0',
            overflowX: 'auto'
          }}>
            {['DOCUMENTATION', 'DEVELOPMENT', 'TESTING', 'SANDBOXING', 'UAT', 'PRODUCTION'].map((stage, index) => (
              <React.Fragment key={stage}>
                <div style={{ textAlign: 'center', minWidth: '120px' }}>
                  <div style={{
                    width: '40px',
                    height: '40px',
                    borderRadius: '50%',
                    backgroundColor: getStageColor(stage),
                    margin: '0 auto 10px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'white',
                    fontWeight: 'bold',
                    fontSize: '12px'
                  }}>
                    {index + 1}
                  </div>
                  <div style={{ fontSize: '11px', fontWeight: '600' }}>{stage}</div>
                  <div style={{ fontSize: '10px', color: '#666' }}>
                    {filteredApis.filter(a => a.lifecycleStage === stage).length}
                  </div>
                </div>
                {index < 5 && (
                  <div style={{ 
                    flex: 1, 
                    height: '3px', 
                    backgroundColor: '#e0e0e0', 
                    margin: '0 10px' 
                  }} />
                )}
              </React.Fragment>
            ))}
          </div>
        </div>

        {/* Filters */}
        <div className="content-card" style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: '200px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Filter by Stage
              </label>
              <select 
                value={filterStage}
                onChange={(e) => setFilterStage(e.target.value)}
                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              >
                <option value="all">All Stages</option>
                <option value="DOCUMENTATION">Documentation</option>
                <option value="DEVELOPMENT">Development</option>
                <option value="TESTING">Testing</option>
                <option value="SANDBOXING">Sandboxing</option>
                <option value="UAT">UAT</option>
                <option value="PRODUCTION">Production</option>
              </select>
            </div>
            
            <div style={{ flex: 1, minWidth: '200px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Filter by Project
              </label>
             <Select
  options={[
    { value: "all", label: "All Projects" },
    ...projects.map(p => ({
      value: p.id,
      label: p.projectName
    }))
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
  isClearable
/>
            </div>

            <div style={{ flex: '0 0 auto' }}>
              <button 
                className="btn btn-primary"
                onClick={() => { setFilterStage('all'); setFilterProject('all'); }}
                style={{ marginTop: '28px' }}
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>

        {/* APIs Table */}
        <div className="content-card">
          <h2>All APIs ({filteredApis.length})</h2>
          
          {filteredApis.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
              <p style={{ fontSize: '16px', marginBottom: '10px' }}>No APIs found</p>
              <p style={{ fontSize: '14px' }}>Click "+ New API" to register your first API</p>
            </div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>API Name</th>
                  <th>Version</th>
                  <th>Project</th>
                  <th>Stage</th>
                  <th>Status</th>
                  <th>Endpoint</th>
                  <th>Documentation</th>
                  {isManager() && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {filteredApis.map((apiItem) => (
                  <tr key={apiItem.id}>
                    <td>
                      <div style={{ fontWeight: '600' }}>{apiItem.apiName}</div>
                      {apiItem.description && (
                        <div style={{ fontSize: '12px', color: '#666', marginTop: '3px' }}>
                          {apiItem.description.substring(0, 40)}{apiItem.description.length > 40 ? '...' : ''}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className="badge badge-green">{apiItem.apiVersion}</span>
                    </td>
                    <td>{apiItem.project?.projectName || 'N/A'}</td>
                    <td>
                      <select
                        value={apiItem.lifecycleStage}
                        onChange={(e) => handleStageChange(apiItem.id, e.target.value)}
                        style={{
                          padding: '5px 10px',
                          borderRadius: '5px',
                          border: '1px solid #ddd',
                          backgroundColor: getStageColor(apiItem.lifecycleStage),
                          color: 'white',
                          fontSize: '11px',
                          fontWeight: '600',
                          cursor: 'pointer',
                          minWidth: '120px'
                        }}
                      >
                        <option value="DOCUMENTATION">📝 Documentation</option>
                        <option value="DEVELOPMENT">💻 Development</option>
                        <option value="TESTING">🧪 Testing</option>
                        <option value="SANDBOXING">🏖️ Sandboxing</option>
                        <option value="UAT">✅ UAT</option>
                        <option value="PRODUCTION">🚀 Production</option>
                      </select>
                    </td>
                    <td>
                      <select
                        value={apiItem.status}
                        onChange={(e) => handleStatusChange(apiItem.id, e.target.value)}
                        style={{
                          padding: '5px 10px',
                          borderRadius: '5px',
                          border: '1px solid #ddd',
                          backgroundColor: getStatusColor(apiItem.status),
                          color: apiItem.status === 'DEPRECATED' ? '#000' : 'white',
                          fontSize: '11px',
                          fontWeight: '600',
                          cursor: 'pointer'
                        }}
                      >
                        <option value="ACTIVE">Active</option>
                        <option value="DEPRECATED">Deprecated</option>
                        <option value="RETIRED">Retired</option>
                        <option value="DRAFT">Draft</option>
                      </select>
                    </td>
                    <td>
                      {apiItem.endpoint ? (
                        <a href={apiItem.endpoint} target="_blank" rel="noopener noreferrer" style={{ color: '#007bff', textDecoration: 'none', fontSize: '12px' }}>
                          {apiItem.endpoint.substring(0, 25)}{apiItem.endpoint.length > 25 ? '...' : ''}
                        </a>
                      ) : 'Not set'}
                    </td>
                    <td>
        {apiItem.documentationUrl ? (
          <a 
            href={apiItem.documentationUrl} 
            target="_blank" 
            rel="noopener noreferrer" 
            style={{ 
              color: '#8B4513', 
              textDecoration: 'none', 
              fontSize: '12px',
              fontWeight: '600'
            }}
             title={apiItem.documentationUrl}
          >
            📖 View Docs
          </a>
        ) : (
          <span style={{ color: '#999', fontSize: '12px' }}>Not set</span>
        )}
      </td>
             {canDeleteApi() && (
  <td>
    <button 
      className="btn btn-danger"
      style={{ padding: '5px 10px', fontSize: '12px' }}
      onClick={() => handleDelete(apiItem.id)}
    >
      Delete
    </button>
  </td>
)}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Create API Modal */}
      {showModal && canCreateApi() && (
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
            maxWidth: '550px',
            maxHeight: '90vh',
            overflow: 'auto'
          }}>
            <h2 style={{ marginBottom: '20px', color: '#8B4513' }}>Register New API</h2>
            
            <form onSubmit={handleSubmit}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '15px' }}>
                <div className="form-group">
                  <label>API Name *</label>
                  <input
                    type="text"
                    name="apiName"
                    value={formData.apiName}
                    onChange={handleInputChange}
                    required
                  />
                </div>

                <div className="form-group">
                  <label>Version *</label>
                  <input
                    type="text"
                    name="apiVersion"
                    value={formData.apiVersion}
                    onChange={handleInputChange}
                    placeholder="e.g., v1.0.0"
                    required
                  />
                </div>
              </div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Description</label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  rows="2"
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

              {/* Endpoint URL - Optional */}
<div className="form-group" style={{ marginBottom: '15px' }}>
  <label>
    Endpoint URL 
    <span style={{ color: '#999', fontWeight: 'normal', fontSize: '13px' }}> (Optional)</span>
  </label>
  <input
    type="url"
    name="endpoint"
    value={formData.endpoint}
    onChange={handleInputChange}
    placeholder="https://api.nib.com/...  "
    style={{
      padding: '12px 15px',
      border: '1px solid #ddd',
      borderRadius: '5px',
      fontSize: '14px',
      width: '100%'
    }}
    /* Remove required attribute if present */
  />
</div>

             
{/* Documentation URL - Optional */}
<div className="form-group" style={{ marginBottom: '15px' }}>
  <label>
    Documentation URL 
    <span style={{ color: '#999', fontWeight: 'normal', fontSize: '13px' }}> (Optional)</span>
  </label>
  <input
    type="url"
    name="documentationUrl"
    value={formData.documentationUrl}
    onChange={handleInputChange}
    placeholder="https://docs.nib.com/...  "
    style={{
      padding: '12px 15px',
      border: '1px solid #ddd',
      borderRadius: '5px',
      fontSize: '14px',
      width: '100%'
    }}
    /* Remove required attribute if present */
  />
</div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Project *</label>
                <Select
  options={projects.map(p => ({
    value: p.id,
    label: p.projectName
  }))}
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
  isClearable
/>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                  Register API
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

export default ApiLifecycle;