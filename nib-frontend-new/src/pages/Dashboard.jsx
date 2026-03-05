import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import NotificationBell from '../components/NotificationBell';

const Dashboard = () => {
  const { currentUser, logout } = useAuth();
  const [stats, setStats] = useState(null);
  const [recentProjects, setRecentProjects] = useState([]);
  const [recentActivities, setRecentActivities] = useState([]); 
  const [loading, setLoading] = useState(true);
  const [activitiesLoading, setActivitiesLoading] = useState(true); 
  const [showComments, setShowComments] = useState(false);
  const [selectedProject, setSelectedProject] = useState(null);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [replyTo, setReplyTo] = useState(null);
  const [replyText, setReplyText] = useState('');

 useEffect(() => {
  console.log('=== DASHBOARD MOUNTED ===');
  console.log('Current user:', currentUser);
  fetchDashboardStats();
  fetchRecentProjects();

 
  if (hasFullAccess()) {
    fetchRecentActivities();
  }
}, []);
const fetchDashboardStats = async () => {
  try {
    console.log('📊 Fetching dashboard stats...');
    console.log('User ID:', currentUser?.id);
    console.log('User Role:', currentUser?.role);

    const response = await api.get('/api/reports/dashboard-summary', {
      params: {
        userId: currentUser?.id,
        userRole: currentUser?.role
      }
    });

    console.log('✅ Dashboard stats response:', response.data);
    
    let statsData = response.data;

    if (isBusiness()) {
      console.log('⚠️ Business user - will calculate stats from projects list');
      const projectsResponse = await api.get('/api/projects', {
        params: {
          userId: currentUser?.id,
          userRole: currentUser?.role
        }
      });

      let projectsData = projectsResponse.data;
      if (typeof projectsData === 'string') {
        projectsData = JSON.parse(projectsData);
      }
      const userProjects = Array.isArray(projectsData)
        ? projectsData.filter(project => {
            return project.manager?.id === currentUser?.id ||
                   project.createdBy?.id === currentUser?.id ||
                   project.managerId === currentUser?.id ||
                   project.createdById === currentUser?.id;
          })
        : [];

      console.log('📊 Calculating stats from', userProjects.length, 'projects');
      statsData = {
        totalProjects: userProjects.length,
        activeProjects: userProjects.filter(p => 
          p.status === 'ACTIVE' || p.status === 'PLANNED'
        ).length,
        completedProjects: userProjects.filter(p => 
          p.status === 'COMPLETED'
        ).length,
        overdueTasks: 0, 
        overdueMilestones: 0,
        activeBlockers: 0,
        upcomingMilestones: statsData.upcomingMilestones || 0,
        criticalProjects: statsData.criticalProjects || 0
      };

      console.log('✅ Calculated business user stats:', statsData);
    }
    
    setStats(statsData);
  } catch (error) {
    console.error('❌ Error fetching dashboard stats:', error);
    console.error('Error response:', error.response);

    setStats({
      totalProjects: 0,
      activeProjects: 0,
      completedProjects: 0,
      overdueTasks: 0,
      overdueMilestones: 0,
      activeBlockers: 0
    });
  } finally {
    setLoading(false);
  }
};

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
const fetchRecentProjects = async () => {
  try {
    console.log('📁 Fetching recent projects...');
    console.log('Current user:', currentUser);
    console.log('Is technical staff?', isTechnicalStaff());
    console.log('Is business?', isBusiness());

    const response = await api.get('/api/projects', {
      params: {
        userId: currentUser?.id,
        userRole: currentUser?.role
      }
    });

    let projectsData = response.data;
    if (typeof projectsData === 'string') {
      projectsData = JSON.parse(projectsData);
    }

    console.log('✅ All projects from API:', projectsData);
    console.log('Number of projects:', Array.isArray(projectsData) ? projectsData.length : 0);

    if (isTechnicalStaff()) {
      console.log('User is technical staff - filtering by assigned tasks...');

      const tasksResponse = await api.get('/api/tasks', {
        params: {
          userId: currentUser?.id,
          userRole: currentUser?.role
        }
      });

      let tasksData = tasksResponse.data;
      if (typeof tasksData === 'string') {
        tasksData = JSON.parse(tasksData);
      }

      console.log('✅ User tasks from API:', tasksData);
      console.log('Number of tasks:', Array.isArray(tasksData) ? tasksData.length : 0);

      const assignedProjectIds = [...new Set(
        tasksData.map(task => {
          console.log('Task:', task);
          console.log('  - task.project:', task.project);
          console.log('  - task.projectId:', task.projectId);

          return task.project?.id ||
                 task.projectId ||
                 task.project?.projectId;
        }).filter(Boolean)
      )];

      console.log('🎯 Assigned project IDs:', assignedProjectIds);

      if (assignedProjectIds.length === 0) {
        console.warn('⚠️ No project IDs found in tasks!');
        console.log('Tasks data structure:', JSON.stringify(tasksData, null, 2));
      }

      const assignedProjects = Array.isArray(projectsData)
        ? projectsData.filter(project => assignedProjectIds.includes(project.id))
        : [];

      console.log('✅ Filtered assigned projects:', assignedProjects);
      console.log('Number of filtered projects:', assignedProjects.length);

      setRecentProjects(assignedProjects.slice(0, 5));
    } 
   else if (isBusiness()) {
  console.log('User is business - filtering by created/managed projects...');
  
  const userProjects = Array.isArray(projectsData)
    ? projectsData.filter(project => {
        const managerId = project.manager?.id || project.managerId;
        const createdById = project.createdBy?.id || project.createdById;
        const initiatedById = project.initiatedBy?.id || project.initiatedById;
        
        console.log(`Project: ${project.projectName}`, {
          managerId,
          createdById,
          initiatedById,
          currentUserId: currentUser?.id
        });
        
        return managerId === currentUser?.id ||
               createdById === currentUser?.id ||
               initiatedById === currentUser?.id;
      })
    : [];

  console.log('✅ Filtered projects for business user:', userProjects);
  console.log('Number of filtered projects:', userProjects.length);

  setRecentProjects(userProjects.slice(0, 5));
}
    else {
      console.log('User is manager/executive - showing all projects');
      const projectsToShow = Array.isArray(projectsData) ? projectsData.slice(0, 5) : [];
      console.log('Projects to show:', projectsToShow);
      setRecentProjects(projectsToShow);
    }
  } catch (error) {
    console.error('❌ Error fetching recent projects:', error);
    console.error('Error response:', error.response);
    setRecentProjects([]);
  }
};

const fetchRecentActivities = async () => {
  try {
    setActivitiesLoading(true);
    const response = await api.get('/api/activity-logs/recent', {
      params: { limit: 10 }
    });
    console.log('📜 Recent activities loaded:', response.data);
    setRecentActivities(Array.isArray(response.data) ? response.data : []);
  } catch (error) {
    console.error('❌ Failed to load recent activities:', error);
    setRecentActivities([]);
  } finally {
    setActivitiesLoading(false);
  }
};

const handleLogout = async () => {
  try {
    
    await api.post('/api/auth/logout', {}, {
      headers: {
        'X-User-Id': currentUser?.id
      }
    });

    console.log('📝 Logout logged successfully');
  } catch (error) {
    console.error('❌ Failed to log logout:', error);
    
  } finally {
    logout();  
    window.location.href = '/login';
  }
};

const getRagColor = (status) => {
  switch (status) {
    case 'GREEN': return '#28a745';
    case 'AMBER': return '#ffc107';
    case 'RED': return '#dc3545';
    default: return '#6c757d';
  }
};



  const isExecutive = () => {
    return ['CEO', 'DEPUTY_CHIEF', 'DIRECTOR'].includes(currentUser?.role);
  };

  
  const hasFullAccess = () => {
    return currentUser?.role === 'DIGITAL_BANKING_MANAGER';
  };

  const isRestrictedManager = () => {
    return currentUser?.role === 'PROJECT_MANAGER';
  };
const isQUALITY = () => {
    return currentUser?.role === 'QUALITY_ASSURANCE';
  };
 
  const isBusiness = () => {
    return currentUser?.role === 'BUSINESS';
  };

const isCEO = () => {
  return currentUser?.role === 'CEO';
};

const isTechnicalStaff = () => {
  const techRoles = [
    'DEVELOPER',
    'SENIOR_IT_OFFICER',
    'JUNIOR_IT_OFFICER',
    'IT_GRADUATE_TRAINEE'
  ];
  return techRoles.includes(currentUser?.role);
};
 const canViewProjects = () => {
    return isTechnicalStaff() || isQUALITY() || isExecutive()||isBusiness() || hasFullAccess() || isRestrictedManager(); 
  };

  
  const canViewTasks = () => {
    return hasFullAccess() || isTechnicalStaff()||isQUALITY();
  };

  
  const canViewMilestones = () => {
    return hasFullAccess() || isQUALITY();
  };

  
  const canViewUpdates = () => {
    return hasFullAccess() || isTechnicalStaff();
  };

 
  const canViewAPIs = () => {
    return hasFullAccess() || isTechnicalStaff()|| isQUALITY();
  };

  
  const canViewUserManagement = () => {
    return hasFullAccess();
  };

 
  const canViewActivityLogs = () => {
    return hasFullAccess();
  };

  
  const canViewReports = () => {
    return hasFullAccess() || isExecutive()|| isQUALITY();
  };

const loadComments = async (projectId) => {
  try {
    console.log('Loading comments for project:', projectId);
    const response = await api.get(`/api/projects/${projectId}/comments`);
    console.log('Comments loaded:', response.data);
    setComments(Array.isArray(response.data) ? response.data : []);
  } catch (error) {
    console.error('Error loading comments:', error);
    setComments([]);
  }
};

const handleAddComment = async (e) => {
  e.preventDefault();
  if (!newComment.trim() || !selectedProject) return;

  try {
    console.log('Adding comment to project:', selectedProject.id);
    await api.post(`/api/projects/${selectedProject.id}/comments`, null, {
      params: {
        userId: currentUser?.id,
        commentText: newComment,
        parentCommentId: null
      }
    });

    setNewComment('');
    await loadComments(selectedProject.id);
  } catch (error) {
    console.error('Error adding comment:', error);
    alert('Failed to add comment: ' + (error.response?.data?.message || error.message));
  }
};

const handleAddReply = async (commentId) => {
  if (!replyText.trim() || !selectedProject) return;

  try {
    console.log('Adding reply to comment:', commentId);
    await api.post(`/api/projects/${selectedProject.id}/comments`, null, {
      params: {
        userId: currentUser?.id,
        commentText: replyText,
        parentCommentId: commentId
      }
    });

    setReplyText('');
    setReplyTo(null);
    await loadComments(selectedProject.id);
  } catch (error) {
    console.error('Error adding reply:', error);
    alert('Failed to add reply: ' + (error.response?.data?.message || error.message));
  }
};

const openCommentsModal = (project) => {
  console.log('Opening comments for project:', project);
  setSelectedProject(project);
  setComments([]);
  setNewComment('');
  setShowComments(true);
  loadComments(project.id);
};

const closeCommentsModal = () => {
  setShowComments(false);
  setSelectedProject(null);
  setComments([]);
  setNewComment('');
  setReplyTo(null);
  setReplyText('');
};

if (loading) {
  return (
    <div className="loading">
      <p>Loading dashboard...</p>
    </div>
  );
}

  return (
    <div className="dashboard-container">
      {/* Navigation Bar */}
      <nav className="navbar">
  <div className="navbar-brand" style={{ 
    display: 'flex', 
    alignItems: 'center', 
    gap: '15px' 
  }}>
    {/* Logo */}
    <img 
      src="/nibicon.jpeg" 
      alt="NIB Logo" 
      style={{
        width: '45px',
        height: '45px',
        objectFit: 'contain',
        borderRadius: '8px',
        backgroundColor: 'white',
        padding: '5px',
        boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
      }}
    />
    
    {/* Text */}
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      <span style={{ 
        fontSize: '20px', 
        fontWeight: 'bold', 
        textShadow: '1px 1px 2px rgba(0,0,0,0.3)'
      }}>
        NIB IT Project Tracking
      </span>
      <h4 style={{ 
        fontSize: '14px', 
        margin: '2px 0 0 0',
        opacity: '0.9'
      }}>
        ንብ ኢንተርናሽናል ባንክ
      </h4>
    </div>
  </div>

  <div className="navbar-user">
    <span>Welcome, {currentUser?.fullName || currentUser?.username}</span>
    <NotificationBell currentUser={currentUser} />
    <button onClick={handleLogout} className="logout-btn">Logout</button>
  </div>
</nav>
{/* Top Navigation Menu - ROLE BASED */}
<div style={{ 
  background: 'linear-gradient(135deg, #8B4513 0%, #A0522D 100%)',
  padding: '10px 20px',
  display: 'flex',
  gap: '20px',
  justifyContent: 'center',
  flexWrap: 'wrap'
}}>
    <a href="/dashboard" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px', backgroundColor: 'rgba(255,255,255,0.1)' }}>Dashboard</a>
 {canViewProjects() && (
          <a href="/projects" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>
            Projects
          </a>
        )}
 {canViewTasks() && (
          <a href="/tasks" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>
            Tasks
          </a>
        )}
 {canViewMilestones() && (
          <a href="/milestones" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>
            Milestones
          </a>
        )}
 {canViewUpdates() && (
          <a href="/progress-updates" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>
            Updates
          </a>
        )}
{canViewAPIs() && (
          <a href="/apis" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>
            APIs
          </a>
        )}
{canViewUserManagement() && (
          <a href="/users" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>
            Users
          </a>
        )}
 {canViewActivityLogs() && (
          <a href="/activity-logs" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>
            Activity Logs
          </a>
        )}
 {canViewReports() && (
          <a href="/reports" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>
            Reports
          </a>
        )}  </div>


 


      {/* Main Content */}
      <div className="main-content">
        {/* Statistics Cards */}
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-label">Total Projects</div>
            <div className="stat-value">{stats?.totalProjects ?? 0}</div>
          </div>

          <div className="stat-card green">
            <div className="stat-label">Active Projects</div>
            <div className="stat-value">{stats?.activeProjects ?? 0}</div>
          </div>

          <div className="stat-card blue">
            <div className="stat-label">Completed Projects</div>
            <div className="stat-value">{stats?.completedProjects ?? 0}</div>
          </div>

          <div className="stat-card amber">
            <div className="stat-label">Overdue Tasks</div>
            <div className="stat-value">{stats?.overdueTasks ?? 0}</div>
          </div>
        </div>

        {/* Quick Actions - ROLE BASED */}
 {(hasFullAccess() || isExecutive() || isRestrictedManager()) && (
          <div className="content-card" style={{ marginBottom: '30px' }}>
            <h2 style={{ marginBottom: '20px' }}>Quick Actions</h2>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '15px' }}>
              
              {/* View Projects - Everyone with management access */}
              {canViewProjects() && (
                <a href="/projects" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>
                    View Projects
                  </button>
                </a>
              )}

              {/* Manage Tasks - Full access or technical */}
              {canViewTasks() && (
                <a href="/tasks" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-success" style={{ width: '100%', padding: '15px' }}>
                    Manage Tasks
                  </button>
                </a>
              )}

              {/* Manage Milestones - Full access or executive */}
              {canViewMilestones() && (
                <a href="/milestones" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>
                    Manage Milestones
                  </button>
                </a>
              )}

              {/* Submit Updates - Full access or technical */}
              {canViewUpdates() && (
                <a href="/progress-updates" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>
                    Submit Update
                  </button>
                </a>
              )}

              {/* API Lifecycle - Full access or technical */}
              {canViewAPIs() && (
                <a href="/apis" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>
                    API Lifecycle
                  </button>
                </a>
              )}

              {/* User Management - Full access or executive only */}
              {canViewUserManagement() && (
                <a href="/users" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>
                    User Management
                  </button>
                </a>
              )}

              {/* Reports - Full access or executive only */}
              {canViewReports() && (
                <a href="/reports" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>
                    Reports
                  </button>
                </a>
              )}

              {/* Activity Logs - Full access or executive only */}
              {canViewActivityLogs() && (
                <a href="/activity-logs" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>
                    Activity Logs
                  </button>
                </a>
              )}
            </div>
          </div>
        )}


        {/* Recent Projects / My Projects Table */}
        <div className="content-card">
          <h2>{isTechnicalStaff() ? 'My Projects' : 'Recent Projects'}</h2>
          <table className="data-table">
            <thead>
              <tr>
                <th>Project Name</th>
                <th>Type</th>
                <th>Status</th>
                <th>RAG Status</th>
                 {canSeeCreatedBy() && <th>Created By</th>}
                <th>Completion</th>
                <th>
                  {isCEO() ? 'Feedback' : 'Team Comments'}
                  {!isCEO() && <span style={{ fontSize: '11px', color: '#666', display: 'block', fontWeight: 'normal' }}>View & Reply</span>}
                </th>
              </tr>
            </thead>
            <tbody>
              {recentProjects.length > 0 ? (
                recentProjects.map((project) => (
                  <tr key={project.id}>
                    <td style={{ fontWeight: '600' }}>{project.projectName}</td>
                    <td><span className="badge badge-green">{project.projectType}</span></td>
                    <td>{project.status}</td>
                    <td>
                      <span className="badge" style={{
                        backgroundColor: getRagColor(project.ragStatus),
                        color: project.ragStatus === 'AMBER' ? '#000' : '#fff'
                      }}>
                        {project.ragStatus}
                      </span>
                    </td>
                     {canSeeCreatedBy() && (
        <td>{project.manager?.fullName || project.initiatedBy?.fullName || 'N/A'}</td>
      )}
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <div style={{ flex: 1, height: '8px', backgroundColor: '#e0e0e0', borderRadius: '4px' }}>
                          <div style={{ 
                            width: `${project.completionPercentage || 0}%`,
                            height: '100%',
                            backgroundColor: '#003366',
                            borderRadius: '4px'
                          }} />
                        </div>
                        <span style={{ fontSize: '12px', minWidth: '40px' }}>
                          {project.completionPercentage || 0}%
                        </span>
                      </div>
                    </td>
                    <td>
                      <button className="btn btn-primary" style={{ padding: '5px 10px', fontSize: '12px' }}
                        onClick={() => openCommentsModal(project)}>
                        💬 {isCEO() ? 'Feedback' : 'Comments'}
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="7" style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
                    {isTechnicalStaff() 
                      ? 'No projects assigned to you. Wait for tasks to be assigned.'
                      : 'No projects available. Create your first project!'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* NEW: Recent Activity - Only for Managers */}
        {canViewActivityLogs() && (
          <div className="content-card" style={{ marginTop: '24px' }}>
            <h2>🕒 Recent Activity</h2>
            {activitiesLoading ? (
              <p style={{ color: '#666', textAlign: 'center' }}>Loading recent activity...</p>
            ) : recentActivities.length === 0 ? (
              <p style={{ color: '#666', textAlign: 'center', padding: '20px' }}>No recent activity recorded.</p>
            ) : (
              <div style={{ maxHeight: '300px', overflowY: 'auto', paddingRight: '8px' }}>
                {recentActivities.map((act) => (
                  <div key={act.id} style={{
                    padding: '12px 16px',
                    borderBottom: '1px solid #eee',
                    fontSize: '13px',
                    lineHeight: '1.4'
                  }}>
                    <div style={{ color: '#555', fontSize: '12px', marginBottom: '4px' }}>
                      {new Date(act.timestamp).toLocaleString()}
                    </div>
                    <div>
                      <strong>{act.user?.fullName || act.user?.username || 'System'}</strong>
                      <span style={{ marginLeft: '8px', color: '#003366', fontWeight: '600' }}>
                        {act.action.replace(/_/g, ' ')}
                      </span>
                      {act.entityType && act.entityId && (
                        <span style={{ marginLeft: '10px', color: '#666' }}>
                          {act.entityType} #{act.entityId}
                        </span>
                      )}
                    </div>
                    <div style={{ color: '#444', marginTop: '4px' }}>{act.details}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Additional Stats - ONLY FOR MANAGERS/EXECUTIVES */}
          {(hasFullAccess() || isExecutive()) && (
          <div className="grid-2" style={{ marginTop: '20px' }}>
            <div className="content-card">
              <h2>Overdue Milestones</h2>
              <div style={{ textAlign: 'center', padding: '20px' }}>
                <div style={{ fontSize: '48px', fontWeight: 'bold', color: '#dc3545' }}>
                  {stats?.overdueMilestones ?? 0}
                </div>
                <p style={{ color: '#666', marginTop: '10px' }}>Milestones past deadline</p>
              </div>
            </div>
            <div className="content-card">
              <h2>Active Blockers</h2>
              <div style={{ textAlign: 'center', padding: '20px' }}>
                <div style={{ fontSize: '48px', fontWeight: 'bold', color: '#ffc107' }}>
                  {stats?.activeBlockers ?? 0}
                </div>
                <p style={{ color: '#666', marginTop: '10px' }}>Issues needing attention</p>
              </div>
            </div>
          </div>
        )}
        {/* Comments/Feedback Modal */}
        {showComments && selectedProject && (
          <div className="content-card" style={{ marginTop: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h2>💬 {isCEO() ? 'Feedback' : 'Team Comments'}: {selectedProject.projectName}</h2>
              <button 
                className="btn"
                onClick={closeCommentsModal}
                style={{ backgroundColor: '#6c757d', color: 'white' }}
              >
                ✕ Close
              </button>
            </div>

            <form onSubmit={handleAddComment} style={{ marginBottom: '20px' }}>
              <label style={{ fontWeight: '600', marginBottom: '10px', display: 'block' }}>
                Add {isCEO() ? 'Feedback' : 'Comment'}:
              </label>
              <textarea
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                placeholder={isCEO() ? "Share your feedback or ask for updates..." : "Add your comment..."}
                rows="3"
                style={{
                  width: '100%',
                  padding: '10px',
                  border: '1px solid #ddd',
                  borderRadius: '5px',
                  marginBottom: '10px',
                  fontSize: '14px'
                }}
                required
              />
              <button type="submit" className="btn btn-primary">
                📤 Post {isCEO() ? 'Feedback' : 'Comment'}
              </button>
            </form>

            <div style={{ marginTop: '20px' }}>
              <h3 style={{ marginBottom: '15px' }}>All {isCEO() ? 'Feedback' : 'Comments'} ({comments.length})</h3>
              
              {comments.length === 0 ? (
                <p style={{ color: '#666', textAlign: 'center', padding: '20px' }}>
                  No {isCEO() ? 'feedback' : 'comments'} yet. Be the first to {isCEO() ? 'comment' : 'add one'}!
                </p>
              ) : (
                comments.map(comment => (
                  <div key={comment.id} style={{ 
                    padding: '15px', 
                    backgroundColor: comment.userRole === 'CEO' ? '#e3f2fd' : '#f8f9fa',
                    borderRadius: '5px', 
                    marginBottom: '10px',
                    borderLeft: comment.userRole === 'CEO' ? '4px solid #003366' : '4px solid #ddd'
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                      <div>
                        <strong>{comment.userName || comment.user?.fullName || 'Unknown User'}</strong>
                        {comment.userRole && (
                          <span style={{ 
                            marginLeft: '10px', 
                            fontSize: '11px', 
                            padding: '2px 8px', 
                            backgroundColor: '#003366', 
                            color: 'white', 
                            borderRadius: '3px' 
                          }}>
                            {comment.userRole}
                          </span>
                        )}
                      </div>
                      <span style={{ fontSize: '12px', color: '#666' }}>
                        {comment.createdAt ? new Date(comment.createdAt).toLocaleDateString() : 'N/A'} 
                        {' '}
                        {comment.createdAt ? new Date(comment.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : ''}
                      </span>
                    </div>
                    <p style={{ margin: '0 0 10px 0', lineHeight: '1.5' }}>{comment.commentText}</p>
                    
                    {!comment.isReplying && (
                      <button 
                        className="btn"
                        onClick={() => {
                          setReplyTo(comment.id);
                          setReplyText('');
                        }}
                        style={{ fontSize: '12px', padding: '5px 10px', backgroundColor: '#e9ecef' }}
                      >
                        ↩️ Reply
                      </button>
                    )}
                    
                    {replyTo === comment.id && (
                      <div style={{ marginTop: '10px', padding: '10px', backgroundColor: 'white', borderRadius: '5px' }}>
                        <textarea
                          value={replyText}
                          onChange={(e) => setReplyText(e.target.value)}
                          placeholder="Write a reply..."
                          rows="2"
                          autoFocus
                          style={{
                            width: '100%',
                            padding: '8px',
                            border: '1px solid #ddd',
                            borderRadius: '5px',
                            marginBottom: '8px',
                            fontSize: '13px'
                          }}
                        />
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button 
                            className="btn btn-primary"
                            onClick={() => handleAddReply(comment.id)}
                            style={{ padding: '5px 15px', fontSize: '12px' }}
                          >
                            Submit Reply
                          </button>
                          <button 
                            className="btn"
                            onClick={() => { setReplyTo(null); setReplyText(''); }}
                            style={{ padding: '5px 15px', fontSize: '12px', backgroundColor: '#6c757d', color: 'white' }}
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    )}

                    {comment.replies && comment.replies.length > 0 && (
                      <div style={{ marginTop: '15px', paddingLeft: '20px', borderLeft: '3px solid #003366' }}>
                        <strong style={{ fontSize: '13px', color: '#666', marginBottom: '10px', display: 'block' }}>
                          Replies ({comment.replies.length})
                        </strong>
                        {comment.replies.map(reply => (
                          <div key={reply.id} style={{ 
                            padding: '10px', 
                            backgroundColor: 'white', 
                            borderRadius: '5px', 
                            marginBottom: '10px' 
                          }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
                              <div>
                                <strong>{reply.userName || reply.user?.fullName || 'Unknown User'}</strong>
                                {reply.userRole && (
                                  <span style={{ 
                                    marginLeft: '10px', 
                                    fontSize: '10px', 
                                    padding: '2px 6px', 
                                    backgroundColor: '#6c757d', 
                                    color: 'white', 
                                    borderRadius: '3px' 
                                  }}>
                                    {reply.userRole}
                                  </span>
                                )}
                              </div>
                              <span style={{ fontSize: '11px', color: '#666' }}>
                                {reply.createdAt ? new Date(reply.createdAt).toLocaleDateString() : 'N/A'}
                              </span>
                            </div>
                            <p style={{ margin: 0, fontSize: '13px' }}>{reply.commentText}</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Dashboard;