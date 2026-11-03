// Shared Rule Library page - Manage reusable rules

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router';
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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Chip,
  Alert,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Divider,
} from '@mui/material';
import {
  Add,
  Edit,
  Delete,
  Visibility,
  Warning,
} from '@mui/icons-material';
import {
  sharedRulesApi
} from '../api/sharedRules';
import { policiesApi } from '../api/policies';
import type { SharedRuleDto, AttributeMatchDto, PolicyTargetDto, PolicyDto } from '../api/types';

// Local type aliases for backward compat within this file
type AttributeMatch = AttributeMatchDto;
type SharedRule = SharedRuleDto;
type PolicyTarget = PolicyTargetDto;

export default function SharedRuleLibrary() {
  const [rules, setRules] = useState<SharedRule[]>([]);
  const [policies, setPolicies] = useState<PolicyDto[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<SharedRule | null>(null);
  const [viewingRule, setViewingRule] = useState<SharedRule | null>(null);
  const [deletingRule, setDeletingRule] = useState<SharedRule | null>(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  // Form state
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  const [effect, setEffect] = useState<'Permit' | 'Deny'>('Permit');
  const [subjects, setSubjects] = useState<AttributeMatch[]>([]);
  const [resources, setResources] = useState<AttributeMatch[]>([]);
  const [actions, setActions] = useState<AttributeMatch[]>([]);
  const [environments, setEnvironments] = useState<AttributeMatch[]>([]);

  useEffect(() => {
    loadRules();
    loadPolicies();
  }, []);

  const loadRules = async () => {
    try {
      const loaded = await sharedRulesApi.getAll();
      setRules(loaded);
    } catch {
      setError('Failed to load shared rules');
    }
  };

  const loadPolicies = async () => {
    try {
      const result = await policiesApi.getAll(0, 1000);
      setPolicies(result.items ?? []);
    } catch {
      // non-critical, policy names will fall back to IDs
    }
  };

  const handleOpenDialog = (rule?: SharedRule) => {
    if (rule) {
      setEditingRule(rule);
      setName(rule.name);
      setDescription(rule.description);
      setCategory(rule.category);
      setEffect(rule.effect);
      setSubjects(rule.target?.subjects || []);
      setResources(rule.target?.resources || []);
      setActions(rule.target?.actions || []);
      setEnvironments(rule.target?.environments || []);
    } else {
      setEditingRule(null);
      setName('');
      setDescription('');
      setCategory('');
      setEffect('Permit');
      setSubjects([]);
      setResources([]);
      setActions([]);
      setEnvironments([]);
    }
    setDialogOpen(true);
    setError('');
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingRule(null);
  };

  const handleAddAttribute = (type: 'subjects' | 'resources' | 'actions' | 'environments') => {
    const newAttr: AttributeMatch = { attributeId: '', matchFunction: 'Equals', value: '' };
    switch (type) {
      case 'subjects':
        setSubjects([...subjects, newAttr]);
        break;
      case 'resources':
        setResources([...resources, newAttr]);
        break;
      case 'actions':
        setActions([...actions, newAttr]);
        break;
      case 'environments':
        setEnvironments([...environments, newAttr]);
        break;
    }
  };

  const handleRemoveAttribute = (
    type: 'subjects' | 'resources' | 'actions' | 'environments',
    index: number
  ) => {
    switch (type) {
      case 'subjects':
        setSubjects(subjects.filter((_, i) => i !== index));
        break;
      case 'resources':
        setResources(resources.filter((_, i) => i !== index));
        break;
      case 'actions':
        setActions(actions.filter((_, i) => i !== index));
        break;
      case 'environments':
        setEnvironments(environments.filter((_, i) => i !== index));
        break;
    }
  };

  const handleAttributeChange = (
    type: 'subjects' | 'resources' | 'actions' | 'environments',
    index: number,
    field: keyof AttributeMatch,
    value: any
  ) => {
    const updateArray = (arr: AttributeMatch[]) => {
      const updated = [...arr];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    };

    switch (type) {
      case 'subjects':
        setSubjects(updateArray(subjects));
        break;
      case 'resources':
        setResources(updateArray(resources));
        break;
      case 'actions':
        setActions(updateArray(actions));
        break;
      case 'environments':
        setEnvironments(updateArray(environments));
        break;
    }
  };

  const handleSave = async () => {
    if (!name.trim()) {
      setError('Rule name is required');
      return;
    }

    if (!category.trim()) {
      setError('Category is required');
      return;
    }

    const target: PolicyTarget = {
      subjects: subjects.length > 0 ? subjects : undefined,
      resources: resources.length > 0 ? resources : undefined,
      actions: actions.length > 0 ? actions : undefined,
      environments: environments.length > 0 ? environments : undefined,
    };

    const hasTarget = Object.values(target).some(Boolean);
    const payload = { name, description, category, effect, target: hasTarget ? target : undefined };

    try {
      if (editingRule) {
        await sharedRulesApi.update(editingRule.id, payload);
      } else {
        await sharedRulesApi.create(payload);
      }
      await loadRules();
      setSuccess(editingRule ? 'Shared rule updated successfully' : 'Shared rule created successfully');
      handleCloseDialog();
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save rule');
    }
  };

  const handleView = (rule: SharedRule) => {
    setViewingRule(rule);
    setViewDialogOpen(true);
  };

  const handleOpenDeleteDialog = (rule: SharedRule) => {
    setDeletingRule(rule);
    setDeleteDialogOpen(true);
  };

  const handleDelete = async () => {
    if (!deletingRule) return;
    try {
      await sharedRulesApi.delete(deletingRule.id);
      await loadRules();
      setDeleteDialogOpen(false);
      setDeletingRule(null);
      setSuccess('Shared rule deleted successfully');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete rule');
    }
  };

  const renderAttributeSection = (
    title: string,
    attrs: AttributeMatch[],
    type: 'subjects' | 'resources' | 'actions' | 'environments'
  ) => (
    <Box sx={{ mb: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
        <Typography variant="subtitle2">{title}</Typography>
        <Button size="small" onClick={() => handleAddAttribute(type)}>
          Add
        </Button>
      </Box>
      {attrs.map((attr, index) => (
        <Paper key={index} sx={{ p: 2, mb: 1 }} variant="outlined">
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'start' }}>
            <TextField
              label="Attribute ID"
              value={attr.attributeId}
              onChange={e => handleAttributeChange(type, index, 'attributeId', e.target.value)}
              size="small"
              sx={{ flex: 1, minWidth: 150 }}
            />
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Match Function</InputLabel>
              <Select
                value={attr.matchFunction}
                label="Match Function"
                onChange={e => handleAttributeChange(type, index, 'matchFunction', e.target.value)}
              >
                <MenuItem value="Equals">Equals</MenuItem>
                <MenuItem value="Contains">Contains</MenuItem>
                <MenuItem value="GreaterThan">Greater Than</MenuItem>
                <MenuItem value="LessThan">Less Than</MenuItem>
                <MenuItem value="Regex">Regex</MenuItem>
              </Select>
            </FormControl>
            <TextField
              label="Value"
              value={attr.value}
              onChange={e => handleAttributeChange(type, index, 'value', e.target.value)}
              size="small"
              sx={{ flex: 1, minWidth: 150 }}
            />
            <IconButton size="small" color="error" onClick={() => handleRemoveAttribute(type, index)}>
              <Delete fontSize="small" />
            </IconButton>
          </Box>
        </Paper>
      ))}
      {attrs.length === 0 && (
        <Typography variant="caption" color="text.secondary">
          No {title.toLowerCase()} defined
        </Typography>
      )}
    </Box>
  );

  const categories = Array.from(new Set(rules.map(r => r.category))).sort();

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <div>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>
            Shared Rule Library
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Create and manage reusable rules that can be shared across multiple policies
          </Typography>
        </div>
        <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenDialog()}>
          Create Shared Rule
        </Button>
      </Box>

      {success && (
        <Alert severity="success" sx={{ mb: 3 }} onClose={() => setSuccess('')}>
          {success}
        </Alert>
      )}

      <Alert severity="info" sx={{ mb: 3 }}>
        <strong>Important:</strong> Shared rules are referenced by policies. When you edit a shared
        rule, the changes will apply to all policies using that rule.
      </Alert>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Effect</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Used By Policies</TableCell>
              <TableCell>Last Modified</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rules.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  <Typography variant="body2" color="text.secondary" sx={{ py: 3 }}>
                    No shared rules found. Create your first shared rule to get started.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              rules.map(rule => {
                const usageCount = rule.usedByPolicies?.length || 0;
                return (
                  <TableRow key={rule.id} hover>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {rule.name}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label={rule.category} size="small" color="primary" variant="outlined" />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={rule.effect}
                        size="small"
                        color={rule.effect === 'Permit' ? 'success' : 'error'}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {rule.description || 'No description'}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip 
                        label={`${usageCount} ${usageCount === 1 ? 'policy' : 'policies'}`}
                        size="small"
                        color={usageCount > 0 ? 'secondary' : 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {new Date(rule.lastModifiedAt).toLocaleDateString()}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <IconButton size="small" onClick={() => handleView(rule)} title="View">
                        <Visibility fontSize="small" />
                      </IconButton>
                      <IconButton size="small" onClick={() => handleOpenDialog(rule)} title="Edit">
                        <Edit fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleOpenDeleteDialog(rule)}
                        title="Delete"
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Create/Edit Dialog */}
      <Dialog open={dialogOpen} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingRule ? 'Edit Shared Rule' : 'Create Shared Rule'}
        </DialogTitle>
        <DialogContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
              {error}
            </Alert>
          )}

          {editingRule && editingRule.usedByPolicies && editingRule.usedByPolicies.length > 0 && (
            <Alert severity="warning" icon={<Warning />} sx={{ mb: 2 }}>
              This rule is used by {editingRule.usedByPolicies.length}{' '}
              {editingRule.usedByPolicies.length === 1 ? 'policy' : 'policies'}. Changes will affect
              all policies using this rule.
            </Alert>
          )}

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            <TextField
              label="Rule Name"
              value={name}
              onChange={e => setName(e.target.value)}
              fullWidth
              required
            />

            <TextField
              label="Description"
              value={description}
              onChange={e => setDescription(e.target.value)}
              fullWidth
              multiline
              rows={2}
            />

            <FormControl fullWidth required>
              <InputLabel>Category</InputLabel>
              <Select value={category} label="Category" onChange={e => setCategory(e.target.value)}>
                {categories.map(cat => (
                  <MenuItem key={cat} value={cat}>
                    {cat}
                  </MenuItem>
                ))}
                <MenuItem value="Access Control">Access Control</MenuItem>
                <MenuItem value="Data Protection">Data Protection</MenuItem>
                <MenuItem value="Compliance">Compliance</MenuItem>
                <MenuItem value="Security">Security</MenuItem>
                <MenuItem value="Resource Access">Resource Access</MenuItem>
                <MenuItem value="Custom">Custom</MenuItem>
              </Select>
            </FormControl>

            <FormControl fullWidth required>
              <InputLabel>Effect</InputLabel>
              <Select
                value={effect}
                label="Effect"
                onChange={e => setEffect(e.target.value as 'Permit' | 'Deny')}
              >
                <MenuItem value="Permit">Permit</MenuItem>
                <MenuItem value="Deny">Deny</MenuItem>
              </Select>
            </FormControl>

            <Divider sx={{ my: 2 }} />
            <Typography variant="h6" gutterBottom>
              Target Constraints
            </Typography>

            {renderAttributeSection('Subjects', subjects, 'subjects')}
            {renderAttributeSection('Resources', resources, 'resources')}
            {renderAttributeSection('Actions', actions, 'actions')}
            {renderAttributeSection('Environments', environments, 'environments')}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSave} variant="contained">
            {editingRule ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* View Dialog */}
      <Dialog open={viewDialogOpen} onClose={() => setViewDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>View Shared Rule</DialogTitle>
        <DialogContent>
          {viewingRule && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Name
                </Typography>
                <Typography variant="body1">{viewingRule.name}</Typography>
              </Box>

              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Category
                </Typography>
                <Chip label={viewingRule.category} size="small" color="primary" />
              </Box>

              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Effect
                </Typography>
                <Chip
                  label={viewingRule.effect}
                  size="small"
                  color={viewingRule.effect === 'Permit' ? 'success' : 'error'}
                />
              </Box>

              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Description
                </Typography>
                <Typography variant="body1">
                  {viewingRule.description || 'No description'}
                </Typography>
              </Box>

              <Box>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  Used By Policies
                </Typography>
                {viewingRule.usedByPolicies && viewingRule.usedByPolicies.length > 0 ? (
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                    {viewingRule.usedByPolicies.map(policyId => {
                      const policy = policies.find(p => p.id === policyId);
                      return (
                        <Chip
                          key={policyId}
                          label={policy?.name || policyId}
                          size="small"
                          clickable
                          onClick={() => navigate(`/policies`)}
                        />
                      );
                    })}
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    Not used by any policies
                  </Typography>
                )}
              </Box>

              <Box>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  Rule Configuration
                </Typography>
                <Paper sx={{ p: 2 }} variant="outlined">
                  <pre style={{ margin: 0, fontSize: '0.875rem', whiteSpace: 'pre-wrap' }}>
                    {JSON.stringify(viewingRule, null, 2)}
                  </pre>
                </Paper>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setViewDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Delete Shared Rule</DialogTitle>
        <DialogContent>
          {deletingRule && deletingRule.usedByPolicies && deletingRule.usedByPolicies.length > 0 ? (
            <Alert severity="error" icon={<Warning />} sx={{ mb: 2 }}>
              This rule is currently used by {deletingRule.usedByPolicies.length}{' '}
              {deletingRule.usedByPolicies.length === 1 ? 'policy' : 'policies'}. Deleting it will
              affect {deletingRule.usedByPolicies.length === 1 ? 'this policy' : 'these policies'}.
            </Alert>
          ) : null}
          <Typography>
            Are you sure you want to delete the shared rule "{deletingRule?.name}"? This action cannot
            be undone.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleDelete} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
