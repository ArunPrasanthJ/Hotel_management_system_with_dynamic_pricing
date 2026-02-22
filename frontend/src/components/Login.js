import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Container, Paper, TextField, Button, Typography } from '@mui/material';
import api from '../utils/axios';

function Login() {
  const [credentials, setCredentials] = useState({
    username: '',
    password: ''
  });
  const navigate = useNavigate();

  const handleChange = (e) => {
    setCredentials({
      ...credentials,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      console.log('Attempting login with:', credentials);
      const response = await api.post('/api/auth/login', credentials);
      console.log('Login response:', response.data);
      
      if (response.data.token) {
        // First set the auth data
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('userRole', response.data.role);
        
        // Create and dispatch a custom event with the auth data
        const authEvent = new CustomEvent('auth-change', {
          detail: {
            token: response.data.token,
            role: response.data.role
          }
        });
        window.dispatchEvent(authEvent);
        
        // Short delay to ensure state updates before navigation
        setTimeout(() => {
          navigate('/rooms');
        }, 100);
      } else {
        console.error('No token in response:', response.data);
        alert('Login failed: No authentication token received');
      }
    } catch (error) {
      console.error('Login error:', error);
      if (error.response) {
        console.error('Error response:', error.response.data);
        alert(error.response.data.message || 'Login failed. Please check your credentials.');
      } else if (error.request) {
        console.error('No response received:', error.request);
        alert('Server is not responding. Please try again later.');
      } else {
        console.error('Error setting up request:', error.message);
        alert('An unexpected error occurred. Please try again.');
      }
    }
  };

  return (
    <Container maxWidth="sm">
      <Paper style={{ padding: 20, marginTop: 50 }}>
        <Typography variant="h5" gutterBottom>
          Login
        </Typography>
        <form onSubmit={handleSubmit}>
          <TextField
            fullWidth
            margin="normal"
            label="Username"
            name="username"
            value={credentials.username}
            onChange={handleChange}
            required
          />
          <TextField
            fullWidth
            margin="normal"
            label="Password"
            name="password"
            type="password"
            value={credentials.password}
            onChange={handleChange}
            required
          />
          <Button
            type="submit"
            variant="contained"
            color="primary"
            fullWidth
            style={{ marginTop: 20 }}
          >
            Login
          </Button>
        </form>
      </Paper>
    </Container>
  );
}

export default Login;