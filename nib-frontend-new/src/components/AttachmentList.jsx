import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

const AttachmentList = ({
  projectId,
  taskId,
  userId,
  refreshTrigger = 0,
  onRefreshAttachments,
  canDelete = true,        
  currentUserId = null,    
}) => {
  const { currentUser } = useAuth();

  const [attachments, setAttachments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState('');


   
  const canUpload = useCallback(() => {
    const technicalRoles = [
      'DEVELOPER', 
      'SENIOR_IT_OFFICER', 
      'JUNIOR_IT_OFFICER', 
      'IT_GRADUATE_TRAINEE'
    ];
    
    return technicalRoles.includes(currentUser?.role);
  }, [currentUser?.role]);

 
const getEndpoint = useCallback(() => {
  if (projectId) return '/api/attachments/project/' + projectId;
  if (taskId) return '/api/attachments/task/' + taskId;
  console.warn('AttachmentList: No projectId or taskId provided');
  return null;
}, [projectId, taskId]);

const fetchAttachments = useCallback(async (silent = false) => {
  const endpoint = getEndpoint();
  if (!endpoint) return;

  if (!silent) setLoading(true);
  setError('');

  console.log(`📡 Fetching attachments → ${endpoint} (silent=${silent})`);

  try {
   
    const res = await api.get(endpoint);
    const data = Array.isArray(res.data) ? res.data : [];
    console.log(`✅ Loaded ${data.length} attachments`);
    setAttachments(data);
  } catch (err) {
    console.error('❌ Fetch failed:', err);
    setError(err.response?.data?.message || 'Failed to load attachments');
  } finally {
    setLoading(false);
  }
}, [getEndpoint]);

useEffect(() => {
  console.log('🔄 AttachmentList: IDs changed or mounted → auto-fetch');
  fetchAttachments();
}, [fetchAttachments]);

useEffect(() => {
  if (refreshTrigger > 0) {
    console.log(`🔄 refreshTrigger changed → ${refreshTrigger}`);
    fetchAttachments(true);
  }
}, [refreshTrigger, fetchAttachments]);

const refreshFn = useCallback(() => {
  console.log('🔄 refreshFn called from parent');
  fetchAttachments(true);
}, [fetchAttachments]);

useEffect(() => {
  if (onRefreshAttachments) {
    console.log('🔗 Parent registered refresh function');
    onRefreshAttachments(refreshFn);
  }
}, [onRefreshAttachments, refreshFn]);

const handleDelete = async (attachmentId) => {
  if (!window.confirm('Delete this file?')) return;

  setDeletingId(attachmentId);
  setError('');

  try {
   
    await api.delete(`/api/attachments/${attachmentId}`);
    console.log('🗑️ Deleted attachment', attachmentId);
    setAttachments(prev => prev.filter(a => a.id !== attachmentId));
    await fetchAttachments(true);
  } catch (err) {
    console.error('Delete error:', err);
    setError('Delete failed: ' + (err.response?.data?.message || 'server error'));
  } finally {
    setDeletingId(null);
  }
};

const handleDownload = (id) => {
 
  window.open(`/api/attachments/${id}/download`, '_blank', 'noopener,noreferrer');
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

  const isTechnicalStaff = () =>
    ['DEVELOPER', 'SENIOR_IT_OFFICER', 'JUNIOR_IT_OFFICER', 'IT_GRADUATE_TRAINEE']
      .includes(currentUser?.role);

  if (loading) {
    return <div style={{ padding: '32px', textAlign: 'center', color: '#6c757d' }}>Loading attachments...</div>;
  }

  return (
    <div style={{ marginTop: '24px' }}>
      <h3 style={{ margin: '0 0 16px', color: '#8B4513' }}>
        📎 Attachments {attachments.length > 0 && `(${attachments.length})`}
      </h3>

      {error && (
        <div style={{
          padding: '12px 16px',
          marginBottom: '16px',
          background: '#f8d7da',
          color: '#721c24',
          borderRadius: '6px',
          border: '1px solid #f5c6cb'
        }}>
          {error}
        </div>
      )}

      {attachments.length === 0 ? (
        <div style={{
          padding: '40px 20px',
          background: '#f8f9fa',
          borderRadius: '8px',
          border: '1px dashed #ced4da',
          textAlign: 'center',
          color: '#6c757d'
        }}>
          No attachments yet.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {attachments.map(att => {
            const isDeleting = deletingId === att.id;
            return (
              <div
                key={att.id}
                style={{
                  padding: '16px',
                  border: '1px solid #dee2e6',
                  borderRadius: '8px',
                  background: isDeleting ? '#f8f9fa' : 'white',
                  opacity: isDeleting ? 0.65 : 1,
                  transition: 'opacity 0.2s',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: '16px'
                }}
              >
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 600, color: '#8B4513', fontSize: '15px' }}>
                    📄 {att.originalFileName || att.fileName || att.filename || 'File'}
                  </div>
                  {att.description && (
                    <div style={{ fontSize: '13px', color: '#555', margin: '4px 0' }}>
                      {att.description}
                    </div>
                  )}
                  <div style={{ fontSize: '12.5px', color: '#6c757d' }}>
                    {formatFileSize(att.fileSize)} • {new Date(att.uploadedAt).toLocaleString('am-ET', {
                      dateStyle: 'medium',
                      timeStyle: 'short'
                    })}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '10px', flexShrink: 0 }}>
                  <button
                    onClick={() => handleDownload(att.id)}
                    disabled={isDeleting}
                    style={{
                      padding: '8px 14px',
                      background: isDeleting ? '#adb5bd' : '#28a745',
                      color: 'white',
                      border: 'none',
                      borderRadius: '6px',
                      cursor: isDeleting ? 'not-allowed' : 'pointer',
                      fontSize: '13px'
                    }}
                  >
                    ⬇ Download
                  </button>

              {canDelete && (
    <button
      onClick={() => handleDelete(att.id)}
      disabled={isDeleting}
      title={isTechnicalStaff() ? 'Delete this file' : 'You can delete files you uploaded'}
      style={{
        padding: '8px 14px',
        background: isDeleting ? '#ffc107' : '#dc3545',
        color: 'white',
        border: 'none',
        borderRadius: '6px',
        cursor: isDeleting ? 'not-allowed' : 'pointer',
        fontSize: '13px',
        minWidth: '90px'
      }}
    >
      {isDeleting ? 'Deleting…' : '🗑 Delete'}
    </button>
  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default AttachmentList;