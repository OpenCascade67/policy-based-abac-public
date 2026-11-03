import React, { useState, useEffect } from 'react';
import { Box, Typography, Grid, Card, CardContent, CardHeader, Chip, CircularProgress } from '@mui/material';
import {
  Security,
  Domain as DomainIcon,
  People,
  Policy as PolicyIcon,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { domainsApi } from '../api/domains';
import { usersApi } from '../api/users';
import { rolesApi } from '../api/roles';
import { policiesApi } from '../api/policies';

export default function Dashboard() {
  const { auth } = useAuth();
  const [stats, setStats] = useState([
    { title: 'Active Domains', value: 0, total: 0, icon: <DomainIcon sx={{ fontSize: 40 }} />, color: '#1976d2' },
    { title: 'Active Users', value: 0, total: 0, icon: <People sx={{ fontSize: 40 }} />, color: '#2e7d32' },
    { title: 'User Roles', value: 0, total: 0, icon: <Security sx={{ fontSize: 40 }} />, color: '#7b1fa2' },
    { title: 'Policies', value: 0, total: 0, icon: <PolicyIcon sx={{ fontSize: 40 }} />, color: '#d32f2f' },
  ]);
  const [allRoles, setAllRoles] = useState<RoleDto[]>([]);
  const [loading, setLoading] = useState(true);

  const currentDomainRoles = auth.user?.domainRoles?.find(dr => dr.domainId === auth.currentDomainId)?.roles ?? [];

  useEffect(() => {
    Promise.all([
      domainsApi.getAll(),
      usersApi.getAll(0, 1000),
      rolesApi.getAll(),
      policiesApi.getAll(0, 1000),
    ]).then(([domains, usersResult, roles, policiesResult]) => {
      setAllRoles(roles);
      const activeDomains = domains.filter((d: { status: string }) => d.status === 'active').length;
      const activeUsers = usersResult.items.filter((u: { status: string }) => u.status === 'active').length;
      setStats([
        { title: 'Active Domains', value: activeDomains, total: domains.length, icon: <DomainIcon sx={{ fontSize: 40 }} />, color: '#1976d2' },
        { title: 'Active Users', value: activeUsers, total: usersResult.totalCount, icon: <People sx={{ fontSize: 40 }} />, color: '#2e7d32' },
        { title: 'User Roles', value: roles.length, total: roles.length, icon: <Security sx={{ fontSize: 40 }} />, color: '#7b1fa2' },
        { title: 'Policies', value: policiesResult.totalCount, total: policiesResult.totalCount, icon: <PolicyIcon sx={{ fontSize: 40 }} />, color: '#d32f2f' },
      ]);
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

  return (
    <Box>
      <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>
        Dashboard
      </Typography>
      <Typography variant="body1" color="text.secondary" gutterBottom sx={{ mb: 4 }}>
        Welcome to the ABAC Policy Administration Point
      </Typography>

      <Grid container spacing={3} sx={{ mb: 4 }}>
        {stats.map(stat => (
          <Grid item xs={12} sm={6} md={3} key={stat.title}>
            <Card elevation={2}>
              <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                  <Box>
                    <Typography variant="body2" color="text.secondary" gutterBottom>
                      {stat.title}
                    </Typography>
                    <Typography variant="h4" sx={{ fontWeight: 600 }}>
                      {stat.value}
                    </Typography>
                    {stat.value !== stat.total && (
                      <Typography variant="caption" color="text.secondary">
                        of {stat.total} total
                      </Typography>
                    )}
                  </Box>
                  <Box sx={{ color: stat.color, opacity: 0.7 }}>{stat.icon}</Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card elevation={2}>
            <CardHeader title="Current User Information" />
            <CardContent>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Username
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 600 }}>
                    {auth.user?.username}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Account Type
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 600, textTransform: 'capitalize' }}>
                    {auth.user?.userType}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    Your Roles in Current Domain
                  </Typography>
                  {currentDomainRoles.length > 0 ? (
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                      {currentDomainRoles.map((roleId: string) => (
                        <Chip
                          key={roleId}
                          label={allRoles.find(r => r.id === roleId)?.name || roleId}
                          color="primary"
                        />
                      ))}
                    </Box>
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      No roles assigned
                    </Typography>
                  )}
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card elevation={2}>
            <CardHeader title="Quick Start Guide" />
            <CardContent>
              <Typography variant="body2" paragraph>
                This Policy Administration Point (PAP) allows you to manage Attribute-Based Access
                Control (ABAC) policies for your organization.
              </Typography>
              <Typography variant="body2" component="div">
                <strong>Key Features:</strong>
                <ul style={{ marginTop: 8, paddingLeft: 24 }}>
                  <li>Manage users, roles, and domains</li>
                  <li>Create and edit access control policies</li>
                  <li>Test policies with ABAC scenarios</li>
                  <li>XACML 3.0 compliant policy structure</li>
                </ul>
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}
