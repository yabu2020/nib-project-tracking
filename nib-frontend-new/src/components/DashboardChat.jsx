import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

const DashboardChat = () => {
  const { currentUser } = useAuth();
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newComment, setNewComment] = useState('');
  const [selectedProject, setSelectedProject] = useState(null);
  const [projects, setProjects] = useState([]);
  const [showAll, setShowAll] = useState(false);
  const chatEndRef = useRef(null);

  useEffect(() => {
    fetchProjectsAndComments();
  
    const interval = setInterval(fetchProjectsAndComments, 30000);
    return () => clearInterval(interval);
  }, []);

 
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [comments]);

  const fetchProjectsAndComments = async () => {
    try {
      
      const projectsRes = await api.get('/api/projects', {
        params: { userId: currentUser?.id, userRole: currentUser?.role }
      });
      setProjects(projectsRes.data || []);


      const commentsPromises = (projectsRes.data || []).map(project =>
        api.get(`/api/projects/${project.id}/comments`)
      );
      
      const commentsResponses = await Promise.all(commentsPromises);
      
      const allComments = [];
      commentsResponses.forEach((response, index) => {
        const project = projectsRes.data[index];
        const projectComments = response.data || [];
        
        projectComments.forEach(comment => {
          allComments.push({
            ...comment,
            projectName: project.projectName,
            projectId: project.id
          });
        });
      });

     
      allComments.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      
      setComments(allComments);
      setLoading(false);
    } catch (error) {
      console.error('❌ Error fetching chat data:', error);
      setLoading(false);
    }
  };

  const handleSendComment = async (projectId) => {
    if (!newComment.trim()) return;

    try {
      await api.post(`/api/projects/${projectId}/comments`, null, {
        params: {
          userId: currentUser.id,
          commentText: newComment,
          parentCommentId: null
        }
      });

      setNewComment('');
      fetchProjectsAndComments(); 
    } catch (error) {
      console.error('❌ Error sending comment:', error);
      alert('Failed to send comment');
    }
  };

  const timeAgo = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);
    
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return date.toLocaleDateString();
  };

  
  const displayedComments = showAll ? comments : comments.slice(0, 10);

  return (
    <div style={{
      backgroundColor: 'white',
      borderRadius: '10px',
      boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
      padding: '20px',
      maxHeight: '600px',
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column'
    }}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '15px',
        paddingBottom: '10px',
        borderBottom: '2px solid #8B4513'
      }}>
        <h2 style={{ margin: 0, color: '#8B4513', fontSize: '20px' }}>
          💬 Project Comments
        </h2>
        <button
          onClick={fetchProjectsAndComments}
          style={{
            padding: '5px 10px',
            backgroundColor: '#8B4513',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            cursor: 'pointer',
            fontSize: '12px'
          }}
        >
          🔄 Refresh
        </button>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
          <p>Loading comments...</p>
        </div>
      ) : comments.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
          <p>No comments yet</p>
          <p style={{ fontSize: '14px' }}>Be the first to comment on a project!</p>
        </div>
      ) : (
        <>
          {/* Comments List */}
          <div style={{
            flex: 1,
            overflowY: 'auto',
            marginBottom: '15px',
            padding: '10px',
            backgroundColor: '#f9f9f9',
            borderRadius: '8px'
          }}>
            {displayedComments.map((comment, index) => (
              <div
                key={comment.id || index}
                style={{
                  marginBottom: '15px',
                  padding: '10px',
                  backgroundColor: 'white',
                  borderRadius: '8px',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
                }}
              >
                {/* Project Name */}
                <div style={{
                  fontSize: '12px',
                  color: '#8B4513',
                  fontWeight: '600',
                  marginBottom: '5px'
                }}>
                  📁 {comment.projectName}
                </div>

                {/* Comment Text */}
                <div style={{
                  fontSize: '14px',
                  color: '#333',
                  marginBottom: '8px',
                  lineHeight: '1.4'
                }}>
                  {comment.commentText}
                </div>

                {/* Author & Time */}
                <div style={{
                  fontSize: '11px',
                  color: '#999',
                  display: 'flex',
                  justifyContent: 'space-between'
                }}>
                  <span>👤 {comment.user?.fullName || comment.user?.username || 'Unknown'}</span>
                  <span>{timeAgo(comment.createdAt)}</span>
                </div>
              </div>
            ))}
            <div ref={chatEndRef} />
          </div>

          {/* Show More/Less Button */}
          {comments.length > 10 && (
            <button
              onClick={() => setShowAll(!showAll)}
              style={{
                padding: '8px 15px',
                backgroundColor: '#D2691E',
                color: 'white',
                border: 'none',
                borderRadius: '5px',
                cursor: 'pointer',
                marginBottom: '15px',
                fontSize: '13px'
              }}
            >
              {showAll ? 'Show Less' : `Show More (${comments.length - 10} more)`}
            </button>
          )}

          {/* Quick Comment Input */}
          <div style={{
            display: 'flex',
            gap: '10px',
            alignItems: 'flex-start'
          }}>
            <select
              value={selectedProject || ''}
              onChange={(e) => setSelectedProject(e.target.value)}
              style={{
                padding: '10px',
                border: '2px solid #D2691E',
                borderRadius: '5px',
                fontSize: '13px',
                flex: '0 0 150px',
                backgroundColor: 'white'
              }}
            >
              <option value="">Select Project...</option>
              {projects.map(project => (
                <option key={project.id} value={project.id}>
                  {project.projectName}
                </option>
              ))}
            </select>
            
            <textarea
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              placeholder="Type your comment..."
              rows="2"
              style={{
                flex: 1,
                padding: '10px',
                border: '2px solid #D2691E',
                borderRadius: '5px',
                fontSize: '14px',
                resize: 'none',
                fontFamily: 'inherit'
              }}
            />
          </div>
          
          <button
            onClick={() => selectedProject && handleSendComment(selectedProject)}
            disabled={!selectedProject || !newComment.trim()}
            style={{
              marginTop: '10px',
              padding: '10px 20px',
              backgroundColor: (!selectedProject || !newComment.trim()) ? '#ccc' : '#8B4513',
              color: 'white',
              border: 'none',
              borderRadius: '5px',
              cursor: (!selectedProject || !newComment.trim()) ? 'not-allowed' : 'pointer',
              fontSize: '14px',
              fontWeight: '600',
              alignSelf: 'flex-end'
            }}
          >
            💬 Send Comment
          </button>
        </>
      )}
    </div>
  );
};

export default DashboardChat;