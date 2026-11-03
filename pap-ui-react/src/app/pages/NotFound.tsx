// 404 Not Found page

import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { Home } from '@mui/icons-material';
import { useNavigate } from 'react-router';

export default function NotFound() {
  const navigate = useNavigate();

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        textAlign: 'center',
        p: 3,
      }}
    >
      <Typography variant="h1" sx={{ fontSize: '6rem', fontWeight: 700, mb: 2 }}>
        404
      </Typography>
      <Typography variant="h4" gutterBottom>
        Page Not Found
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        The page you are looking for does not exist.
      </Typography>
      <Button variant="contained" startIcon={<Home />} onClick={() => navigate('/')}>
        Go to Dashboard
      </Button>
    </Box>
  );
}
