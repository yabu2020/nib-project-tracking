import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import '../styles/App.css';
import { useNavigate } from 'react-router-dom';
import FileUpload from '../components/FileUpload';
import AttachmentList from '../components/AttachmentList';
import Select from 'react-select';
const Tasks = () => {
  const { currentUser, logout } = useAuth();
  const navigate = useNavigate();  
  const [tasks, setTasks] = useState([]);
  const [projects, setProjects] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [filterProject, setFilterProject] = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');
  const [filterPriority, setFilterPriority] = useState('all');
  const [editingTask, setEditingTask] = useState(null);
  
  const [selectedTask, setSelectedTask] = useState(null);
  const [showAttachments, setShowAttachments] = useState(false);
  const [attachmentRefresh, setAttachmentRefresh] = useState(0);
  const [tasksRefreshTrigger, setTasksRefreshTrigger] = useState(0);
  
  const [formData, setFormData] = useState({
    taskName: '',
    description: '',
    projectId: '',
    assignedTo: '',
    dueDate: '',
    priority: 2,
    status: 'PENDING'
  });

  useEffect(() => {
    fetchData();
  }, [tasksRefreshTrigger]);

  const fetchData = async () => {
    try {
      console.log('🔄 Fetching tasks data...');
      
      const [tasksRes, projectsRes, usersRes] = await Promise.all([
        api.get('/api/tasks', {
          params: {
            userId: currentUser?.id,
            userRole: currentUser?.role
          },
          headers: {
            'X-User-Id': currentUser?.id
          }
        }),
        api.get('/api/projects'),
        api.get('/api/users')
      ]);
      
      let tasksData = tasksRes.data;
      if (typeof tasksData === 'string') tasksData = JSON.parse(tasksData);
      
      let projectsData = projectsRes.data;
      if (typeof projectsData === 'string') projectsData = JSON.parse(projectsData);
      
      let usersData = usersRes.data;
      if (typeof usersData === 'string') usersData = JSON.parse(usersData);
      
      console.log('✅ Tasks loaded:', tasksData?.length || 0);
      console.log('✅ Projects loaded:', projectsData?.length || 0);
      console.log('✅ Users loaded:', usersData?.length || 0);
      
      setTasks(Array.isArray(tasksData) ? tasksData : []);
      setProjects(Array.isArray(projectsData) ? projectsData : []);
      setUsers(Array.isArray(usersData) ? usersData : []);
    } catch (error) {
      console.error('❌ Error fetching ', error);
      setTasks([]);
      setProjects([]);
      setUsers([]);
    } finally {
      setLoading(false);
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      console.log(editingTask ? 'Updating task...' : 'Creating task...');
      
      const requestData = {
        ...formData,
        initiatedById: currentUser?.id
      };

      let response;
      if (editingTask) {
       
        response = await api.put(`/api/tasks/${editingTask.id}`, requestData, {
          headers: {
            'X-User-Id': currentUser?.id
          }
        });
        alert('Task updated successfully!');
      } else {
       
        response = await api.post('/api/tasks', requestData, {
          headers: {
            'X-User-Id': currentUser?.id
          }
        });
        alert('Task created successfully!');
      }

      setShowModal(false);
      setEditingTask(null); 
      fetchData();
      setFormData({
  taskName: '',
  description: '',
  projectId: '',
  assignedTo: '',
  dueDate: '',
  priority: 2,
  status: 'PENDING'
});
    } catch (error) {
      console.error('Error:', error);
      const errorMsg = error.response?.data?.error || 'Unknown error occurred';
      alert('Error: ' + errorMsg);
    }
  };

  
  const handleEdit = (task) => {
  setEditingTask(task);
  setFormData({
    taskName: task.taskName,
    description: task.description || '',
    projectId: task.project?.id || '',
    assignedTo: task.assignedTo?.id || '',  
    dueDate: task.dueDate || '',
    priority: task.priority || 2,
    status: task.status || 'PENDING'
  });
  setShowModal(true);
};

  const handleStatusChange = async (taskId, newStatus) => {
    try {
      await api.put(`/api/tasks/${taskId}/status`, { status: newStatus }, {
        headers: {
          'X-User-Id': currentUser?.id
        }
      });
      fetchData();
    } catch (error) {
      console.error('Error updating task status:', error);
    }
  };

  const handleDelete = async (taskId) => {
    if (!window.confirm('Are you sure you want to delete this task?')) return;
    
    try {
      await api.delete(`/api/tasks/${taskId}`, {
        headers: {
          'X-User-Id': currentUser?.id
        }
      });
      fetchData();
      alert('Task deleted successfully!');
    } catch (error) {
      alert('Error deleting task: ' + (error.response?.data?.error || 'Unknown error'));
    }
  };

  const handleLogout = () => {
    logout();
    window.location.href = '/login';
  };

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 4: return '#dc3545';
      case 3: return '#fd7e14';
      case 2: return '#ffc107';
      case 1: return '#28a745';
      default: return '#6c757d';
    }
  };

  const getPriorityLabel = (priority) => {
    switch (priority) {
      case 4: return 'Critical';
      case 3: return 'High';
      case 2: return 'Medium';
      case 1: return 'Low';
      default: return 'Unknown';
    }
  };

  const isExecutive = () => {
    const executiveRoles = [
      'CEO', 'DEPUTY_CHIEF', 'DIRECTOR', 'BUSINESS', 'QUALITY_ASSURANCE'
    ];
    return executiveRoles.includes(currentUser?.role);
  };

  const isManager = () => {
    const managerRoles = [
      'CEO', 'DEPUTY_CHIEF', 'DIRECTOR', 'BUSINESS',
      'PROJECT_MANAGER', 'CORE_BANKING_MANAGER', 'DIGITAL_BANKING_MANAGER'
    ];
    return managerRoles.includes(currentUser?.role);
  };

  const isTechnicalStaff = () => {
    const techRoles = [
      'DEVELOPER', 'SENIOR_IT_OFFICER', 'JUNIOR_IT_OFFICER', 'IT_GRADUATE_TRAINEE'
    ];
    return techRoles.includes(currentUser?.role);
  };

  const canUploadToTask = (task) => {
    if (isManager()) return true;
    if (task.assignedTo?.id === currentUser?.id && isTechnicalStaff()) return true;
    if (task.project?.manager?.id === currentUser?.id) return true;
    return false;
  };

  const handleViewTask = (task) => {
    console.log('👁️ Viewing task:', task);
    setSelectedTask(task);
    setShowAttachments(true);
  };

  const handleCloseAttachments = () => {
    setShowAttachments(false);
    setSelectedTask(null);
    setTasksRefreshTrigger(prev => prev + 1);
  };

  const handleAttachmentUploadSuccess = () => {
    console.log('📤 Upload success → refreshing both attachments & tasks list');
    setAttachmentRefresh(prev => prev + 1);
    setTasksRefreshTrigger(prev => prev + 1);
  };

  

  const filteredTasks = (Array.isArray(tasks) ? tasks : []).filter(t => {
  if (filterProject !== 'all' && t.project?.id !== parseInt(filterProject)) return false;
  if (filterStatus !== 'all' && t.status !== filterStatus) return false;
  
  if (filterPriority !== 'all') {
    const priorityNum = parseInt(filterPriority);
    if (t.priority !== priorityNum) return false;
  }
  
  return true;
});

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
        <div className="loading"><p>Loading tasks...</p></div>
      </div>
    );
  }

  if (showAttachments && selectedTask) {
    return (
      <div className="dashboard-container">
        <nav className="navbar">
          <div className="navbar-brand">NIB IT Project Tracking
            <h4>ንብ ኢንተርናሽናል ባንክ</h4>
          </div>
          <div className="navbar-user">
            <span>Welcome, {currentUser?.fullName || currentUser?.username}</span>
            <button onClick={handleLogout} className="logout-btn">Logout</button>
          </div>
        </nav>

        <div className="main-content" style={{ padding: '30px', maxWidth: '1200px', margin: '0 auto' }}>
          <button 
            onClick={handleCloseAttachments}
            className="btn"
            style={{ 
              padding: '8px 15px', 
              backgroundColor: '#6c757d', 
              color: 'white',
              border: 'none',
              borderRadius: '5px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '5px',
              marginBottom: '20px'
            }}
          >
            ← Back to Tasks
          </button>

          <div style={{ 
            backgroundColor: 'white',
            padding: '25px',
            borderRadius: '10px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            marginBottom: '20px'
          }}>
            <h1 style={{ margin: '0 0 10px 0', color: '#8B4513' }}>{selectedTask.taskName}</h1>
            <p style={{ margin: '0 0 15px 0', color: '#666' }}>{selectedTask.description}</p>
            
            <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
              <div><strong>Project:</strong> {selectedTask.project?.projectName || 'N/A'}</div>
              <div><strong>Assigned To:</strong> {selectedTask.assignedTo?.fullName || 'Unassigned'}</div>
              <div>
                <strong>Priority:</strong>{' '}
                <span style={{
                  padding: '3px 10px',
                  borderRadius: '4px',
                  backgroundColor: getPriorityColor(selectedTask.priority),
                  color: selectedTask.priority <= 2 ? '#fff' : '#000',
                  fontSize: '12px'
                }}>
                  {getPriorityLabel(selectedTask.priority)}
                </span>
              </div>
              <div>
                <strong>Status:</strong>{' '}
                <span style={{
                  padding: '3px 10px',
                  borderRadius: '4px',
                  backgroundColor: selectedTask.status === 'COMPLETED' ? '#28a745' : 
                                   selectedTask.status === 'IN_PROGRESS' ? '#007bff' : '#6c757d',
                  color: selectedTask.status === 'PENDING' ? '#000' : '#fff',
                  fontSize: '12px'
                }}>
                  {selectedTask.status}
                </span>
              </div>
              <div><strong>Due:</strong> {selectedTask.dueDate || 'Not set'}</div>
            </div>
          </div>

          {canUploadToTask(selectedTask) && (
            <div className="content-card" style={{ marginBottom: '20px' }}>
              <h3>📤 Upload File</h3>
              <p style={{ color: '#666', fontSize: '14px', marginBottom: '15px' }}>
                Upload files related to this task (code, documentation, test results, etc.)
              </p>
              <FileUpload
                taskId={selectedTask.id}
                userId={currentUser?.id}
                onUploadSuccess={handleAttachmentUploadSuccess}
              />
            </div>
          )}

          {isManager() && !canUploadToTask(selectedTask) && (
            <div style={{ 
              padding: '15px', 
              backgroundColor: '#e7f3ff', 
              borderRadius: '8px', 
              marginBottom: '20px',
              borderLeft: '4px solid #007bff'
            }}>
              <p style={{ margin: 0, color: '#004085', fontSize: '14px' }}>
                👁️ <strong>View Only:</strong> IT officers can upload files to this task. 
                Contact your technical team to add documents.
              </p>
            </div>
          )}

          <div className="content-card">
            <AttachmentList
              taskId={selectedTask.id}
              userId={currentUser?.id}
              refreshTrigger={attachmentRefresh}
            />
          </div>
        </div>
      </div>
    );
  }

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

      <div className="main-content">
        <div className="page-header">
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center',
            position: 'relative'
          }}>
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
                gap: '5px',
                position: 'absolute',
                left: 0
              }}
            >
              ← Back
            </button>
            
            <div style={{ 
              flex: 1, 
              display: 'flex', 
              flexDirection: 'column', 
              alignItems: 'center',
              justifyContent: 'center',
              textAlign: 'center'
            }}>
              <h1 style={{ margin: 0, fontSize: '32px', fontWeight: '700', color: '#8B4513' }}>
                {isManager() ? 'All Tasks' : 'My Tasks'}
              </h1>
              <p style={{ margin: '8px 0 0 0', color: '#666', fontSize: '14px' }}>
                {isManager() ? 'Assign and track tasks for IT projects' : 'Tasks assigned to you'}
              </p>
            </div>
            
            {isManager() && (
              <button 
                className="btn btn-primary" 
                onClick={() => setShowModal(true)}
                style={{ 
                  padding: '12px 24px', 
                  fontSize: '14px',
                  position: 'absolute',
                  right: 0
                }}
              >
                + New Task
              </button>
            )}
          </div>
        </div>

        <div className="content-card" style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '15px', flexWrap: 'wrap' }}>
            <div style={{ flex: 1, minWidth: '200px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
  Filter by Project
</label>

<Select
  options={[
    { value: 'all', label: 'All Projects' },
    ...projects.map(p => ({
      value: p.id,
      label: p.projectName
    }))
  ]}
  value={
    filterProject === 'all'
      ? { value: 'all', label: 'All Projects' }
      : projects
          .map(p => ({ value: p.id, label: p.projectName }))
          .find(o => o.value === parseInt(filterProject))
  }
  onChange={(option) => setFilterProject(option.value)}
  placeholder="Search project..."
  isSearchable
/>
            </div>
            
            <div style={{ flex: 1, minWidth: '200px' }}>
              <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
                Filter by Status
              </label>
              <select 
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
                style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
              >
                <option value="all">All Status</option>
                <option value="PENDING">Pending</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="COMPLETED">Completed</option>
              </select>
            </div>
             <div style={{ flex: 1, minWidth: '200px' }}>
    <label style={{ fontWeight: '600', fontSize: '14px', display: 'block', marginBottom: '5px' }}>
      Filter by Priority
    </label>
    <select 
      value={filterPriority}
      onChange={(e) => setFilterPriority(e.target.value)}
      style={{ width: '100%', padding: '10px', borderRadius: '5px', border: '1px solid #ddd' }}
    >
      <option value="all">All Priorities</option>
      <option value="4" style={{ color: '#dc3545', fontWeight: 'bold' }}>🔴 Critical</option>
      <option value="3" style={{ color: '#fd7e14', fontWeight: 'bold' }}>🟠 High</option>
      <option value="2" style={{ color: '#ffc107' }}>🟡 Medium</option>
      <option value="1" style={{ color: '#28a745' }}>🟢 Low</option>
    </select>
  </div>
            <div style={{ flex: '0 0 auto' }}>
              <button 
                className="btn btn-primary"
                onClick={() => { setFilterProject('all'); setFilterStatus('all');  setFilterPriority('all');}}
                style={{ marginTop: '28px' }}
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>

        <div className="content-card">
          <h2>{isManager() ? `All Tasks (${filteredTasks.length})` : `My Tasks (${filteredTasks.length})`}</h2>
          
          {filteredTasks.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>
              <p style={{ fontSize: '16px', marginBottom: '10px' }}>No tasks found</p>
              <p style={{ fontSize: '14px' }}>
                {isManager() ? 'Click "New Task" to create your first task' : 'No tasks assigned to you'}
              </p>
            </div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Task Name</th>
                  <th>Project</th>
                  <th>Assigned To</th>
                  <th>Priority</th>
                  <th>Due Date</th>
                  <th>Status</th>
                  <th>Attachments</th>
                  {isManager() && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {filteredTasks.map((task) => (
                  <tr key={task.id}>
                    <td>
                      <div style={{ fontWeight: '600' }}>
                        <button
                          onClick={() => handleViewTask(task)}
                          style={{
                            background: 'none',
                            border: 'none',
                            color: '#8B4513',
                            cursor: 'pointer',
                            fontWeight: '600',
                            fontSize: 'inherit',
                            textAlign: 'left',
                            padding: 0,
                            textDecoration: 'underline'
                          }}
                        >
                          {task.taskName}
                        </button>
                      </div>
                      {task.description && (
                        <div style={{ fontSize: '12px', color: '#666', marginTop: '3px' }}>
                          {task.description.substring(0, 50)}{task.description.length > 50 ? '...' : ''}
                        </div>
                      )}
                    </td>
                    <td>{task.project?.projectName || task.projectName || 'N/A'}</td>
                    <td>{task.assignedTo?.fullName || task.assignedTo?.username || task.assignedToName || 'Unassigned'}</td>
                    <td>
                      <span 
                        className="badge"
                        style={{
                          backgroundColor: getPriorityColor(task.priority),
                          color: task.priority === 2 ? '#000' : '#fff'
                        }}
                      >
                        {getPriorityLabel(task.priority)}
                      </span>
                    </td>
                    <td>{task.dueDate || 'Not set'}</td>
                    <td>
                      <select
                        value={task.status}
                        onChange={(e) => handleStatusChange(task.id, e.target.value)}
                        disabled={!isManager()}
                        style={{
                          padding: '5px 10px',
                          borderRadius: '5px',
                          border: '1px solid #ddd',
                          backgroundColor: task.status === 'COMPLETED' ? '#28a745' : 
                                         task.status === 'IN_PROGRESS' ? '#007bff' : '#6c757d',
                          color: 'white',
                          fontSize: '12px',
                          fontWeight: '600',
                          cursor: isManager() ? 'pointer' : 'default',
                          opacity: isManager() ? 1 : 0.7
                        }}
                      >
                        <option value="PENDING">Pending</option>
                        <option value="IN_PROGRESS">In Progress</option>
                        <option value="COMPLETED">Completed</option>
                      </select>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontSize: '12px', color: '#666' }}>
                          📎 {task.attachmentCount || 0}
                        </span>
                        <button
                          onClick={() => handleViewTask(task)}
                          style={{
                            padding: '4px 8px',
                            backgroundColor: '#8B4513',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer',
                            fontSize: '11px'
                          }}
                        >
                          View
                        </button>
                      </div>
                    </td>
                    <td>
                      {isManager() && (
                        <div style={{ display: 'flex', gap: '5px' }}>
                          {/* ✅ ADD: Edit Button */}
                          <button 
                            className="btn btn-primary"
                            style={{ padding: '5px 10px', fontSize: '12px', backgroundColor: '#17a2b8' }}
                            onClick={() => handleEdit(task)}
                          >
                            ✏️ Edit
                          </button>
                          <button 
                            className="btn btn-danger"
                            style={{ padding: '5px 10px', fontSize: '12px' }}
                            onClick={() => handleDelete(task.id)}
                          >
                            🗑️ Delete
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Create/Edit Task Modal */}
      {showModal && isManager() && (
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
            maxWidth: '550px',
            maxHeight: '90vh',
            overflow: 'auto'
          }}>
            {/* ✅ Dynamic title based on edit/create */}
            <h2 style={{ marginBottom: '20px', color: '#8B4513' }}>
              {editingTask ? '✏️ Edit Task' : ' Create New Task'}
            </h2>
            
            <form onSubmit={handleSubmit}>
              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Task Name *</label>
                <input
                  type="text"
                  name="taskName"
                  value={formData.taskName}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div className="form-group" style={{ marginBottom: '15px' }}>
                <label>Description</label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  rows="3"
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

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '15px' }}>
                <div className="form-group">
                <label>Project *</label>
<Select
  options={projects.map(p => ({
    value: p.id,
    label: p.projectName
  }))}
  value={
    projects
      .map(p => ({ value: p.id, label: p.projectName }))
      .find(o => o.value === formData.projectId) || null
  }
  onChange={(option) =>
    setFormData(prev => ({ ...prev, projectId: option?.value || '' }))
  }
  placeholder="Search project..."
  isSearchable
/>
                </div>

                <div className="form-group">
                 <label>Assign To *</label>
<Select
  options={users.map(u => ({
    value: u.id,
    label: u.fullName
  }))}
  value={
    formData.assignedTo  
      ? users
          .map(u => ({ value: u.id, label: u.fullName }))
          .find(o => o.value === parseInt(formData.assignedTo))
      : null
  }
  onChange={(option) =>
    setFormData(prev => ({ ...prev, assignedTo: option?.value || '' }))  
  }
  placeholder="Search user..."
  isSearchable
/>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '15px', marginBottom: '15px' }}>
                <div className="form-group">
                  <label>Due Date</label>
                  <input
                    type="date"
                    name="dueDate"
                    value={formData.dueDate}
                    onChange={handleInputChange}
                  />
                </div>

                <div className="form-group">
                  <label>Priority</label>
                  <select
                    name="priority"
                    value={formData.priority}
                    onChange={handleInputChange}
                    style={{
                      padding: '12px 15px',
                      border: '1px solid #ddd',
                      borderRadius: '5px',
                      fontSize: '14px',
                      width: '100%'
                    }}
                  >
                    <option value={1}>Low</option>
                    <option value={2}>Medium</option>
                    <option value={3}>High</option>
                    <option value={4}>Critical</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '20px' }}>
                {/* ✅ Dynamic button text */}
                <button type="submit" className="btn btn-primary" style={{ flex: 1 }}>
                  {editingTask ? 'Update Task' : 'Create Task'}
                </button>
                <button 
                  type="button" 
                  className="btn"
                  style={{ flex: 1, backgroundColor: '#6c757d', color: 'white' }}
                  onClick={() => {
                    setShowModal(false);
                    setEditingTask(null);
                    setFormData({
                      taskName: '',
                      description: '',
                      projectId: '',
                      assignedTo: '',
                      dueDate: '',
                      priority: 2,
                      status: 'PENDING'
                    });
                  }}
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

export default Tasks;