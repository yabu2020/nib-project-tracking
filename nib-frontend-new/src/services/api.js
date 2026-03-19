
import axios from 'axios';


const api = axios.create({
  baseURL: '',  
  withCredentials: true,  
  headers: {
    'Content-Type': 'application/json',
  },
});


api.interceptors.request.use(
  (config) => {

    console.log('→ API REQUEST:', {
      method: config.method?.toUpperCase(),
      url: config.url,
      fullUrl: window.location.origin + config.url,
      hasXUserId: !!config.headers['X-User-Id'],
      hasAuth: !!config.headers.Authorization
    });
    
    
    const token = localStorage.getItem('accessToken') || 
                  localStorage.getItem('token') ||
                  localStorage.getItem('jwt');
    
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    
    const user = JSON.parse(localStorage.getItem('user') || 'null');
    if (user?.id && !config.headers['X-User-Id']) {
      config.headers['X-User-Id'] = user.id;
    }
    
    return config;
  },
  (error) => {
    console.error('❌ Request interceptor error:', error);
    return Promise.reject(error);
  }
);


api.interceptors.response.use(
  (response) => {
   
    console.log('← API RESPONSE:', {
      status: response.status,
      url: response.config.url,
      hasData: !!response.data
    });
    return response;
  },
  (error) => {
    
    if (error.response?.status === 401 || error.response?.status === 403) {
      console.log('⚠️ Auth error detected:', {
        status: error.response.status,
        url: error.config?.url,
        message: error.response.data?.error || error.response.data?.message
      });
      
      localStorage.removeItem('accessToken');
      localStorage.removeItem('token');
      localStorage.removeItem('jwt');
      localStorage.removeItem('user');
      
   if (!window.location.href.includes('/login')) {
  console.log('🔄 Redirecting to /login due to auth error');
  window.location.href = '/login'; 
}
      
      
      return Promise.resolve({
         
        status: error.response.status,
        isUnauthenticated: true
      });
    }

    console.error('← API ERROR:', {
      status: error.response?.status,
      url: error.config?.url,
      message: error.message,
      data: error.response?.data
    });

    return Promise.reject(error);
  }
);

export default api;