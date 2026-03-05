import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import { useNavigate } from 'react-router-dom';
import Select from "react-select";
const Reports = () => {
  const { currentUser, logout } = useAuth();
  const [activeTab, setActiveTab] = useState('dashboard');
  const [dashboardStats, setDashboardStats] = useState(null);
  const [ragReport, setRagReport] = useState(null);
  const [myPerformance, setMyPerformance] = useState(null);
  const [allDevelopersReport, setAllDevelopersReport] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  
  
  const [selectedProject, setSelectedProject] = useState(null);
  const [detailedReport, setDetailedReport] = useState(null);
  const [projects, setProjects] = useState([]);
  const [reportLoading, setReportLoading] = useState(false);
  const [reportError, setReportError] = useState('');
  
  const [dateRange, setDateRange] = useState({
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0]
  });

  
 useEffect(() => {
  fetchReports();
}, [dateRange]);

useEffect(() => {
  const fetchProjects = async () => {
    if (!currentUser?.id) {
      console.log('⚠️ No current user ID, skipping project fetch');
      return;
    }
    
    try {
      console.log('📥 Fetching projects for reports...');
      console.log('Current user:', currentUser.id, 'Role:', currentUser.role);
      
  
      let projectsData = [];
      
      try {
        const res = await api.get('/api/reports/projects/for-reports', {
          headers: {
            'X-User-Id': currentUser?.id,
            'X-User-Role': currentUser?.role
          }
        });
        
        console.log('📊 Reports projects response:', res.data);
  
        if (Array.isArray(res.data)) {
          projectsData = res.data;
        } else if (res.data && Array.isArray(res.data.projects)) {
          projectsData = res.data.projects;
        } else if (res.data && Array.isArray(res.data.content)) {
         
          projectsData = res.data.content;
        }
        
      } catch (endpointError) {
        console.warn('⚠️ Reports endpoint failed, trying fallback:', endpointError.message);
        
        const fallbackRes = await api.get('/api/projects', {
          params: {
            userId: currentUser?.id,
            userRole: currentUser?.role
          }
        });
        
        console.log('🔄 Fallback projects response:', fallbackRes.data);
        
        if (Array.isArray(fallbackRes.data)) {
          projectsData = fallbackRes.data;
        }
      }
      
      const mappedProjects = projectsData.map(p => ({
        id: p.id,
        name: p.projectName || p.name || 'Unknown Project',
        status: p.status,
        ragStatus: p.ragStatus,
        manager: p.manager,
        description: p.description
      }));
      
      console.log('✅ Loaded', mappedProjects.length, 'projects for reports');
      console.log('Projects:', mappedProjects);
      
      setProjects(mappedProjects);
      
    } catch (error) {
      console.error('❌ Failed to fetch projects for reports:', error);
      setReportError('Failed to load projects: ' + (error.response?.data?.message || error.message));
      setProjects([]);
    }
  };
  
  fetchProjects();
}, [currentUser]);


useEffect(() => {
  if (activeTab === 'detailed' && selectedProject) {
    fetchDetailedReport();
  }
}, [selectedProject, dateRange, activeTab]);

const fetchReports = async () => {
  try {
    console.log('📊 Fetching reports with date range:', dateRange);

    const [dashboardRes, ragRes] = await Promise.all([
    
      api.get('/api/reports/dashboard-summary', {
        params: {
          userId: currentUser?.id,
          userRole: currentUser?.role,
          startDate: dateRange.startDate,
          endDate: dateRange.endDate
        }
      }),
     
      api.get('/api/reports/rag-status', {
        params: {
          userId: currentUser?.id,
          userRole: currentUser?.role,
          startDate: dateRange.startDate,
          endDate: dateRange.endDate
        }
      })
    ]);

    console.log('📊 Dashboard Response:', dashboardRes.data);
    console.log('🚦 RAG Response:', ragRes.data);

    setDashboardStats(dashboardRes.data);
    setRagReport(ragRes.data);

    if (isManager()) {
      
      const devsRes = await api.get('/api/reports/developers-performance', {
        params: {
          startDate: dateRange.startDate,
          endDate: dateRange.endDate
        }
      });
      console.log('👥 Developers Performance:', devsRes.data);
      setAllDevelopersReport(devsRes.data);
    } else {
   
      const myPerfRes = await api.get('/api/reports/my-performance', {
        params: {
          userId: currentUser?.id,
          startDate: dateRange.startDate,
          endDate: dateRange.endDate
        }
      });
      console.log('📈 My Performance:', myPerfRes.data);
      setMyPerformance(myPerfRes.data);
    }
  } catch (error) {
    console.error('❌ Error fetching reports:', error);
    console.error('Error response:', error.response);
  } finally {
    setLoading(false);
  }
};


const fetchDetailedReport = useCallback(async () => {
  if (!selectedProject) return;

  setReportLoading(true);
  setReportError('');

  try {
    console.log('📋 Fetching detailed report for project:', selectedProject, dateRange);

   
    const res = await api.get(`/api/reports/project/${selectedProject}/detailed`, {
      params: {
        startDate: dateRange.startDate,
        endDate: dateRange.endDate,
        userId: currentUser?.id
      }
    });

    console.log('✅ Detailed report loaded:', res.data);
    setDetailedReport(res.data);
  } catch (error) {
    console.error('❌ Failed to fetch detailed report:', error);
    setReportError('Failed to load detailed report: ' + (error.response?.data?.message || error.message));
  } finally {
    setReportLoading(false);
  }
}, [selectedProject, dateRange, currentUser]);

const handleLogout = () => {
  logout();
  window.location.href = '/login';
};

const handleDateChange = (e) => {
  setDateRange({
    ...dateRange,
    [e.target.name]: e.target.value
  });
};

const handleApplyFilters = () => {
  fetchReports();
  if (activeTab === 'detailed' && selectedProject) {
    fetchDetailedReport();
  }
};


const isManager = () => {
  const managerRoles = ['CEO', 'PROJECT_MANAGER', 'CORE_BANKING_MANAGER', 'DIGITAL_BANKING_MANAGER'];
  return managerRoles.includes(currentUser?.role);
};

const getPerformanceColor = (rating) => {
  switch (rating) {
    case 'Excellent': return '#28a745';
    case 'Good': return '#17a2b8';
    case 'Needs Improvement': return '#ffc107';
    case 'At Risk': return '#dc3545';
    default: return '#6c757d';
  }
};

const handleBackToDashboard = () => {
  navigate('/dashboard');
};


const handleExport = async (type, format) => {
  try {
    console.log(`Exporting ${type} as ${format} with date range:`, dateRange);

    let url = '';
    let filename = '';

    if (type === 'dashboard') {
      if (format === 'pdf') {
        url = `/api/reports/export/dashboard/pdf?userId=${currentUser?.id}&userRole=${currentUser?.role}&startDate=${dateRange.startDate}&endDate=${dateRange.endDate}`;
        filename = `dashboard-summary-${dateRange.startDate}-to-${dateRange.endDate}.pdf`;
      } else if (format === 'excel') {
        url = `/api/reports/export/dashboard/excel?userId=${currentUser?.id}&userRole=${currentUser?.role}&startDate=${dateRange.startDate}&endDate=${dateRange.endDate}`;
        filename = `dashboard-summary-${dateRange.startDate}-to-${dateRange.endDate}.xlsx`;
      }
    } else if (type === 'projects') {
      if (format === 'pdf') {
        url = `/api/reports/export/projects/pdf?userId=${currentUser?.id}&userRole=${currentUser?.role}&startDate=${dateRange.startDate}&endDate=${dateRange.endDate}`;
        filename = `projects-report-${dateRange.startDate}-to-${dateRange.endDate}.pdf`;
      } else if (format === 'excel') {
        url = `/api/reports/export/projects/excel?userId=${currentUser?.id}&userRole=${currentUser?.role}&startDate=${dateRange.startDate}&endDate=${dateRange.endDate}`;
        filename = `projects-report-${dateRange.startDate}-to-${dateRange.endDate}.xlsx`;
      }
    } else if (type === 'tasks') {
      url = `/api/reports/export/tasks/excel?userId=${currentUser?.id}&userRole=${currentUser?.role}&startDate=${dateRange.startDate}&endDate=${dateRange.endDate}`;
      filename = `tasks-report-${dateRange.startDate}-to-${dateRange.endDate}.xlsx`;
    }

    const response = await api.get(url, { responseType: 'blob' });

    const blob = new Blob([response.data], {
      type: format === 'pdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });

    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);

    console.log('Export successful!');
  } catch (error) {
    console.error('❌ Export failed:', error);
    alert('Failed to export report. Please try again.');
  }
};


const handleExportDetailed = async (format) => {
  if (!selectedProject) {
    alert('Please select a project first');
    return;
  }

  try {
    console.log(`Exporting detailed report as ${format}:`, { projectId: selectedProject, dateRange });

    const url = `/api/reports/project/${selectedProject}/export/${format}`;
    const filename = `project-${selectedProject}-detailed-report-${dateRange.startDate}-to-${dateRange.endDate}.${format === 'pdf' ? 'pdf' : 'xlsx'}`;

    const response = await api.get(url, {
      params: {
        startDate: dateRange.startDate,
        endDate: dateRange.endDate,
        userId: currentUser?.id
      },
      responseType: 'blob'
    });

    const blob = new Blob([response.data], {
      type: format === 'pdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });

    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);

    console.log('Detailed report export successful!');
  } catch (error) {
    console.error('❌ Detailed report export failed:', error);
    alert('Failed to export detailed report: ' + (error.response?.data?.message || error.message));
  }
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


const CollapsibleSection = ({ title, children, defaultOpen = true }) => {
  const [isOpen, setIsOpen] = useState(defaultOpen);
 
    
    return (
      <div style={{ marginBottom: '20px', border: '1px solid #e0e0e0', borderRadius: '8px', overflow: 'hidden' }}>
        <button
          onClick={() => setIsOpen(!isOpen)}
          style={{
            width: '100%',
            padding: '12px 16px',
            background: '#f8f9fa',
            border: 'none',
            textAlign: 'left',
            fontWeight: '600',
            color: '#8B4513#8B4513',
            cursor: 'pointer',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            fontSize: '15px'
          }}
        >
          {title}
          <span style={{ fontSize: '12px' }}>{isOpen ? '▼' : '▶'}</span>
        </button>
        {isOpen && <div style={{ padding: '16px' }}>{children}</div>}
      </div>
    );
  };

  const MilestoneItem = ({ milestone }) => {
    const statusColors = {
      COMPLETED: '#28a745',
      IN_PROGRESS: '#007bff',
      PLANNED: '#6c757d',
      DELAYED: '#ffc107',
      CANCELLED: '#dc3545'
    };
    
    return (
      <div style={{
        padding: '12px',
        border: '1px solid #dee2e6',
        borderRadius: '6px',
        marginBottom: '8px',
        backgroundColor: milestone.isOverdue ? '#fff5f5' : '#fff'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
          <div>
            <strong style={{ fontSize: '14px' }}>{milestone.name}</strong>
            {milestone.description && (
              <p style={{ margin: '4px 0 0 0', fontSize: '13px', color: '#666' }}>{milestone.description}</p>
            )}
          </div>
          <span style={{
            padding: '4px 10px',
            borderRadius: '4px',
            fontSize: '11px',
            fontWeight: '600',
            backgroundColor: statusColors[milestone.status] || '#6c757d',
            color: milestone.status === 'COMPLETED' ? '#fff' : '#000'
          }}>
            {milestone.status}
          </span>
        </div>
        <div style={{ fontSize: '12px', color: '#666', marginTop: '8px' }}>
          Target: {milestone.targetDate}
          {milestone.completedDate && ` • Completed: ${milestone.completedDate}`}
          {milestone.isOverdue && !milestone.completedDate && (
            <span style={{ color: '#dc3545', fontWeight: '500' }}> • {Math.abs(milestone.daysUntilTarget)} days overdue</span>
          )}
        </div>
      </div>
    );
  };

 
  const TaskItem = ({ task }) => {
    const statusColors = {
      COMPLETED: '#28a745',
      IN_PROGRESS: '#007bff',
      PENDING: '#6c757d',
      BLOCKED: '#dc3545',
      CANCELLED: '#dc3545'
    };
    
    return (
      <div style={{
        padding: '12px',
        border: '1px solid #dee2e6',
        borderRadius: '6px',
        marginBottom: '8px',
        backgroundColor: task.isOverdue ? '#fff5f5' : '#fff'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', flexWrap: 'wrap', gap: '10px' }}>
          <div style={{ flex: '1 1 200px', minWidth: 0 }}>
            <strong style={{ fontSize: '14px' }}>{task.taskName}</strong>
            {task.description && (
              <p style={{ margin: '4px 0 0 0', fontSize: '13px', color: '#666', lineHeight: '1.3' }}>{task.description}</p>
            )}
            <div style={{ fontSize: '12px', color: '#666', marginTop: '6px', display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
              <span>👤 {task.assignedToName}</span>
              <span>📅 Due: {task.dueDate || 'Not set'}</span>
              {task.isOverdue && <span style={{ color: '#dc3545', fontWeight: '500' }}>⚠ OVERDUE</span>}
            </div>
          </div>
          <div style={{ textAlign: 'right', minWidth: '120px' }}>
            <span style={{
              padding: '4px 10px',
              borderRadius: '4px',
              fontSize: '11px',
              fontWeight: '600',
              backgroundColor: statusColors[task.status] || '#6c757d',
              color: task.status === 'COMPLETED' ? '#fff' : '#000'
            }}>
              {task.status}
            </span>
            {task.priority && (
              <div style={{ marginTop: '6px', fontSize: '12px' }}>
                Priority: <span style={{ color: task.priority >= 3 ? '#dc3545' : '#ffc107', fontWeight: '500' }}>{task.priorityLabel}</span>
              </div>
            )}
            {task.completionPercentage != null && (
              <div style={{ marginTop: '6px', fontSize: '12px' }}>
                Progress: {task.completionPercentage}%
              </div>
            )}
          </div>
        </div>
        
        {/* Task updates preview */}
        {task.updates && task.updates.length > 0 && (
          <div style={{ marginTop: '10px', borderTop: '1px dashed #dee2e6', paddingTop: '10px' }}>
            <div style={{ fontSize: '12px', fontWeight: '600', marginBottom: '6px', color: '#8B4513' }}>
              Recent Updates ({task.updates.length})
            </div>
            {task.updates.slice(0, 2).map(update => (
              <div key={update.id} style={{
                fontSize: '12px',
                padding: '6px 10px',
                backgroundColor: '#f8f9fa',
                borderRadius: '4px',
                marginBottom: '4px'
              }}>
                <div style={{ color: '#666', fontSize: '11px' }}>
                  {update.updateDate} • {update.submittedByName}
                </div>
                <div style={{ marginTop: '2px' }}>{update.content}</div>
                {update.blockers && (
                  <div style={{ marginTop: '4px', color: '#856404', fontSize: '11px' }}>
                    ⚠ Blockers: {update.blockers}
                  </div>
                )}
              </div>
            ))}
            {task.updates.length > 2 && (
              <div style={{ fontSize: '11px', color: '#8B4513', textAlign: 'center', marginTop: '4px' }}>
                + {task.updates.length - 2} more updates
              </div>
            )}
          </div>
        )}
      </div>
    );
  };

 
  const UpdateItem = ({ update }) => (
    <div style={{
      padding: '12px',
      border: '1px solid #e9ecef',
      borderRadius: '6px',
      marginBottom: '8px',
      backgroundColor: '#fff'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#666', marginBottom: '6px' }}>
        <span>📅 {update.updateDate}</span>
        <span>👤 {update.submittedByName} ({update.submittedByRole})</span>
      </div>
      {update.content && <p style={{ margin: '0 0 8px 0', fontSize: '14px' }}>{update.content}</p>}
      {update.blockers && (
        <div style={{ 
          padding: '6px 10px', 
          backgroundColor: '#fff3cd', 
          borderRadius: '4px', 
          fontSize: '12px',
          color: '#856404',
          marginBottom: update.nextSteps ? '6px' : 0
        }}>
          ⚠️ Blockers: {update.blockers}
        </div>
      )}
      {update.nextSteps && (
        <div style={{ fontSize: '12px', color: '#8B4513' }}>
          ➡️ Next: {update.nextSteps}
        </div>
      )}
      {update.progressPercentage != null && (
        <div style={{ fontSize: '11px', color: '#666', marginTop: '6px' }}>
          Progress: {update.progressPercentage}%
        </div>
      )}
    </div>
  );

 
  const AttachmentItem = ({ attachment }) => (
    <div style={{
      padding: '10px',
      border: '1px solid #dee2e6',
      borderRadius: '6px',
      marginBottom: '6px',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      fontSize: '13px'
    }}>
      <div>
        <strong>📄 {attachment.fileName}</strong>
        {attachment.description && <div style={{ fontSize: '12px', color: '#666', marginTop: '2px' }}>{attachment.description}</div>}
        <div style={{ fontSize: '11px', color: '#666', marginTop: '4px' }}>
          {formatFileSize(attachment.fileSize)} • {attachment.uploadedByName} • {new Date(attachment.uploadedAt).toLocaleDateString()}
        </div>
      </div>
      <button
        onClick={() => window.open(`/api/attachments/${attachment.id}/download`, '_blank')}
        style={{
          padding: '6px 12px',
          backgroundColor: '#8B4513',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer',
          fontSize: '12px'
        }}
      >
        ⬇ Download
      </button>
    </div>
  );

  if (loading) {
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
        <div className="loading"><p>Loading reports...</p></div>
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
              <h1>📊 Reports & Analytics</h1>
              <p>Comprehensive insights into project performance</p>
            </div>
            
            {/* Export Buttons for Dashboard Summary */}
            {activeTab === 'dashboard' && (
              <div style={{ display: 'flex', gap: '10px' }}>
                <button
                  onClick={() => handleExport('dashboard', 'pdf')}
                  className="btn btn-danger"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '10px 20px',
                    backgroundColor: '#dc3545',
                    color: 'white',
                    border: 'none',
                    borderRadius: '5px',
                    cursor: 'pointer',
                    fontWeight: '500'
                  }}
                >
                  📄 Export PDF
                </button>
                
                <button
                  onClick={() => handleExport('dashboard', 'excel')}
                  className="btn btn-success"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '10px 20px',
                    backgroundColor: '#28a745',
                    color: 'white',
                    border: 'none',
                    borderRadius: '5px',
                    cursor: 'pointer',
                    fontWeight: '500'
                  }}
                >
                  📊 Export Excel
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Report Tabs */}
        <div className="content-card" style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', borderBottom: '2px solid #e0e0e0', paddingBottom: '10px' }}>
            <button
              className={`btn ${activeTab === 'dashboard' ? 'btn-primary' : 'btn'}`}
              onClick={() => setActiveTab('dashboard')}
              style={{ 
                backgroundColor: activeTab === 'dashboard' ? '#8B4513' : 'white',
                color: activeTab === 'dashboard' ? 'white' : '#8B4513',
                border: activeTab === 'dashboard' ? '1px solid #8B4513' : '1px solid #8B4513'
              }}
            >
              📊 Dashboard Summary
            </button>
            <button
              className={`btn ${activeTab === 'rag' ? 'btn-primary' : 'btn'}`}
              onClick={() => setActiveTab('rag')}
              style={{ 
                backgroundColor: activeTab === 'rag' ? '#8B4513' : 'white',
                color: activeTab === 'rag' ? 'white' : '#8B4513',
                border: activeTab === 'rag' ? '1px solid #8B4513' : '1px solid #8B4513'
              }}
            >
              🚦 RAG Status
            </button>
            <button
              className={`btn ${activeTab === 'performance' ? 'btn-primary' : 'btn'}`}
              onClick={() => setActiveTab('performance')}
              style={{ 
                backgroundColor: activeTab === 'performance' ? '#8B4513' : 'white',
                color: activeTab === 'performance' ? 'white' : '#8B4513',
                border: activeTab === 'performance' ? '1px solid #8B4513' : '1px solid #8B4513'
              }}
            >
              {isManager() ? '👥 Team Performance' : '👤 My Performance'}
            </button>
            {/* ✅ NEW: Detailed Report Tab */}
            <button
              className={`btn ${activeTab === 'detailed' ? 'btn-primary' : 'btn'}`}
              onClick={() => setActiveTab('detailed')}
              style={{ 
                backgroundColor: activeTab === 'detailed' ? '#8B4513' : 'white',
                color: activeTab === 'detailed' ? 'white' : '#8B4513',
                border: activeTab === 'detailed' ? '1px solid #8B4513' : '1px solid #8B4513'
              }}
            >
              📋 Detailed Project Report
            </button>
          </div>
        </div>

        {/* Date Filter */}
        <div className="content-card" style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '15px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Start Date
              </label>
              <input
                type="date"
                name="startDate"
                value={dateRange.startDate}
                onChange={handleDateChange}
                style={{ padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              />
            </div>
            
            <div>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                End Date
              </label>
              <input
                type="date"
                name="endDate"
                value={dateRange.endDate}
                onChange={handleDateChange}
                style={{ padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              />
            </div>

            <button 
              className="btn btn-primary"
              onClick={handleApplyFilters}
              style={{ padding: '10px 20px' }}
            >
              Apply Filters
            </button>
          </div>
        </div>

        {/* ✅ NEW: Project Selector - Only show on detailed tab */}
        {activeTab === 'detailed' && (
          <div className="content-card" style={{ marginBottom: '20px' }}>
            <div style={{ display: 'flex', gap: '15px', alignItems: 'flex-end', flexWrap: 'wrap' }}>
              <div style={{ flex: '1', minWidth: '250px' }}>
                <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                  Select Project *
                </label>
               <Select
  options={projects.map(p => ({
    value: p.id,
    label: `${p.name}${p.ragStatus ? ` (${p.ragStatus})` : ''}`
  }))}
  value={
    projects
      .map(p => ({
        value: p.id,
        label: `${p.name}${p.ragStatus ? ` (${p.ragStatus})` : ''}`
      }))
      .find(o => o.value === selectedProject) || null
  }
  onChange={(option) => {
    console.log("📋 Project selected:", option);
    setSelectedProject(option ? option.value : null);
    setDetailedReport(null);
    setReportError("");
  }}
  placeholder="Search and select project..."
  isSearchable
/>
              </div>
              
              <button 
                className="btn btn-primary"
                onClick={fetchDetailedReport}
                disabled={!selectedProject || reportLoading}
                style={{ padding: '10px 20px' }}
              >
                {reportLoading ? 'Generating...' : 'Generate Report'}
              </button>
              
              {selectedProject && detailedReport && (
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button
                    onClick={() => handleExportDetailed('pdf')}
                    className="btn btn-danger"
                    style={{ padding: '10px 15px', backgroundColor: '#dc3545', color: 'white', border: 'none', borderRadius: '5px' }}
                  >
                    📄 Export PDF
                  </button>
                  <button
                    onClick={() => handleExportDetailed('excel')}
                    className="btn btn-success"
                    style={{ padding: '10px 15px', backgroundColor: '#28a745', color: 'white', border: 'none', borderRadius: '5px' }}
                  >
                    📊 Export Excel
                  </button>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Dashboard Summary Tab */}
        {activeTab === 'dashboard' && (
          <div>
            {/* Statistics Cards */}
            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-label">Total Projects</div>
                <div className="stat-value">{dashboardStats?.totalProjects || 0}</div>
              </div>

              <div className="stat-card green">
                <div className="stat-label">Active Projects</div>
                <div className="stat-value">{dashboardStats?.activeProjects || 0}</div>
              </div>

              <div className="stat-card red">
                <div className="stat-label">Critical Projects</div>
                <div className="stat-value">{dashboardStats?.criticalProjects || 0}</div>
              </div>

              <div className="stat-card amber">
                <div className="stat-label">Overdue Tasks</div>
                <div className="stat-value">{dashboardStats?.overdueTasks || 0}</div>
              </div>

              <div className="stat-card">
                <div className="stat-label">Completed Projects</div>
                <div className="stat-value" style={{ color: '#28a745' }}>{dashboardStats?.completedProjects || 0}</div>
              </div>

              <div className="stat-card">
                <div className="stat-label">Active Blockers</div>
                <div className="stat-value" style={{ color: '#dc3545' }}>{dashboardStats?.activeBlockers || 0}</div>
              </div>

              <div className="stat-card">
                <div className="stat-label">Overdue Milestones</div>
                <div className="stat-value" style={{ color: '#dc3545' }}>{dashboardStats?.overdueMilestones || 0}</div>
              </div>

              <div className="stat-card">
                <div className="stat-label">APIs in Development</div>
                <div className="stat-value" style={{ color: '#007bff' }}>{dashboardStats?.apisInDevelopment || 0}</div>
              </div>
            </div>

            {/* RAG Status Overview */}
            <div className="content-card" style={{ marginTop: '20px' }}>
              <h2>RAG Status Overview</h2>
              <div style={{ display: 'flex', gap: '30px', flexWrap: 'wrap', marginTop: '20px' }}>
                <div style={{ flex: 1, minWidth: '200px', textAlign: 'center' }}>
                  <div style={{ 
                    width: '120px', 
                    height: '120px', 
                    borderRadius: '50%', 
                    backgroundColor: '#28a745',
                    margin: '0 auto 15px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'white',
                    fontSize: '32px',
                    fontWeight: 'bold'
                  }}>
                    {dashboardStats?.ragStatus?.GREEN || 0}
                  </div>
                  <p style={{ fontWeight: '600', color: '#28a745' }}>GREEN (On Track)</p>
                </div>

                <div style={{ flex: 1, minWidth: '200px', textAlign: 'center' }}>
                  <div style={{ 
                    width: '120px', 
                    height: '120px', 
                    borderRadius: '50%', 
                    backgroundColor: '#ffc107',
                    margin: '0 auto 15px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#000',
                    fontSize: '32px',
                    fontWeight: 'bold'
                  }}>
                    {dashboardStats?.ragStatus?.AMBER || 0}
                  </div>
                  <p style={{ fontWeight: '600', color: '#ffc107' }}>AMBER (At Risk)</p>
                </div>

                <div style={{ flex: 1, minWidth: '200px', textAlign: 'center' }}>
                  <div style={{ 
                    width: '120px', 
                    height: '120px', 
                    borderRadius: '50%', 
                    backgroundColor: '#dc3545',
                    margin: '0 auto 15px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'white',
                    fontSize: '32px',
                    fontWeight: 'bold'
                  }}>
                    {dashboardStats?.ragStatus?.RED || 0}
                  </div>
                  <p style={{ fontWeight: '600', color: '#dc3545' }}>RED (Critical)</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* RAG Status Tab */}
        {activeTab === 'rag' && (
          <div className="content-card">
            <h2>RAG Status Detailed Report</h2>
            
            {ragReport ? (
              <div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px', marginTop: '20px' }}>
                  <div style={{ padding: '20px', backgroundColor: '#d4edda', borderRadius: '8px', textAlign: 'center' }}>
                    <h3 style={{ margin: '0 0 10px 0', color: '#155724' }}>Green Projects</h3>
                    <div style={{ fontSize: '48px', fontWeight: 'bold', color: '#28a745' }}>
                      {ragReport.green ?? dashboardStats?.ragStatus?.GREEN ?? 0}
                    </div>
                    <p style={{ margin: '10px 0 0 0', color: '#155724' }}>
                      {(() => {
                        const green = ragReport.green ?? dashboardStats?.ragStatus?.GREEN ?? 0;
                        const total = ragReport.total ?? dashboardStats?.totalProjects ?? 0;
                        const percentage = total > 0 ? Math.round((green / total) * 100) : 0;
                        return `${percentage}% of total`;
                      })()}
                    </p>
                  </div>

                  <div style={{ padding: '20px', backgroundColor: '#fff3cd', borderRadius: '8px', textAlign: 'center' }}>
                    <h3 style={{ margin: '0 0 10px 0', color: '#856404' }}>Amber Projects</h3>
                    <div style={{ fontSize: '48px', fontWeight: 'bold', color: '#ffc107' }}>
                      {ragReport.amber ?? dashboardStats?.ragStatus?.AMBER ?? 0}
                    </div>
                    <p style={{ margin: '10px 0 0 0', color: '#856404' }}>
                      {(() => {
                        const amber = ragReport.amber ?? dashboardStats?.ragStatus?.AMBER ?? 0;
                        const total = ragReport.total ?? dashboardStats?.totalProjects ?? 0;
                        const percentage = total > 0 ? Math.round((amber / total) * 100) : 0;
                        return `${percentage}% of total`;
                      })()}
                    </p>
                  </div>

                  <div style={{ padding: '20px', backgroundColor: '#f8d7da', borderRadius: '8px', textAlign: 'center' }}>
                    <h3 style={{ margin: '0 0 10px 0', color: '#721c24' }}>Red Projects</h3>
                    <div style={{ fontSize: '48px', fontWeight: 'bold', color: '#dc3545' }}>
                      {ragReport.red ?? dashboardStats?.ragStatus?.RED ?? 0}
                    </div>
                    <p style={{ margin: '10px 0 0 0', color: '#721c24' }}>
                      {(() => {
                        const red = ragReport.red ?? dashboardStats?.ragStatus?.RED ?? 0;
                        const total = ragReport.total ?? dashboardStats?.totalProjects ?? 0;
                        const percentage = total > 0 ? Math.round((red / total) * 100) : 0;
                        return `${percentage}% of total`;
                      })()}
                    </p>
                  </div>
                </div>

                {ragReport.criticalProjects && ragReport.criticalProjects.length > 0 && (
                  <div style={{ marginTop: '30px' }}>
                    <h3 style={{ color: '#dc3545', marginBottom: '15px' }}>⚠ Critical Projects Requiring Attention</h3>
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Project Name</th>
                          <th>Status</th>
                          <th>RAG Status</th>
                          <th>End Date</th>
                          <th>Completion</th>
                        </tr>
                      </thead>
                      <tbody>
                        {ragReport.criticalProjects.map(project => (
                          <tr key={project.id}>
                            <td style={{ fontWeight: '600' }}>{project.projectName}</td>
                            <td>{project.status}</td>
                            <td>
                              <span className="badge" style={{ 
                                backgroundColor: project.ragStatus === 'RED' ? '#dc3545' : '#ffc107',
                                color: project.ragStatus === 'RED' ? 'white' : 'black'
                              }}>
                                {project.ragStatus}
                              </span>
                            </td>
                            <td>{project.endDate || 'Not set'}</td>
                            <td>{project.completionPercentage || 0}%</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                <p>Loading RAG report...</p>
              </div>
            )}
          </div>
        )}

        {/* Performance Tab */}
        {activeTab === 'performance' && (
          <div className="content-card">
            <h2>{isManager() ? 'Team Performance Report' : 'My Performance Report'}</h2>
            <p style={{ color: '#666', marginBottom: '20px' }}>
              Period: {dateRange.startDate} to {dateRange.endDate}
            </p>
            
            {isManager() ? (
              allDevelopersReport.length > 0 ? (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Developer</th>
                      <th>Role</th>
                      <th>Total Tasks</th>
                      <th>Completed</th>
                      <th>Pending</th>
                      <th>Completion Rate</th>
                      <th>Progress Updates</th>
                      <th>Performance</th>
                    </tr>
                  </thead>
                  <tbody>
                    {allDevelopersReport.map(dev => (
                      <tr key={dev.userId}>
                        <td style={{ fontWeight: '600' }}>{dev.fullName || dev.username}</td>
                        <td>
                          <span className="badge badge-green">{dev.role}</span>
                        </td>
                        <td>{dev.totalTasks || 0}</td>
                        <td style={{ color: '#28a745', fontWeight: '600' }}>{dev.completedTasks || 0}</td>
                        <td>{dev.pendingTasks || 0}</td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            <div style={{ flex: 1, height: '8px', backgroundColor: '#e0e0e0', borderRadius: '4px' }}>
                              <div 
                                style={{ 
                                  width: `${dev.completionRate || 0}%`,
                                  height: '100%',
                                  backgroundColor: dev.completionRate >= 80 ? '#28a745' : dev.completionRate >= 50 ? '#ffc107' : '#dc3545',
                                  borderRadius: '4px'
                                }}
                              />
                            </div>
                            <span style={{ fontSize: '12px', minWidth: '40px' }}>
                              {dev.completionRate || 0}%
                            </span>
                          </div>
                        </td>
                        <td>{dev.totalUpdates || 0}</td>
                        <td>
                          <span className="badge" style={{ 
                            backgroundColor: dev.completionRate >= 80 ? '#28a745' : dev.completionRate >= 50 ? '#ffc107' : '#dc3545',
                            color: dev.completionRate >= 80 ? 'white' : 'black'
                          }}>
                            {dev.completionRate >= 80 ? 'Excellent' : dev.completionRate >= 50 ? 'Good' : 'Needs Improvement'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                  <p style={{ fontSize: '16px' }}>No developer data available for this period</p>
                </div>
              )
            ) : (
              myPerformance ? (
                <div style={{ maxWidth: '800px', margin: '0 auto' }}>
                  <div className="stats-grid" style={{ marginBottom: '30px' }}>
                    <div className="stat-card">
                      <div className="stat-label">Total Tasks</div>
                      <div className="stat-value">{myPerformance.totalTasks || 0}</div>
                    </div>
                    <div className="stat-card green">
                      <div className="stat-label">Completed</div>
                      <div className="stat-value">{myPerformance.completedTasks || 0}</div>
                    </div>
                    <div className="stat-card amber">
                      <div className="stat-label">Pending</div>
                      <div className="stat-value">{myPerformance.pendingTasks || 0}</div>
                    </div>
                    <div className="stat-card">
                      <div className="stat-label">Completion Rate</div>
                      <div className="stat-value" style={{ color: myPerformance.completionRate >= 80 ? '#28a745' : myPerformance.completionRate >= 50 ? '#ffc107' : '#dc3545' }}>
                        {myPerformance.completionRate || 0}%
                      </div>
                    </div>
                  </div>

                  <div style={{ 
                    padding: '20px', 
                    borderRadius: '10px', 
                    backgroundColor: getPerformanceColor(myPerformance.performanceRating),
                    color: 'white',
                    textAlign: 'center',
                    marginBottom: '30px'
                  }}>
                    <h3 style={{ margin: '0 0 10px 0' }}>Performance Rating</h3>
                    <div style={{ fontSize: '48px', fontWeight: 'bold' }}>
                      {myPerformance.performanceRating || 'N/A'}
                    </div>
                    <p style={{ margin: '10px 0 0 0', opacity: 0.9 }}>
                      Based on task completion rate
                    </p>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '20px' }}>
                    <div style={{ padding: '20px', backgroundColor: '#f8f9fa', borderRadius: '8px' }}>
                      <h4 style={{ margin: '0 0 10px 0', color: '#8B4513' }}>📝 Progress Updates</h4>
                      <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#8B4513' }}>
                        {myPerformance.totalUpdates || 0}
                      </div>
                      <p style={{ margin: '5px 0 0 0', color: '#666', fontSize: '14px' }}>
                        Total updates submitted
                      </p>
                    </div>

                    <div style={{ padding: '20px', backgroundColor: '#f8f9fa', borderRadius: '8px' }}>
                      <h4 style={{ margin: '0 0 10px 0', color: '#8B4513' }}>⚠ Updates with Blockers</h4>
                      <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#dc3545' }}>
                        {myPerformance.updatesWithBlockers || 0}
                      </div>
                      <p style={{ margin: '5px 0 0 0', color: '#666', fontSize: '14px' }}>
                        Issues reported
                      </p>
                    </div>
                  </div>

                  <div style={{ marginTop: '30px', padding: '20px', backgroundColor: '#e9ecef', borderRadius: '8px' }}>
                    <h4 style={{ margin: '0 0 15px 0', color: '#8B4513' }}>📊 Productivity Score</h4>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                      <div style={{ flex: 1, height: '20px', backgroundColor: '#dee2e6', borderRadius: '10px' }}>
                        <div 
                          style={{ 
                            width: `${myPerformance.productivityScore || 0}%`,
                            height: '100%',
                            backgroundColor: myPerformance.productivityScore >= 80 ? '#28a745' : 
                                           myPerformance.productivityScore >= 50 ? '#ffc107' : '#dc3545',
                            borderRadius: '10px',
                            transition: 'width 0.3s ease'
                          }}
                        />
                      </div>
                      <span style={{ fontWeight: 'bold', fontSize: '18px' }}>
                        {myPerformance.productivityScore || 0}%
                      </span>
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                  <p style={{ fontSize: '16px' }}>No performance data available for this period</p>
                  <p style={{ fontSize: '14px', marginTop: '10px' }}>
                    Start submitting daily updates to track your performance!
                  </p>
                </div>
              )
            )}
          </div>
        )}

        {/* ✅ NEW: Detailed Project Report Tab */}
        {activeTab === 'detailed' && (
          <div>
            {reportError && (
              <div style={{
                padding: '12px 16px',
                marginBottom: '20px',
                background: '#f8d7da',
                color: '#721c24',
                borderRadius: '6px',
                border: '1px solid #f5c6cb'
              }}>
                {reportError}
              </div>
            )}
            
            {reportLoading ? (
              <div style={{ padding: '40px', textAlign: 'center', color: '#666' }}>
                <div style={{ fontSize: '24px', marginBottom: '10px' }}>⏳</div>
                <p>Generating comprehensive report...</p>
                <p style={{ fontSize: '12px', color: '#999' }}>This may take a moment for large projects</p>
              </div>
            ) : detailedReport ? (
              <div className="content-card">
                {/* Report Header */}
                <div style={{ marginBottom: '25px', paddingBottom: '15px', borderBottom: '2px solid #e0e0e0' }}>
                  <h2 style={{ margin: '0 0 5px 0', color: '#8B4513' }}>
                    📋 {detailedReport.projectName}
                  </h2>
                  <p style={{ margin: '0', color: '#666' }}>
                    {detailedReport.description}
                  </p>
                  <div style={{ display: 'flex', gap: '20px', marginTop: '10px', flexWrap: 'wrap' }}>
                    <span><strong>Manager:</strong> {detailedReport.managerName}</span>
                    <span><strong>Status:</strong> {detailedReport.status}</span>
                    <span>
                      <strong>RAG:</strong>{' '}
                      <span style={{
                        padding: '3px 10px',
                        borderRadius: '4px',
                        backgroundColor: detailedReport.ragStatus === 'GREEN' ? '#28a745' : 
                                       detailedReport.ragStatus === 'AMBER' ? '#ffc107' : '#dc3545',
                        color: detailedReport.ragStatus === 'AMBER' ? '#000' : '#fff',
                        fontSize: '12px',
                        fontWeight: '600'
                      }}>
                        {detailedReport.ragStatus}
                      </span>
                    </span>
                    <span><strong>Period:</strong> {dateRange.startDate} to {dateRange.endDate}</span>
                  </div>
                </div>

                {/* Summary Cards */}
                {detailedReport.summary && (
                  <CollapsibleSection title="📊 Summary Statistics" defaultOpen={true}>
                    <div className="stats-grid">
                      <div className="stat-card">
                        <div className="stat-label">Total Tasks</div>
                        <div className="stat-value">{detailedReport.summary.totalTasks || 0}</div>
                      </div>
                      <div className="stat-card green">
                        <div className="stat-label">Completed</div>
                        <div className="stat-value">{detailedReport.summary.completedTasks || 0}</div>
                      </div>
                      <div className="stat-card amber">
                        <div className="stat-label">Pending</div>
                        <div className="stat-value">{detailedReport.summary.pendingTasks || 0}</div>
                      </div>
                      <div className="stat-card">
                        <div className="stat-label">Completion</div>
                        <div className="stat-value" style={{ color: detailedReport.summary.completionPercentage >= 80 ? '#28a745' : detailedReport.summary.completionPercentage >= 50 ? '#ffc107' : '#dc3545' }}>
                          {detailedReport.summary.completionPercentage || 0}%
                        </div>
                      </div>
                      <div className="stat-card">
                        <div className="stat-label">Updates</div>
                        <div className="stat-value">{detailedReport.summary.totalUpdates || 0}</div>
                      </div>
                      <div className="stat-card">
                        <div className="stat-label">With Blockers</div>
                        <div className="stat-value" style={{ color: '#dc3545' }}>{detailedReport.summary.updatesWithBlockers || 0}</div>
                      </div>
                      <div className="stat-card">
                        <div className="stat-label">Milestones</div>
                        <div className="stat-value">{detailedReport.summary.milestonesCompleted || 0}/{detailedReport.summary.milestonesTotal || 0}</div>
                      </div>
                      <div className="stat-card">
                        <div className="stat-label">Attachments</div>
                        <div className="stat-value">{detailedReport.summary.totalAttachments || 0}</div>
                      </div>
                    </div>
                    {detailedReport.summary.daysUntilDeadline != null && (
                      <div style={{ marginTop: '15px', fontSize: '14px', color: detailedReport.summary.daysUntilDeadline < 0 ? '#dc3545' : '#666' }}>
                        {detailedReport.summary.daysUntilDeadline < 0 
                          ? `⚠ Deadline passed ${Math.abs(detailedReport.summary.daysUntilDeadline)} days ago`
                          : `📅 ${detailedReport.summary.daysUntilDeadline} days until deadline`}
                      </div>
                    )}
                  </CollapsibleSection>
                )}

                {/* Milestones Section */}
                {detailedReport.milestones && detailedReport.milestones.length > 0 && (
                  <CollapsibleSection title="🎯 Project Milestones" defaultOpen={true}>
                    <div style={{ display: 'grid', gap: '12px' }}>
                      {detailedReport.milestones.map(milestone => (
                        <MilestoneItem key={milestone.id} milestone={milestone} />
                      ))}
                    </div>
                  </CollapsibleSection>
                )}

                {/* Tasks Section */}
                {detailedReport.tasks && detailedReport.tasks.length > 0 && (
                  <CollapsibleSection title="✅ Tasks & Progress" defaultOpen={true}>
                    <div style={{ display: 'grid', gap: '15px' }}>
                      {detailedReport.tasks.map(task => (
                        <TaskItem key={task.id} task={task} />
                      ))}
                    </div>
                  </CollapsibleSection>
                )}

                {/* Daily Updates Timeline */}
                {detailedReport.dailyUpdates && detailedReport.dailyUpdates.length > 0 && (
                  <CollapsibleSection title="📅 Daily Progress Updates" defaultOpen={false}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                      {detailedReport.dailyUpdates.map(update => (
                        <UpdateItem key={update.id} update={update} />
                      ))}
                    </div>
                  </CollapsibleSection>
                )}

                {/* Attachments Section */}
                {detailedReport.attachments && detailedReport.attachments.length > 0 && (
                  <CollapsibleSection title={`📎 Attachments (${detailedReport.totalAttachments || 0})`} defaultOpen={false}>
                    <div style={{ display: 'grid', gap: '8px' }}>
                      {detailedReport.attachments.map(attachment => (
                        <AttachmentItem key={attachment.id} attachment={attachment} />
                      ))}
                      {detailedReport.totalAttachments > 10 && (
                        <div style={{ textAlign: 'center', fontSize: '13px', color: '#666', marginTop: '10px' }}>
                          + {detailedReport.totalAttachments - 10} more attachments (showing recent 10)
                        </div>
                      )}
                    </div>
                  </CollapsibleSection>
                )}

                {/* Empty state */}
                {!detailedReport.summary && !detailedReport.milestones?.length && !detailedReport.tasks?.length && !detailedReport.dailyUpdates?.length && !detailedReport.attachments?.length && (
                  <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                    <p style={{ fontSize: '16px' }}>No data available for this project and date range</p>
                    <p style={{ fontSize: '14px', marginTop: '10px' }}>
                      Try adjusting the date range or check if the project has activity
                    </p>
                  </div>
                )}
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                <p style={{ fontSize: '16px' }}>Select a project and click "Generate Report" to view details</p>
                <p style={{ fontSize: '14px', marginTop: '10px' }}>
                  The detailed report includes project summary, milestones, tasks, daily updates, and attachments
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default Reports;