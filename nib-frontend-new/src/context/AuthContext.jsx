
import React, { createContext, useState, useContext, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const checkSession = async () => {
    try {
     
      const response = await api.get('/api/auth/me');

      if (response.data?.id) {
        console.log('✅ Active session found for:', response.data.username);
        setCurrentUser(response.data);
        setError(null);
      } else {
        setCurrentUser(null);
      }
    } catch (err) {

      if (err.response?.status === 401) {
      
      } else {
       
        console.error('❌ Session check failed:', err.message);
        setError(err.message || 'Session check failed');
      }
      setCurrentUser(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkSession();
  }, []);


  const login = async (username, password) => {
    setError(null);
    try {
      console.log('🔐 Attempting login for:', username);


      const response = await api.post('/api/auth/login', { username, password }, {
        headers: {
    
          'X-User-Id': username
        }
      });

      console.log('📥 Login response:', response.data);

      if (response.data?.success) {
        const user = response.data.user;
        setCurrentUser(user);
        console.log('✅ Login successful - user:', user.username);


        await checkSession();

        return { success: true, user };
      }

      throw new Error(response.data?.error || 'Login failed');
    } catch (err) {
      const message = err.response?.data?.error || err.message || 'Login failed';
      console.error('❌ Login error:', message);
      setError(message);
      return { success: false, error: message };
    }
  };

const logout = async () => {
  try {
    console.log('🔓 Logging out...');
    console.log('Current user:', currentUser);
    console.log('Current user ID:', currentUser?.id);

    const headers = {};
    

    if (currentUser?.id && typeof currentUser.id === 'number') {
      headers['X-User-Id'] = currentUser.id;
      console.log('📝 Sending X-User-Id header:', currentUser.id);
    } else if (currentUser?.username) {
  
      headers['X-User-Id'] = currentUser.username;
      console.log('📝 Sending X-User-Id fallback (username):', currentUser.username);
    }

   
    await api.post('/api/auth/logout', {}, { headers });

    console.log('✅ Backend logout successful');
    
  } catch (err) {
    console.warn('⚠️ Logout request failed:', err.message);
  
  } finally {

    setCurrentUser(null);
    setError(null);
    
    localStorage.removeItem('accessToken');
    localStorage.removeItem('token');
    localStorage.removeItem('jwt');
    localStorage.removeItem('user');
    

    setTimeout(() => {

      console.log('🔄 Redirecting to /login (frontend route)');  
      window.location.href = '/login';  
    }, 100);
  }
};

 
  const refreshUser = async () => {
    await checkSession();
  };

  const value = {
    currentUser,
    loading,
    error,
    login,
    logout,
    refreshUser,
    isAuthenticated: !!currentUser,
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export default AuthContext;
