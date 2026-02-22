import React from 'react';
import { BrowserRouter as Router, Route, Routes, Navigate } from 'react-router-dom';
import Login from './components/Login';
import Register from './components/Register';
import RoomList from './components/RoomList';
import BookingList from './components/BookingList';
import AdminDashboard from './components/AdminDashboard';
import BookingManagement from './components/BookingManagement';
import Navbar from './components/Navbar';

function App() {
  const [isAuthenticated, setIsAuthenticated] = React.useState(!!localStorage.getItem('token'));
  const [isAdmin, setIsAdmin] = React.useState(localStorage.getItem('userRole') === 'ROLE_ADMIN');

  React.useEffect(() => {
    const checkAuth = (event) => {
      if (event?.type === 'auth-change') {
        // If we have event details, use them directly
        setIsAuthenticated(!!event.detail?.token);
        setIsAdmin(event.detail?.role === 'ROLE_ADMIN');
      } else {
        // Otherwise check localStorage
        setIsAuthenticated(!!localStorage.getItem('token'));
        setIsAdmin(localStorage.getItem('userRole') === 'ROLE_ADMIN');
      }
    };

    // Check initial auth state
    checkAuth();

    // Listen for auth changes
    window.addEventListener('auth-change', checkAuth);
    window.addEventListener('storage', checkAuth);
    
    return () => {
      window.removeEventListener('auth-change', checkAuth);
      window.removeEventListener('storage', checkAuth);
    };
  }, []);

  return (
    <Router>
      <div>
        <Navbar onAuthChange={() => {
          setIsAuthenticated(!!localStorage.getItem('token'));
          setIsAdmin(localStorage.getItem('userRole') === 'ROLE_ADMIN');
        }} />
        <Routes>
          <Route path="/login" element={!isAuthenticated ? <Login /> : <Navigate to="/rooms" />} />
          <Route path="/register" element={!isAuthenticated ? <Register /> : <Navigate to="/rooms" />} />
          <Route 
            path="/rooms" 
            element={isAuthenticated ? <RoomList /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/bookings" 
            element={isAuthenticated ? <BookingList /> : <Navigate to="/login" />} 
          />
          <Route 
            path="/admin" 
            element={isAdmin ? <AdminDashboard /> : <Navigate to="/rooms" />} 
          />
          <Route 
            path="/admin/bookings" 
            element={isAdmin ? <BookingManagement /> : <Navigate to="/rooms" />} 
          />
          <Route path="/" element={<Navigate to={isAuthenticated ? "/rooms" : "/login"} />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;