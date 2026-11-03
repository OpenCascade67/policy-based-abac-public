// Manage Domains page

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
  Switch,
  FormControlLabel,
  CircularProgress,
  Alert,
} from '@mui/material';
import { Add, Edit, Delete } from '@mui/icons-material';
import { domainsApi } from '../api/domains';
import type { DomainDto } from '../api/types';
import SlideOutDrawer from '../components/SlideOutDrawer';
import { format } from 'date-fns';

export default function ManageDomains() {
  const [domains, setDomains] = useState<DomainDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingDomain, setEditingDomain] = useState<DomainDto | null>(null);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    status: 'active' as 'active' | 'inactive',
  });

  const loadDomains = useCallback(async () => {
    setLoading(true);
    try {
      setDomains(await domainsApi.getAll());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load domains');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadDomains(); }, [loadDomains]);

  const handleOpenDrawer = (domain?: DomainDto) => {
    if (domain) {
      setEditingDomain(domain);
      setFormData({ name: domain.name, description: domain.description, status: domain.status });
    } else {
      setEditingDomain(null);
      setFormData({ name: '', description: '', status: 'active' });
    }
    setDrawerOpen(true);
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
    setEditingDomain(null);
    setFormData({ name: '', description: '', status: 'active' });
  };

  const handleSave = async () => {
    if (!formData.name.trim()) return;
    setSaving(true);
    try {
      if (editingDomain) {
        await domainsApi.update(editingDomain.id, formData);
      } else {
        await domainsApi.create(formData);
      }
      await loadDomains();
      handleCloseDrawer();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save domain');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (domainId: string) => {
    if (!window.confirm('Are you sure you want to delete this domain? This may affect users and policies.')) return;
    try {
      await domainsApi.delete(domainId);
      await loadDomains();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete domain');
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>
            Manage Domains
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Organize users and policies into separate administrative domains
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenDrawer()}>
          Create Domain
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Domain Name</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Last Modified</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={6} align="center" sx={{ py: 4 }}>
                  <CircularProgress size={32} />
                </TableCell>
              </TableRow>
            ) : domains.map(domain => (
              <TableRow key={domain.id} hover>
                <TableCell>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>{domain.name}</Typography>
                </TableCell>
                <TableCell>{domain.description}</TableCell>
                <TableCell>
                  {domain.status === 'active'
                    ? <Chip label="Active" size="small" color="success" />
                    : <Chip label="Inactive" size="small" color="default" />}
                </TableCell>
                <TableCell>{format(new Date(domain.createdAt), 'PP')}</TableCell>
                <TableCell>{format(new Date(domain.lastModifiedAt), 'PP')}</TableCell>
                <TableCell align="right">
                  <IconButton size="small" onClick={() => handleOpenDrawer(domain)} aria-label="edit domain">
                    <Edit fontSize="small" />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleDelete(domain.id)} aria-label="delete domain">
                    <Delete fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <SlideOutDrawer
        open={drawerOpen}
        onClose={handleCloseDrawer}
        title={editingDomain ? 'Edit Domain' : 'Create New Domain'}
        onSave={handleSave}
        saveDisabled={!formData.name.trim() || saving}
      >
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <TextField
            label="Domain Name"
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
          <FormControlLabel
            control={
              <Switch
                checked={formData.status === 'active'}
                onChange={e => setFormData({ ...formData, status: e.target.checked ? 'active' : 'inactive' })}
              />
            }
            label={formData.status === 'active' ? 'Active' : 'Inactive'}
          />
          <Typography variant="caption" color="text.secondary">
            When a domain is inactive, all domain-related functionality becomes unavailable for users in that domain.
          </Typography>
        </Box>
      </SlideOutDrawer>
    </Box>
  );
}
