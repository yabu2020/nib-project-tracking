
import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { useNavigate } from 'react-router-dom';

const Notifications = () => {
  const { currentUser, logout } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all'); 
  const navigate = useNavigate();

  useEffect(() => {
    if (currentUser?.id) {
      fetchNotifications();
    }
  }, [filter, currentUser?.id]);

  const fetchNotifications = async () => {
  if (!currentUser?.id) return;
  
  setLoading(true);
  try {
    const response = await api.get('/api/notifications', {
      params: { userId: currentUser.id, limit: 100 }
    });
    
    console.log('📥 Notifications response:', response.data);
    
    let data = response.data;
    
    // ... your existing parsing logic ...
    
    // ✅ ADD THIS DEBUG LOGGING
    console.log('🔍 Notification details:');
    if (Array.isArray(data)) {
      data.forEach((n, i) => {
        console.log(`[${i}] ID: ${n.id}, Title: "${n.title}", Type: ${n.type}, Read: ${n.read}, Priority: ${n.priority}`);
        console.log(`    Keys:`, Object.keys(n || {}));
      });
    }
    
    if (!Array.isArray(data)) {
      console.warn('⚠️ Notifications data is not an array:', data);
      data = [];
    }
    
    let filtered = data;
    
    if (filter === 'unread') {
      filtered = filtered.filter(n => !n.read);
      console.log('🔍 Filtered to unread:', filtered.length);
    } else if (filter === 'read') {
      filtered = filtered.filter(n => n.read);
      console.log('🔍 Filtered to read:', filtered.length);
    }
    
    console.log('📦 Setting notifications state:', filtered.length);
    setNotifications(filtered);
    
  } catch (error) {
    console.error('❌ Error fetching notifications:', error);
    setNotifications([]);
  } finally {
    setLoading(false);
  }
};

  const handleBackToDashboard = () => {
    navigate('/dashboard');
  };

  const markAsRead = async (notificationId) => {
    try {
     await api.put(`/api/notifications/${notificationId}/read`);
      fetchNotifications();
    } catch (error) {
      console.error('❌ Error marking notification as read:', error);
    }
  };

  const markAllAsRead = async () => {
    try {
      await api.put('/api/notifications/read-all', null, {
        params: { userId: currentUser?.id }
      });
      fetchNotifications();
    } catch (error) {
      console.error('❌ Error marking all as read:', error);
    }
  };

  const deleteNotification = async (notificationId) => {
    if (!window.confirm('Delete this notification?')) return;
    try {
      await api.delete(`/api/notifications/${notificationId}`);
      fetchNotifications();
    } catch (error) {
      console.error('❌ Error deleting notification:', error);
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
    if (seconds < 604800) return `${Math.floor(seconds / 86400)}d ago`;
    return date.toLocaleDateString();
  };

  const getPriorityColor = (priority) => {
    switch (priority?.toUpperCase()) {
      case 'URGENT': return '#dc3545';
      case 'HIGH': return '#fd7e14';
      case 'MEDIUM': return '#ffc107';
      case 'LOW': return '#28a745';
      default: return '#6c757d';
    }
  };

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  
  const getUnreadCount = () => {
    if (!Array.isArray(notifications)) return 0;
    return notifications.filter(n => !n.read).length;
  };

  const getReadCount = () => {
    if (!Array.isArray(notifications)) return 0;
    return notifications.filter(n => n.read).length;
  };

  return (
    <div className="dashboard-container" style={{ minHeight: '100vh', backgroundColor: '#f3f4f6' }}>
      {/* Navigation Bar */}
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

      {/* Main Content */}
      <div className="main-content" style={{ padding: '30px', maxWidth: '1200px', margin: '0 auto' }}>
        
        {/* Back Button */}
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
            marginBottom: '20px'
          }}
        >
          ← Back
        </button>

        {/* Header */}
        <div style={{ marginBottom: '30px', textAlign: 'center' }}>
          <h1 style={{ margin: '0 0 10px 0', color: '#1f2937', fontSize: '32px', fontWeight: '700' }}>
            🔔 Notifications
          </h1>
          <p style={{ margin: '0', color: '#6b7280', fontSize: '15px' }}>
            Manage your notifications and stay updated
          </p>
        </div>

        {/* Stats Cards */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px', marginBottom: '30px' }}>
          <div style={{
            backgroundColor: 'white',
            padding: '20px',
            borderRadius: '10px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
          }}>
            <div style={{ color: '#6b7280', fontSize: '14px', marginBottom: '5px' }}>Total Notifications</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#8B4513' }}>
              {Array.isArray(notifications) ? notifications.length : 0}
            </div>
          </div>
          
          <div style={{
            backgroundColor: 'white',
            padding: '20px',
            borderRadius: '10px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
          }}>
            <div style={{ color: '#6b7280', fontSize: '14px', marginBottom: '5px' }}>Unread</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#8B4513' }}>
              {getUnreadCount()}
            </div>
          </div>
          
          <div style={{
            backgroundColor: 'white',
            padding: '20px',
            borderRadius: '10px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
          }}>
            <div style={{ color: '#6b7280', fontSize: '14px', marginBottom: '5px' }}>Read</div>
            <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#8B4513' }}>
              {getReadCount()}
            </div>
          </div>
        </div>

        {/* Filters and Actions */}
        <div style={{
          backgroundColor: 'white',
          padding: '20px',
          borderRadius: '10px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          marginBottom: '20px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '15px'
        }}>
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <span style={{ fontWeight: '500', color: '#374151' }}>Filter:</span>
            <button
              onClick={() => setFilter('all')}
              style={{
                padding: '8px 16px',
                borderRadius: '5px',
                border: 'none',
                backgroundColor: filter === 'all' ? '#8B4513' : '#e5e7eb',
                color: filter === 'all' ? 'white' : '#374151',
                cursor: 'pointer',
                fontWeight: '500'
              }}
            >
              All
            </button>
            <button
              onClick={() => setFilter('unread')}
              style={{
                padding: '8px 16px',
                borderRadius: '5px',
                border: 'none',
                backgroundColor: filter === 'unread' ? '#8B4513' : '#e5e7eb',
                color: filter === 'unread' ? 'white' : '#374151',
                cursor: 'pointer',
                fontWeight: '500'
              }}
            >
              Unread
            </button>
            <button
              onClick={() => setFilter('read')}
              style={{
                padding: '8px 16px',
                borderRadius: '5px',
                border: 'none',
                backgroundColor: filter === 'read' ? '#8B4513' : '#e5e7eb',
                color: filter === 'read' ? 'white' : '#374151',
                cursor: 'pointer',
                fontWeight: '500'
              }}
            >
              Read
            </button>
          </div>
          
          {getUnreadCount() > 0 && (
            <button
              onClick={markAllAsRead}
              style={{
                padding: '8px 16px',
                borderRadius: '5px',
                border: 'none',
                backgroundColor: '#8B4513',
                color: 'white',
                cursor: 'pointer',
                fontWeight: '500'
              }}
            >
              Mark All as Read
            </button>
          )}
        </div>

        {/* Notifications List */}
        <div style={{
          backgroundColor: 'white',
          borderRadius: '10px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          overflow: 'hidden'
        }}>
          {loading ? (
            <div style={{ padding: '40px', textAlign: 'center', color: '#6b7280' }}>
              Loading notifications...
            </div>
          ) : !Array.isArray(notifications) || notifications.length === 0 ? (
            <div style={{ padding: '60px 20px', textAlign: 'center', color: '#6b7280' }}>
              <div style={{ fontSize: '48px', marginBottom: '15px' }}>🔔</div>
              <p style={{ fontSize: '18px', fontWeight: '500', marginBottom: '5px' }}>
                {Array.isArray(notifications) ? 'No notifications yet' : 'Error loading notifications'}
              </p>
              <p style={{ fontSize: '14px' }}>
                {Array.isArray(notifications) ? "You're all caught up!" : 'Please try again later'}
              </p>
            </div>
          ) : (
            <div>
              {notifications.map(notification => (
                <div
                  key={notification?.id || Math.random()}
                  className={`notification-item ${!notification?.read ? 'unread' : ''}`}
                  style={{
                    padding: '20px',
                    borderBottom: '1px solid #e5e7eb',
                    backgroundColor: !notification?.read ? 'rgba(239, 246, 255, 0.5)' : 'white',
                    display: 'flex',
                    gap: '15px',
                    transition: 'background-color 0.2s'
                  }}
                >
                  {/* Icon */}
                  <div style={{ fontSize: '24px', flexShrink: '0' }}>
                    {notification?.icon || '🔔'}
                  </div>
                  
                  {/* Content */}
                  <div style={{ flex: '1', minWidth: '0' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                      <div>
                        <h3 style={{
                          margin: '0 0 5px 0',
                          fontSize: '16px',
                          fontWeight: '600',
                          color: !notification?.read ? '#111827' : '#374151'
                        }}>
                          {notification?.title || 'Untitled'}
                        </h3>
                        {notification?.priority && (
                          <span style={{
                            display: 'inline-block',
                            padding: '2px 8px',
                            borderRadius: '4px',
                            fontSize: '11px',
                            fontWeight: '600',
                            backgroundColor: getPriorityColor(notification.priority),
                            color: notification.priority === 'MEDIUM' ? '#000' : 'white'
                          }}>
                            {notification.priority}
                          </span>
                        )}
                        {notification?.type && (
                          <span style={{ marginLeft: '8px', fontSize: '12px', color: '#6b7280' }}>
                            • {notification.type}
                          </span>
                        )}
                      </div>
                      <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                        <span style={{ fontSize: '12px', color: '#9ca3af' }}>
                          {timeAgo(notification?.createdAt)}
                        </span>
                        {!notification?.read && (
                          <button
                            onClick={() => markAsRead(notification.id)}
                            style={{
                              padding: '4px 8px',
                              borderRadius: '4px',
                              border: '1px solid #8B4513',
                              backgroundColor: 'white',
                              color: '#8B4513',
                              cursor: 'pointer',
                              fontSize: '12px'
                            }}
                          >
                            Mark as read
                          </button>
                        )}
                        <button
                          onClick={() => deleteNotification(notification.id)}
                          style={{
                            padding: '4px 8px',
                            borderRadius: '4px',
                            border: 'none',
                            backgroundColor: '#fee2e2',
                            color: '#dc3545',
                            cursor: 'pointer',
                            fontSize: '12px'
                          }}
                        >
                          🗑️
                        </button>
                      </div>
                    </div>
                    <p style={{ margin: '0', fontSize: '14px', color: '#4b5563', lineHeight: '1.5' }}>
                      {notification?.message || 'No message'}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Notifications;