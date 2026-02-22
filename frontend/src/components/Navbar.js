import React from 'react';
import { AppBar, Toolbar, Typography, Button } from '@mui/material';
import { Link, useNavigate } from 'react-router-dom';

function Navbar({ onAuthChange }) {
  const navigate = useNavigate();
  const [isAuthenticated, setIsAuthenticated] = React.useState(!!localStorage.getItem('token'));
  const [isAdmin, setIsAdmin] = React.useState(localStorage.getItem('userRole') === 'ROLE_ADMIN');

  React.useEffect(() => {
    const handleAuthChange = (event) => {
      if (event?.type === 'auth-change') {
        setIsAuthenticated(!!event.detail?.token);
        setIsAdmin(event.detail?.role === 'ROLE_ADMIN');
      } else {
        setIsAuthenticated(!!localStorage.getItem('token'));
        setIsAdmin(localStorage.getItem('userRole') === 'ROLE_ADMIN');
      }
    };

    // Initial check
    handleAuthChange();

    // Listen for auth changes
    window.addEventListener('auth-change', handleAuthChange);
    window.addEventListener('storage', handleAuthChange);
    
    return () => {
      window.removeEventListener('auth-change', handleAuthChange);
      window.removeEventListener('storage', handleAuthChange);
    };
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userRole');
    
    // Dispatch auth change event
    const authEvent = new CustomEvent('auth-change', {
      detail: {
        token: null,
        role: null
      }
    });
    window.dispatchEvent(authEvent);
    
    setIsAuthenticated(false);
    setIsAdmin(false);
    if (onAuthChange) onAuthChange();
    navigate('/login');
  };

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography variant="h6" style={{ flexGrow: 1 }}>
          Hotel Management System
        </Typography>
        {isAuthenticated ? (
          <>
            <Button color="inherit" component={Link} to="/rooms">
              Rooms
            </Button>
            {!isAdmin && (
              <Button color="inherit" component={Link} to="/bookings">
                My Bookings
              </Button>
            )}
            {isAdmin && (
              <>
                <Button color="inherit" component={Link} to="/admin">
                  Room Management
                </Button>
                <Button color="inherit" component={Link} to="/admin/bookings">
                  Booking Management
                </Button>
              </>
            )}
            <Button color="inherit" onClick={handleLogout}>
              Logout
            </Button>
          </>
        ) : (
          <>
            <Button color="inherit" component={Link} to="/login">
              Login
            </Button>
            <Button color="inherit" component={Link} to="/register">
              Register
            </Button>
          </>
        )}
      </Toolbar>
    </AppBar>
  );
}

export default Navbar;