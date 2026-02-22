import React, { useState, useEffect } from 'react';
import { Container, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, CircularProgress, Alert, Button } from '@mui/material';
import api from '../utils/axios';

function BookingList() {
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    const fetchBookings = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await api.get('/api/bookings/my-bookings');
        console.log('Bookings response:', response.data);  // Debug log
        setBookings(response.data || []);
      } catch (err) {
        console.error('Error fetching bookings:', err);
        setError('Failed to fetch bookings. ' + (err.response?.data?.message || err.message));
      } finally {
        setLoading(false);
      }
    };

    fetchBookings();
  }, []);

  const formatDate = (dateString) => {
    if (!dateString) return '';
    try {
      // Accept ISO string, array [YYYY,MM,DD], or object {year,month,day}
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
        My Bookings
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
      ) : bookings.length === 0 ? (
        <Alert severity="info">You don't have any bookings yet.</Alert>
      ) : (
        <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Room Number</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Price</TableCell>
              <TableCell>Check-in Date</TableCell>
              <TableCell>Check-out Date</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {bookings.map((booking) => (
              <TableRow key={booking.id}>
                <TableCell>{booking.roomNumber || booking.room?.roomNumber || 'N/A'}</TableCell>
                <TableCell>{booking.roomType || 'N/A'}</TableCell>
                <TableCell>
                  {(() => {
                    const p = (booking.price && booking.price > 0) ? booking.price : null;
                    return p ? `Rs. ${Number(p).toLocaleString()}` : 'N/A';
                  })()}
                </TableCell>
                <TableCell>{formatDate(booking.checkInDate)}</TableCell>
                <TableCell>{formatDate(booking.checkOutDate)}</TableCell>
                <TableCell>{booking.status}</TableCell>
                <TableCell>
                  <Button
                    variant={booking.status === 'CANCELLED' ? 'outlined' : 'contained'}
                    color={booking.status === 'CANCELLED' ? 'secondary' : 'error'}
                    onClick={async () => {
                      try {
                        const newStatus = booking.status === 'CANCELLED' ? 'PENDING' : 'CANCELLED';
                        await api.put(`/api/bookings/${booking.id}`, { status: newStatus });
                        setBookings(prev => prev.map(b => b.id === booking.id ? { ...b, status: newStatus } : b));
                      } catch (err) {
                        console.error('Failed to toggle cancel:', err);
                        setError('Failed to update booking.');
                      }
                    }}
                  >
                    {booking.status === 'CANCELLED' ? 'Undo Cancel' : 'Cancel Booking'}
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

export default BookingList;