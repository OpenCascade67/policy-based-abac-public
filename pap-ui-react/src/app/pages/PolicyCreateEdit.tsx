// Policy Creation/Edit page

import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router';
import {
  Box,
  Typography,
  Button,
  TextField,
  Paper,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  CircularProgress,
} from '@mui/material';
import { Save, ArrowBack } from '@mui/icons-material';
import { policiesApi } from '../api/policies';
import { sharedRulesApi } from '../api/sharedRules';
import { ApiError } from '../api/client';
import type { PolicyDto, PolicyDefinitionDto, PolicyRuleDto, PolicyRuleReferenceDto, ObligationDto, SharedRuleDto } from '../api/types';
import { toFullDataType, toEnumDataType } from '../utils/xacml';

import PolicyEditor from '../components/PolicyEditor';
import ObligationsEditor from '../components/ObligationsEditor';

export default function PolicyCreateEdit() {
  const { policyId } = useParams();
  const navigate = useNavigate();
  const isEditMode = !!policyId;

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [algorithm, setAlgorithm] = useState<PolicyDefinitionDto['ruleCombiningAlgorithm']>('permit-overrides');
  const [ruleReferences, setRuleReferences] = useState<PolicyRuleReferenceDto[]>([]);
  const [obligations, setObligations] = useState<ObligationDto[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const [sharedRules, setSharedRules] = useState<SharedRuleDto[]>([]);

  const loadSharedRules = useCallback(async () => {
    try {
      const data = await sharedRulesApi.getAll();
      setSharedRules(data);
    } catch {
      // safe to ignore
    }
  }, []);

  useEffect(() => {
    loadSharedRules();
  }, [loadSharedRules]);

  useEffect(() => {
    if (isEditMode && policyId) {
      setLoading(true);
      policiesApi.getById(policyId)
        .then(policy => {
          setName(policy.name);
          setDescription(policy.description);
          const rawAlg = policy.policyDefinition.ruleCombiningAlgorithm || 'permit-overrides';
          setAlgorithm(rawAlg.includes(':') ? rawAlg.split(':').pop()! as any : rawAlg as any);
          
          if (policy.policyDefinition.ruleReferences && policy.policyDefinition.ruleReferences.length > 0) {
            setRuleReferences(policy.policyDefinition.ruleReferences);
          } else if (policy.policyDefinition.rules && policy.policyDefinition.rules.length > 0) {
            // Migrate inline rules
            const refs: PolicyRuleReferenceDto[] = policy.policyDefinition.rules.map(rule => ({
              type: 'inline',
              inlineRule: rule,
            }));
            setRuleReferences(refs);
          } else {
            setRuleReferences([]);
          }

          if (policy.policyDefinition.obligations) {
            // Ensure obligations use the correct dataType property name
            const mappedObligations: ObligationDto[] = policy.policyDefinition.obligations.map(obl => ({
              ...obl,
              attributeAssignments: obl.attributeAssignments.map(attr => ({
                ...attr,
                dataType: (attr as any).dataType || (attr as any).DataType
              }))
            }));
            setObligations(mappedObligations);
          }
        })
        .catch(() => setError('Policy not found'))
        .finally(() => setLoading(false));
    }
  }, [isEditMode, policyId]);

  const handleSave = async () => {
    if (!name.trim()) {
      setError('Policy name is required');
      return;
    }

    if (ruleReferences.length === 0) {
      setError('At least one rule is required');
      return;
    }

    setSaving(true);
    try {
      // The backend will handle expanding rules from ruleReferences. 
      // We pass the resolved rules if necessary, but actually the .NET DTO requires Rules array
      // so let's populate Rules from inline rules and let the backend do whatever with shared rules.
      // Wait, let's just populate resolvedRules from our sharedRules state.
      const resolvedRules: PolicyRuleDto[] = ruleReferences.map((ref, index) => {
        if (ref.type === 'shared' && ref.sharedRuleId) {
          const sharedRule = sharedRules.find(r => r.id === ref.sharedRuleId);
          if (sharedRule) {
            return {
              ruleId: sharedRule.id, // using shared rule id directly aligns with .NET DTO
              effect: sharedRule.effect,
              description: sharedRule.description,
              target: sharedRule.target,
            };
          }
        } else if (ref.type === 'inline' && ref.inlineRule) {
          return ref.inlineRule;
        }
        return {
          ruleId: `rule-${index}-${Date.now()}`,
          effect: 'Deny',
          description: 'Invalid rule reference',
        };
      });

      const policyDefinition: PolicyDefinitionDto = {
        version: '3.0',
        policyId: isEditMode ? policyId! : `policy-${Date.now()}`,
        ruleCombiningAlgorithm: algorithm.includes(':') 
          ? algorithm 
          : `urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:${algorithm}`,
        rules: resolvedRules,
        ruleReferences: ruleReferences,
        obligations: obligations.length > 0 ? obligations.map(obl => ({
          ...obl,
          attributeAssignments: obl.attributeAssignments.map(attr => ({
            ...attr,
            dataType: toEnumDataType((attr as any).dataType || (attr as any).DataType)
          }))
        })) : undefined,
      };

      const payload: any = {
        id: isEditMode ? policyId : undefined,
        name,
        description,
        policyDefinition,
      };

      if (isEditMode) {
        await policiesApi.update(policyId!, payload);
      } else {
        await policiesApi.create(payload);
      }
      navigate('/policies');
    } catch (err) {
      console.error('Save error:', err);
      let errorMsg = 'Failed to save policy';
      if (err instanceof ApiError) {
        const details = err.details as any;
        if (details.errors) {
          const validationErrors = Object.entries(details.errors)
            .map(([field, msgs]) => `${field}: ${(msgs as string[]).join(', ')}`)
            .join(' | ');
          errorMsg = `${err.message}: ${validationErrors}`;
        } else {
          errorMsg = `${err.message} (${err.status}): ${JSON.stringify(err.details)}`;
        }
      } else if (err instanceof Error) {
        errorMsg = err.message;
      }
      setError(errorMsg);
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Button
        startIcon={<ArrowBack />}
        onClick={() => navigate('/policies')}
        sx={{ mb: 2 }}
      >
        Back to Policies
      </Button>

      <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>
        {isEditMode ? 'Edit Policy' : 'Create New Policy'}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
        Define XACML 3.0 compliant access control policies with rules and conditions
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Policy Information
          </Typography>

          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            <TextField
              label="Policy Name"
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
              rows={3}
            />

            <FormControl fullWidth>
              <InputLabel>Rule Combining Algorithm</InputLabel>
              <Select
                value={algorithm}
                label="Rule Combining Algorithm"
                onChange={e =>
                  setAlgorithm(e.target.value as PolicyDefinitionDto['ruleCombiningAlgorithm'])
                }
              >
                <MenuItem value="permit-overrides">Permit Overrides</MenuItem>
                <MenuItem value="deny-overrides">Deny Overrides</MenuItem>
                <MenuItem value="first-applicable">First Applicable</MenuItem>
              </Select>
            </FormControl>

            <Alert severity="info">
              <strong>Combining Algorithms:</strong>
              <ul style={{ margin: '8px 0', paddingLeft: '24px' }}>
                <li>
                  <strong>Permit Overrides:</strong> If any rule permits, the result is Permit
                </li>
                <li>
                  <strong>Deny Overrides:</strong> If any rule denies, the result is Deny
                </li>
                <li>
                  <strong>First Applicable:</strong> Use the decision of the first applicable rule
                </li>
              </ul>
            </Alert>
          </Box>
        </Paper>

        <Paper sx={{ p: 3 }}>
          <PolicyEditor 
            rules={[]} 
            onChange={() => {}}
            ruleReferences={ruleReferences}
            onRuleReferencesChange={setRuleReferences}
            policyId={policyId}
            sharedRules={sharedRules}
            onSharedRulesChange={loadSharedRules}
          />
        </Paper>

        <Paper sx={{ p: 3 }}>
          <ObligationsEditor
            obligations={obligations}
            onChange={setObligations}
          />
        </Paper>

        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
          <Button variant="outlined" onClick={() => navigate('/policies')} disabled={saving}>
            Cancel
          </Button>
          <Button variant="contained" startIcon={saving ? <CircularProgress size={20} color="inherit" /> : <Save />} onClick={handleSave} disabled={saving}>
            {saving ? 'Saving...' : (isEditMode ? 'Update Policy' : 'Create Policy')}
          </Button>
        </Box>
      </Box>
    </Box>
  );
}