import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import { useNavigate } from 'react-router-dom';

const CompletedProjects = () => {
  const { currentUser, logout } = useAuth();
  const navigate = useNavigate();
  
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('all');
  const [selectedProject, setSelectedProject] = useState(null);
  const [showDetails, setShowDetails] = useState(false);

  useEffect(() => {
    fetchCompletedProjects();
  }, []);

  const fetchCompletedProjects = async () => {
    try {
      console.log('🔄 Fetching completed projects...');
      const response = await api.get('/api/projects/completed', {
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
        projectsData = JSON.parse(projectsData);
      }

      setProjects(Array.isArray(projectsData) ? projectsData : []);
    } catch (error) {
      console.error('❌ Error fetching completed projects:', error);
      setProjects([]);
    } finally {
      setLoading(false);
    }
  };

  const handleBackToDashboard = () => {
    navigate('/dashboard');
  };
 const formatFileSize = (bytes) => {
  if (bytes == null || isNaN(bytes)) return '—';
  const units = ['B', 'KB', 'MB', 'GB'];
  let size = bytes, i = 0;
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024;
    i++;
  }
  return `${size.toFixed(1)} ${units[i]}`;
};

const handleDownloadAttachment = (attachmentId) => {
  window.open(`/api/attachments/${attachmentId}/download`, '_blank', 'noopener,noreferrer');
};
  const handleViewProject = async (project) => {
    try {
      const response = await api.get(`/api/projects/${project.id}`);
      setSelectedProject(response.data);
      setShowDetails(true);
    } catch (error) {
      console.error('Error fetching project details:', error);
      setSelectedProject(project);
      setShowDetails(true);
    }
  };

  const handleCloseDetails = () => {
    setShowDetails(false);
    setSelectedProject(null);
  };

  const getRagColor = (status) => {
    switch (status) {
      case 'GREEN': return '#28a745';
      case 'AMBER': return '#ffc107';
      case 'RED': return '#dc3545';
      default: return '#6c757d';
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString();
  };

  const filteredProjects = projects.filter(project => {
    const matchesSearch = project.projectName?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesType = filterType === 'all' || project.projectType === filterType;
    return matchesSearch && matchesType;
  });

  if (loading) {
    return (
      <div className="dashboard-container">
        <nav className="navbar">
          <div className="navbar-brand">NIB IT Project Tracking</div>
          <div className="navbar-user">
            <span>Welcome, {currentUser?.fullName}</span>
            <button onClick={() => { logout(); window.location.href = '/login'; }} className="logout-btn">Logout</button>
          </div>
        </nav>
        <div className="loading"><p>Loading completed projects...</p></div>
      </div>
    );
  }

  // ✅ Project Details View
  if (showDetails && selectedProject) {
    return (
      <div className="dashboard-container">
        <nav className="navbar">
          <div className="navbar-brand" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <img src="/nibicon.jpeg" alt="NIB Logo" style={{ width: '40px', height: '40px', objectFit: 'contain', borderRadius: '6px' }} />
            <div>
              <div style={{ fontSize: '20px', fontWeight: 'bold' }}>NIB IT Project Tracking</div>
              <div style={{ fontSize: '13px', opacity: '0.9' }}>ንብ ኢንተርናሽናል ባንክ</div>
            </div>
          </div>
          <div className="navbar-user">
            <span>Welcome, {currentUser?.fullName}</span>
            <button onClick={() => { logout(); window.location.href = '/login'; }} className="logout-btn">Logout</button>
          </div>
        </nav>

        <div className="main-content" style={{ padding: '30px', maxWidth: '1200px', margin: '0 auto' }}>
         <button 
  onClick={handleCloseDetails} 
  className="btn" 
  style={{ 
    marginBottom: '20px', 
    padding: '10px 20px',
    backgroundColor: '#8B4513',
    color: 'white',
    border: 'none',
    borderRadius: '5px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '600',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    boxShadow: '0 2px 4px rgba(139, 69, 19, 0.3)',
    transition: 'all 0.3s ease'
  }}
  onMouseEnter={(e) => {
    e.target.style.backgroundColor = '#A0522D';
    e.target.style.transform = 'translateY(-2px)';
    e.target.style.boxShadow = '0 4px 8px rgba(139, 69, 19, 0.4)';
  }}
  onMouseLeave={(e) => {
    e.target.style.backgroundColor = '#8B4513';
    e.target.style.transform = 'translateY(0)';
    e.target.style.boxShadow = '0 2px 4px rgba(139, 69, 19, 0.3)';
  }}
>
  ← Back to Completed Projects
</button>

          <div className="content-card">
            <h1 style={{ color: '#8B4513', marginBottom: '10px' }}>{selectedProject.projectName}</h1>
            <span className="badge" style={{ backgroundColor: '#28a745', color: 'white', padding: '5px 10px', marginBottom: '15px', display: 'inline-block' }}>
               COMPLETED
            </span>
            
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '15px', marginTop: '20px' }}>
              <div><strong>Type:</strong> {selectedProject.projectType}</div>
              <div><strong>Initiated By:</strong> {selectedProject.initiatedBy?.fullName || 'N/A'}</div>
              <div><strong>Manager:</strong> {selectedProject.manager?.fullName || 'N/A'}</div>
              <div><strong>Start Date:</strong> {formatDate(selectedProject.startDate)}</div>
              <div><strong>End Date:</strong> {formatDate(selectedProject.endDate)}</div>
              <div>
                <strong>RAG Status:</strong>{' '}
                <span style={{ padding: '3px 10px', borderRadius: '4px', backgroundColor: getRagColor(selectedProject.ragStatus), color: 'white', fontSize: '12px' }}>
                  {selectedProject.ragStatus}
                </span>
              </div>
              <div><strong>Completion:</strong> {selectedProject.completionPercentage || 0}%</div>
            </div>

            {selectedProject.description && (
              <div style={{ marginTop: '20px', padding: '15px', backgroundColor: '#f8f9fa', borderRadius: '5px' }}>
                <strong>Description:</strong>
                <p style={{ margin: '10px 0 0 0', color: '#666' }}>{selectedProject.description}</p>
              </div>
            )}
          </div>

          {/* Tasks Section */}
          <div className="content-card" style={{ marginTop: '20px' }}>
            <h3>📋 Tasks</h3>
            {selectedProject.tasks?.length > 0 ? (
              <table className="data-table" style={{ fontSize: '13px' }}>
                <thead>
                  <tr>
                    <th>Task Name</th>
                    <th>Assigned To</th>
                    <th>Priority</th>
                    <th>Status</th>
                    <th>Due Date</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedProject.tasks.map(task => (
                    <tr key={task.id}>
                      <td>{task.taskName}</td>
                      <td>{task.assignedTo?.fullName || 'Unassigned'}</td>
                      <td>
                        <span style={{ padding: '2px 8px', borderRadius: '3px', backgroundColor: task.priority >= 3 ? '#dc3545' : task.priority === 2 ? '#ffc107' : '#28a745', color: 'white', fontSize: '11px' }}>
                          {task.priority === 4 ? 'Critical' : task.priority === 3 ? 'High' : task.priority === 2 ? 'Medium' : 'Low'}
                        </span>
                      </td>
                      <td>{task.status}</td>
                      <td>{formatDate(task.dueDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p style={{ color: '#666' }}>No tasks recorded for this project.</p>
            )}
          </div>

          {/* Milestones Section */}
          <div className="content-card" style={{ marginTop: '20px' }}>
            <h3>🎯 Milestones</h3>
            {selectedProject.milestones?.length > 0 ? (
              <table className="data-table" style={{ fontSize: '13px' }}>
                <thead>
                  <tr>
                    <th>Milestone</th>
                    <th>Target Date</th>
                    <th>End Date</th>
                    <th>Progress</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedProject.milestones.map(milestone => (
                    <tr key={milestone.id}>
                      <td>{milestone.milestoneName}</td>
                      <td>{formatDate(milestone.targetDate)}</td>
                      <td>{formatDate(milestone.actualDate)}</td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <div style={{ flex: 1, height: '6px', backgroundColor: '#e0e0e0', borderRadius: '3px' }}>
                            <div style={{ width: `${milestone.completionPercentage || 0}%`, height: '100%', backgroundColor: '#8B4513', borderRadius: '3px' }} />
                          </div>
                          <span style={{ fontSize: '11px' }}>{milestone.completionPercentage || 0}%</span>
                        </div>
                      </td>
                      <td>{milestone.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p style={{ color: '#666' }}>No milestones recorded for this project.</p>
            )}
          </div>

       {/* APIs Section */}
<div className="content-card" style={{ marginTop: '20px' }}>
  <h3>🔌 APIs Delivered</h3>
  {selectedProject.apis?.length > 0 ? (
    <table className="data-table" style={{ fontSize: '13px' }}>
      <thead>
        <tr>
          <th>API Name</th>
          <th>Version</th>
          <th>Endpoint</th>
          <th>Documentation</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        {selectedProject.apis.map(api => (
          <tr key={api.id}>
            <td>{api.apiName}</td>
            <td><code>{api.version}</code></td>
            <td><code style={{ fontSize: '11px' }}>{api.endpoint}</code></td>
            <td>
              {api.documentationUrl ? (
                <a href={api.documentationUrl} target="_blank" rel="noopener noreferrer" style={{ color: '#007bff' }}>View Docs</a>
              ) : 'N/A'}
            </td>
            <td>{api.status}</td>
          </tr>
        ))}
      </tbody>
    </table>
  ) : (
    <p style={{ color: '#666' }}>No APIs recorded for this project.</p>
  )}
</div>

{/* ✅ ADD THIS: Attachments Section */}
<div className="content-card" style={{ marginTop: '20px' }}>
  <h3>📎 Attachments</h3>
  {selectedProject.attachments?.length > 0 ? (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
      {selectedProject.attachments.map(attachment => (
        <div
          key={attachment.id}
          style={{
            padding: '16px',
            border: '1px solid #dee2e6',
            borderRadius: '8px',
            background: 'white',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: '16px'
          }}
        >
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontWeight: 600, color: '#8B4513', fontSize: '15px' }}>
              📄 {attachment.originalFileName || 'File'}
            </div>
            {attachment.description && (
              <div style={{ fontSize: '13px', color: '#555', margin: '4px 0' }}>
                {attachment.description}
              </div>
            )}
            <div style={{ fontSize: '12.5px', color: '#6c757d' }}>
              {formatFileSize(attachment.fileSize)} • Uploaded by {attachment.uploadedBy?.fullName || 'Unknown'} • {new Date(attachment.uploadedAt).toLocaleString('am-ET', {
                dateStyle: 'medium',
                timeStyle: 'short'
              })}
            </div>
          </div>

          <div style={{ display: 'flex', gap: '10px', flexShrink: 0 }}>
            <button
              onClick={() => handleDownloadAttachment(attachment.id)}
              style={{
                padding: '8px 14px',
                background: '#28a745',
                color: 'white',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer',
                fontSize: '13px'
              }}
            >
              ⬇ Download
            </button>
          </div>
        </div>
      ))}
    </div>
  ) : (
    <p style={{ color: '#666' }}>No attachments for this project.</p>
  )}
</div>
        </div>
      </div>
    );
  }

  // ✅ Main List View
  return (
    <div className="dashboard-container">
      <nav className="navbar">
        <div className="navbar-brand" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <img src="/nibicon.jpeg" alt="NIB Logo" style={{ width: '40px', height: '40px', objectFit: 'contain', borderRadius: '6px' }} />
          <div>
            <div style={{ fontSize: '20px', fontWeight: 'bold' }}>NIB IT Project Tracking</div>
            <div style={{ fontSize: '13px', opacity: '0.9' }}>ንብ ኢንተርናሽናል ባንክ</div>
          </div>
        </div>
        <div className="navbar-user">
          <span>Welcome, {currentUser?.fullName}</span>
          <button onClick={() => { logout(); window.location.href = '/login'; }} className="logout-btn">Logout</button>
        </div>
      </nav>

      <div className="main-content">
        <div className="page-header">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <button 
  onClick={handleBackToDashboard} 
  className="btn" 
  style={{ 
    padding: '10px 20px',
    backgroundColor: '#8B4513',
    color: 'white',
    border: 'none',
    borderRadius: '5px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: '600',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    boxShadow: '0 2px 4px rgba(139, 69, 19, 0.3)',
    transition: 'all 0.3s ease'
  }}
  onMouseEnter={(e) => {
    e.target.style.backgroundColor = '#A0522D';
    e.target.style.transform = 'translateY(-2px)';
    e.target.style.boxShadow = '0 4px 8px rgba(139, 69, 19, 0.4)';
  }}
  onMouseLeave={(e) => {
    e.target.style.backgroundColor = '#8B4513';
    e.target.style.transform = 'translateY(0)';
    e.target.style.boxShadow = '0 2px 4px rgba(139, 69, 19, 0.3)';
  }}
>
  ← Back to Dashboard
</button>
            <div style={{ textAlign: 'center' }}>
              <h1 style={{ color: '#8B4513' }}>Completed Projects Archive</h1>
              <p>Historical record of successfully delivered projects</p>
            </div>
            <div style={{ width: '120px' }}></div>
          </div>
        </div>

        {/* Filters */}
        <div className="content-card" style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: '200px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                🔍 Search
              </label>
              <input
                type="text"
                placeholder="Search by project name..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              />
            </div>
            <div style={{ flex: 1, minWidth: '200px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Filter by Type
              </label>
              <select 
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              >
                <option value="all">All Types</option>
                <option value="API">API Development</option>
                <option value="Application">Application</option>
                <option value="Infrastructure">Infrastructure</option>
                <option value="System Integration">System Integration</option>
              </select>
            </div>
            <div style={{ flex: '0 0 auto', alignSelf: 'flex-end' }}>
              <button 
                className="btn"
                onClick={() => { setSearchTerm(''); setFilterType('all'); }}
                style={{ backgroundColor: '#6c757d', color: 'white', padding: '10px 20px' }}
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>

        {/* Projects Table */}
        <div className="content-card">
          <h2>Completed Projects ({filteredProjects.length})</h2>
          
          {filteredProjects.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
              <p style={{ fontSize: '16px', marginBottom: '10px' }}>No completed projects found</p>
              <p style={{ fontSize: '14px' }}>
                {searchTerm || filterType !== 'all' 
                  ? 'Try adjusting your filters' 
                  : 'Completed projects will appear here once they are marked as finished'}
              </p>
            </div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Project Name</th>
                  <th>Type</th>
                  <th>Initiated By</th>
                  <th>Duration</th>
                  <th>Completion</th>
                  <th>RAG</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredProjects.map((project) => (
                  <tr key={project.id}>
                    <td style={{ fontWeight: '600' }}>
                      <button
                        onClick={() => handleViewProject(project)}
                        style={{ background: 'none', border: 'none', color: '#8B4513', cursor: 'pointer', fontWeight: '600', fontSize: 'inherit', textAlign: 'left', padding: 0, textDecoration: 'underline' }}
                      >
                        {project.projectName}
                      </button>
                    </td>
                    <td><span className="badge badge-green">{project.projectType}</span></td>
                    <td>{project.manager?.fullName || 'Unassigned'}</td>
                    <td style={{ fontSize: '13px' }}>
                      {project.startDate && project.endDate 
                        ? `${formatDate(project.startDate)} → ${formatDate(project.endDate)}`
                        : 'N/A'}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <div style={{ flex: 1, height: '6px', backgroundColor: '#e0e0e0', borderRadius: '3px', minWidth: '80px' }}>
                          <div style={{ width: `${project.completionPercentage || 0}%`, height: '100%', backgroundColor: '#28a745', borderRadius: '3px' }} />
                        </div>
                        <span style={{ fontSize: '11px' }}>{project.completionPercentage || 0}%</span>
                      </div>
                    </td>
                    <td>
                      <span className="badge" style={{ backgroundColor: getRagColor(project.ragStatus), color: 'white', padding: '3px 8px', fontSize: '11px' }}>
                        {project.ragStatus}
                      </span>
                    </td>
                    <td>
                      <button 
                        onClick={() => handleViewProject(project)}
                        className="btn btn-primary"
                        style={{ padding: '4px 10px', fontSize: '11px' }}
                      >
                        View Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
};

export default CompletedProjects;