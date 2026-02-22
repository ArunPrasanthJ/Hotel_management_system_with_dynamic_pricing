import React, { useState, useEffect } from 'react';
import {
  Container,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Typography,
  CircularProgress,
  Alert,
} from '@mui/material';
import api from '../utils/axios';

function BookingManagement() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchBookings();
  }, []);

  const fetchBookings = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get('/api/bookings');
      setBookings(response.data);
    } catch (error) {
      console.error('Error fetching bookings:', error);
      setError('Failed to load bookings. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (bookingId, status) => {
    try {
      await api.put(`/api/bookings/${bookingId}`, { status });
      fetchBookings();
    } catch (error) {
      console.error('Error updating booking:', error);
      setError('Failed to update booking status.');
    }
  };

  const handleAdminConfirm = async (bookingId, type) => {
    try {
      // bookingId can be an object when called from UI; normalize
      let id = bookingId;
      let bookingObj = null;
      if (typeof bookingId === 'object') {
        bookingObj = bookingId;
        id = bookingId.id;
      }
      // determine current state to toggle
      const payload = {};
      if (type === 'checkin') {
        const current = bookingObj ? bookingObj.checkInConfirmedByAdmin : null;
        payload.checkInConfirmedByAdmin = !(current === true);
      }
      if (type === 'checkout') {
        const current = bookingObj ? bookingObj.checkOutConfirmedByAdmin : null;
        payload.checkOutConfirmedByAdmin = !(current === true);
      }
      await api.put(`/api/bookings/${id}`, payload);
      fetchBookings();
    } catch (error) {
      console.error('Error confirming booking:', error);
      setError('Failed to confirm booking check-in/out.');
    }
  };

  const handleDeleteBooking = async (bookingId) => {
    if (window.confirm('Are you sure you want to delete this booking?')) {
      try {
        await api.delete(`/api/bookings/${bookingId}`);
        fetchBookings();
      } catch (error) {
        console.error('Error deleting booking:', error);
        setError('Failed to delete booking.');
      }
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    try {
      if (typeof dateString === 'string') return new Date(dateString).toLocaleDateString();
      if (Array.isArray(dateString) && dateString.length >= 3) {
        const [y, m, d] = dateString;
        return new Date(`${y}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')}`).toLocaleDateString();
      }
      if (typeof dateString === 'object' && dateString.year) {
        const y = dateString.year; const m = dateString.month; const d = dateString.day;
        return new Date(`${y}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')}`).toLocaleDateString();
      }
      return new Date(dateString).toLocaleDateString();
    } catch (e) {
      return '';
    }
  };

  return (
    <Container>
      <Typography variant="h4" style={{ margin: '20px 0' }}>
        Booking Management
      </Typography>
      
      {error && (
        <Alert severity="error" style={{ marginBottom: '20px' }}>
          {error}
        </Alert>
      )}
      
      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '20px' }}>
          <CircularProgress />
        </div>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Booking ID</TableCell>
                <TableCell>Room Number</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Price</TableCell>
                <TableCell>User</TableCell>
                <TableCell>Check-in</TableCell>
                <TableCell>Check-out</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {bookings.map((booking) => (
                <TableRow key={booking.id}>
                  <TableCell>{booking.id}</TableCell>
                  <TableCell>{booking.roomNumber || booking.room?.roomNumber || 'N/A'}</TableCell>
                  <TableCell>{booking.roomType || 'N/A'}</TableCell>
                  <TableCell>{(() => { const p = (booking.price && booking.price > 0) ? booking.price : null; return p ? `Rs. ${Number(p).toLocaleString()}` : 'N/A'; })()}</TableCell>
                  <TableCell>{booking.username || booking.user?.username || 'N/A'}</TableCell>
                  <TableCell>{formatDate(booking.checkInDate)}</TableCell>
                  <TableCell>{formatDate(booking.checkOutDate)}</TableCell>
                  <TableCell>{booking.status}</TableCell>
                  <TableCell>
                    <Button
                      variant="contained"
                      color="primary"
                      onClick={() => handleUpdateStatus(booking.id, 'CONFIRMED')}
                      style={{ marginRight: '8px' }}
                      disabled={booking.status === 'CONFIRMED'}
                    >
                      Confirm
                    </Button>
                    { /* Admin check-in / check-out confirm buttons */ }
                    <Button
                      variant={booking.checkInConfirmedByAdmin ? 'contained' : 'outlined'}
                      color={booking.checkInConfirmedByAdmin ? 'warning' : 'secondary'}
                      onClick={() => handleAdminConfirm(booking, 'checkin')}
                      style={{ marginRight: '8px' }}
                    >
                      {booking.checkInConfirmedByAdmin ? 'Undo Confirm' : 'Confirm Check-in'}
                    </Button>

                    <Button
                      variant={booking.checkOutConfirmedByAdmin ? 'contained' : 'outlined'}
                      color={booking.checkOutConfirmedByAdmin ? 'warning' : 'secondary'}
                      onClick={() => handleAdminConfirm(booking, 'checkout')}
                      style={{ marginRight: '8px' }}
                    >
                      {booking.checkOutConfirmedByAdmin ? 'Undo Confirm' : 'Confirm Check-out'}
                    </Button>
                    <Button
                      variant="contained"
                      color="error"
                      onClick={() => handleDeleteBooking(booking.id)}
                    >
                      Delete
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Container>
  );
}

export default BookingManagement;