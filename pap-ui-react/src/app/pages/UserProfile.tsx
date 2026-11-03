// User Profile page

import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Paper,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
} from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { domainsApi } from '../api/domains';
import { rolesApi } from '../api/roles';
import type { DomainDto, RoleDto } from '../api/types';

export default function UserProfile() {
  const [tabValue, setTabValue] = React.useState(0);
  const { auth } = useAuth();
  const [domains, setDomains] = useState<DomainDto[]>([]);
  const [roles, setRoles] = useState<RoleDto[]>([]);

  useEffect(() => {
    domainsApi.getAll().then(setDomains).catch(() => {});
    rolesApi.getAll().then(setRoles).catch(() => {});
  }, []);

  if (!auth.user) {
    return (
      <Box>
        <Typography variant="h4">Profile</Typography>
        <Typography>No user logged in</Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>
        User Profile
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        View your account information and domain assignments
      </Typography>

      <Paper>
        <Tabs
          value={tabValue}
          onChange={(_, newValue) => setTabValue(newValue)}
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab label="Basic Information" />
          <Tab label="Domain Information" />
        </Tabs>

        <Box sx={{ p: 3 }}>
          {tabValue === 0 && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
              <Box>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Username
                </Typography>
                <Typography variant="h6">{auth.user.username}</Typography>
              </Box>

              {auth.user.email && (
                <Box>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    Email Address
                  </Typography>
                  <Typography variant="h6">{auth.user.email}</Typography>
                </Box>
              )}

              <Box>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Account Type
                </Typography>
                <Chip
                  label={auth.user.userType === 'predefined' ? 'Predefined' : 'Email Login'}
                  color={auth.user.userType === 'predefined' ? 'primary' : 'secondary'}
                />
              </Box>

              <Box>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Account Status
                </Typography>
                <Chip label="Active" color="success" />
              </Box>

              <Box>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Default Domain
                </Typography>
                <Typography variant="body1">
                  {auth.user.defaultDomainId
                    ? domains.find(d => d.id === auth.user!.defaultDomainId)?.name ||
                      'Unknown Domain'
                    : 'Not set'}
                </Typography>
              </Box>
            </Box>
          )}

          {tabValue === 1 && (
            <Box>
              <Typography variant="h6" gutterBottom>
                Your Domain Assignments
              </Typography>
              {auth.user.domainRoles.length > 0 ? (
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Domain</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Assigned Roles</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {auth.user.domainRoles.map(dr => {
                        const domain = domains.find(d => d.id === dr.domainId);
                        const roleNames = dr.roles
                          .map(roleId => roles.find(r => r.id === roleId)?.name)
                          .filter(Boolean);

                        return (
                          <TableRow key={dr.domainId}>
                            <TableCell>
                              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                                {domain?.name || 'Unknown Domain'}
                              </Typography>
                              {domain?.description && (
                                <Typography variant="caption" color="text.secondary">
                                  {domain.description}
                                </Typography>
                              )}
                            </TableCell>
                            <TableCell>
                              {domain?.status === 'active' ? (
                                <Chip label="Active" size="small" color="success" />
                              ) : (
                                <Chip label="Inactive" size="small" color="default" />
                              )}
                            </TableCell>
                            <TableCell>
                              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                                {roleNames.map(roleName => (
                                  <Chip
                                    key={roleName}
                                    label={roleName}
                                    size="small"
                                    color="primary"
                                    variant="outlined"
                                  />
                                ))}
                              </Box>
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  You are not assigned to any domains. Contact your system administrator.
                </Typography>
              )}
            </Box>
          )}
        </Box>
      </Paper>
    </Box>
  );
}
