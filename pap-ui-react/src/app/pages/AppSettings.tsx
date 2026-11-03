// Application Settings page

import React from 'react';
import {
  Box,
  Typography,
  Paper,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Switch,
} from '@mui/material';
import { Brightness4, Brightness7, Palette } from '@mui/icons-material';
import { useThemeSettings } from '../context/ThemeContext';

export default function AppSettings() {
  const { settings, toggleTheme, setColorScheme } = useThemeSettings();

  const colorSchemes = [
    { value: 'blue', label: 'Blue', color: '#1976d2' },
    { value: 'green', label: 'Green', color: '#2e7d32' },
    { value: 'purple', label: 'Purple', color: '#7b1fa2' },
    { value: 'teal', label: 'Teal', color: '#00796b' },
  ];

  return (
    <Box>
      <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>
        Application Settings
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Customize your application appearance and preferences
      </Typography>

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
            {settings.theme === 'light' ? <Brightness7 /> : <Brightness4 />}
            <Typography variant="h6">Theme Mode</Typography>
          </Box>

          <FormControlLabel
            control={<Switch checked={settings.theme === 'dark'} onChange={toggleTheme} />}
            label={settings.theme === 'light' ? 'Light Mode' : 'Dark Mode'}
          />

          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            Toggle between light and dark appearance
          </Typography>
        </Paper>

        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
            <Palette />
            <Typography variant="h6">Color Scheme</Typography>
          </Box>

          <FormControl fullWidth>
            <InputLabel>Primary Color</InputLabel>
            <Select
              value={settings.colorScheme}
              label="Primary Color"
              onChange={e => setColorScheme(e.target.value)}
            >
              {colorSchemes.map(scheme => (
                <MenuItem key={scheme.value} value={scheme.value}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box
                      sx={{
                        width: 20,
                        height: 20,
                        borderRadius: 1,
                        bgcolor: scheme.color,
                      }}
                    />
                    {scheme.label}
                  </Box>
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
            Select the primary color scheme for the application
          </Typography>
        </Paper>

        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            About
          </Typography>
          <Typography variant="body2" color="text.secondary" paragraph>
            ABAC Policy Administration Point (PAP)
          </Typography>
          <Typography variant="body2" color="text.secondary" paragraph>
            Version: 1.0.0
          </Typography>
          <Typography variant="body2" color="text.secondary">
            This application provides enterprise-grade Attribute-Based Access Control (ABAC) policy
            management, compliant with OASIS XACML 3.0 standards. It supports multi-domain
            administration, role-based access control, and comprehensive policy testing capabilities.
          </Typography>
        </Paper>
      </Box>
    </Box>
  );
}
