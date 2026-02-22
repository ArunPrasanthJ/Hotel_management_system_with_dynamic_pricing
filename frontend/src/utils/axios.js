import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080'
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('userRole');
      window.location.href = '/login';
    }
    if (error.response?.status === 403) {
      // Show a clear message for forbidden requests
      const msg = error.response?.data?.message || 'You do not have permission to perform this action.';
      window.alert('Forbidden: ' + msg);
    }
    return Promise.reject(error);
  }
);

export default api;