import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import NotificationBell from '../components/NotificationBell';

const Dashboard = () => {
  const { currentUser, logout } = useAuth();
  
  // Dashboard States
  const [stats, setStats] = useState(null);
  const [recentProjects, setRecentProjects] = useState([]);
  const [recentActivities, setRecentActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activitiesLoading, setActivitiesLoading] = useState(true);
  
  // Inline Comments States
  const [showComments, setShowComments] = useState(false);
  const [selectedProject, setSelectedProject] = useState(null);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [replyTo, setReplyTo] = useState(null);
  const [replyText, setReplyText] = useState('');
  
  // Chat Modal States
  const [showChatModal, setShowChatModal] = useState(false);
  const [chatComments, setChatComments] = useState([]);
  const [chatLoading, setChatLoading] = useState(false);
  const [chatNewComment, setChatNewComment] = useState('');
  const [chatSelectedProject, setChatSelectedProject] = useState('');
  const [chatProjects, setChatProjects] = useState([]);
  const [chatReplyTo, setChatReplyTo] = useState(null);
  
  
 
  const [commentRefreshTrigger, setCommentRefreshTrigger] = useState(0);

  
  useEffect(() => {
    console.log('=== DASHBOARD MOUNTED ===');
    console.log('Current user:', currentUser);
    fetchDashboardStats();
    fetchRecentProjects();
    if (hasFullAccess()) {
      fetchRecentActivities();
    }
  }, []);


  useEffect(() => {
    let interval;
    if (showComments && selectedProject) {
      console.log('🔄 Starting inline comments auto-refresh...');
      loadComments(selectedProject.id);
      interval = setInterval(() => {
        console.log('🔄 Auto-refreshing inline comments...');
        loadComments(selectedProject.id);
      }, 5000); 
    }
    return () => {
      if (interval) {
        clearInterval(interval);
        console.log('⏹️ Stopped inline comments polling');
      }
    };
  }, [showComments, selectedProject]);


useEffect(() => {
  let interval;
  if (showChatModal) {
    console.log('🔄 Starting chat modal auto-refresh...');
    fetchAllComments(); 
    
    
    interval = setInterval(() => {
      console.log('🔄 Auto-refreshing chat modal comments...');
      fetchAllComments();
    }, 30000); 
  }
  
 
  return () => {
    if (interval) {
      clearInterval(interval);
      console.log('⏹️ Stopped chat modal auto-refresh');
    }
  };
}, [showChatModal]); 

  const filteredComments = chatSelectedProject
    ? chatComments.filter(comment => {
        const commentProjectId = String(comment.projectId);
        const selectedId = String(chatSelectedProject);
        const match = commentProjectId === selectedId;
        return match;
      })
    : chatComments;


  const renderReplies = (replies, depth = 0) => {
    if (!replies || replies.length === 0) return null;
    
    return (
      <div style={{
        marginLeft: depth > 0 ? '25px' : '0',
        paddingLeft: depth > 0 ? '15px' : '0',
        borderLeft: depth > 0 ? '3px solid #D2691E' : 'none'
      }}>
        {replies.map((reply, replyIndex) => (
          <div key={reply.id || replyIndex}>
            <div
              style={{
                marginBottom: '8px',
                padding: '10px',
                backgroundColor: depth > 0 ? '#fff9f0' : '#f0f0f0',
                borderRadius: '6px',
                marginLeft: depth > 0 ? '10px' : '0'
              }}
            >
              <div style={{
                fontSize: '12px',
                color: '#333',
                marginBottom: '6px',
                lineHeight: '1.4'
              }}>
                {reply.commentText}
              </div>

              <div style={{
                fontSize: '10px',
                color: '#999',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: '8px'
              }}>
                <span>👤 {reply.userName || 'Unknown'}</span>
                <span>{chatTimeAgo(reply.createdAt)}</span>
              </div>

             
            </div>

            {reply.replies && reply.replies.length > 0 && (
              renderReplies(reply.replies, depth + 1)
            )}
          </div>
        ))}
      </div>
    );
  };

 
  const fetchDashboardStats = async () => {
    try {
      console.log('📊 Fetching dashboard stats...');
      const response = await api.get('/api/reports/dashboard-summary', {
        params: {
          userId: currentUser?.id,
          userRole: currentUser?.role
        }
      });
      console.log('✅ Dashboard stats response:', response.data);
      let statsData = response.data;
      
      if (isBusiness()) {
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
      }
      setStats(statsData);
    } catch (error) {
      console.error('❌ Error fetching dashboard stats:', error);
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

  const fetchRecentProjects = async () => {
    try {
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

      if (isTechnicalStaff()) {
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
        const assignedProjectIds = [...new Set(
          tasksData.map(task => {
            return task.project?.id || task.projectId || task.project?.projectId;
          }).filter(Boolean)
        )];
        const assignedProjects = Array.isArray(projectsData)
          ? projectsData.filter(project => assignedProjectIds.includes(project.id))
          : [];
        setRecentProjects(assignedProjects.slice(0, 5));
      } else if (isBusiness()) {
        const userProjects = Array.isArray(projectsData)
          ? projectsData.filter(project => {
              const managerId = project.manager?.id || project.managerId;
              const createdById = project.createdBy?.id || project.createdById;
              const initiatedById = project.initiatedBy?.id || project.initiatedById;
              return managerId === currentUser?.id ||
                     createdById === currentUser?.id ||
                     initiatedById === currentUser?.id;
            })
          : [];
        setRecentProjects(userProjects.slice(0, 5));
      } else {
        const projectsToShow = Array.isArray(projectsData) ? projectsData.slice(0, 5) : [];
        setRecentProjects(projectsToShow);
      }
    } catch (error) {
      console.error('❌ Error fetching recent projects:', error);
      setRecentProjects([]);
    }
  };

 
  const fetchRecentActivities = async () => {
    try {
      setActivitiesLoading(true);
      const response = await api.get('/api/activity-logs/recent', {
        params: { limit: 10 }
      });
      setRecentActivities(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('❌ Failed to load recent activities:', error);
      setRecentActivities([]);
    } finally {
      setActivitiesLoading(false);
    }
  };


const loadComments = async (projectId) => {
  try {
    console.log('📡 Loading comments for project:', projectId);
    const response = await api.get(`/api/projects/${projectId}/comments`, {
      params: { _t: Date.now() },
      headers: {
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache'
      }
    });
    
    const allComments = response.data || [];
    console.log('✅ Comments loaded:', allComments.length);
    
    
    const commentsMap = new Map();
    allComments.forEach(comment => {
      commentsMap.set(comment.id, {
        ...comment,
        replies: []
      });
    });
    
    const rootComments = [];
    allComments.forEach(comment => {
      const parentCommentId = comment.parentCommentId;
      if (parentCommentId) {
        const parentComment = commentsMap.get(parentCommentId);
        if (parentComment) {
          if (!parentComment.replies) {
            parentComment.replies = [];
          }
          parentComment.replies.push(comment);
          console.log(`🔗 Attached reply ${comment.id} to parent ${parentCommentId}`);
        } else {
          console.warn(`⚠️ Reply ${comment.id} has unknown parent ${parentCommentId}`);
          rootComments.push(commentsMap.get(comment.id));
        }
      } else {
        rootComments.push(commentsMap.get(comment.id));
      }
    });
    
 
    rootComments.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    
    console.log('✅ Total root comments:', rootComments.length);
    console.log('✅ Comments with replies:', rootComments.filter(c => c.replies?.length > 0).length);
    
    setComments(rootComments);
  } catch (error) {
    console.error('❌ Error loading comments:', error);
    setComments([]);
  }
};

  
  const fetchAllComments = async () => {
    try {
      setChatLoading(true);
      console.log('🔄 Fetching all comments for chat...');
      
      const projectsRes = await api.get('/api/projects', {
        params: { userId: currentUser?.id, userRole: currentUser?.role }
      });
      const projectsData = projectsRes.data || [];
      setChatProjects(projectsData);
      
      const allCommentsPromises = projectsData.map(project =>
        api.get(`/api/projects/${project.id}/comments`, {
          params: { _t: Date.now() }
        })
        .then(res => ({
          projectId: project.id,
          projectName: project.projectName,
          comments: res.data || []
        }))
        .catch(err => {
          console.error(`Error fetching comments for project ${project.id}:`, err);
          return { projectId: project.id, projectName: project.projectName, comments: [] };
        })
      );
      
      const allCommentsResults = await Promise.all(allCommentsPromises);
      
      const allComments = [];
      allCommentsResults.forEach(result => {
        result.comments.forEach(comment => {
          allComments.push({
            ...comment,
            projectName: result.projectName,
            projectId: result.projectId,
            replies: []
          });
        });
      });
      
      console.log('📊 Total comments fetched:', allComments.length);
      
      const commentsMap = new Map();
      allComments.forEach(comment => {
        commentsMap.set(comment.id, comment);
      });
      
      const rootComments = [];
      
      allComments.forEach(comment => {
        const parentCommentId = comment.parentCommentId;
        if (parentCommentId) {
          const parentComment = commentsMap.get(parentCommentId);
          if (parentComment) {
            if (!parentComment.replies) {
              parentComment.replies = [];
            }
            parentComment.replies.push(comment);
            console.log(`🔗 Attached reply ${comment.id} to parent ${parentCommentId}`);
          } else {
            console.warn(`⚠️ Reply ${comment.id} has unknown parent ${parentCommentId}`);
            rootComments.push(comment);
          }
        } else {
          rootComments.push(comment);
        }
      });
      
      rootComments.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      
      const sortReplies = (comments) => {
        comments.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        comments.forEach(comment => {
          if (comment.replies && comment.replies.length > 0) {
            sortReplies(comment.replies);
          }
        });
      };
      sortReplies(rootComments);
      
      console.log('✅ Total root comments:', rootComments.length);
      console.log('✅ Comments with replies:', rootComments.filter(c => c.replies?.length > 0).length);
      
      setChatComments(rootComments);
      setChatLoading(false);
    } catch (error) {
      console.error('❌ Error fetching chat comments:', error);
      setChatLoading(false);
    }
  };

 
  const handleAddComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim() || !selectedProject) return;
    try {
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

 
  const handleChatSendComment = async () => {
    if (!chatNewComment.trim() || !chatSelectedProject) {
      alert('Please select a project and enter a comment');
      return;
    }
    try {
      console.log('📝 Sending comment:', {
        projectId: chatSelectedProject,
        parentCommentId: chatReplyTo,
        text: chatNewComment
      });
      await api.post(`/api/projects/${chatSelectedProject}/comments`, null, {
        params: {
          userId: currentUser.id,
          commentText: chatNewComment,
          parentCommentId: chatReplyTo || null
        }
      });
      setChatNewComment('');
      setChatReplyTo(null);
      await fetchAllComments();
    } catch (error) {
      console.error('❌ Error sending comment:', error);
      alert('Failed to send comment');
    }
  };

  
  const handleDeleteComment = async (commentId) => {
    if (!window.confirm('Are you sure you want to delete this comment?')) return;
    try {
      console.log('🗑️ Deleting comment:', commentId);
      await api.delete(`/api/projects/comments/${commentId}`);
      setCommentRefreshTrigger(prev => prev + 1);
      alert('Comment deleted successfully!');
    } catch (error) {
      console.error('❌ Error deleting comment:', error);
      alert('Failed to delete comment');
    }
  };

  
  const refreshAllComments = () => {
    console.log('🔄 Triggering comment refresh...');
    setCommentRefreshTrigger(prev => prev + 1);
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

  const chatTimeAgo = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return date.toLocaleDateString();
  };

 
  const getRagColor = (status) => {
    switch (status) {
      case 'GREEN': return '#28a745';
      case 'AMBER': return '#ffc107';
      case 'RED': return '#dc3545';
      default: return '#6c757d';
    }
  };

  const isExecutive = () => ['CEO', 'DEPUTY_CHIEF', 'DIRECTOR'].includes(currentUser?.role);
  const hasFullAccess = () => currentUser?.role === 'DIGITAL_BANKING_MANAGER';
  const isRestrictedManager = () => currentUser?.role === 'PROJECT_MANAGER';
  const isQUALITY = () => ['SECURITY', 'QUALITY_ASSURANCE'].includes(currentUser?.role);
  const isBusiness = () => currentUser?.role === 'BUSINESS';
  const isCEO = () => currentUser?.role === 'CEO';
  const isTechnicalStaff = () => {
    const techRoles = ['DEVELOPER', 'SENIOR_IT_OFFICER', 'JUNIOR_IT_OFFICER', 'IT_GRADUATE_TRAINEE'];
    return techRoles.includes(currentUser?.role);
  };

  const canSeeCreatedBy = () => {
    const technicalRoles = ['DEVELOPER', 'SENIOR_IT_OFFICER', 'JUNIOR_IT_OFFICER', 'IT_GRADUATE_TRAINEE'];
    if (technicalRoles.includes(currentUser?.role)) return false;
    return true;
  };

  const canViewProjects = () => isTechnicalStaff() || isQUALITY() || isExecutive() || isBusiness() || hasFullAccess() || isRestrictedManager();
  const canViewTasks = () => hasFullAccess() || isTechnicalStaff() || isQUALITY();
  const canViewMilestones = () => hasFullAccess() || isQUALITY();
  const canViewUpdates = () => hasFullAccess() || isTechnicalStaff();
  const canViewAPIs = () => hasFullAccess() || isTechnicalStaff() || isQUALITY();
  const canViewUserManagement = () => hasFullAccess();
  const canViewActivityLogs = () => hasFullAccess();
  const canViewReports = () => hasFullAccess() || isExecutive() || isQUALITY();

  const handleLogout = async () => {
    try {
      await api.post('/api/auth/logout', {}, {
        headers: { 'X-User-Id': currentUser?.id }
      });
    } catch (error) {
      console.error('❌ Failed to log logout:', error);
    } finally {
      logout();
      window.location.href = '/login';
    }
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
        <div className="navbar-brand" style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <img src="/nibicon.jpeg" alt="NIB Logo" style={{
            width: '45px', height: '45px', objectFit: 'contain', borderRadius: '8px',
            backgroundColor: 'white', padding: '5px', boxShadow: '0 2px 4px rgba(0,0,0,0.2)'
          }} />
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <span style={{ fontSize: '20px', fontWeight: 'bold', textShadow: '1px 1px 2px rgba(0,0,0,0.3)' }}>
              NIB IT Project Tracking
            </span>
            <h4 style={{ fontSize: '14px', margin: '2px 0 0 0', opacity: '0.9' }}>
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

      {/* Top Navigation Menu */}
      <div style={{
        background: 'linear-gradient(135deg, #8B4513 0%, #A0522D 100%)',
        padding: '10px 20px', display: 'flex', gap: '20px',
        justifyContent: 'center', flexWrap: 'wrap'
      }}>
        <a href="/dashboard" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px', backgroundColor: 'rgba(255,255,255,0.1)' }}>Dashboard</a>
        {canViewProjects() && <a href="/projects" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>Projects</a>}
        {canViewTasks() && <a href="/tasks" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>Tasks</a>}
        {canViewMilestones() && <a href="/milestones" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>Milestones</a>}
        {canViewUpdates() && <a href="/progress-updates" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>Updates</a>}
        {canViewAPIs() && <a href="/apis" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>APIs</a>}
        {canViewUserManagement() && <a href="/users" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>Users</a>}
        {canViewActivityLogs() && <a href="/activity-logs" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>Activity Logs</a>}
        {canViewReports() && <a href="/reports" style={{ color: 'white', textDecoration: 'none', padding: '8px 15px', borderRadius: '5px' }}>Reports</a>}
      </div>

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

        {/* Quick Actions */}
        {(hasFullAccess() || isExecutive() || isRestrictedManager()) && (
          <div className="content-card" style={{ marginBottom: '30px' }}>
            <h2 style={{ marginBottom: '20px' }}>Quick Actions</h2>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '15px' }}>
              {canViewProjects() && (
                <a href="/projects" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>View Projects</button>
                </a>
              )}
              {canViewTasks() && (
                <a href="/tasks" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-success" style={{ width: '100%', padding: '15px' }}>Manage Tasks</button>
                </a>
              )}
              {canViewMilestones() && (
                <a href="/milestones" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>Manage Milestones</button>
                </a>
              )}
              {canViewUpdates() && (
                <a href="/progress-updates" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>Submit Update</button>
                </a>
              )}
              {canViewAPIs() && (
                <a href="/apis" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>API Lifecycle</button>
                </a>
              )}
              {canViewUserManagement() && (
                <a href="/users" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>User Management</button>
                </a>
              )}
              {canViewReports() && (
                <a href="/reports" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>Reports</button>
                </a>
              )}
              {canViewActivityLogs() && (
                <a href="/activity-logs" style={{ textDecoration: 'none', flex: '1 1 calc(25% - 15px)', minWidth: '200px' }}>
                  <button className="btn btn-primary" style={{ width: '100%', padding: '15px' }}>Activity Logs</button>
                </a>
              )}
            </div>
          </div>
        )}

        {/* Recent Projects Table */}
        <div className="content-card">
          <h2>{isTechnicalStaff() ? 'My Projects' : 'Recent Projects'}</h2>
          <table className="data-table">
            <thead>
              <tr>
                <th>Project Name</th>
                <th>Type</th>
                <th>RAG Status</th>
                {canSeeCreatedBy() && <th>Created By</th>}
                <th>Completion</th>
                <th>{isCEO() ? 'Feedback' : 'Team Comments'}</th>
              </tr>
            </thead>
            <tbody>
              {recentProjects.length > 0 ? (
                recentProjects.map((project) => (
                  <tr key={project.id}>
                    <td style={{ fontWeight: '600' }}>{project.projectName}</td>
                    <td><span className="badge badge-green">{project.projectType}</span></td>
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

        {/* Recent Activity */}
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
                    padding: '12px 16px', borderBottom: '1px solid #eee',
                    fontSize: '13px', lineHeight: '1.4'
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

        {/* Additional Stats */}
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

        {/* Inline Comments Modal */}
        {showComments && selectedProject && (
          <div className="content-card" style={{ marginTop: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <h2>💬 {isCEO() ? 'Feedback' : 'Team Comments'}: {selectedProject.projectName}</h2>
              <button className="btn" onClick={closeCommentsModal} style={{ backgroundColor: '#6c757d', color: 'white' }}>
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
                style={{ width: '100%', padding: '10px', border: '1px solid #ddd', borderRadius: '5px', marginBottom: '10px', fontSize: '14px' }}
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
                    padding: '15px', backgroundColor: comment.userRole === 'CEO' ? '#e3f2fd' : '#f8f9fa',
                    borderRadius: '5px', marginBottom: '10px',
                    borderLeft: comment.userRole === 'CEO' ? '4px solid #003366' : '4px solid #ddd'
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                      <div>
                        <strong>{comment.userName || comment.user?.fullName || 'Unknown User'}</strong>
                        {comment.userRole && (
                          <span style={{ marginLeft: '10px', fontSize: '11px', padding: '2px 8px', backgroundColor: '#003366', color: 'white', borderRadius: '3px' }}>
                            {comment.userRole}
                          </span>
                        )}
                      </div>
                      <span style={{ fontSize: '12px', color: '#666' }}>
                        {comment.createdAt ? new Date(comment.createdAt).toLocaleString() : 'N/A'}
                      </span>
                    </div>
                    <p style={{ margin: '0 0 10px 0', lineHeight: '1.5' }}>{comment.commentText}</p>
                    {!comment.isReplying && (
                      <button className="btn" onClick={() => { setReplyTo(comment.id); setReplyText(''); }}
                        style={{ fontSize: '12px', padding: '5px 10px', backgroundColor: '#e9ecef' }}>
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
                          style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '5px', marginBottom: '8px', fontSize: '13px' }}
                        />
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button className="btn btn-primary" onClick={() => handleAddReply(comment.id)}
                            style={{ padding: '5px 15px', fontSize: '12px' }}>Submit Reply</button>
                          <button className="btn" onClick={() => { setReplyTo(null); setReplyText(''); }}
                            style={{ padding: '5px 15px', fontSize: '12px', backgroundColor: '#6c757d', color: 'white' }}>Cancel</button>
                        </div>
                      </div>
                    )}
                    {comment.replies && comment.replies.length > 0 && (
                      <div style={{ marginBottom: '12px', paddingLeft: '25px', borderLeft: '3px solid #D2691E' }}>
                        {comment.replies.map((reply, replyIndex) => (
                          <div key={reply.id || replyIndex}>
                            <div style={{ marginBottom: '8px', padding: '10px', backgroundColor: '#fff9f0', borderRadius: '6px' }}>
                              <div style={{ fontSize: '12px', color: '#333', marginBottom: '6px', lineHeight: '1.4' }}>
                                {reply.commentText}
                              </div>
                              <div style={{ fontSize: '10px', color: '#999', display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                                <span>👤 {reply.userName || 'Unknown'}</span>
                                <span>{chatTimeAgo(reply.createdAt)}</span>
                              </div>
                              
                            </div>
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

      {/* Floating Chat Button */}
      <button
        onClick={() => { setShowChatModal(true); fetchAllComments(); }}
        style={{
          position: 'fixed', bottom: '30px', right: '30px', width: '60px', height: '60px',
          backgroundColor: '#8B4513', color: 'white', border: 'none', borderRadius: '50%',
          boxShadow: '0 4px 12px rgba(139, 69, 19, 0.4)', cursor: 'pointer', fontSize: '28px',
          display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
          transition: 'all 0.3s ease', animation: 'pulse 2s infinite'
        }}
        onMouseEnter={(e) => { e.target.style.transform = 'scale(1.1)'; e.target.style.boxShadow = '0 6px 16px rgba(139, 69, 19, 0.5)'; }}
        onMouseLeave={(e) => { e.target.style.transform = 'scale(1)'; e.target.style.boxShadow = '0 4px 12px rgba(139, 69, 19, 0.4)'; }}
        title="Project Comments Chat"
      >
        💬
      </button>

      {/* Chat Modal */}
      {showChatModal && (
        <>
          <div onClick={() => setShowChatModal(false)} style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)', zIndex: 1000
          }} />
          <div style={{
            position: 'fixed', bottom: '100px', right: '30px', width: '450px',
            maxWidth: '90vw', height: '690px',  
    maxHeight: '80vh', backgroundColor: 'white', borderRadius: '12px',
            boxShadow: '0 8px 24px rgba(0,0,0,0.15)', zIndex: 1001,
            display: 'flex', flexDirection: 'column', overflow: 'hidden'
          }}>
            {/* Header */}
            <div style={{
              backgroundColor: '#8B4513', color: 'white', padding: '15px 20px',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexShrink: 0
            }}>
              <h3 style={{ margin: 0, fontSize: '18px' }}>💬 Project Comments</h3>
              <button onClick={() => setShowChatModal(false)} style={{
                background: 'rgba(255,255,255,0.2)', border: 'none', color: 'white',
                fontSize: '20px', width: '32px', height: '32px', borderRadius: '50%',
                cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center'
              }}>✕</button>
            </div>

            {/* Project Selector */}
            <div style={{ padding: '15px', borderBottom: '1px solid #e0e0e0', backgroundColor: 'white', flexShrink: 0 }}>
              <label style={{ display: 'block', fontSize: '12px', color: '#666', marginBottom: '5px', fontWeight: '600' }}>
                Select Project:
              </label>
              <select value={chatSelectedProject} onChange={(e) => {
                console.log('📁 Project selected:', e.target.value);
                setChatSelectedProject(e.target.value);
              }} style={{
                width: '100%', padding: '10px', border: '2px solid #D2691E',
                borderRadius: '6px', fontSize: '14px', backgroundColor: 'white', cursor: 'pointer'
              }}>
                <option value="">All Projects</option>
                {chatProjects.map(project => (
                  <option key={project.id} value={project.id}>{project.projectName}</option>
                ))}
              </select>
            </div>

            {/* Comments List */}
            <div style={{ flex: 1, overflowY: 'auto', padding: '15px', backgroundColor: '#f9f9f9' }}>
              {chatLoading ? (
                <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}><p>Loading comments...</p></div>
              ) : filteredComments.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
                  <p>No comments yet</p>
                  <p style={{ fontSize: '13px' }}>Be the first to comment!</p>
                </div>
              ) : (
                filteredComments.map((comment, index) => (
                  <div key={comment.id || index}>
                    <div style={{ marginBottom: '12px', padding: '12px', backgroundColor: 'white', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
                      <div style={{ fontSize: '11px', color: '#8B4513', fontWeight: '600', marginBottom: '5px' }}>
                        📁 {comment.projectName || 'Unknown Project'}
                      </div>
                      <div style={{ fontSize: '13px', color: '#333', marginBottom: '8px', lineHeight: '1.4' }}>
                        {comment.commentText}
                      </div>
                      <div style={{ fontSize: '11px', color: '#999', display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          👤 {comment.userName || 'Unknown User'}
                        </span>
                        <span>{chatTimeAgo(comment.createdAt)}</span>
                      </div>
                    </div>
                    {/* ✅ Render Nested Replies Recursively */}
{comment.replies && comment.replies.length > 0 && (
  <div style={{
    marginBottom: '12px',
    marginTop: '8px'
  }}>
    {renderReplies(comment.replies, 0)}
  </div>
)}
                  </div>
                ))
              )}
            </div>

            {/* Comment Input */}
            <div style={{ padding: '15px', borderTop: '1px solid #e0e0e0', backgroundColor: 'white', flexShrink: 0 }}>
              {chatReplyTo && (
                <div style={{
                  marginBottom: '10px', padding: '8px', backgroundColor: '#f0f0f0', borderRadius: '6px',
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center'
                }}>
                  <span style={{ fontSize: '12px', color: '#666' }}>💬 Replying to comment #{chatReplyTo}</span>
                  <button onClick={() => setChatReplyTo(null)} style={{
                    background: 'none', border: 'none', color: '#999', cursor: 'pointer', fontSize: '16px', padding: '0 5px'
                  }}>✕</button>
                </div>
              )}
              <textarea id="chat-comment-textarea" value={chatNewComment} onChange={(e) => setChatNewComment(e.target.value)}
                placeholder={chatReplyTo ? "Type your reply..." : "Type your comment..."} rows="3" style={{
                  width: '100%', padding: '10px', border: '2px solid #D2691E', borderRadius: '6px',
                  fontSize: '14px', resize: 'none', marginBottom: '10px', fontFamily: 'inherit'
                }} />
              <button onClick={handleChatSendComment} disabled={!chatNewComment.trim()} style={{
                width: '100%', padding: '12px', backgroundColor: (!chatNewComment.trim()) ? '#ccc' : '#8B4513',
                color: 'white', border: 'none', borderRadius: '6px',
                cursor: (!chatNewComment.trim()) ? 'not-allowed' : 'pointer', fontSize: '14px', fontWeight: '600'
              }}>
                💬 {chatReplyTo ? 'Send Reply' : 'Send Comment'}
              </button>
            </div>
          </div>
        </>
      )}

      {/* Pulse Animation */}
      <style>{`
        @keyframes pulse {
          0%, 100% { transform: scale(1); box-shadow: 0 4px 12px rgba(139, 69, 19, 0.4); }
          50% { transform: scale(1.05); box-shadow: 0 6px 16px rgba(139, 69, 19, 0.5); }
        }
      `}</style>
    </div>
  );
};

export default Dashboard;