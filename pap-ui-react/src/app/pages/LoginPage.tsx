// Login page with username/password and OTP email authentication

import React, { useState } from 'react';
import { useNavigate } from 'react-router';
import {
  Box,
  Container,
  Paper,
  Tabs,
  Tab,
  TextField,
  Button,
  Typography,
  Alert,
  InputAdornment,
  IconButton,
} from '@mui/material';
import { Visibility, VisibilityOff, Email, Lock, Send } from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [tabValue, setTabValue] = useState(0);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const navigate = useNavigate();
  const { login, loginWithOtp, sendOtp, isLoading } = useAuth();

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
    setError('');
    setSuccessMessage('');
    setOtpSent(false);
  };

  const handleUsernamePasswordLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login(username, password);
      navigate('/');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Login failed';
      setError(msg);
    }
  };

  const handleSendOTP = async () => {
    try {
      const code = await sendOtp(email);
      setOtpSent(true);
      setError('');
      setSuccessMessage(`OTP code sent to ${email}. For demo purposes, your code is: ${code}`);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to send OTP');
    }
  };

  const handleEmailOTPLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await loginWithOtp(email, otpCode);
      navigate('/');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Invalid OTP code');
    }
  };

  const isUsernamePasswordValid = username.trim() !== '' && password.trim() !== '';
  const isEmailValid = email.trim() !== '' && email.includes('@');
  const isOTPValid = otpCode.trim() !== '';

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: theme =>
          theme.palette.mode === 'light'
            ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
            : 'linear-gradient(135deg, #2c3e50 0%, #34495e 100%)',
      }}
    >
      <Container maxWidth="sm">
        <Paper
          elevation={6}
          sx={{
            p: 4,
            borderRadius: 2,
          }}
        >
          <Typography
            variant="h4"
            component="h1"
            gutterBottom
            align="center"
            sx={{ mb: 1, fontWeight: 600 }}
          >
            ABAC Policy Administration
          </Typography>
          <Typography variant="body2" align="center" color="text.secondary" sx={{ mb: 3 }}>
            Enterprise-grade Access Control Management
          </Typography>

          <Tabs value={tabValue} onChange={handleTabChange} centered sx={{ mb: 3 }}
            aria-label="Login method tabs">
            <Tab label="Username & Password" id="login-tab-0" aria-controls="login-panel-0" />
            <Tab label="Email OTP" id="login-tab-1" aria-controls="login-panel-1" />
          </Tabs>

          {tabValue === 0 && (
            <Box component="form" onSubmit={handleUsernamePasswordLogin}>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Sign in with your predefined username and password. Default credentials: username{' '}
                <strong>admin</strong>, password <strong>admin123</strong>
              </Typography>

              {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {error}
                </Alert>
              )}

              <TextField
                fullWidth
                label="Username"
                value={username}
                onChange={e => setUsername(e.target.value)}
                margin="normal"
                required
                autoComplete="username"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Lock />
                    </InputAdornment>
                  ),
                }}
              />

              <TextField
                fullWidth
                label="Password"
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={e => setPassword(e.target.value)}
                margin="normal"
                required
                autoComplete="current-password"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Lock />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label="toggle password visibility"
                        onClick={() => setShowPassword(!showPassword)}
                        edge="end"
                      >
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />

              <Button
                type="submit"
                fullWidth
                variant="contained"
                size="large"
                disabled={!isUsernamePasswordValid}
                sx={{ mt: 3 }}
              >
                Sign In
              </Button>
            </Box>
          )}

          {tabValue === 1 && (
            <Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Enter your email address to receive a one-time authentication code. For demo
                purposes, the code will be displayed on screen.
              </Typography>

              {error && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {error}
                </Alert>
              )}

              {successMessage && (
                <Alert severity="success" sx={{ mb: 2 }}>
                  {successMessage}
                </Alert>
              )}

              <TextField
                fullWidth
                label="Email Address"
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                margin="normal"
                required
                disabled={otpSent}
                autoComplete="email"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Email />
                    </InputAdornment>
                  ),
                }}
              />

              {!otpSent ? (
                <Button
                  fullWidth
                  variant="contained"
                  size="large"
                  disabled={!isEmailValid}
                  onClick={handleSendOTP}
                  startIcon={<Send />}
                  sx={{ mt: 3 }}
                >
                  Send OTP Code
                </Button>
              ) : (
                <Box component="form" onSubmit={handleEmailOTPLogin}>
                  <TextField
                    fullWidth
                    label="Enter OTP Code"
                    value={otpCode}
                    onChange={e => setOtpCode(e.target.value)}
                    margin="normal"
                    required
                    inputProps={{
                      maxLength: 6,
                      pattern: '[0-9]{6}',
                    }}
                  />

                  <Button
                    type="submit"
                    fullWidth
                    variant="contained"
                    size="large"
                    disabled={!isOTPValid}
                    sx={{ mt: 2 }}
                  >
                    Verify & Sign In
                  </Button>

                  <Button
                    fullWidth
                    variant="text"
                    size="small"
                    onClick={() => {
                      setOtpSent(false);
                      setOtpCode('');
                      setSuccessMessage('');
                    }}
                    sx={{ mt: 1 }}
                  >
                    Use Different Email
                  </Button>
                </Box>
              )}
            </Box>
          )}
        </Paper>
      </Container>
    </Box>
  );
}