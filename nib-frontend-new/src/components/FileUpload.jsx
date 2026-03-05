
import React, { useState } from 'react';
import api from '../services/api';

const FileUpload = ({
  projectId,
  taskId,
  userId,
  onUploadSuccess,           
  onAttachmentAdded,         
}) => {
  const [uploading, setUploading] = useState(false);
  const [description, setDescription] = useState('');
  const [message, setMessage] = useState({ type: '', text: '' });
  const [selectedFile, setSelectedFile] = useState(null);

  const handleFileSelect = (e) => {
    setSelectedFile(e.target.files[0]);
    setMessage({ type: '', text: '' });
  };



const handleUpload = async () => {
  if (!selectedFile) {
    setMessage({ type: 'error', text: 'Please select a file first' });
    return;
  }

  
  if (selectedFile.size > 10 * 1024 * 1024) {
    setMessage({ type: 'error', text: 'File size exceeds 10MB limit' });
    return;
  }

  setUploading(true);
  setMessage({ type: '', text: '' });

  try {
    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('userId', userId);
    if (description) formData.append('description', description);

    const endpoint = projectId
      ? `/api/attachments/project/${projectId}`
      : `/api/attachments/task/${taskId}`;

    const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
    const currentUserId = storedUser.id || userId;

    const response = await api.post(endpoint, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'X-User-Id': currentUserId,  
      },
    });

    const newAttachment = response.data.attachment;

    
    setMessage({ type: 'success', text: '✅ File uploaded successfully!' });

    setDescription('');
    setSelectedFile(null);
    const fileInput = document.getElementById('file-input');
    if (fileInput) fileInput.value = '';

    
    if (onAttachmentAdded) {
      onAttachmentAdded(newAttachment);
    }
    if (onUploadSuccess) {
      onUploadSuccess(newAttachment);
    }

   
    setTimeout(() => {
      setMessage({ type: '', text: '' });
    }, 3000);

  } catch (error) {
    console.error('Upload error:', error);
    setMessage({
      type: 'error',
      text: '❌ Upload failed: ' + (error.response?.data?.error || error.message || 'Unknown error'),
    });
  } finally {
    setUploading(false);
  }
};


  return (
    <div style={{ marginBottom: '15px' }}>
      <div style={{ marginBottom: '10px' }}>
        <input
          id="file-input"
          type="file"
          onChange={handleFileSelect}
          disabled={uploading}
          style={{
            padding: '8px',
            border: '1px solid #ddd',
            borderRadius: '5px',
            width: '100%',
            cursor: uploading ? 'not-allowed' : 'pointer',
          }}
        />
      </div>

      <input
        type="text"
        placeholder="Description (optional)"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        disabled={uploading}
        style={{
          width: '100%',
          padding: '10px',
          border: '1px solid #ddd',
          borderRadius: '5px',
          marginBottom: '10px',
          fontSize: '14px',
        }}
      />

      <button
        onClick={handleUpload}
        disabled={uploading || !selectedFile}
        style={{
          padding: '10px 20px',
          backgroundColor: uploading || !selectedFile ? '#ccc' : '#8B4513',
          color: 'white',
          border: 'none',
          borderRadius: '5px',
          cursor: uploading || !selectedFile ? 'not-allowed' : 'pointer',
          fontSize: '14px',
          fontWeight: '600',
        }}
      >
        {uploading ? '⏳ Uploading...' : '📤 Upload File'}
      </button>

      {message.text && (
        <div
          style={{
            marginTop: '10px',
            padding: '10px',
            borderRadius: '5px',
            backgroundColor: message.type === 'success' ? '#d4edda' : '#f8d7da',
            color: message.type === 'success' ? '#155724' : '#721c24',
            border: `1px solid ${message.type === 'success' ? '#c3e6cb' : '#f5c6cb'}`,
            fontSize: '14px',
          }}
        >
          {message.text}
        </div>
      )}
    </div>
  );
};

export default FileUpload;