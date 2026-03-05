import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import '../styles/App.css';

const Register = () => {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    fullName: '',
    email: '',
    role: 'DEVELOPER',
    department: 'IT'
  });
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
 
  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    setError('');

    try {
      const response = await api.post('/api/auth/register', formData);
      setMessage('User registered successfully! Redirecting to login...');
      setTimeout(() => {
        navigate('/login');
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed');
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <div className="login-header">
          <h1>NIB International Bank</h1>
          <p>Create Account</p>
        </div>

        {message && (
          <div style={{ 
            backgroundColor: '#d4edda', 
            color: '#155724', 
            padding: '12px', 
            borderRadius: '5px',
            marginBottom: '15px'
          }}>
            {message}
          </div>
        )}

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label>Username</label>
            <input
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Full Name</label>
            <input
              type="text"
              name="fullName"
              value={formData.fullName}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Role</label>
            <select
              name="role"
              value={formData.role}
              onChange={handleChange}
              style={{
                padding: '12px 15px',
                border: '1px solid #ddd',
                borderRadius: '5px',
                fontSize: '14px'
              }}
            >
              <option value="CEO">CEO</option>
              <option value="CORE_BANKING_MANAGER">Core Banking Manager</option>
              <option value="DIGITAL_BANKING_MANAGER">Digital Banking Manager</option>
              <option value="SENIOR_IT_OFFICER">Senior IT Officer</option>
              <option value="JUNIOR_IT_OFFICER">Junior IT Officer</option>
              <option value="IT_GRADUATE_TRAINEE">IT Graduate Trainee</option>
              <option value="DEVELOPER">Developer</option>
              <option value="PROJECT_MANAGER">Project Manager</option>
            </select>
          </div>

          <div className="form-group">
            <label>Department</label>
            <input
              type="text"
              name="department"
              value={formData.department}
              onChange={handleChange}
              required
            />
          </div>

          <button type="submit" className="login-btn">
            Register
          </button>
        </form>

        <div style={{ marginTop: '15px', textAlign: 'center' }}>
          <a href="/login" style={{ color: '#003366' }}>
            Already have an account? Login
          </a>
        </div>
      </div>
    </div>
  );
};

export default Register;