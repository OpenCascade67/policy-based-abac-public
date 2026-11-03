// Manage User Roles page

import React, { useState, useEffect, useCallback } from 'react';
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
  Alert,
} from '@mui/material';
import { Add, Edit, Delete, Lock } from '@mui/icons-material';
import { rolesApi } from '../api/roles';
import type { RoleDto } from '../api/types';
import SlideOutDrawer from '../components/SlideOutDrawer';

export default function ManageUserRoles() {
  const [roles, setRoles] = useState<RoleDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingRole, setEditingRole] = useState<RoleDto | null>(null);
  const [formData, setFormData] = useState({ name: '', description: '' });

  const loadRoles = useCallback(async () => {
    setLoading(true);
    try { setRoles(await rolesApi.getAll()); }
    catch (err) { setError(err instanceof Error ? err.message : 'Failed to load roles'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadRoles(); }, [loadRoles]);

  const handleOpenDrawer = (role?: RoleDto) => {
    if (role) {
      setEditingRole(role);
      setFormData({ name: role.name, description: role.description });
    } else {
      setEditingRole(null);
      setFormData({ name: '', description: '' });
    }
    setDrawerOpen(true);
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setEditingRole(null);
    setFormData({ name: '', description: '' });
  };

  const handleSave = async () => {
    if (!formData.name.trim()) return;
    setSaving(true);
    try {
      if (editingRole) { await rolesApi.update(editingRole.id, formData); }
      else { await rolesApi.create(formData); }
      await loadRoles();
      handleCloseDrawer();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save role');
    } finally { setSaving(false); }
  };

  const handleDelete = async (roleId: string) => {
    if (!window.confirm('Are you sure you want to delete this role?')) return;
    try { await rolesApi.delete(roleId); await loadRoles(); }
    catch (err) { setError(err instanceof Error ? err.message : 'Failed to delete role'); }
  };

  const getPermissionChips = (permissions: string[]) => {
    const permissionLabels: Record<string, string> = {
      manage_users: 'Manage Users',
      manage_domains: 'Manage Domains',
      manage_roles: 'Manage Roles',
      manage_policies: 'Manage Policies',
      test_policies: 'Test Policies',
      manage_scenarios: 'Manage Scenarios',
    };

    return permissions.map(p => permissionLabels[p] || p);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>
            Manage User Roles
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Define roles and their capabilities within the system
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenDrawer()}>
          Create Role
        </Button>
      </Box>

      <Alert severity="info" sx={{ mb: 3 }}>
        System-defined roles (System Admin, Domain Admin, Policy Author, Policy Viewer) cannot be
        edited or deleted. You can create custom roles with specific permissions.
      </Alert>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Role Name</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Permissions</TableCell>
              <TableCell>Type</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {roles.map(role => (
              <TableRow key={role.id} hover>
                <TableCell>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {role.isSystemRole && <Lock fontSize="small" color="disabled" />}
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {role.name}
                    </Typography>
                  </Box>
                </TableCell>
                <TableCell>{role.description}</TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {getPermissionChips(role.permissions).map(permission => (
                      <Chip key={permission} label={permission} size="small" variant="outlined" />
                    ))}
                  </Box>
                </TableCell>
                <TableCell>
                  {role.isSystemRole ? (
                    <Chip label="System" size="small" color="primary" />
                  ) : (
                    <Chip label="Custom" size="small" color="default" />
                  )}
                </TableCell>
                <TableCell align="right">
                  {!role.isSystemRole && (
                    <>
                      <IconButton
                        size="small"
                        onClick={() => handleOpenDrawer(role)}
                        aria-label="edit role"
                      >
                        <Edit fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        onClick={() => handleDelete(role.id)}
                        aria-label="delete role"
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </>
                  )}
                  {role.isSystemRole && (
                    <Typography variant="caption" color="text.secondary">
                      Protected
                    </Typography>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <SlideOutDrawer
        open={drawerOpen}
        onClose={handleCloseDrawer}
        title={editingRole ? 'Edit Role' : 'Create New Role'}
        onSave={handleSave}
        saveDisabled={!formData.name.trim()}
      >
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <TextField
            label="Role Name"
            value={formData.name}
            onChange={e => setFormData({ ...formData, name: e.target.value })}
            fullWidth
            required
          />
          <TextField
            label="Description"
            value={formData.description}
            onChange={e => setFormData({ ...formData, description: e.target.value })}
            fullWidth
            multiline
            rows={4}
          />
          <Alert severity="info">
            Custom roles can be assigned to users but do not automatically grant system
            permissions. Contact your system administrator to configure permissions for custom
            roles.
          </Alert>
        </Box>
      </SlideOutDrawer>
    </Box>
  );
}
