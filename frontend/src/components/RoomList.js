import React, { useState, useEffect } from 'react';
import {
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  CircularProgress,
  Alert
} from '@mui/material';
import api from '../utils/axios';

function RoomList() {
  const [rooms, setRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isAdmin, setIsAdmin] = useState(
    localStorage.getItem('userRole') === 'ROLE_ADMIN'
  );

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      fetchRooms();
    }
  }, []);

  // 🔔 SSE for real-time availability updates
  useEffect(() => {
    const es = new EventSource(
      'http://localhost:8080/api/rooms/availability/stream'
    );

    es.onmessage = (e) => {
      try {
        const dto = JSON.parse(e.data);

        setRooms((prev) =>
          prev.map((r) => {
            if (r.id !== dto.roomId) return r;

            const base =
              r.basePrice && r.basePrice > 0
                ? r.basePrice
                : r.currentPrice && r.currentPrice > 0
                ? r.currentPrice
                : r.price || 0;

            let discount = r.discountPercent;

            if (base > 0) {
              let d = Math.round(((base - dto.price) / base) * 100);
              if (d < 0) d = 0;
              discount = d;
            }

            return {
              ...r,
              available: dto.available,
              status: dto.status,
              currentPrice: dto.price,
              price: dto.price,
              discountPercent: discount
            };
          })
        );
      } catch (err) {
        console.warn('Invalid SSE message', err);
      }
    };

    es.onerror = () => {
      es.close();
    };

    return () => es.close();
  }, []);

  const fetchRooms = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get('/api/rooms');
      setRooms(response.data);
    } catch (error) {
      if (error.response?.status !== 401) {
        setError('Failed to load rooms. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleBookRoom = async (roomId) => {
    try {
      const checkIn = window.prompt(
        'Enter check-in date (DD-MM-YYYY or YYYY-MM-DD):'
      );
      if (!checkIn) return;

      const checkOut = window.prompt(
        'Enter check-out date (DD-MM-YYYY or YYYY-MM-DD):'
      );
      if (!checkOut) return;

      function convertToIso(input) {
        if (!input) return null;
        input = input.trim();

        if (/^\d{4}-\d{2}-\d{2}$/.test(input)) return input;

        const m1 = input.match(/^(\d{1,2})[-\/](\d{1,2})[-\/](\d{4})$/);
        if (m1) {
          const dd = m1[1].padStart(2, '0');
          const mm = m1[2].padStart(2, '0');
          const yyyy = m1[3];
          return `${yyyy}-${mm}-${dd}`;
        }

        return null;
      }

      const inIso = convertToIso(checkIn);
      const outIso = convertToIso(checkOut);

      const inDate = inIso ? new Date(inIso + 'T00:00:00') : null;
      const outDate = outIso ? new Date(outIso + 'T00:00:00') : null;

      if (
        !inIso ||
        !outIso ||
        isNaN(inDate?.getTime()) ||
        isNaN(outDate?.getTime()) ||
        outDate <= inDate
      ) {
        alert('Invalid dates. Ensure check-out is after check-in.');
        return;
      }

      await api.post('/api/bookings', {
        room: { id: roomId },
        checkInDate: inIso,
        checkOutDate: outIso,
        status: 'PENDING'
      });

      alert('Booking request submitted successfully!');
      fetchRooms();
    } catch (error) {
      alert(
        'Failed to book room: ' +
          (error.response?.data?.message || error.message)
      );
    }
  };

  return (
    <Container>
      <Typography variant="h4" style={{ margin: '20px 0' }}>
        Available Rooms
      </Typography>

      {loading && (
        <div style={{ display: 'flex', justifyContent: 'center', margin: 40 }}>
          <CircularProgress />
        </div>
      )}

      {error && (
        <Alert severity="error" style={{ marginBottom: 20 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {rooms.map((room) => {
          const base =
            room.basePrice && room.basePrice > 0
              ? room.basePrice
              : room.currentPrice && room.currentPrice > 0
              ? room.currentPrice
              : room.price && room.price > 0
              ? room.price
              : null;

          const discounted =
            room.price && room.price > 0 ? room.price : null;

          const discount =
            room.discountPercent ??
            (base && discounted && base > discounted
              ? Math.round(((base - discounted) / base) * 100)
              : 0);

          const hasDiscount =
            discounted && base && discounted < base && discount > 0;
          console.log('Room', room.id, 'Base:', base,  'Discounted:', discounted, 'Discount%:', discount , 'Has Discount:', hasDiscount);


          return (
            <Grid item xs={12} sm={6} md={4} key={room.id}>
              <Card>
                <CardContent>
                  <Typography variant="h6">
                    Room {room.roomNumber}
                  </Typography>

                  <Typography variant="body1">
                    Type: {room.type}
                  </Typography>

                  {/* Price - with discount if applicable */}
                  <Typography variant="body1">
                    {hasDiscount ? (
                      <>
                        Price: <span style={{ fontWeight: 'bold', color: '#d32f2f' }}>Rs. {Number(discounted).toLocaleString()}</span> 
                        <span style={{ textDecoration: 'line-through', color: 'gray', marginLeft: '8px' }}>Rs. {Number(base).toLocaleString()}</span>
                        <span style={{ fontWeight: 'bold', color: '#d32f2f', marginLeft: '8px' }}>({discount}% off)</span>
                      </>
                    ) : (
                      `Price: Rs. ${base ? Number(base).toLocaleString() : 'N/A'}`
                    )}
                  </Typography>

                  {/* Status */}
                  <Typography variant="body2" color="textSecondary">
                    Status:{' '}
                    {room.status ??
                      (room.available ? 'AVAILABLE' : 'UNAVAILABLE')}
                    {room.hasPending ? ' — PENDING' : ''}
                  </Typography>

                  <Typography
                    variant="body2"
                    color="textSecondary"
                    style={{ marginTop: 6 }}
                  >
                    {room.description}
                  </Typography>

                  {!isAdmin && (
                    <Button
                      variant="contained"
                      color="primary"
                      disabled={!room.available}
                      onClick={() => handleBookRoom(room.id)}
                      style={{ marginTop: 10 }}
                    >
                      {room.available ? 'Book Now' : 'Not Available'}
                    </Button>
                  )}
                </CardContent>
              </Card>
            </Grid>
          );
        })}
      </Grid>
    </Container>
  );
}

export default RoomList;