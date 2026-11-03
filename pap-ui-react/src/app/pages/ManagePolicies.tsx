// Manage Access Control Policies page

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
  TextField,
  InputAdornment,
  TablePagination,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  CircularProgress,
  Alert,
} from '@mui/material';
import { Add, Edit, Delete, Search, Download, MoreVert } from '@mui/icons-material';
import { useNavigate } from 'react-router';
import { policiesApi } from '../api/policies';
import type { PolicyDto } from '../api/types';
import { format } from 'date-fns';

export default function ManagePolicies() {
  const [policies, setPolicies] = useState<PolicyDto[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [exportMenuAnchor, setExportMenuAnchor] = useState<null | HTMLElement>(null);
  const [selectedPolicy, setSelectedPolicy] = useState<PolicyDto | null>(null);
  const navigate = useNavigate();

  const loadPolicies = useCallback(async () => {
    setLoading(true);
    try {
      const result = await policiesApi.getAll(page, rowsPerPage, searchQuery || undefined);
      setPolicies(result.items);
      setTotalCount(result.totalCount);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load policies');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, searchQuery]);

  useEffect(() => { loadPolicies(); }, [loadPolicies]);

  const handleDelete = async (policyId: string) => {
    if (!window.confirm('Are you sure you want to delete this policy?')) return;
    try {
      await policiesApi.delete(policyId);
      await loadPolicies();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete policy');
    }
  };

  const handleOpenExportMenu = (event: React.MouseEvent<HTMLElement>, policy: PolicyDto) => {
    setExportMenuAnchor(event.currentTarget);
    setSelectedPolicy(policy);
  };

  const handleCloseExportMenu = () => {
    setExportMenuAnchor(null);
    setSelectedPolicy(null);
  };

  const handleExport = async (format: 'json' | 'xml') => {
    if (!selectedPolicy) return;
    try {
      const content = format === 'json'
        ? await policiesApi.exportJson(selectedPolicy.id)
        : await policiesApi.exportXml(selectedPolicy.id);

      const blob = new Blob([content], { type: format === 'json' ? 'application/json' : 'application/xml' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${selectedPolicy.name.replace(/\s+/g, '_')}_policy.${format}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to export policy');
    } finally {
      handleCloseExportMenu();
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>
            Manage Access Control Policies
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Create and manage XACML-compliant access control policies
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<Add />} onClick={() => navigate('/policies/create')}>
          Create Policy
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      <Paper sx={{ p: 2, mb: 2 }}>
        <TextField
          fullWidth
          placeholder="Search policies by name..."
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
              <TableCell>Policy Name</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Rules</TableCell>
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
            ) : policies.map(policy => (
              <TableRow key={policy.id} hover>
                <TableCell>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>{policy.name}</Typography>
                </TableCell>
                <TableCell>{policy.description}</TableCell>
                <TableCell>{policy.policyDefinition.rules.length}</TableCell>
                <TableCell>{format(new Date(policy.createdAt), 'PP')}</TableCell>
                <TableCell>{format(new Date(policy.lastModifiedAt), 'PP')}</TableCell>
                <TableCell align="right">
                  <IconButton size="small" onClick={() => navigate(`/policies/edit/${policy.id}`)} aria-label="edit policy">
                    <Edit fontSize="small" />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleDelete(policy.id)} aria-label="delete policy">
                    <Delete fontSize="small" />
                  </IconButton>
                  <IconButton size="small" onClick={e => handleOpenExportMenu(e, policy)} aria-label="export policy">
                    <MoreVert fontSize="small" />
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

      {!loading && policies.length === 0 && (
        <Box sx={{ textAlign: 'center', py: 4 }}>
          <Typography variant="body1" color="text.secondary">
            {searchQuery ? 'No policies match your search' : 'No policies created yet'}
          </Typography>
        </Box>
      )}

      <Menu anchorEl={exportMenuAnchor} open={Boolean(exportMenuAnchor)} onClose={handleCloseExportMenu}>
        <MenuItem onClick={() => handleExport('json')}>
          <ListItemIcon><Download /></ListItemIcon>
          <ListItemText>Export as JSON</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleExport('xml')}>
          <ListItemIcon><Download /></ListItemIcon>
          <ListItemText>Export as XML</ListItemText>
        </MenuItem>
      </Menu>
    </Box>
  );
}