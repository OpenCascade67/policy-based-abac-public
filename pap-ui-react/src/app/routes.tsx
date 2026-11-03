// Router configuration

import { createBrowserRouter, Navigate, Outlet } from 'react-router';
import MainLayout from './components/MainLayout';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import ManageUserRoles from './pages/ManageUserRoles';
import ManageDomains from './pages/ManageDomains';
import ManageUsers from './pages/ManageUsers';
import ManagePolicies from './pages/ManagePolicies';
import PolicyCreateEdit from './pages/PolicyCreateEdit';
import TestPolicies from './pages/TestPolicies';
import UserProfile from './pages/UserProfile';
import AppSettings from './pages/AppSettings';
import SharedRuleLibrary from './pages/SharedRuleLibrary';
import NotFound from './pages/NotFound';
import { useAuth } from './context/AuthContext';
import React, { useEffect, useState } from 'react';
import { CircularProgress, Box } from '@mui/material';

// Protected route wrapper
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { auth, isLoading } = useAuth();
  
  if (isLoading) return <Box display="flex" justifyContent="center" mt={10}><CircularProgress /></Box>;
  if (!auth.isAuthenticated) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

// Public route wrapper (redirect to dashboard if already authenticated)
function PublicRoute({ children }: { children: React.ReactNode }) {
  const { auth, isLoading } = useAuth();
  
  if (isLoading) return <Box display="flex" justifyContent="center" mt={10}><CircularProgress /></Box>;
  if (auth.isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

export const router = createBrowserRouter([
  {
    path: '/login',
    element: (
      <PublicRoute>
        <LoginPage />
      </PublicRoute>
    ),
  },
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <MainLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: <Dashboard />,
      },
      {
        path: 'roles',
        element: <ManageUserRoles />,
      },
      {
        path: 'domains',
        element: <ManageDomains />,
      },
      {
        path: 'users',
        element: <ManageUsers />,
      },
      {
        path: 'policies',
        element: <ManagePolicies />,
      },
      {
        path: 'policies/create',
        element: <PolicyCreateEdit />,
      },
      {
        path: 'policies/edit/:policyId',
        element: <PolicyCreateEdit />,
      },
      {
        path: 'test-policies',
        element: <TestPolicies />,
      },
      {
        path: 'profile',
        element: <UserProfile />,
      },
      {
        path: 'settings',
        element: <AppSettings />,
      },
      {
        path: 'rule-templates',
        element: <SharedRuleLibrary />,
      },
    ],
  },
  {
    path: '*',
    element: <NotFound />,
  },
]);