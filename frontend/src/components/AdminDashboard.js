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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel
} from '@mui/material';
import api from '../utils/axios';

function AdminDashboard() {
  const [rooms, setRooms] = useState([]);
  const [openDialog, setOpenDialog] = useState(false);
  const [newRoom, setNewRoom] = useState({
    roomNumber: '',
    type: 'SINGLE',
    price: '',
    description: '',
    available: true
  });

  useEffect(() => {
    fetchRooms();
  }, []);

  const fetchRooms = async () => {
    try {
      const response = await api.get('/api/rooms');
      setRooms(response.data);
    } catch (error) {
      console.error('Error fetching rooms:', error);
      setError('Failed to load rooms. Please try again.');
    }
  };

  const handleAddRoom = async () => {
    try {
      await api.post('/api/rooms', newRoom);
      setOpenDialog(false);
      fetchRooms();
      setNewRoom({
        roomNumber: '',
        type: 'SINGLE',
        price: '',
        description: '',
        available: true
      });
    } catch (error) {
      console.error('Error adding room:', error);
      setError('Failed to add room. Please try again.');
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString();
  };

  return (
    <Container>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '20px 0' }}>
        <Typography variant="h4">
          Admin Dashboard
        </Typography>
        <Button
          variant="contained"
          color="primary"
          onClick={() => setOpenDialog(true)}
        >
          Add New Room
        </Button>
      </div>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)}>
        <DialogTitle>Add New Room</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Room Number"
            fullWidth
            variant="outlined"
            value={newRoom.roomNumber}
            onChange={(e) => setNewRoom({ ...newRoom, roomNumber: e.target.value })}
          />
          <FormControl fullWidth margin="dense">
            <InputLabel>Type</InputLabel>
            <Select
              value={newRoom.type}
              label="Type"
              onChange={(e) => setNewRoom({ ...newRoom, type: e.target.value })}
            >
              <MenuItem value="SINGLE">Single</MenuItem>
              <MenuItem value="DOUBLE">Double</MenuItem>
              <MenuItem value="SUITE">Suite</MenuItem>
            </Select>
          </FormControl>
          <TextField
            margin="dense"
            label="Price"
            type="number"
            fullWidth
            variant="outlined"
            value={newRoom.price}
            onChange={(e) => setNewRoom({ ...newRoom, price: e.target.value })}
          />
          <TextField
            margin="dense"
            label="Description"
            fullWidth
            multiline
            rows={4}
            variant="outlined"
            value={newRoom.description}
            onChange={(e) => setNewRoom({ ...newRoom, description: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleAddRoom} variant="contained" color="primary">
            Add Room
          </Button>
        </DialogActions>
      </Dialog>

      <Typography variant="h5" style={{ margin: '20px 0' }}>
        Room Management
      </Typography>
      <TableContainer component={Paper} style={{ marginBottom: '40px' }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Room Number</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Price</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Description</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rooms.map((room) => (
              <TableRow key={room.id}>
                <TableCell>{room.roomNumber}</TableCell>
                <TableCell>{room.type}</TableCell>
                <TableCell>Rs. {room.price}</TableCell>
                <TableCell>{room.available ? 'Available' : 'Booked'}</TableCell>
                <TableCell>{room.description}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

    </Container>
  );
}

export default AdminDashboard;