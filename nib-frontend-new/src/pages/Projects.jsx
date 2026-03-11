import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import { useNavigate } from 'react-router-dom';
import FileUpload from '../components/FileUpload';
import AttachmentList from '../components/AttachmentList';

const Projects = () => {
  const { currentUser, logout } = useAuth();
  const [projects, setProjects] = useState([]);
  const [editingProject, setEditingProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const [showModal, setShowModal] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  
  const [selectedProject, setSelectedProject] = useState(null);
  const [showAttachments, setShowAttachments] = useState(false);

  const [attachmentRefresh, setAttachmentRefresh] = useState(0);
  const [projectsRefreshTrigger, setProjectsRefreshTrigger] = useState(0);

  const [formData, setFormData] = useState({
    projectName: '',
    projectType: 'API',
    description: '',
    startDate: '',
    endDate: '',
    status: 'PLANNED'
  });
const filteredProjects = projects.filter(project =>
  project.projectName?.toLowerCase().includes(searchTerm.toLowerCase())
);
  const canSeeCreatedBy = () => {
  const technicalRoles = [
    'DEVELOPER', 
    'SENIOR_IT_OFFICER', 
    'JUNIOR_IT_OFFICER', 
    'IT_GRADUATE_TRAINEE'
  ];
  if (technicalRoles.includes(currentUser?.role)) {
    return false;
  }
  return true;
};
const calculateProjectStatus = (endDate, currentStatus = 'GREEN') => {
  if (!endDate) return currentStatus;
  const end = new Date(endDate);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const diffDays = Math.ceil((end - today) / (1000 * 60 * 60 * 24));
  if (diffDays < 0) return 'RED';
  if (diffDays <= 7) return 'AMBER';
  return 'GREEN';
};

const getDaysRemaining = (endDate) => {
  if (!endDate) return null;
  const end = new Date(endDate);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.ceil((end - today) / (1000 * 60 * 60 * 24));
};

  useEffect(() => {
    fetchData();
  }, [projectsRefreshTrigger]);

  const fetchData = async () => {
    console.log('🔄 Fetching projects...');
    try {
      const response = await api.get('/api/projects', {
        params: {
          userId: currentUser?.id,
          userRole: currentUser?.role
        },
        headers: {
          'X-User-Id': currentUser?.id  
        }
      });

      let projectsData = response.data;

      if (typeof projectsData === 'string') {
        console.log('📝 Data is a string, parsing...');
        projectsData = JSON.parse(projectsData);
      }

      if (!Array.isArray(projectsData)) {
        console.log('⚠️ Data is not an array:', projectsData);
        projectsData = [];
      }

      console.log('✅ Response received:', response);
      console.log('📦 Parsed ', projectsData);
      console.log('📊 Is array?', Array.isArray(projectsData));

      setProjects(projectsData);
    } catch (error) {
      console.error('❌ Error fetching projects:', error);
      console.error('Error response:', error.response);
      setProjects([]);
    } finally {
      console.log('⏹️ Setting loading to false');
      setLoading(false);
    }
  };
useEffect(() => {
  const urlParams = new URLSearchParams(window.location.search);
  const projectId = urlParams.get('projectId');
  const openComments = urlParams.get('openComments');
  
  if (projectId && openComments === 'true') {
   
    const project = projects.find(p => p.id === parseInt(projectId));
    if (project) {
      handleViewProject(project);
    }
  }
}, [projects]);
  const handleInputChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleBackToDashboard = () => {
    navigate('/dashboard');
  };

 const handleSubmit = async (e) => {
  e.preventDefault();
  try {
    console.log(editingProject ? 'Updating project...' : 'Creating project...');

    const calculatedStatus = calculateProjectStatus(formData.endDate, formData.status);
    
    if (calculatedStatus !== formData.status && formData.endDate) {
      const days = getDaysRemaining(formData.endDate);
      const statusMsg = calculatedStatus === 'RED' ? 'overdue' : 
                       calculatedStatus === 'AMBER' ? 'due soon' : 'on track';
      console.log(`📊 Status auto-updated: ${formData.status} → ${calculatedStatus} (${days} days ${statusMsg})`);
    }

    const requestData = {
      ...formData,
      status: calculatedStatus,
      initiatedById: currentUser?.id,
      managerId: isManager() ? currentUser?.id : null
    };

    
    if (!canEditProjectDates()) {
      delete requestData.startDate;
      delete requestData.endDate;
      console.log('🔒 Business role: Date fields removed from request');
    }

    let response;
    if (editingProject) {
      response = await api.put(`/api/projects/${editingProject.id}`, requestData, {
        headers: { 'X-User-Id': currentUser?.id }
      });
      alert(`Project updated successfully! Status: ${calculatedStatus}`);
    } else {
      response = await api.post('/api/projects', requestData, {
        headers: { 'X-User-Id': currentUser?.id }
      });
      alert(`Project created successfully! Status: ${calculatedStatus}`);
    }

    setShowModal(false);
    setEditingProject(null);
    fetchData();
    setFormData({
      projectName: '',
      projectType: 'API',
      description: '',
      startDate: '',
      endDate: '',
      status: 'PLANNED'
    });
  } catch (error) {
    console.error('Error:', error);
    const errorMsg = error.response?.data?.error || 'Unknown error occurred';
    alert('Error: ' + errorMsg);
  }
};

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  const getRagColor = (status) => {
    switch (status) {
      case 'GREEN': return '#28a745';
      case 'AMBER': return '#ffc107';
      case 'RED': return '#dc3545';
      default: return '#6c757d';
    }
  };

  const handleEdit = (project) => {
    setEditingProject(project);
    setFormData({
      projectName: project.projectName,
      projectType: project.projectType,
      description: project.description || '',
      startDate: project.startDate || '',
      endDate: project.endDate || '',
      status: project.status
    });
    setShowModal(true);
  };

  const handleDelete = async (projectId) => {
    if (!window.confirm('Are you sure you want to delete this project?')) return;

    try {
     
      await api.delete(`/api/projects/${projectId}`, {
        headers: {
          'X-User-Id': currentUser?.id
        }
      });
      fetchData();
      alert('Project deleted successfully!');
    } catch (error) {
      alert('Error deleting project: ' + (error.response?.data?.error || 'Unknown error'));
    }
  };

  const handleViewProject = async (project) => {
  console.log('📂 Viewing project:', project);
  

  try {
    const response = await api.get(`/api/projects/${project.id}`);
    console.log('📥 Fresh project data:', response.data);
    console.log('🔍 initiatedBy from fresh fetch:', response.data.initiatedBy);
  
    setSelectedProject(response.data);
  } catch (error) {
    console.error('❌ Error fetching project details:', error);
    setSelectedProject(project); 
  }
  
  setShowAttachments(true);
};

  const handleCloseAttachments = () => {
    setShowAttachments(false);
    setSelectedProject(null);
    
    setProjectsRefreshTrigger(prev => prev + 1);
  };

 const isManager = () => {
  const managerRoles = [
    'CEO', 'DEPUTY_CHIEF', 'QUALITY_ASSURANCE','DIRECTOR', 'PROJECT_MANAGER', 'DIGITAL_BANKING_MANAGER'
  ];
  return managerRoles.includes(currentUser?.role);
};


  const isTechnicalStaff = () => {
  const techRoles = ['DEVELOPER', 'SENIOR_IT_OFFICER', 'JUNIOR_IT_OFFICER', 'IT_GRADUATE_TRAINEE'];
  return techRoles.includes(currentUser?.role);
};
const isBusinessRole = () => {
  const businessRoles = ['BUSINESS'];
  return businessRoles.includes(currentUser?.role);
};

const canEditProjectDates = () => {
  const dateEditorRoles = [
    'PROJECT_MANAGER', 'CEO', 'DEPUTY_CHIEF', 'DIRECTOR', 'DIGITAL_BANKING_MANAGER'
  ];
  return dateEditorRoles.includes(currentUser?.role);
};
const canEditProjects = () => {
  if (currentUser?.role === 'QUALITY_ASSURANCE') {
    return false;
  }
  const editRoles = ['CEO', 'DEPUTY_CHIEF', 'DIRECTOR', 'PROJECT_MANAGER', 'DIGITAL_BANKING_MANAGER'];
  if (editRoles.includes(currentUser?.role)) {
    return true;
  }
  if (isBusinessRole()) {
    return true;
  }
  
  return false;
};
const canCreateProjects = () => {
  const allowedRoles = ['BUSINESS', 'DIGITAL_BANKING_MANAGER'];
  return allowedRoles.includes(currentUser?.role);
};


const canUploadAttachments = () => {
 
  if (isTechnicalStaff()) return true;
 
  if (isManager()) return true;
  
 
  if (isBusinessRole()) return true;
  
  return false;
};


const canDeleteAttachments = () => {
  
  if (isTechnicalStaff()) return true;
  

  if (isManager()) return true;
  

  if (isBusinessRole()) return true;
  
  return false;
};
  if (loading) {
    return (
      <div className="dashboard-container">
        <nav className="navbar">
          <div className="navbar-brand" >NIB IT Project Tracking</div>
          <div className="navbar-user">
            <span>Welcome, {currentUser?.fullName || currentUser?.username}</span>
            <button onClick={handleLogout} className="logout-btn">Logout</button>
          </div>
        </nav>
        <div className="loading">
          <p>Loading projects...</p>
        </div>
      </div>
    );
  }


  if (showAttachments && selectedProject) {
    return (
      <div className="dashboard-container">
        <nav className="navbar">
          <div className="navbar-brand">NIB IT Project Tracking</div>
          <div className="navbar-user">
            <span>Welcome, {currentUser?.fullName || currentUser?.username}</span>
            <button onClick={handleLogout} className="logout-btn">Logout</button>
          </div>
        </nav>

        <div className="main-content" style={{ padding: '30px', maxWidth: '1200px', margin: '0 auto' }}>
          <button
            onClick={handleCloseAttachments}
            className="btn"
            style={{
              padding: '8px 15px',
              backgroundColor: '#6c757d',
              color: 'white',
              border: 'none',
              borderRadius: '5px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '5px',
              marginBottom: '20px'
            }}
          >
            ← Back to Projects
          </button>

          <div style={{
            backgroundColor: 'white',
            padding: '25px',
            borderRadius: '10px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            marginBottom: '20px'
          }}>
            <h1 style={{ margin: '0 0 10px 0', color: '#8B4513' }}>{selectedProject.projectName}</h1>
            <p style={{ margin: '0 0 15px 0', color: '#666' }}>{selectedProject.description}</p>

            <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
              <div><strong>Type:</strong> {selectedProject.projectType}</div>
              
              <div>
                <strong>RAG:</strong>{' '}
                <span style={{
                  padding: '3px 10px',
                  borderRadius: '4px',
                  backgroundColor: getRagColor(selectedProject.ragStatus),
                  color: selectedProject.ragStatus === 'AMBER' ? '#000' : '#fff',
                  fontSize: '12px'
                }}>
                  {selectedProject.ragStatus}
                </span>
              </div>
              <div><strong>Completion:</strong> {selectedProject.completionPercentage || 0}%</div>
              <div><strong>Start:</strong> {selectedProject.startDate || 'Not set'}</div>
              <div><strong>End:</strong> {selectedProject.endDate || 'Not set'}</div>
            </div>
          </div>

         {/* In Projects.jsx - Attachment View Section */}
{/* ✅ NEW: Technical staff, managers, OR business users who initiated the project can upload */}
{(isTechnicalStaff() || isManager() || (isBusinessRole() && selectedProject?.initiatedBy?.id === currentUser?.id)) && (
  <div className="content-card" style={{ marginBottom: '20px' }}>
    <h3>📤 Upload File</h3>
    <p style={{ color: '#666', fontSize: '14px', marginBottom: '15px' }}>
      {isBusinessRole() 
        ? 'Upload business requirements, specifications, or related documents for this project.'
        : 'Upload technical documents, code files, or test results for this project.'
      }
    </p>
    
    {/* Show ownership info for business users */}
    {isBusinessRole() && (
      <small style={{ 
        display: 'block', 
        color: '#8B4513', 
        fontSize: '12px', 
        marginBottom: '10px',
        fontStyle: 'italic'
      }}>
        ✅ You can upload files to your projects
      </small>
    )}

    <FileUpload
      projectId={selectedProject.id}
      userId={currentUser?.id}
      onUploadSuccess={() => {
        console.log('📤 File uploaded successfully → refreshing views...');
        setAttachmentRefresh(prev => prev + 1);
        setProjectsRefreshTrigger(prev => prev + 1);
      }}
    />
  </div>
)}

          <div className="content-card">
            <AttachmentList
              projectId={selectedProject.id}
              userId={currentUser?.id}
              refreshTrigger={attachmentRefresh}
            />
          </div>
        </div>
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
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            position: 'relative'
          }}>
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

           <div style={{
  flex: 1,
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  textAlign: 'center'
}}>
  <h1>
    {isManager() ? 'All Projects' : 
     isBusinessRole() ? 'My Projects' : 'My Projects'}
  </h1>
  <p>
    {isManager() ? 'Manage IT projects and track progress' : 
     isBusinessRole() ? 'Projects you created' : 'Projects assigned to you'}
  </p>
</div>

          {/* ✅ NEW: Managers AND business users can create projects */}
{/* ✅ NEW: Only BUSINESS & DIGITAL_BANKING_MANAGER can create projects */}
{canCreateProjects() && (
  <button
    className="btn btn-primary"
    onClick={() => setShowModal(true)}
    style={{ padding: '12px 24px', fontSize: '14px' }}
  >
    + New Project
  </button>
)}
          </div>
        </div>

        <div className="content-card">
<h2>
  {isManager() ? `All Projects (${projects.length})` : 
   isBusinessRole() ? `My Projects (${projects.length})` : 
   `My Projects (${projects.length})`}
</h2>
<div style={{ marginBottom: '15px' }}>
  <input
    type="text"
    placeholder="🔍 Search project by name..."
    value={searchTerm}
    onChange={(e) => setSearchTerm(e.target.value)}
   style={{
  width: '100%',
  padding: '12px 15px',
  border: '2px solid #D2691E',
  borderRadius: '6px',
  fontSize: '14px',
  background: '#fff8dc',
  color: '#4a3728'
}}
  />
</div>
         {projects.length === 0 ? (
  <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
    <p style={{ fontSize: '16px', marginBottom: '10px' }}>No projects found</p>
    <p style={{ fontSize: '14px' }}>
      {isManager()
        ? 'Click "New Project" to create your first project'
        : isBusinessRole()
        ? 'Click "New Project" to create your first project'
        : 'No projects assigned to you. Wait for tasks to be assigned.'}
    </p>
  </div>
) : (
           <table className="data-table">
  <thead>
    <tr>
      <th>Project Name</th>
      <th>Type</th>
      <th>RAG Status</th>
      <th>Start Date</th>
      <th>End Date</th>
      
      {/* ✅ Conditionally show "Created By" */}
      {canSeeCreatedBy() && <th>Created By</th>}
      
      <th>Completion</th>
      <th>Attachments</th>
      {isManager() && <th>Actions</th>}
    </tr>
  </thead>
  <tbody>
    {filteredProjects.map((project) => (
      <tr key={project.id}>
        <td style={{ fontWeight: '600' }}>
          <button
            onClick={() => handleViewProject(project)}
            style={{
              background: 'none',
              border: 'none',
              color: '#8B4513',
              cursor: 'pointer',
              fontWeight: '600',
              fontSize: 'inherit',
              textAlign: 'left',
              padding: 0,
              textDecoration: 'underline'
            }}
          >
            {project.projectName}
          </button>
        </td>
        <td><span className="badge badge-green">{project.projectType}</span></td>
        
        {/* RAG Status */}
        <td>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="badge" style={{
              backgroundColor: getRagColor(project.ragStatus),
              color: project.ragStatus === 'AMBER' ? '#000' : '#fff',
              padding: '4px 10px',
              borderRadius: '4px',
              fontSize: '12px'
            }}>
              {project.ragStatus}
            </span>
            {project.endDate && (
              <span style={{ fontSize: '11px', color: '#666' }}>
                {getDaysRemaining(project.endDate) >= 0 
                  ? `${getDaysRemaining(project.endDate)}d left`
                  : `Overdue by ${Math.abs(getDaysRemaining(project.endDate))}d`
                }
              </span>
            )}
          </div>
        </td>
        
        <td>{project.startDate || 'Not set'}</td>
        <td>{project.endDate || 'Not set'}</td>
        
        {/* ✅ Conditionally show "Created By" data */}
        {canSeeCreatedBy() && (
          <td>{project.manager?.fullName || project.initiatedBy?.fullName || 'N/A'}</td>
        )}
        
        {/* Completion */}
        <td>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ flex: 1, height: '8px', backgroundColor: '#e0e0e0', borderRadius: '4px' }}>
              <div
                style={{
                  width: `${project.completionPercentage || 0}%`,
                  height: '100%',
                  backgroundColor: '#8B4513',
                  borderRadius: '4px'
                }}
              />
            </div>
            <span style={{ fontSize: '12px', minWidth: '40px' }}>
              {project.completionPercentage || 0}%
            </span>
          </div>
        </td>
        
        {/* Attachments */}
        <td>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontSize: '12px', color: '#666' }}>
              📎 {project.attachmentCount || 0}
            </span>
            <button
              onClick={() => handleViewProject(project)}
              style={{
                padding: '4px 8px',
                backgroundColor: '#8B4513',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '11px'
              }}
            >
              View
            </button>
          </div>
        </td>
        
        {/* Actions - only for managers or editable projects */}
        {canEditProjects() && (
          <td>
            <div style={{ display: 'flex', gap: '5px' }}>
              {(currentUser?.role !== 'BUSINESS' || project.initiatedBy?.id === currentUser?.id) && (
                <>
                  <button
                    className="btn btn-primary"
                    style={{ padding: '5px 10px', fontSize: '12px' }}
                    onClick={() => handleEdit(project)}
                  >
                    ✏️ Edit
                  </button>
                  <button
                    className="btn btn-danger"
                    style={{ padding: '5px 10px', fontSize: '12px' }}
                    onClick={() => handleDelete(project.id)}
                  >
                    🗑️ Delete
                  </button>
                </>
              )}
              {currentUser?.role === 'BUSINESS' && project.initiatedBy?.id !== currentUser?.id && (
                <span style={{ fontSize: '12px', color: '#999', fontStyle: 'italic' }}>
                  View only
                </span>
              )}
            </div>
          </td>
        )}
        
        {/* View only for QA */}
        {currentUser?.role === 'QUALITY_ASSURANCE' && (
          <td>
            <span style={{ fontSize: '12px', color: '#999', fontStyle: 'italic' }}>
              View only
            </span>
          </td>
        )}
      </tr>
    ))}
  </tbody>
</table>
          )}
        </div>
      </div>

      {/* Create/Edit Modal */}
      {/* ✅ NEW: Managers AND business users see the modal */}
{/* ✅ NEW: Only BUSINESS & DIGITAL_BANKING_MANAGER see the modal */}
{showModal && canCreateProjects() && (
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
            <h2 style={{ marginBottom: '20px', color: '#8B4513' }}>
              {editingProject ? '✏️ Edit Project' : ' Create New Project'}
            </h2>

            <form onSubmit={handleSubmit}>
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Project Name *</label>
                <input
                  type="text"
                  name="projectName"
                  value={formData.projectName}
                  onChange={handleInputChange}
                  required
                />
              </div>
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Project Type *</label>
                <select
                  name="projectType"
                  value={formData.projectType}
                  onChange={handleInputChange}
                  style={{
                    padding: '12px 15px',
                    border: '1px solid #ddd',
                    borderRadius: '5px',
                    fontSize: '14px',
                    width: '100%'
                  }}
                >
                  <option value="API">API Development</option>
                  <option value="Application">Application</option>
                  <option value="Infrastructure">Infrastructure</option>
                  <option value="System Integration">System Integration</option>
                </select>
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
              {/* Replace the date inputs section in your modal form with this: */}

<div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '15px' }}>
  
  {/* ✅ Show date fields ONLY to users who can edit dates */}
  {canEditProjectDates() ? (
    <>
      <div className="form-group">
        <label>Start Date</label>
        <input
          type="date"
          name="startDate"
          value={formData.startDate}
          onChange={handleInputChange}
        />
      </div>
      <div className="form-group">
        <label>End Date *</label>
        <input
          type="date"
          name="endDate"
          value={formData.endDate}
          onChange={handleInputChange}
          required={editingProject} 
        />
        {/* Show hint for business users */}
        <small style={{ color: '#666', fontSize: '11px', display: 'block', marginTop: '4px' }}>
          Set project timeline
        </small>
      </div>
    </>
  ) : (
    <>
      {/* Show read-only info for business users */}
      <div className="form-group">
        <label>Start Date</label>
        <div style={{ 
          padding: '12px 15px', 
          backgroundColor: '#f8f9fa', 
          borderRadius: '5px',
          color: '#666',
          fontSize: '14px'
        }}>
          {formData.startDate || 'Not set by Business'}
        </div>
        <small style={{ color: '#999', fontSize: '11px', display: 'block', marginTop: '4px' }}>
          📋 Project Manager will set timeline
        </small>
      </div>
      <div className="form-group">
        <label>End Date</label>
        <div style={{ 
          padding: '12px 15px', 
          backgroundColor: '#f8f9fa', 
          borderRadius: '5px',
          color: '#666',
          fontSize: '14px'
        }}>
          {formData.endDate || 'Not set by Business'}
        </div>
        <small style={{ color: '#999', fontSize: '11px', display: 'block', marginTop: '4px' }}>
          📋 Project Manager will set timeline
        </small>
      </div>
    </>
  )}
  
</div>
              <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                  {editingProject ? 'Update Project' : 'Create Project'}
                </button>
                <button
                  type="button"
                  className="btn"
                  style={{ flex: 1, backgroundColor: '#6c757d', color: 'white' }}
                  onClick={() => {
                    setShowModal(false);
                    setEditingProject(null);
                    setFormData({
                      projectName: '',
                      projectType: 'API',
                      description: '',
                      startDate: '',
                      endDate: '',
                      status: 'PLANNED'
                    });
                  }}
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

export default Projects;