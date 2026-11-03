// Manage Users page

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  Box,
  Typography,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Chip,
  TextField,
  Switch,
  FormControlLabel,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  TablePagination,
  InputAdornment,
  Alert,
  Autocomplete,
  CircularProgress,
} from '@mui/material';
import { Add, Edit, Delete, Search } from '@mui/icons-material';
import { usersApi } from '../api/users';
import { domainsApi } from '../api/domains';
import { rolesApi } from '../api/roles';
import type { UserDto, DomainDto, RoleDto } from '../api/types';
import SlideOutDrawer from '../components/SlideOutDrawer';
import { format } from 'date-fns';

export default function ManageUsers() {
  const [users, setUsers] = useState<UserDto[]>([]);
  const [domains, setDomains] = useState<DomainDto[]>([]);
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserDto | null>(null);
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    status: 'active' as 'active' | 'inactive',
    domainRoles: [] as Array<{ domainId: string; roles: string[] }>,
  });

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      const result = await usersApi.getAll(page, rowsPerPage, searchQuery || undefined);
      setUsers(result.items);
      setTotalCount(result.totalCount);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load users');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, searchQuery]);

  useEffect(() => { loadUsers(); }, [loadUsers]);

  useEffect(() => {
    domainsApi.getAll().then(setDomains).catch(() => {});
    rolesApi.getAll().then(setRoles).catch(() => {});
  }, []);

  const handleOpenDrawer = (user?: UserDto) => {
    if (user) {
      setEditingUser(user);
      setFormData({
        username: user.username,
        password: '',
        status: user.status,
        domainRoles: user.domainRoles.map(dr => ({ domainId: dr.domainId, roles: dr.roles })),
      });
    } else {
      setEditingUser(null);
      setFormData({ username: '', password: '', status: 'active', domainRoles: [] });
    }
    setDrawerOpen(true);
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setEditingUser(null);
  };

  const handleSave = async () => {
    if (!formData.username.trim()) return;
    if (!editingUser && !formData.password.trim()) return;
    setSaving(true);
    try {
      if (editingUser) {
        await usersApi.update(editingUser.id, {
          password: formData.password || undefined,
          status: formData.status,
          domainRoles: formData.domainRoles,
        });
      } else {
        await usersApi.create({
          username: formData.username,
          password: formData.password,
          status: formData.status,
          domainRoles: formData.domainRoles,
        });
      }
      await loadUsers();
      handleCloseDrawer();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save user');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (userId: string) => {
    if (!window.confirm('Are you sure you want to delete this user?')) return;
    try {
      await usersApi.delete(userId);
      await loadUsers();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete user');
    }
  };

  const handleAddDomainRole = () => {
    if (domains.length > 0) {
      setFormData({
        ...formData,
        domainRoles: [...formData.domainRoles, { domainId: domains[0].id, roles: [] }],
      });
    }
  };

  const handleRemoveDomainRole = (index: number) => {
    setFormData({ ...formData, domainRoles: formData.domainRoles.filter((_, i) => i !== index) });
  };

  const handleDomainRoleChange = (index: number, field: 'domainId' | 'roles', value: string | string[]) => {
    const newDomainRoles = [...formData.domainRoles];
    if (field === 'domainId') {
      newDomainRoles[index].domainId = value as string;
    } else {
      newDomainRoles[index].roles = value as string[];
    }
    setFormData({ ...formData, domainRoles: newDomainRoles });
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>Manage Users</Typography>
          <Typography variant="body2" color="text.secondary">
            Create and manage user accounts and their domain assignments
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenDrawer()}>
          Create User
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      <Paper sx={{ p: 2, mb: 2 }}>
        <TextField
          fullWidth
          placeholder="Search users by username..."
          value={searchQuery}
          onChange={e => { setSearchQuery(e.target.value); setPage(0); }}
          InputProps={{
            startAdornment: <InputAdornment position="start"><Search /></InputAdornment>,
          }}
        />
      </Paper>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Username</TableCell>
              <TableCell>User Type</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Domains</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Last Modified</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                  <CircularProgress size={32} />
                </TableCell>
              </TableRow>
            ) : users.map(user => (
              <TableRow key={user.id} hover>
                <TableCell>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>{user.username}</Typography>
                  {user.email && <Typography variant="caption" color="text.secondary">{user.email}</Typography>}
                </TableCell>
                <TableCell>
                  <Chip
                    label={user.userType === 'local' ? 'Local' : 'Email Login'}
                    size="small"
                    color={user.userType === 'local' ? 'primary' : 'secondary'}
                  />
                </TableCell>
                <TableCell>
                  {user.status === 'active'
                    ? <Chip label="Active" size="small" color="success" />
                    : <Chip label="Inactive" size="small" color="default" />}
                </TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {user.domainRoles.map(dr => {
                      const domain = domains.find(d => d.id === dr.domainId);
                      return domain ? <Chip key={dr.domainId} label={domain.name} size="small" variant="outlined" /> : null;
                    })}
                    {user.domainRoles.length === 0 && (
                      <Typography variant="caption" color="text.secondary">No domains</Typography>
                    )}
                  </Box>
                </TableCell>
                <TableCell>{format(new Date(user.createdAt), 'PP')}</TableCell>
                <TableCell>{format(new Date(user.lastModifiedAt), 'PP')}</TableCell>
                <TableCell align="right">
                  <IconButton size="small" onClick={() => handleOpenDrawer(user)} aria-label="edit user">
                    <Edit fontSize="small" />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleDelete(user.id)} aria-label="delete user">
                    <Delete fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={totalCount}
          page={page}
          onPageChange={(_, newPage) => setPage(newPage)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={e => { setRowsPerPage(parseInt(e.target.value, 10)); setPage(0); }}
        />
      </TableContainer>

      <SlideOutDrawer
        open={drawerOpen}
        onClose={handleCloseDrawer}
        title={editingUser ? 'Edit User' : 'Create New User'}
        onSave={handleSave}
        saveDisabled={!formData.username.trim() || (!editingUser && !formData.password.trim()) || saving}
      >
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <TextField
            label="Username"
            value={formData.username}
            onChange={e => setFormData({ ...formData, username: e.target.value })}
            fullWidth required disabled={!!editingUser}
          />

          {(!editingUser || editingUser.userType === 'local') && (
            <TextField
              label="Password"
              type="password"
              value={formData.password}
              onChange={e => setFormData({ ...formData, password: e.target.value })}
              fullWidth
              required={!editingUser}
              helperText={editingUser ? 'Leave blank to keep current password' : ''}
            />
          )}

          {editingUser && editingUser.userType === 'email' && (
            <Alert severity="info">Email login users cannot have their passwords changed through the admin interface.</Alert>
          )}

          <FormControlLabel
            control={
              <Switch
                checked={formData.status === 'active'}
                onChange={e => setFormData({ ...formData, status: e.target.checked ? 'active' : 'inactive' })}
              />
            }
            label={formData.status === 'active' ? 'Active' : 'Inactive'}
          />

          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="subtitle2">Domain &amp; Role Assignments</Typography>
              <Button size="small" onClick={handleAddDomainRole}>Add Domain</Button>
            </Box>

            {formData.domainRoles.map((dr, index) => (
              <Paper key={index} sx={{ p: 2, mb: 2 }}>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <FormControl fullWidth>
                    <InputLabel>Domain</InputLabel>
                    <Select
                      value={dr.domainId}
                      label="Domain"
                      onChange={e => handleDomainRoleChange(index, 'domainId', e.target.value)}
                    >
                      {domains.map(domain => (
                        <MenuItem key={domain.id} value={domain.id}>{domain.name}</MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <Autocomplete
                    multiple
                    options={roles}
                    getOptionLabel={option => option.name}
                    value={roles.filter(r => dr.roles.includes(r.id))}
                    onChange={(_, newValue) => handleDomainRoleChange(index, 'roles', newValue.map(v => v.id))}
                    renderInput={params => <TextField {...params} label="Roles" />}
                  />

                  <Button size="small" color="error" onClick={() => handleRemoveDomainRole(index)}>
                    Remove Domain Assignment
                  </Button>
                </Box>
              </Paper>
            ))}

            {formData.domainRoles.length === 0 && (
              <Alert severity="warning">
                User has no domain assignments. They will not be able to access most features.
              </Alert>
            )}
          </Box>
        </Box>
      </SlideOutDrawer>
    </Box>
  );
}
