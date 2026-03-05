import React, { useState, useEffect, useRef } from 'react';
import api from '../services/api';

const NotificationBell = ({ currentUser }) => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [showDropdown, setShowDropdown] = useState(false);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef(null);


  const fetchUnreadCount = async () => {
    if (!currentUser?.id) return;
    try {
   
      const response = await api.get('/api/notifications/count', {
        params: { userId: currentUser.id }
      });
      setUnreadCount(response.data.count || 0);
    } catch (error) {
      console.error('Error fetching notification count:', error);
    }
  };

  const fetchNotifications = async () => {
    if (!currentUser?.id) return;
    setLoading(true);
    try {
     
      const response = await api.get('/api/notifications', {
        params: { userId: currentUser.id, limit: 10 }
      });

 
      let data = response.data || [];
      if (!Array.isArray(data)) {
        console.warn('Backend returned non-array for notifications:', data);
        data = [];
      }

      setNotifications(data);
    } catch (error) {
      console.error('Error fetching notifications:', error);
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  };


  const markAsRead = async (notificationId) => {
    try {

      await api.put(`/api/notifications/${notificationId}/read`);
      fetchUnreadCount();
      fetchNotifications();
    } catch (error) {
      console.error('Error marking notification as read:', error);
    }
  };


  const markAllAsRead = async () => {
    try {
      
      await api.put('/api/notifications/read-all', null, {
        params: { userId: currentUser.id }
      });
      setUnreadCount(0);
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
    } catch (error) {
      console.error('Error marking all as read:', error);
    }
  };


  const timeAgo = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);

    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return date.toLocaleDateString();
  };

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    if (currentUser?.id) {
      fetchUnreadCount();
      fetchNotifications();

      const interval = setInterval(() => {
        fetchUnreadCount();
        if (showDropdown) fetchNotifications();
      }, 30000);

      return () => clearInterval(interval);
    }
  }, [currentUser?.id, showDropdown]);

  return (
    <div className="relative inline-block" ref={dropdownRef} style={{ position: 'relative' }}>
      {/* Bell Icon */}
      <button
        onClick={() => {
          setShowDropdown(!showDropdown);
          if (!showDropdown) fetchNotifications();
        }}
        className="relative p-2 text-white hover:bg-white/10 rounded-lg transition-colors"
        title="Notifications"
        style={{
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          fontSize: '20px',
          padding: '8px'
        }}
      >
        <span>🔔</span>

        {/* Unread Badge */}
        {unreadCount > 0 && (
          <span
            className="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center animate-pulse"
            style={{
              position: 'absolute',
              top: '-4px',
              right: '-4px',
              backgroundColor: '#dc3545',
              color: 'white',
              fontSize: '11px',
              fontWeight: 'bold',
              borderRadius: '50%',
              height: '18px',
              width: '18px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              minWidth: '18px'
            }}
          >
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown */}
      {showDropdown && (
        <div
          className="absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-xl border border-gray-200 z-50 max-h-96 overflow-hidden"
          style={{
            position: 'absolute',
            right: '0',
            top: '100%',
            marginTop: '8px',
            width: '320px',
            backgroundColor: 'white',
            borderRadius: '8px',
            boxShadow: '0 10px 40px rgba(0,0,0,0.2)',
            border: '1px solid #e5e7eb',
            zIndex: '9999',
            maxHeight: '400px',
            overflow: 'hidden',
            overflowY: 'auto'
          }}
        >
          {/* Header */}
          <div
            className="p-4 border-b border-gray-200 flex justify-between items-center bg-gray-50"
            style={{
              padding: '12px 16px',
              borderBottom: '1px solid #e5e7eb',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              backgroundColor: '#f9fafb'
            }}
          >
            <h3 className="font-semibold text-gray-800" style={{ margin: '0', fontSize: '16px', fontWeight: '600' }}>
              Notifications
            </h3>
            {unreadCount > 0 && (
              <button
                onClick={markAllAsRead}
                className="text-xs text-blue-600 hover:text-blue-800 font-medium"
                style={{
                  background: 'none',
                  border: 'none',
                  color: '#2563eb',
                  fontSize: '12px',
                  cursor: 'pointer',
                  fontWeight: '500'
                }}
              >
                Mark all read
              </button>
            )}
          </div>

          {/* Notifications List */}
          <div className="max-h-72 overflow-y-auto" style={{ maxHeight: '300px', overflowY: 'auto' }}>
            {loading ? (
              <div className="p-4 text-center text-gray-500" style={{ padding: '16px', textAlign: 'center', color: '#6b7280' }}>
                Loading...
              </div>
            ) : notifications.length === 0 ? (
              <div className="p-4 text-center text-gray-500 text-sm" style={{ padding: '16px', textAlign: 'center', color: '#6b7280', fontSize: '14px' }}>
                No notifications yet
              </div>
            ) : (
              notifications.map(notification => (
                <div
                  key={notification.id}
                  className={`p-4 border-b border-gray-100 hover:bg-gray-50 cursor-pointer transition-colors ${!notification.read ? 'bg-blue-50/50' : ''}`}
                  onClick={() => {
                    markAsRead(notification.id);
                 
                  }}
                  style={{
                    padding: '16px',
                    borderBottom: '1px solid #f3f4f6',
                    backgroundColor: !notification.read ? 'rgba(239, 246, 255, 0.5)' : 'white',
                    cursor: 'pointer',
                    transition: 'background-color 0.2s'
                  }}
                  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f9fafb'}
                  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = !notification.read ? 'rgba(239, 246, 255, 0.5)' : 'white'}
                >
                  <div className="flex gap-3" style={{ display: 'flex', gap: '12px' }}>
                    {/* Icon */}
                    <span className="text-xl flex-shrink-0" style={{ fontSize: '20px', flexShrink: '0' }}>
                      {notification.icon || '🔔'}
                    </span>

                    {/* Content */}
                    <div className="flex-1 min-w-0" style={{ flex: '1', minWidth: '0' }}>
                      <div className="flex justify-between items-start gap-2" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px' }}>
                        <p className={`font-medium text-sm ${!notification.read ? 'text-gray-900' : 'text-gray-700'}`}
                           style={{
                             margin: '0',
                             fontSize: '14px',
                             fontWeight: '500',
                             color: !notification.read ? '#111827' : '#374151'
                           }}
                        >
                          {notification.title}
                        </p>
                        {!notification.read && (
                          <span className="w-2 h-2 bg-blue-500 rounded-full flex-shrink-0 mt-1"
                                style={{
                                  width: '8px',
                                  height: '8px',
                                  backgroundColor: '#3b82f6',
                                  borderRadius: '50%',
                                  flexShrink: '0',
                                  marginTop: '2px'
                                }}
                          ></span>
                        )}
                      </div>
                      <p className="text-xs text-gray-600 mt-1 line-clamp-2"
                         style={{
                           margin: '4px 0 0 0',
                           fontSize: '12px',
                           color: '#4b5563',
                           display: '-webkit-box',
                           WebkitLineClamp: '2',
                           WebkitBoxOrient: 'vertical',
                           overflow: 'hidden',
                           lineHeight: '1.4'
                         }}
                      >
                        {notification.message}
                      </p>
                      <p className="text-xs text-gray-400 mt-2"
                         style={{
                           margin: '8px 0 0 0',
                           fontSize: '11px',
                           color: '#9ca3af'
                         }}
                      >
                        {timeAgo(notification.createdAt)}
                      </p>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Footer */}
          <div className="p-3 border-t border-gray-200 bg-gray-50 text-center"
               style={{
                 padding: '12px',
                 borderTop: '1px solid #e5e7eb',
                 backgroundColor: '#f9fafb',
                 textAlign: 'center'
               }}
          >
            <a
              href="/notifications"
              className="text-xs text-blue-600 hover:text-blue-800 font-medium"
              onClick={() => setShowDropdown(false)}
              style={{
                color: '#2563eb',
                fontSize: '12px',
                fontWeight: '500',
                textDecoration: 'none'
              }}
            >
              View all notifications
            </a>
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell;