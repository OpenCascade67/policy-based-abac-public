// Obligations Editor for XACML Policy Obligations

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
} from '@mui/material';
import { Add, Delete, ExpandMore, ExpandLess } from '@mui/icons-material';
import type { ObligationDto, ObligationAttributeAssignmentDto } from '../api/types';
import { toShortDataType, toFullDataType } from '../utils/xacml';

// Map to API DTOs
type Obligation = ObligationDto;
type ObligationAttributeAssignment = ObligationAttributeAssignmentDto;

interface ObligationsEditorProps {
  obligations: Obligation[];
  onChange: (obligations: Obligation[]) => void;
}

export default function ObligationsEditor({ obligations, onChange }: ObligationsEditorProps) {
  const [expandedObligations, setExpandedObligations] = useState<Set<string>>(new Set());

  const handleAddObligation = () => {
    const newObligation: Obligation = {
      obligationId: `obligation-${Date.now()}`,
      fulfillOn: 'Permit',
      description: '',
      attributeAssignments: [],
    };
    onChange([...obligations, newObligation]);
    setExpandedObligations(new Set([...expandedObligations, newObligation.obligationId]));
  };

  const handleRemoveObligation = (obligationId: string) => {
    onChange(obligations.filter(o => o.obligationId !== obligationId));
    const newExpanded = new Set(expandedObligations);
    newExpanded.delete(obligationId);
    setExpandedObligations(newExpanded);
  };

  const handleObligationChange = (obligationId: string, field: keyof Obligation, value: any) => {
    onChange(
      obligations.map(o => (o.obligationId === obligationId ? { ...o, [field]: value } : o))
    );
  };

  const handleAddAttributeAssignment = (obligationId: string) => {
    const obligation = obligations.find(o => o.obligationId === obligationId);
    if (!obligation) return;

    const newAssignment: ObligationAttributeAssignment = {
      attributeId: '',
      value: '',
      dataType: toFullDataType('string'),
    };

    const updatedAssignments = [...obligation.attributeAssignments, newAssignment];
    handleObligationChange(obligationId, 'attributeAssignments', updatedAssignments);
  };

  const handleRemoveAttributeAssignment = (obligationId: string, index: number) => {
    const obligation = obligations.find(o => o.obligationId === obligationId);
    if (!obligation) return;

    const updatedAssignments = obligation.attributeAssignments.filter((_, i) => i !== index);
    handleObligationChange(obligationId, 'attributeAssignments', updatedAssignments);
  };

  const handleAttributeAssignmentChange = (
    obligationId: string,
    index: number,
    field: keyof ObligationAttributeAssignment,
    value: any
  ) => {
    const obligation = obligations.find(o => o.obligationId === obligationId);
    if (!obligation) return;

    const updatedAssignments = [...obligation.attributeAssignments];
    updatedAssignments[index] = { ...updatedAssignments[index], [field]: value };
    handleObligationChange(obligationId, 'attributeAssignments', updatedAssignments);
  };

  const toggleObligationExpanded = (obligationId: string) => {
    const newExpanded = new Set(expandedObligations);
    if (newExpanded.has(obligationId)) {
      newExpanded.delete(obligationId);
    } else {
      newExpanded.add(obligationId);
    }
    setExpandedObligations(newExpanded);
  };

  const getFulfillOnColor = (fulfillOn: Obligation['fulfillOn']) => {
    switch (fulfillOn) {
      case 'Permit':
        return 'success';
      case 'Deny':
        return 'error';
      case 'Both':
        return 'warning';
      default:
        return 'default';
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">Obligations</Typography>
        <Button variant="outlined" startIcon={<Add />} onClick={handleAddObligation}>
          Add Obligation
        </Button>
      </Box>

      <Alert severity="info" sx={{ mb: 2 }}>
        Obligations are actions that <strong>must be performed</strong> by the Policy Enforcement
        Point (PEP) in conjunction with the policy decision. Examples include logging, notifications,
        or data masking requirements.
      </Alert>

      {obligations.map((obligation, index) => {
        const isExpanded = expandedObligations.has(obligation.obligationId);

        return (
          <Paper key={obligation.obligationId} sx={{ mb: 2, p: 2 }}>
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
                  Obligation {index + 1}
                </Typography>
                <Chip
                  label={`Fulfill On: ${obligation.fulfillOn}`}
                  size="small"
                  color={getFulfillOnColor(obligation.fulfillOn)}
                />
              </Box>
              <Box>
                <IconButton
                  size="small"
                  onClick={() => toggleObligationExpanded(obligation.obligationId)}
                >
                  {isExpanded ? <ExpandLess /> : <ExpandMore />}
                </IconButton>
                <IconButton
                  size="small"
                  color="error"
                  onClick={() => handleRemoveObligation(obligation.obligationId)}
                >
                  <Delete fontSize="small" />
                </IconButton>
              </Box>
            </Box>

            {isExpanded && (
              <>
                <Divider sx={{ mb: 2 }} />

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                  <TextField
                    label="Obligation ID"
                    value={obligation.obligationId}
                    onChange={e =>
                      handleObligationChange(obligation.obligationId, 'obligationId', e.target.value)
                    }
                    fullWidth
                    required
                    helperText="Unique identifier for this obligation (e.g., 'urn:oasis:names:tc:xacml:1.0:action:log')"
                  />

                  <FormControl fullWidth required>
                    <InputLabel>Fulfill On</InputLabel>
                    <Select
                      value={obligation.fulfillOn}
                      label="Fulfill On"
                      onChange={e =>
                        handleObligationChange(
                          obligation.obligationId,
                          'fulfillOn',
                          e.target.value as Obligation['fulfillOn']
                        )
                      }
                    >
                      <MenuItem value="Permit">Permit - Execute only on Permit decisions</MenuItem>
                      <MenuItem value="Deny">Deny - Execute only on Deny decisions</MenuItem>
                      <MenuItem value="Both">Both - Execute on both Permit and Deny</MenuItem>
                    </Select>
                  </FormControl>

                  <TextField
                    label="Description"
                    value={obligation.description || ''}
                    onChange={e =>
                      handleObligationChange(obligation.obligationId, 'description', e.target.value)
                    }
                    fullWidth
                    multiline
                    rows={2}
                    helperText="Human-readable description of what this obligation does"
                  />

                  <Divider sx={{ my: 1 }} />

                  <Box sx={{ mb: 2 }}>
                    <Box
                      sx={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        mb: 1,
                      }}
                    >
                      <Typography variant="subtitle2">Attribute Assignments</Typography>
                      <Button
                        size="small"
                        onClick={() => handleAddAttributeAssignment(obligation.obligationId)}
                      >
                        Add Assignment
                      </Button>
                    </Box>

                    <Alert severity="info" sx={{ mb: 2 }}>
                      Attribute assignments provide parameters for the obligation (e.g., log level,
                      notification recipients, audit trail details).
                    </Alert>

                    {obligation.attributeAssignments.map((assignment, assignmentIndex) => (
                      <Paper
                        key={assignmentIndex}
                        sx={{ p: 2, mb: 1 }}
                        variant="outlined"
                      >
                        <Box
                          sx={{
                            display: 'flex',
                            gap: 2,
                            flexWrap: 'wrap',
                            alignItems: 'start',
                          }}
                        >
                          <TextField
                            label="Attribute ID"
                            value={assignment.attributeId}
                            onChange={e =>
                              handleAttributeAssignmentChange(
                                obligation.obligationId,
                                assignmentIndex,
                                'attributeId',
                                e.target.value
                              )
                            }
                            size="small"
                            sx={{ flex: 1, minWidth: 150 }}
                            helperText="e.g., 'log-level', 'notify-email'"
                          />

                          <FormControl size="small" sx={{ minWidth: 120 }}>
                            <InputLabel>Data Type</InputLabel>
                            <Select
                              value={toShortDataType(assignment.dataType)}
                              label="Data Type"
                              onChange={e =>
                                handleAttributeAssignmentChange(
                                  obligation.obligationId,
                                  assignmentIndex,
                                  'dataType',
                                  toFullDataType(e.target.value as string)
                                )
                              }
                            >
                              <MenuItem value="string">String</MenuItem>
                              <MenuItem value="integer">Integer</MenuItem>
                              <MenuItem value="boolean">Boolean</MenuItem>
                              <MenuItem value="double">Double</MenuItem>
                              <MenuItem value="date">Date</MenuItem>
                              <MenuItem value="time">Time</MenuItem>
                              <MenuItem value="dateTime">DateTime</MenuItem>
                              <MenuItem value="anyURI">URI</MenuItem>
                            </Select>
                          </FormControl>

                          <TextField
                            label="Value"
                            value={assignment.value as string}
                            onChange={e =>
                              handleAttributeAssignmentChange(
                                obligation.obligationId,
                                assignmentIndex,
                                'value',
                                e.target.value
                              )
                            }
                            size="small"
                            sx={{ flex: 1, minWidth: 150 }}
                            helperText="The value for this assignment"
                          />

                          <IconButton
                            size="small"
                            color="error"
                            onClick={() =>
                              handleRemoveAttributeAssignment(
                                obligation.obligationId,
                                assignmentIndex
                              )
                            }
                          >
                            <Delete fontSize="small" />
                          </IconButton>
                        </Box>
                      </Paper>
                    ))}

                    {obligation.attributeAssignments.length === 0 && (
                      <Typography variant="caption" color="text.secondary">
                        No attribute assignments defined
                      </Typography>
                    )}
                  </Box>
                </Box>
              </>
            )}
          </Paper>
        );
      })}

      {obligations.length === 0 && (
        <Alert severity="info">
          No obligations defined. Obligations are optional but useful for enforcing additional
          requirements like logging, notifications, or audit trails.
        </Alert>
      )}
    </Box>
  );
}
