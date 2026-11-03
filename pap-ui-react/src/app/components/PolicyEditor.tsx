// Visual Policy Editor for XACML-aligned policies with shared rule support

import React, { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Button,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  IconButton,
  Divider,
  Chip,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
} from '@mui/material';
import {
  Add,
  Delete,
  ExpandMore,
  ExpandLess,
  BookmarkAdd,
  Lock,
  Save as SaveIcon,
} from '@mui/icons-material';
import type { PolicyRuleDto, PolicyTargetDto, AttributeMatchDto, PolicyRuleReferenceDto, SharedRuleDto } from '../api/types';
import { sharedRulesApi } from '../api/sharedRules';

// map DTOs to aliases for less renaming
type PolicyRule = PolicyRuleDto;
type PolicyTarget = PolicyTargetDto;
type AttributeMatch = AttributeMatchDto;
type PolicyRuleReference = PolicyRuleReferenceDto;
type SharedRule = SharedRuleDto;

interface PolicyEditorProps {
  rules: PolicyRule[];
  onChange: (rules: PolicyRule[]) => void;
  ruleReferences?: PolicyRuleReference[];
  onRuleReferencesChange?: (refs: PolicyRuleReference[]) => void;
  policyId?: string;
  sharedRules: SharedRule[];
  onSharedRulesChange: () => void;
}

export default function PolicyEditor({
  rules,
  onChange,
  ruleReferences,
  onRuleReferencesChange,
  policyId,
  sharedRules,
  onSharedRulesChange,
}: PolicyEditorProps) {
  const [expandedRules, setExpandedRules] = useState<Set<string>>(new Set());
  const [openSharedRuleDialog, setOpenSharedRuleDialog] = useState(false);
  const [openSaveAsSharedDialog, setOpenSaveAsSharedDialog] = useState(false);
  const [selectedSharedRule, setSelectedSharedRule] = useState<SharedRule | null>(null);
  const [ruleToSave, setRuleToSave] = useState<PolicyRule | null>(null);
  const [sharedRuleName, setSharedRuleName] = useState('');
  const [sharedRuleCategory, setSharedRuleCategory] = useState('');
  const [sharedRuleDescription, setSharedRuleDescription] = useState('');
  const [savingSharedRule, setSavingSharedRule] = useState(false);

  // Use ruleReferences if provided, otherwise fall back to legacy rules array
  const useReferences = ruleReferences !== undefined && onRuleReferencesChange !== undefined;

  const handleAddInlineRule = () => {
    const newRule: PolicyRule = {
      ruleId: `rule-${Date.now()}`,
      effect: 'Permit',
      description: '',
      target: {
        subjects: [],
        resources: [],
        actions: [],
        environments: [],
      },
    };

    if (useReferences && onRuleReferencesChange) {
      const newRef: PolicyRuleReference = {
        type: 'inline',
        inlineRule: newRule,
      };
      onRuleReferencesChange([...(ruleReferences || []), newRef]);
    } else {
      onChange([...rules, newRule]);
    }
    setExpandedRules(new Set([...expandedRules, newRule.ruleId]));
  };

  const handleAddSharedRule = () => {
    setOpenSharedRuleDialog(true);
  };

  const handleSharedRuleSelect = (rule: SharedRule) => {
    setSelectedSharedRule(rule);
  };

  const handleSharedRuleApply = () => {
    if (!selectedSharedRule) return;

    if (useReferences && onRuleReferencesChange) {
      const newRef: PolicyRuleReference = {
        type: 'shared',
        sharedRuleId: selectedSharedRule.id,
      };
      onRuleReferencesChange([...(ruleReferences || []), newRef]);
    }

    setOpenSharedRuleDialog(false);
    setSelectedSharedRule(null);
  };

  const handleRemoveRule = (index: number) => {
    if (useReferences && onRuleReferencesChange && ruleReferences) {
      onRuleReferencesChange(ruleReferences.filter((_, i) => i !== index));
    } else {
      onChange(rules.filter((_, i) => i !== index));
    }
  };

  const handleRuleChange = (index: number, field: keyof PolicyRule, value: any) => {
    if (useReferences && onRuleReferencesChange && ruleReferences) {
      const ref = ruleReferences[index];
      if (ref.type === 'inline' && ref.inlineRule) {
        const updatedRefs = [...ruleReferences];
        updatedRefs[index] = {
          ...ref,
          inlineRule: { ...ref.inlineRule, [field]: value },
        };
        onRuleReferencesChange(updatedRefs);
      }
    } else {
      onChange(rules.map((r, i) => (i === index ? { ...r, [field]: value } : r)));
    }
  };

  const handleAddAttribute = (
    index: number,
    targetType: 'subjects' | 'resources' | 'actions' | 'environments'
  ) => {
    const rule = getCurrentRule(index);
    if (!rule || !rule.target) {
        if (!rule) return;
        rule.target = { subjects: [], resources: [], actions: [], environments: [] };
    }

    const newAttribute: AttributeMatch = {
      attributeId: '',
      matchFunction: 'Equals',
      value: '',
    };

    const updatedTarget: PolicyTarget = {
      ...rule.target,
      [targetType]: [...(rule.target[targetType] || []), newAttribute],
    };

    handleRuleChange(index, 'target', updatedTarget);
  };

  const handleRemoveAttribute = (
    index: number,
    targetType: 'subjects' | 'resources' | 'actions' | 'environments',
    attrIndex: number
  ) => {
    const rule = getCurrentRule(index);
    if (!rule || !rule.target) return;

    const updatedTarget: PolicyTarget = {
      ...rule.target,
      [targetType]: rule.target[targetType]?.filter((_, i) => i !== attrIndex) || [],
    };

    handleRuleChange(index, 'target', updatedTarget);
  };

  const handleAttributeChange = (
    index: number,
    targetType: 'subjects' | 'resources' | 'actions' | 'environments',
    attrIndex: number,
    field: keyof AttributeMatch,
    value: any
  ) => {
    const rule = getCurrentRule(index);
    if (!rule || !rule.target) return;

    const updatedAttributes = [...(rule.target[targetType] || [])];
    updatedAttributes[attrIndex] = { ...updatedAttributes[attrIndex], [field]: value };

    const updatedTarget: PolicyTarget = {
      ...rule.target,
      [targetType]: updatedAttributes,
    };

    handleRuleChange(index, 'target', updatedTarget);
  };

  const getCurrentRule = (index: number): PolicyRule | null => {
    if (useReferences && ruleReferences) {
      const ref = ruleReferences[index];
      if (ref.type === 'shared' && ref.sharedRuleId) {
        const sharedRule = sharedRules.find(r => r.id === ref.sharedRuleId);
        if (sharedRule) {
          return {
            ruleId: sharedRule.id,
            effect: sharedRule.effect,
            description: sharedRule.description,
            target: sharedRule.target,
          };
        }
      } else if (ref.type === 'inline' && ref.inlineRule) {
        return ref.inlineRule;
      }
      return null;
    }
    return rules[index] || null;
  };

  const isSharedRule = (index: number): boolean => {
    if (useReferences && ruleReferences) {
      return ruleReferences[index]?.type === 'shared';
    }
    return false;
  };

  const toggleRuleExpanded = (ruleId: string) => {
    const newExpanded = new Set(expandedRules);
    if (newExpanded.has(ruleId)) {
      newExpanded.delete(ruleId);
    } else {
      newExpanded.add(ruleId);
    }
    setExpandedRules(newExpanded);
  };

  const handleSaveAsShared = (index: number) => {
    const rule = getCurrentRule(index);
    if (rule && !isSharedRule(index)) {
      setRuleToSave(rule);
      setSharedRuleName('');
      setSharedRuleCategory('');
      setSharedRuleDescription(rule.description || '');
      setOpenSaveAsSharedDialog(true);
    }
  };

  const handleSaveAsSharedConfirm = async () => {
    if (!ruleToSave || !sharedRuleName.trim() || !sharedRuleCategory.trim()) {
      return;
    }

    setSavingSharedRule(true);
    try {
        const hasTarget = ruleToSave.target && Object.values(ruleToSave.target).some(Boolean);
        const payload = {
            name: sharedRuleName,
            description: sharedRuleDescription,
            category: sharedRuleCategory,
            effect: ruleToSave.effect,
            target: hasTarget ? ruleToSave.target : undefined
        };
        const newSharedRule = await sharedRulesApi.create(payload);

        // Replace the inline rule with a shared rule reference
        if (useReferences && onRuleReferencesChange && ruleReferences) {
          const ruleIndex = ruleReferences.findIndex(
            ref => ref.type === 'inline' && ref.inlineRule?.ruleId === ruleToSave.ruleId
          );
          if (ruleIndex !== -1) {
            const updatedRefs = [...ruleReferences];
            updatedRefs[ruleIndex] = {
              type: 'shared',
              sharedRuleId: newSharedRule.id,
            };
            onRuleReferencesChange(updatedRefs);
          }
        }
        
        onSharedRulesChange();
        setOpenSaveAsSharedDialog(false);
        setRuleToSave(null);
    } catch {
        // error handled implicitly
    } finally {
        setSavingSharedRule(false);
    }
  };

  const renderAttributeSection = (
    index: number,
    rule: PolicyRule,
    targetType: 'subjects' | 'resources' | 'actions' | 'environments',
    label: string,
    readonly: boolean
  ) => {
    const attributes = rule.target?.[targetType] || [];

    return (
      <Box sx={{ mb: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="subtitle2">{label}</Typography>
          {!readonly && (
            <Button size="small" onClick={() => handleAddAttribute(index, targetType)}>
              Add Attribute
            </Button>
          )}
        </Box>

        {attributes.map((attr, attrIndex) => (
          <Paper key={attrIndex} sx={{ p: 2, mb: 1 }} variant="outlined">
            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'start' }}>
              <TextField
                label="Attribute ID"
                value={attr.attributeId}
                onChange={e =>
                  handleAttributeChange(index, targetType, attrIndex, 'attributeId', e.target.value)
                }
                size="small"
                sx={{ flex: 1, minWidth: 150 }}
                disabled={readonly}
              />
              <FormControl size="small" sx={{ minWidth: 150 }} disabled={readonly}>
                <InputLabel>Match Function</InputLabel>
                <Select
                  value={attr.matchFunction}
                  label="Match Function"
                  onChange={e =>
                    handleAttributeChange(index, targetType, attrIndex, 'matchFunction', e.target.value)
                  }
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
                value={attr.value as string}
                onChange={e =>
                  handleAttributeChange(index, targetType, attrIndex, 'value', e.target.value)
                }
                size="small"
                sx={{ flex: 1, minWidth: 150 }}
                disabled={readonly}
              />
              {!readonly && (
                <IconButton
                  size="small"
                  color="error"
                  onClick={() => handleRemoveAttribute(index, targetType, attrIndex)}
                >
                  <Delete fontSize="small" />
                </IconButton>
              )}
            </Box>
          </Paper>
        ))}

        {attributes.length === 0 && (
          <Typography variant="caption" color="text.secondary">
            No {label.toLowerCase()} defined
          </Typography>
        )}
      </Box>
    );
  };

  const displayRules = useReferences && ruleReferences ? ruleReferences : rules;
  const categories = Array.from(new Set(sharedRules.map(r => r.category))).sort();

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">Policy Rules</Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button variant="outlined" startIcon={<BookmarkAdd />} onClick={handleAddSharedRule}>
            Add Shared Rule
          </Button>
          <Button variant="outlined" startIcon={<Add />} onClick={handleAddInlineRule}>
            Add Inline Rule
          </Button>
        </Box>
      </Box>

      <Alert severity="info" sx={{ mb: 2 }}>
        Rules are evaluated based on the combining algorithm. <strong>Shared rules</strong> (
        <Lock fontSize="small" sx={{ verticalAlign: 'middle' }} />) are read-only and changes to them
        affect all policies. <strong>Inline rules</strong> can be edited and saved as shared rules for
        reuse.
      </Alert>

      {displayRules.map((item, index) => {
        const rule = getCurrentRule(index);
        if (!rule) return null;

        const isExpanded = expandedRules.has(rule.ruleId);
        const readonly = isSharedRule(index);
        const sharedRuleName = readonly
          ? sharedRules.find(r => r.id === rule.ruleId)?.name
          : null;

        return (
          <Paper key={`${rule.ruleId}-${index}`} sx={{ mb: 2, p: 2 }}>
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                mb: isExpanded ? 2 : 0,
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                  {readonly ? sharedRuleName : `Rule ${index + 1}`}
                </Typography>
                <Chip label={rule.effect} size="small" color={rule.effect === 'Permit' ? 'success' : 'error'} />
                {readonly && (
                  <Chip icon={<Lock fontSize="small" />} label="Shared" size="small" color="secondary" />
                )}
              </Box>
              <Box>
                {!readonly && (
                  <IconButton
                    size="small"
                    onClick={() => handleSaveAsShared(index)}
                    title="Save as Shared Rule"
                  >
                    <SaveIcon fontSize="small" />
                  </IconButton>
                )}
                <IconButton size="small" onClick={() => toggleRuleExpanded(rule.ruleId)}>
                  {isExpanded ? <ExpandLess /> : <ExpandMore />}
                </IconButton>
                <IconButton size="small" color="error" onClick={() => handleRemoveRule(index)}>
                  <Delete fontSize="small" />
                </IconButton>
              </Box>
            </Box>

            {isExpanded && (
              <>
                <Divider sx={{ mb: 2 }} />

                {readonly && (
                  <Alert severity="info" icon={<Lock />} sx={{ mb: 2 }}>
                    This is a shared rule and cannot be edited here. Changes must be made in the Shared
                    Rule Library and will affect all policies using this rule.
                  </Alert>
                )}

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <FormControl fullWidth disabled={readonly}>
                    <InputLabel>Effect</InputLabel>
                    <Select
                      value={rule.effect}
                      label="Effect"
                      onChange={e => handleRuleChange(index, 'effect', e.target.value)}
                    >
                      <MenuItem value="Permit">Permit</MenuItem>
                      <MenuItem value="Deny">Deny</MenuItem>
                    </Select>
                  </FormControl>

                  <TextField
                    label="Description"
                    value={rule.description || ''}
                    onChange={e => handleRuleChange(index, 'description', e.target.value)}
                    fullWidth
                    multiline
                    rows={2}
                    disabled={readonly}
                  />

                  <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
                    Target Constraints
                  </Typography>

                  {renderAttributeSection(index, rule, 'subjects', 'Subjects', readonly)}
                  {renderAttributeSection(index, rule, 'resources', 'Resources', readonly)}
                  {renderAttributeSection(index, rule, 'actions', 'Actions', readonly)}
                  {renderAttributeSection(index, rule, 'environments', 'Environments', readonly)}
                </Box>
              </>
            )}
          </Paper>
        );
      })}

      {displayRules.length === 0 && (
        <Alert severity="warning">
          No rules defined. Add at least one rule to create a functional policy.
        </Alert>
      )}

      {/* Add Shared Rule Dialog */}
      <Dialog open={openSharedRuleDialog} onClose={() => setOpenSharedRuleDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Select a Shared Rule</DialogTitle>
        <DialogContent>
          {sharedRules.length === 0 ? (
            <Alert severity="info" sx={{ mt: 2 }}>
              No shared rules available. Create shared rules in the Shared Rule Library first.
            </Alert>
          ) : (
            <List>
              {sharedRules.map(rule => (
                <ListItem key={rule.id} disablePadding>
                  <ListItemButton
                    onClick={() => handleSharedRuleSelect(rule)}
                    selected={selectedSharedRule?.id === rule.id}
                  >
                    <ListItemText
                      primary={rule.name}
                      secondary={
                        <Box component="span">
                          <Chip label={rule.category} size="small" sx={{ mr: 1 }} />
                          <Chip
                            label={rule.effect}
                            size="small"
                            color={rule.effect === 'Permit' ? 'success' : 'error'}
                          />
                        </Box>
                      }
                    />
                  </ListItemButton>
                </ListItem>
              ))}
            </List>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenSharedRuleDialog(false)}>Cancel</Button>
          <Button onClick={handleSharedRuleApply} disabled={!selectedSharedRule} variant="contained">
            Add to Policy
          </Button>
        </DialogActions>
      </Dialog>

      {/* Save as Shared Rule Dialog */}
      <Dialog open={openSaveAsSharedDialog} onClose={() => setOpenSaveAsSharedDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Save as Shared Rule</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            <TextField
              label="Rule Name"
              value={sharedRuleName}
              onChange={e => setSharedRuleName(e.target.value)}
              fullWidth
              required
            />

            <TextField
              label="Description"
              value={sharedRuleDescription}
              onChange={e => setSharedRuleDescription(e.target.value)}
              fullWidth
              multiline
              rows={2}
            />

            <FormControl fullWidth required>
              <InputLabel>Category</InputLabel>
              <Select
                value={sharedRuleCategory}
                label="Category"
                onChange={e => setSharedRuleCategory(e.target.value)}
              >
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

            <Alert severity="info">
              This rule will be saved to the Shared Rule Library and can be reused in other policies.
              The current inline rule will be replaced with a reference to the shared rule.
            </Alert>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenSaveAsSharedDialog(false)}>Cancel</Button>
          <Button
            onClick={handleSaveAsSharedConfirm}
            disabled={!sharedRuleName.trim() || !sharedRuleCategory.trim() || savingSharedRule}
            variant="contained"
          >
            {savingSharedRule ? 'Saving...' : 'Save as Shared'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
