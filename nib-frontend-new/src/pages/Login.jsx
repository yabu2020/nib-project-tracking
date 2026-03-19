import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import '../styles/App.css';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
const handleSubmit = async (e) => {
  e.preventDefault();
  console.log('Form submit triggered!');

  setError('');
  setLoading(true);

  try {
    console.log('🔐 Attempting login for:', username);

    const result = await login(username, password);

    console.log('🔍 Login result:', result);
    console.log('🔍 user.mustResetPassword:', result.user?.mustResetPassword);

    if (result.success) {
      console.log(' Login successful, user:', result.user);

      await new Promise(resolve => setTimeout(resolve, 300));

      if (result.user?.mustResetPassword === true) {
        console.log('🔐 Redirecting to reset-password');
        navigate('/reset-password', { 
          state: { user: result.user },
          replace: true 
        });
      } else {
        console.log(' Redirecting to dashboard');
        navigate('/dashboard', { replace: true });
      }
    } else {
      console.error(' Login failed:', result.error);
      
    
      setError(' Invalid username or password');
    }
  } catch (err) {
    console.error('💥 Login error:', err);
    
    
    if (err.response) {
      const status = err.response.status;
      
      
      if (status === 401 || status === 403) {
        setError(' Invalid username or password');
      } else if (status === 404) {
        
        setError(' Invalid username or password');
      } else {
        
        setError(' Invalid username or password');
      }
    } else if (err.request) {
   
      setError('⚠️ Network error. Please check your connection and try again.');
    } else {
      setError(' Invalid username or password');
    }
  } finally {
    setLoading(false);
  }
};
  const handleButtonClick = () => {
    console.log('Button clicked directly');
  };

  return (
    <div className="login-container" style={{
      background: 'linear-gradient(135deg, #fff8dc 0%, #f5f5dc 100%)',
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '20px'
    }}>
      <div className="login-box" style={{
        backgroundColor: 'white',
        borderRadius: '12px',
        boxShadow: '0 8px 24px rgba(139, 69, 19, 0.15)',
        padding: '40px',
        width: '100%',
        maxWidth: '450px',
        border: '1px solid rgba(139, 69, 19, 0.1)',
        borderTop: '5px solid #8B4513'
      }}>
        <div className="login-header" style={{ textAlign: 'center', marginBottom: '30px' }}>
  {/* Logo */}
  <div style={{ 
    marginBottom: '20px',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center'
  }}>
    <img 
      src="/nibicon.jpeg" 
      alt="NIB Logo" 
      style={{
        width: '80px',
        height: '80px',
        objectFit: 'contain',
        borderRadius: '10px'
      }}
    />
  </div>
  
  <h1 style={{
    color: '#8B4513',
    fontSize: '28px',
    fontWeight: '700',
    marginBottom: '5px',
    textShadow: '1px 1px 2px rgba(0,0,0,0.1)'
  }}>
    NIB International Bank
  </h1>
  <h4 style={{
    color: '#A0522D',
    fontSize: '18px',
    fontWeight: '600',
    marginBottom: '8px'
  }}>
    ንብ ኢንተርናሽናል ባንክ
  </h4>
  <p style={{
    color: '#666',
    fontSize: '14px',
    margin: 0
  }}>
    IT Project Tracking System
  </p>
</div>

        <h2 style={{ 
          textAlign: 'center', 
          marginBottom: '25px', 
          color: '#8B4513',
          fontSize: '24px',
          fontWeight: '600'
        }}>
          Please sign in
        </h2>

        {error && (
          <div className="error-message" style={{
            backgroundColor: '#fff3cd',
            border: '2px solid #ffc107',
            borderLeft: '4px solid #dc3545',
            color: '#856404',
            padding: '12px 16px',
            borderRadius: '6px',
            marginBottom: '20px',
            fontSize: '14px',
            fontWeight: '500'
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label htmlFor="username" style={{
              display: 'block',
              marginBottom: '8px',
              color: '#8B4513',
              fontWeight: '600',
              fontSize: '14px'
            }}>
              Username
            </label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value.trim())}
              placeholder="Enter your username"
              required
              style={{
                width: '100%',
                padding: '12px 15px',
                border: '2px solid #D2691E',
                borderRadius: '6px',
                fontSize: '14px',
                transition: 'all 0.3s ease',
                boxSizing: 'border-box'
              }}
              onFocus={(e) => {
                e.target.style.borderColor = '#FFD700';
                e.target.style.boxShadow = '0 0 0 3px rgba(255, 215, 0, 0.2)';
              }}
              onBlur={(e) => {
                e.target.style.borderColor = '#D2691E';
                e.target.style.boxShadow = 'none';
              }}
            />
          </div>

          <div className="form-group" style={{ marginBottom: '25px' }}>
            <label htmlFor="password" style={{
              display: 'block',
              marginBottom: '8px',
              color: '#8B4513',
              fontWeight: '600',
              fontSize: '14px'
            }}>
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              required
              style={{
                width: '100%',
                padding: '12px 15px',
                border: '2px solid #D2691E',
                borderRadius: '6px',
                fontSize: '14px',
                transition: 'all 0.3s ease',
                boxSizing: 'border-box'
              }}
              onFocus={(e) => {
                e.target.style.borderColor = '#FFD700';
                e.target.style.boxShadow = '0 0 0 3px rgba(255, 215, 0, 0.2)';
              }}
              onBlur={(e) => {
                e.target.style.borderColor = '#D2691E';
                e.target.style.boxShadow = 'none';
              }}
            />
          </div>

          <button 
            type="submit" 
            className="login-btn" 
            disabled={loading}
            onClick={handleButtonClick}
            style={{
              width: '100%',
              padding: '14px',
              background: 'linear-gradient(135deg, #8B4513 0%, #A0522D 100%)',
              color: '#FFD700',
              border: '2px solid #8B4513',
              borderRadius: '6px',
              fontSize: '16px',
              fontWeight: '700',
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'all 0.3s ease',
              boxShadow: '0 4px 8px rgba(139, 69, 19, 0.3)',
              opacity: loading ? 0.7 : 1
            }}
            onMouseEnter={(e) => {
              if (!loading) {
                e.target.style.background = 'linear-gradient(135deg, #A0522D 0%, #8B4513 100%)';
                e.target.style.transform = 'translateY(-2px)';
                e.target.style.boxShadow = '0 6px 12px rgba(139, 69, 19, 0.4)';
              }
            }}
            onMouseLeave={(e) => {
              if (!loading) {
                e.target.style.background = 'linear-gradient(135deg, #8B4513 0%, #A0522D 100%)';
                e.target.style.transform = 'translateY(0)';
                e.target.style.boxShadow = '0 4px 8px rgba(139, 69, 19, 0.3)';
              }
            }}
          >
            {loading ? (
              <span className="loading-text" style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <span className="spinner" style={{
                  width: '18px',
                  height: '18px',
                  border: '3px solid #FFD700',
                  borderTop: '3px solid transparent',
                  borderRadius: '50%',
                  animation: 'spin 1s linear infinite',
                  marginRight: '10px'
                }}></span>
                Signing in...
              </span>
            ) : 'Sign in'}
          </button>
        </form>

        {/* <div className="demo-credentials" style={{
          marginTop: '25px',
          padding: '15px',
          backgroundColor: '#fff8dc',
          borderRadius: '6px',
          border: '1px solid #D2691E',
          textAlign: 'center'
        }}>
          <p style={{ margin: '0 0 5px 0', color: '#8B4513', fontWeight: '600' }}>
            <strong>Demo credentials:</strong>
          </p>
          <p style={{ margin: 0, color: '#666', fontSize: '13px' }}>
            Username: <strong style={{ color: '#8B4513' }}>admin</strong> | 
            Password: <strong style={{ color: '#8B4513' }}>admin123</strong>
          </p>
        </div> */}
      </div>

      {/* Styles */}
      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }

        .login-container {
          min-height: 100vh;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .login-box {
          animation: slideIn 0.4s ease-out;
        }

        @keyframes slideIn {
          from {
            opacity: 0;
            transform: translateY(-20px);
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

export default Login;