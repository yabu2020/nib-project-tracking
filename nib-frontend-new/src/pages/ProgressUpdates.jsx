

import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import { useNavigate } from 'react-router-dom';
import Select from "react-select";
const ProgressUpdates = () => {
  const { currentUser, logout } = useAuth();
  const [updates, setUpdates] = useState([]);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [filterProject, setFilterProject] = useState('all');
  const [filterUser, setFilterUser] = useState('all'); 
  const navigate = useNavigate(); 
  const [solvedBlockers, setSolvedBlockers] = useState({});
  const [comments, setComments] = useState({}); 
const [newComment, setNewComment] = useState({}); 
const [acknowledgedUpdates, setAcknowledgedUpdates] = useState({}); 
const [showComments, setShowComments] = useState({});
  const [formData, setFormData] = useState({
    projectId: '',
    completedWork: '',
    ongoingWork: '',
    blockers: '',
    estimatedResolution: ''
  });

  

  const isTechnicalStaff = () => {
    const techRoles = ['DEVELOPER', 'SENIOR_IT_OFFICER', 'JUNIOR_IT_OFFICER', 'IT_GRADUATE_TRAINEE'];
    return techRoles.includes(currentUser?.role);
  };

  const isManager = () => {
    const managerRoles = ['CEO', 'PROJECT_MANAGER', 'CORE_BANKING_MANAGER', 'DIGITAL_BANKING_MANAGER', 'DEPUTY_CHIEF', 'DIRECTOR'];
    return managerRoles.includes(currentUser?.role);
  };



  useEffect(() => {
    fetchData();
  }, [filterProject, filterUser]);
  const fetchData = async () => {
  try {
    setLoading(true);
    
    const updatesParams = {
      userId: currentUser?.id,
      userRole: currentUser?.role
    };
 
    if (isManager()) {
      if (filterProject !== 'all') updatesParams.projectId = filterProject;
      if (filterUser !== 'all') updatesParams.submittedById = filterUser;
    }
    
    const updatesResponse = await api.get('/api/progress-updates', {
      params: updatesParams
    });
    
    let updatesData = updatesResponse.data;
    if (typeof updatesData === 'string') {
      updatesData = JSON.parse(updatesData);
    }
    
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
    
    setUpdates(Array.isArray(updatesData) ? updatesData : []);
    setProjects(Array.isArray(projectsData) ? projectsData : []);
    const ackStatus = {};
    for (const update of updatesData) {
      if (update.acknowledged === true || update.acknowledged === 'true') {
        ackStatus[update.id] = true;
      }
    }
    setAcknowledgedUpdates(ackStatus);
    const commentsData = {};
    for (const update of updatesData) {
      try {
        const res = await api.get(`/api/progress-updates/${update.id}/comments`);
        commentsData[update.id] = res.data;
      } catch (error) {
        console.error(`Failed to load comments for update ${update.id}:`, error);
        commentsData[update.id] = [];
      }
    }
    setComments(commentsData);
    
  } catch (error) {
    console.error('Error fetching ', error);
    setUpdates([]);
    setProjects([]);
  } finally {
    setLoading(false);
  }
};
 
  const fetchTeamMembers = async () => {
    try {
      const response = await api.get('/api/users/team', {
        params: { managerId: currentUser?.id }
      });
     
    } catch (error) {
      console.error('Error fetching team members:', error);
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
const fetchComments = async (updateId) => {
  try {
    const res = await api.get(`/api/progress-updates/${updateId}/comments`);
    setComments(prev => ({ ...prev, [updateId]: res.data }));
  } catch (error) {
    console.error('Error fetching comments:', error);
  }
};

const handleAddComment = async (updateId) => {
  if (!newComment[updateId]?.trim()) return;
  
  try {
    await api.post(`/api/progress-updates/${updateId}/comments`, {
      comment: newComment[updateId],
      commenterRole: currentUser?.role
    }, {
      headers: { 'X-User-Id': currentUser?.id }
    });
    
    setNewComment(prev => ({ ...prev, [updateId]: '' }));
    fetchComments(updateId); 
  } catch (error) {
    alert('Failed to add comment: ' + (error.response?.data?.error || error.message));
  }
};
const handleAcknowledge = async (updateId) => {
  try {
    const response = await api.put(`/api/progress-updates/${updateId}/acknowledge`, {}, {
      headers: { 'X-User-Id': currentUser?.id }
    });
    setAcknowledgedUpdates(prev => ({ ...prev, [updateId]: true }));
    setUpdates(prevUpdates => 
      prevUpdates.map(update => 
        update.id === updateId 
          ? { ...update, acknowledged: true }
          : update
      )
    );
    
    alert('✅ Update acknowledged!');
  } catch (error) {
    alert('Failed to acknowledge: ' + (error.response?.data?.error || error.message));
  }
};
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const updateData = {
        ...formData,
        submittedById: currentUser?.id,        
        submittedByRole: currentUser?.role,     
        updateDate: new Date().toISOString().split('T')[0],
     
        submittedToManagement: isTechnicalStaff()
      };
      
      await api.post('/api/progress-updates', updateData, {
        headers: {
          'X-User-Id': currentUser?.id  
        }
      });
      
      alert('✅ Progress update submitted to management!');
      setShowModal(false);
      fetchData();
      setFormData({
        projectId: '',
        completedWork: '',
        ongoingWork: '',
        blockers: '',
        estimatedResolution: ''
      });
    } catch (error) {
      console.error('Submit error:', error);
      alert('Error submitting update: ' + (error.response?.data?.error || 'Unknown error'));
    }
  };

  const handleSolveBlocker = async (updateId) => {
  try {
    const response = await api.put(`/api/progress-updates/${updateId}/blocker-status`, {
      blockerStatus: 'SOLVED',
      solvedById: currentUser?.id,
      solvedByRole: currentUser?.role,
      solvedAt: new Date().toISOString()
    });
    
    console.log('✅ Blocker solved response:', response.data);
    await fetchData();
    
    alert('✅ Blocker marked as solved!');
  } catch (error) {
    console.error('Error solving blocker:', error);
    alert('Error marking blocker as solved: ' + (error.response?.data?.error || 'Unknown error'));
  }
};

const isBlockerSolved = (update) => {
  return update.blockerStatus === 'SOLVED' || solvedBlockers[update.id];
};
  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };


  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const formatTime = (dateString) => {
    if (!dateString) return '';
    return new Date(dateString).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

 

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
        <div className="loading"><p>Loading progress updates...</p></div>
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
            <div style={{ textAlign: 'center' }}>
              <h1>
                {isManager() ? 'Team Progress Updates' : 'My Progress Updates'}
              </h1>
              <p>
                {isManager() 
                  ? 'Review daily updates from your technical team' 
                  : 'Submit and track your daily work progress'}
              </p>
            </div>
            {/* ✅ Only technical staff can submit updates */}
            {isTechnicalStaff() && (
              <button 
                className="btn btn-primary" 
                onClick={() => setShowModal(true)}
                style={{ padding: '12px 24px', fontSize: '14px' }}
              >
                📝 Submit Daily Update
              </button>
            )}
            {/* ✅ Managers see a different button */}
            {isManager() && !isTechnicalStaff() && (
              <div style={{ padding: '12px 24px', fontSize: '14px', color: '#666' }}>
                📋 Viewing team updates
              </div>
            )}
          </div>
        </div>

        {/* Filters - Enhanced for managers */}
        <div className="content-card" style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap', alignItems: 'flex-end' }}>
            
            {/* Project Filter */}
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

            {/* Clear Filters Button */}
            <div style={{ flex: '0 0 auto' }}>
              <button 
                className="btn"
                onClick={() => { setFilterProject('all'); setFilterUser('all'); }}
                style={{ padding: '10px 20px', backgroundColor: '#6c757d', color: 'white' }}
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>

        {/* Recent Updates */}
        <div className="content-card">
          <h2>
            {isManager() 
              ? `Team Updates (${updates.length})` 
              : `My Updates (${updates.length})`}
          </h2>
          
          {updates.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
              <p style={{ fontSize: '16px', marginBottom: '10px' }}>No progress updates found</p>
              <p style={{ fontSize: '14px' }}>
                {isManager() 
                  ? 'Your team has not submitted updates yet. Ask them to submit their daily progress!' 
                  : 'Submit your first daily update to share progress with management!'}
              </p>
              {isTechnicalStaff() && (
                <button 
                  className="btn btn-primary" 
                  onClick={() => setShowModal(true)}
                  style={{ marginTop: '15px' }}
                >
                  📝 Submit Your First Update
                </button>
              )}
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
              {updates.map((update) => (
                <div key={update.id} style={{
                  padding: '20px',
                  border: '1px solid #e0e0e0',
                  borderRadius: '8px',
                  backgroundColor: '#f8f9fa',
                 
                  borderLeft: update.blockers ? '4px solid #ffc107' : '4px solid #003366'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px', flexWrap: 'wrap', gap: '10px' }}>
                    <div>
                      <h4 style={{ margin: '0 0 5px 0', color: '#003366' }}>
                     
                        {update.submittedBy?.fullName || update.userName || update.submittedByUsername || 'Unknown User'}
                       
                        {update.submittedByRole && (
                          <span style={{
                            marginLeft: '10px',
                            fontSize: '11px',
                            padding: '2px 8px',
                            backgroundColor: isTechnicalStaff() ? '#003366' : '#6c757d',
                            color: 'white',
                            borderRadius: '3px'
                          }}>
                            {update.submittedByRole.replace(/_/g, ' ')}
                          </span>
                        )}
                      </h4>
                      <p style={{ margin: '0', color: '#666', fontSize: '14px' }}>
                        Project: <strong>{update.project?.projectName || update.projectName || 'N/A'}</strong>
                      </p>
                    </div>
                    <div style={{ textAlign: 'right', minWidth: '150px' }}>
                      <div style={{ fontSize: '13px', color: '#666' }}>
                        {formatDate(update.updateDate || update.createdAt)}
                        {' '}
                        <span style={{ color: '#999' }}>{formatTime(update.updateDate || update.createdAt)}</span>
                      </div>
                      {/* ✅ Show blocker badge if present */}
                      {update.blockers && update.blockers.trim() && (
                        <span style={{
                          display: 'inline-block',
                          padding: '3px 8px',
                          backgroundColor: '#ffc107',
                          color: '#000',
                          borderRadius: '3px',
                          fontSize: '11px',
                          marginTop: '5px',
                          fontWeight: '600'
                        }}>
                          ⚠ Has Blockers
                        </span>
                      )}
                      {/* ✅ Show submission status for managers */}
                      {isManager() && update.submittedToManagement && (
                        <span style={{
                          display: 'inline-block',
                          padding: '3px 8px',
                          backgroundColor: '#28a745',
                          color: 'white',
                          borderRadius: '3px',
                          fontSize: '11px',
                          marginTop: '5px',
                          marginLeft: '5px'
                        }}>
                          ✓ Submitted to Mgmt
                        </span>
                      )}
                    </div>
                  </div>
                  
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginTop: '15px' }}>
                    <div>
                      <h5 style={{ margin: '0 0 5px 0', color: '#28a745', fontSize: '14px' }}>✓ Completed Work</h5>
                      <p style={{ margin: '0', fontSize: '14px', lineHeight: '1.4' }}>{update.completedWork || 'N/A'}</p>
                    </div>
                    <div>
                      <h5 style={{ margin: '0 0 5px 0', color: '#007bff', fontSize: '14px' }}>🔄 Ongoing Work</h5>
                      <p style={{ margin: '0', fontSize: '14px', lineHeight: '1.4' }}>{update.ongoingWork || 'N/A'}</p>
                    </div>
                  </div>
                  
                  {/* ✅ Blockers section with estimated resolution */}
                {/* ✅ Blockers section with estimated resolution and SOLVE button */}
{(update.blockers?.trim() || update.estimatedResolution?.trim()) && (
  <div style={{
    marginTop: '15px',
    padding: '12px',
    backgroundColor: isBlockerSolved(update) ? '#d4edda' : '#fff3cd',
    borderLeft: `4px solid ${isBlockerSolved(update) ? '#28a745' : '#ffc107'}`,
    borderRadius: '4px',
    position: 'relative'
  }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
      <h5 style={{ margin: 0, color: isBlockerSolved(update) ? '#155724' : '#856404', fontSize: '14px' }}>
        {isBlockerSolved(update) ? '✅ Blocker Resolved' : '⚠ Blockers / Issues'}
      </h5>
      
      {/* ✅ SOLVED button - Only for managers */}
      {isManager() && !isBlockerSolved(update) && (
        <button
          onClick={() => handleSolveBlocker(update.id)}
          style={{
            padding: '6px 15px',
            backgroundColor: '#28a745',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            fontSize: '12px',
            fontWeight: '600',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '5px',
            transition: 'all 0.3s ease'
          }}
          onMouseEnter={(e) => {
            e.target.style.backgroundColor = '#218838';
            e.target.style.transform = 'translateY(-2px)';
          }}
          onMouseLeave={(e) => {
            e.target.style.backgroundColor = '#28a745';
            e.target.style.transform = 'translateY(0)';
          }}
        >
          ✓ Mark as Solved
        </button>
      )}
      
      {/* ✅ Show SOLVED badge */}
      {isBlockerSolved(update) && (
        <span style={{
          padding: '4px 12px',
          backgroundColor: '#28a745',
          color: 'white',
          borderRadius: '4px',
          fontSize: '11px',
          fontWeight: '600',
          display: 'flex',
          alignItems: 'center',
          gap: '5px'
        }}>
          ✓ SOLVED
          {update.solvedBy && (
            <span style={{ fontSize: '10px', opacity: 0.9 }}>
              by {update.solvedBy?.fullName || update.solvedByUsername || 'Manager'}
            </span>
          )}
        </span>
      )}
    </div>
    
    {update.blockers?.trim() && (
      <p style={{ 
        margin: '0 0 5px 0', 
        fontSize: '14px', 
        color: isBlockerSolved(update) ? '#155724' : '#856404', 
        lineHeight: '1.4',
        textDecoration: isBlockerSolved(update) ? 'line-through' : 'none',
        opacity: isBlockerSolved(update) ? 0.7 : 1
      }}>
        {update.blockers}
      </p>
    )}
    
    {update.estimatedResolution?.trim() && (
      <p style={{ margin: '0', fontSize: '13px', color: isBlockerSolved(update) ? '#155724' : '#856404' }}>
        <strong>Estimated Resolution:</strong> {update.estimatedResolution}
      </p>
    )}
    
    {/* Show who solved it and when */}
    {isBlockerSolved(update) && update.solvedAt && (
      <p style={{ 
        margin: '8px 0 0 0', 
        fontSize: '12px', 
        color: '#155724',
        fontStyle: 'italic'
      }}>
        Resolved on {formatDate(update.solvedAt)} at {formatTime(update.solvedAt)}
      </p>
    )}
  </div>
)}
                  
                  {/* ✅ Manager actions: acknowledge or comment on update */}
                 {/* ✅ Manager actions: acknowledge or comment */}
{/* ✅ Actions section - Different for managers and developers */}
<div style={{
  marginTop: '15px',
  paddingTop: '15px',
  borderTop: '1px dashed #ddd',
  display: 'flex',
  gap: '10px',
  flexWrap: 'wrap'
}}>
  {/* Acknowledge Button - Only for managers */}
  {isManager() && (
    <button 
      className="btn"
      style={{ 
        padding: '6px 12px', 
        fontSize: '12px', 
        backgroundColor: acknowledgedUpdates[update.id] ? '#6c757d' : '#28a745',
        color: 'white' 
      }}
      onClick={() => handleAcknowledge(update.id)}
      disabled={acknowledgedUpdates[update.id]}
    >
      {acknowledgedUpdates[update.id] ? '✓ Acknowledged' : '👍 Acknowledge'}
    </button>
  )}
  
  {/* Toggle Comments - For both managers and developers */}
  <button 
    className="btn"
    style={{ 
      padding: '6px 12px', 
      fontSize: '12px', 
      backgroundColor: '#007bff', 
      color: 'white' 
    }}
    onClick={() => {
      setShowComments(prev => ({ ...prev, [update.id]: !prev[update.id] }));
      if (!showComments[update.id]) {
        fetchComments(update.id); 
      }
    }}
  >
    💬 Comments ({comments[update.id]?.length || 0})
  </button>
</div>

{/* ✅ Show acknowledgment status to developers */}
{!isManager() && acknowledgedUpdates[update.id] && (
  <div style={{
    marginTop: '10px',
    padding: '8px 12px',
    backgroundColor: '#d4edda',
    borderRadius: '4px',
    fontSize: '12px',
    color: '#155724',
    display: 'flex',
    alignItems: 'center',
    gap: '5px'
  }}>
    ✓ Acknowledged by manager
  </div>
)}

{/* ✅ Comments Section */}
{showComments[update.id] && (
  <div style={{
    marginTop: '15px',
    padding: '15px',
    backgroundColor: '#f8f9fa',
    borderRadius: '6px',
    borderTop: '1px solid #dee2e6'
  }}>
    <h6 style={{ margin: '0 0 10px 0', color: '#8B4513' }}>💬 Comments</h6>
    
    {/* Comments List */}
    <div style={{ maxHeight: '200px', overflowY: 'auto', marginBottom: '10px' }}>
      {comments[update.id]?.length > 0 ? (
        comments[update.id].map(comment => (
          <div key={comment.id} style={{
            padding: '8px 12px',
            backgroundColor: comment.commenter_role === 'MANAGER' ? '#e3f2fd' : '#fff',
            borderRadius: '4px',
            marginBottom: '8px',
            borderLeft: comment.commenter_role === 'MANAGER' ? '3px solid #007bff' : '3px solid #6c757d'
          }}>
            <div style={{ fontSize: '12px', color: '#666', marginBottom: '4px' }}>
              <strong>{comment.commenter_name || 'Unknown'}</strong> 
              {' • '}
              {formatDate(comment.created_at)} {formatTime(comment.created_at)}
              {comment.commenter_role && (
                <span style={{ 
                  marginLeft: '8px', 
                  fontSize: '10px', 
                  padding: '2px 6px', 
                  backgroundColor: comment.commenter_role === 'MANAGER' ? '#007bff' : '#6c757d',
                  color: 'white',
                  borderRadius: '3px'
                }}>
                  {comment.commenter_role}
                </span>
              )}
            </div>
            <p style={{ margin: 0, fontSize: '13px' }}>{comment.comment}</p>
          </div>
        ))
      ) : (
        <p style={{ fontSize: '13px', color: '#666', fontStyle: 'italic' }}>No comments yet</p>
      )}
    </div>
    
    {/* Add Comment Form */}
    <div style={{ display: 'flex', gap: '8px' }}>
      <input
        type="text"
        value={newComment[update.id] || ''}
        onChange={(e) => setNewComment(prev => ({ ...prev, [update.id]: e.target.value }))}
        placeholder="Add a comment..."
        onKeyPress={(e) => e.key === 'Enter' && handleAddComment(update.id)}
        style={{
          flex: 1,
          padding: '8px 12px',
          border: '1px solid #ddd',
          borderRadius: '4px',
          fontSize: '13px'
        }}
      />
      <button
        onClick={() => handleAddComment(update.id)}
        style={{
          padding: '8px 15px',
          backgroundColor: '#8B4513',
          color: 'white',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer',
          fontSize: '12px'
        }}
      >
        Send
      </button>
    </div>
  </div>
)}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Submit Daily Update Modal - Only for technical staff */}
      {showModal && isTechnicalStaff() && (
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
            maxWidth: '600px',
            maxHeight: '90vh',
            overflow: 'auto'
          }}>
            <h2 style={{ marginBottom: '10px', color: '#003366' }}>Submit Daily Progress Update</h2>
            <p style={{ color: '#666', marginBottom: '20px', fontSize: '14px' }}>
              📅 {new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
              <br/>
              <span style={{ color: '#003366', fontWeight: '600' }}>
                ✓ This update will be submitted to management for review
              </span>
            </p>
            
            <form onSubmit={handleSubmit}>
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

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Completed Work Today *</label>
                <textarea
                  name="completedWork"
                  value={formData.completedWork}
                  onChange={handleInputChange}
                  rows="3"
                  placeholder="What tasks did you complete today? Be specific about deliverables."
                  required
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
                <label>Ongoing Work *</label>
                <textarea
                  name="ongoingWork"
                  value={formData.ongoingWork}
                  onChange={handleInputChange}
                  rows="3"
                  placeholder="What are you currently working on? Include expected completion timelines."
                  required
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
                <label>Blockers / Issues</label>
                <textarea
                  name="blockers"
                  value={formData.blockers}
                  onChange={handleInputChange}
                  rows="2"
                  placeholder="Any blockers preventing progress? Describe the issue and what help you need."
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

              {formData.blockers?.trim() && (
                <div className="form-group" style={{ marginBottom: '15px' }}>
                  <label>Estimated Resolution Date</label>
                  <input
                    type="date"
                    name="estimatedResolution"
                    value={formData.estimatedResolution}
                    onChange={handleInputChange}
                    min={new Date().toISOString().split('T')[0]}
                    style={{
                      padding: '12px 15px',
                      border: '1px solid #ddd',
                      borderRadius: '5px',
                      fontSize: '14px',
                      width: '100%'
                    }}
                  />
                  <small style={{ color: '#666', fontSize: '12px' }}>
                    When do you expect this blocker to be resolved?
                  </small>
                </div>
              )}

              <div style={{ 
                display: 'flex', 
                gap: '10px', 
                marginTop: '20px',
                padding: '15px',
                backgroundColor: '#e7f3ff',
                borderRadius: '5px',
                fontSize: '13px',
                color: '#003366'
              }}>
                <span style={{ fontSize: '18px' }}>📤</span>
                <div>
                  <strong>Submission Notice:</strong><br/>
                  Your daily update will be visible to project managers and leadership for progress tracking.
                </div>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '15px' }}>
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                  ✅ Submit to Management
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

export default ProgressUpdates;