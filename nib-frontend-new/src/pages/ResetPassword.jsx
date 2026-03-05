import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import '../styles/App.css';

const ResetPassword = () => {
  const { currentUser, logout } = useAuth();
  const navigate = useNavigate();
  
  const [formData, setFormData] = useState({
    newPassword: '',
    confirmPassword: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    if (formData.newPassword !== formData.confirmPassword) {
      setError('Passwords do not match!');
      setLoading(false);
      return;
    }

    if (formData.newPassword.length < 6) {
      setError('Password must be at least 6 characters');
      setLoading(false);
      return;
    }

    try {
      await api.put(`/api/users/${currentUser?.id}/reset-password`, {
        newPassword: formData.newPassword,
        confirmPassword: formData.confirmPassword
      });
      
      setSuccess('✅ Password reset successful! Redirecting...');
      
      setTimeout(() => {
        navigate('/dashboard');
      }, 1500);
      
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to reset password');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #8B4513 0%, #A0522D 50%, #CD853F 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px'
    }}>
      <div style={{
        backgroundColor: 'white',
        borderRadius: '15px',
        boxShadow: '0 10px 40px rgba(0,0,0,0.3)',
        maxWidth: '500px',
        width: '100%',
        padding: '40px',
        animation: 'slideIn 0.5s ease-out'
      }}>
        {/* Header with Logo/Icon */}
        <div style={{ textAlign: 'center', marginBottom: '30px' }}>
          <h1 style={{
            color: '#8B4513',
            fontSize: '28px',
            fontWeight: 'bold',
            marginBottom: '5px'
          }}>
            NIB International Bank
          </h1>
          <h2 style={{
            color: '#A0522D',
            fontSize: '18px',
            marginBottom: '5px'
          }}>
            ንብ ኢንተርናሽናል ባንክ
          </h2>
          <p style={{
            color: '#666',
            fontSize: '14px'
          }}>
            IT Project Tracking System
          </p>
        </div>

        {/* Reset Password Section */}
        <div style={{
          backgroundColor: '#FFF8DC',
          border: '2px solid #D2691E',
          borderRadius: '10px',
          padding: '25px',
          marginBottom: '25px'
        }}>
          <h2 style={{
            textAlign: 'center',
            color: '#8B4513',
            fontSize: '24px',
            marginBottom: '10px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '10px'
          }}>
            🔑 Reset Your Password
          </h2>
          
          <p style={{
            textAlign: 'center',
            color: '#555',
            fontSize: '15px',
            lineHeight: '1.2',
            marginBottom: '0'
          }}>
            Welcome, <strong style={{ color: '#8B4513' }}>{currentUser?.fullName}</strong>!<br/>
            Please set a new password to continue.
          </p>
        </div>

        {/* Error Message */}
        {error && (
          <div style={{
            backgroundColor: '#FEE2E2',
            border: '2px solid #DC3545',
            color: '#DC3545',
            padding: '12px 15px',
            borderRadius: '8px',
            marginBottom: '20px',
            fontSize: '14px',
            display: 'flex',
            alignItems: 'center',
            gap: '10px'
          }}>
            <span style={{ fontSize: '18px' }}>⚠️</span>
            <span>{error}</span>
          </div>
        )}

        {/* Success Message */}
        {success && (
          <div style={{
            backgroundColor: '#D4EDDA',
            border: '2px solid #28A745',
            color: '#28A745',
            padding: '12px 15px',
            borderRadius: '8px',
            marginBottom: '20px',
            fontSize: '14px',
            display: 'flex',
            alignItems: 'center',
            gap: '10px'
          }}>
            <span style={{ fontSize: '18px' }}>✅</span>
            <span>{success}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '20px' }}>
            <label style={{
              display: 'block',
              color: '#4a3728',
              fontWeight: '600',
              marginBottom: '8px',
              fontSize: '14px'
            }}>
              New Password <span style={{ color: '#DC3545' }}>*</span>
            </label>
            <input
              type="password"
              name="newPassword"
              value={formData.newPassword}
              onChange={handleChange}
              placeholder="Enter new password (min 6 characters)"
              required
              minLength="6"
              style={{
                width: '100%',
                padding: '12px 15px',
                border: '2px solid #D2691E',
                borderRadius: '8px',
                fontSize: '14px',
                transition: 'all 0.3s',
                boxSizing: 'border-box'
              }}
              onFocus={(e) => {
                e.target.style.borderColor = '#8B4513';
                e.target.style.boxShadow = '0 0 0 3px rgba(139, 69, 19, 0.1)';
              }}
              onBlur={(e) => {
                e.target.style.borderColor = '#D2691E';
                e.target.style.boxShadow = 'none';
              }}
            />
          </div>

          <div style={{ marginBottom: '25px' }}>
            <label style={{
              display: 'block',
              color: '#4a3728',
              fontWeight: '600',
              marginBottom: '8px',
              fontSize: '14px'
            }}>
              Confirm Password <span style={{ color: '#DC3545' }}>*</span>
            </label>
            <input
              type="password"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Confirm new password"
              required
              minLength="6"
              style={{
                width: '100%',
                padding: '12px 15px',
                border: '2px solid #D2691E',
                borderRadius: '8px',
                fontSize: '14px',
                transition: 'all 0.3s',
                boxSizing: 'border-box'
              }}
              onFocus={(e) => {
                e.target.style.borderColor = '#8B4513';
                e.target.style.boxShadow = '0 0 0 3px rgba(139, 69, 19, 0.1)';
              }}
              onBlur={(e) => {
                e.target.style.borderColor = '#D2691E';
                e.target.style.boxShadow = 'none';
              }}
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '14px',
              backgroundColor: loading ? '#A0522D' : '#8B4513',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              fontSize: '16px',
              fontWeight: '600',
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'all 0.3s',
              boxShadow: '0 4px 6px rgba(139, 69, 19, 0.3)',
              opacity: loading ? 0.7 : 1
            }}
            onMouseEnter={(e) => {
              if (!loading) {
                e.target.style.backgroundColor = '#A0522D';
                e.target.style.transform = 'translateY(-2px)';
                e.target.style.boxShadow = '0 6px 12px rgba(139, 69, 19, 0.4)';
              }
            }}
            onMouseLeave={(e) => {
              if (!loading) {
                e.target.style.backgroundColor = '#8B4513';
                e.target.style.transform = 'translateY(0)';
                e.target.style.boxShadow = '0 4px 6px rgba(139, 69, 19, 0.3)';
              }
            }}
          >
            {loading ? '⏳ Resetting...' : '🔐 Reset Password & Continue'}
          </button>
        </form>

        {/* Logout Link */}
        <div style={{ textAlign: 'center', marginTop: '20px' }}>
          <button
            onClick={handleLogout}
            style={{
              background: 'none',
              border: 'none',
              color: '#8B4513',
              cursor: 'pointer',
              fontSize: '14px',
              textDecoration: 'underline',
              fontWeight: '500',
              transition: 'all 0.3s'
            }}
            onMouseEnter={(e) => {
              e.target.style.color = '#A0522D';
            }}
            onMouseLeave={(e) => {
              e.target.style.color = '#8B4513';
            }}
          >
            ← Logout instead
          </button>
        </div>
      </div>

      {/* Add CSS animation */}
      <style>{`
        @keyframes slideIn {
          from {
            opacity: 0;
            transform: translateY(-30px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
      `}</style>
    </div>
  );
};

export default ResetPassword;