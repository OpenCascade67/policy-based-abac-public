// Test Access Control Policies page

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
  TablePagination,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Autocomplete,
  Alert,
  Chip,
  CircularProgress,
} from '@mui/material';
import { Add, Edit, Delete, PlayArrow } from '@mui/icons-material';
import { format } from 'date-fns';
import Editor from '@monaco-editor/react';
import { testCasesApi } from '../api/testCases';
import type { RunTestCaseResponse } from '../api/testCases';
import { scenariosApi } from '../api/scenarios';
import { policiesApi } from '../api/policies';
import type { TestCaseDto, ScenarioDto, PolicyDto } from '../api/types';
import SlideOutDrawer from '../components/SlideOutDrawer';

export default function TestPolicies() {
  const [testCases, setTestCases] = useState<TestCaseDto[]>([]);
  const [scenarios, setScenarios] = useState<ScenarioDto[]>([]);
  const [policies, setPolicies] = useState<PolicyDto[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Test case editor
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingTestCase, setEditingTestCase] = useState<TestCaseDto | null>(null);
  const [testCaseForm, setTestCaseForm] = useState({
    name: '',
    description: '',
    policyId: 'all' as string,
    scenarioId: '',
    inputData: '{}',
  });
  const [testResult, setTestResult] = useState<RunTestCaseResponse | null>(null);
  const [running, setRunning] = useState(false);
  const [saving, setSaving] = useState(false);

  // Scenario editor
  const [scenarioDrawerOpen, setScenarioDrawerOpen] = useState(false);
  const [editingScenario, setEditingScenario] = useState<ScenarioDto | null>(null);
  const [scenarioForm, setScenarioForm] = useState({ name: '', description: '', value: '{}' });

  const loadTestCases = useCallback(async () => {
    setLoading(true);
    try {
      const result = await testCasesApi.getAll(page, rowsPerPage);
      setTestCases(result.items);
      setTotalCount(result.totalCount);
    } catch { setError('Failed to load test cases'); }
    finally { setLoading(false); }
  }, [page, rowsPerPage]);

  const loadScenarios = useCallback(async () => {
    try { setScenarios(await scenariosApi.getAll()); } catch { /* ignore */ }
  }, []);

  const loadPolicies = useCallback(async () => {
    try { setPolicies((await policiesApi.getAll(0, 1000)).items); } catch { /* ignore */ }
  }, []);

  useEffect(() => { loadTestCases(); }, [loadTestCases]);
  useEffect(() => { loadScenarios(); loadPolicies(); }, [loadScenarios, loadPolicies]);

  // Test Case handlers
  const handleOpenTestCaseDrawer = (testCase?: TestCaseDto) => {
    if (testCase) {
      setEditingTestCase(testCase);
      setTestCaseForm({
        name: testCase.name,
        description: testCase.description,
        policyId: testCase.policyId || 'all',
        scenarioId: testCase.scenarioId || '',
        inputData: testCase.inputData,
      });
      setTestResult(testCase.result
        ? { decision: testCase.result.decision, matchedPolicies: testCase.result.matchedPolicies, executedAt: testCase.result.executedAt }
        : null);
    } else {
      setEditingTestCase(null);
      setTestCaseForm({ name: '', description: '', policyId: 'all', scenarioId: '', inputData: '{}' });
      setTestResult(null);
    }
    setDrawerOpen(true);
  };

  const handleCloseTestCaseDrawer = () => {
    setDrawerOpen(false);
    setEditingTestCase(null);
    setTestResult(null);
  };

  const handleSaveTestCase = async () => {
    if (!testCaseForm.name.trim()) return;
    setSaving(true);
    try {
      const payload = {
        name: testCaseForm.name,
        description: testCaseForm.description,
        policyId: testCaseForm.policyId === 'all' ? undefined : testCaseForm.policyId,
        scenarioId: testCaseForm.scenarioId || undefined,
        inputData: testCaseForm.inputData,
      };
      if (editingTestCase) { await testCasesApi.update(editingTestCase.id, payload); }
      else { await testCasesApi.create(payload); }
      await loadTestCases();
      handleCloseTestCaseDrawer();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save test case');
    } finally { setSaving(false); }
  };

  const handleDeleteTestCase = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this test case?')) return;
    try { await testCasesApi.delete(id); await loadTestCases(); }
    catch (err) { setError(err instanceof Error ? err.message : 'Failed to delete test case'); }
  };

  const handleRunTest = async () => {
    setRunning(true);
    try {
      const payload = {
        name: testCaseForm.name || 'Quick Test',
        description: testCaseForm.description,
        policyId: testCaseForm.policyId === 'all' ? undefined : testCaseForm.policyId,
        scenarioId: testCaseForm.scenarioId || undefined,
        inputData: testCaseForm.inputData,
      };
      let tcId: string;
      if (editingTestCase?.id) {
        // Always sync current form data before running so inputData is up-to-date
        await testCasesApi.update(editingTestCase.id, payload);
        tcId = editingTestCase.id;
      } else {
        const created = await testCasesApi.create(payload);
        tcId = created.id;
        await loadTestCases();
      }
      const ran = await testCasesApi.run(tcId, testCaseForm.inputData);
      setTestResult(ran);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to run test');
    } finally { setRunning(false); }
  };

  const handleScenarioSelect = (scenarioId: string) => {
    if (scenarioId) {
      const scenario = scenarios.find(s => s.id === scenarioId);
      if (scenario) { setTestCaseForm({ ...testCaseForm, scenarioId, inputData: scenario.value }); }
    } else {
      setTestCaseForm({ ...testCaseForm, scenarioId: '' });
    }
  };

  // Scenario handlers
  const handleOpenScenarioDrawer = (scenario?: ScenarioDto) => {
    if (scenario) {
      setEditingScenario(scenario);
      setScenarioForm({ name: scenario.name, description: scenario.description, value: scenario.value });
    } else {
      setEditingScenario(null);
      setScenarioForm({ name: '', description: '', value: '{}' });
    }
    setScenarioDrawerOpen(true);
  };

  const handleCloseScenarioDrawer = () => {
    setScenarioDrawerOpen(false);
    setEditingScenario(null);
  };

  const handleSaveScenario = async () => {
    if (!scenarioForm.name.trim()) return;
    try {
      if (editingScenario) { await scenariosApi.update(editingScenario.id, scenarioForm); }
      else { await scenariosApi.create(scenarioForm); }
      await loadScenarios();
      handleCloseScenarioDrawer();
    } catch (err) { setError(err instanceof Error ? err.message : 'Failed to save scenario'); }
  };

  const handleDeleteScenario = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this scenario?')) return;
    try { await scenariosApi.delete(id); await loadScenarios(); }
    catch (err) { setError(err instanceof Error ? err.message : 'Failed to delete scenario'); }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ fontWeight: 600 }}>
            Test Access Control Policies
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Create and execute test cases for your ABAC policies
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button variant="outlined" startIcon={<Add />} onClick={() => handleOpenScenarioDrawer()}>
            Manage Scenarios
          </Button>
          <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenTestCaseDrawer()}>
            Create Test Case
          </Button>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>{error}</Alert>}

      {/* Scenarios Section */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          ABAC Scenarios
        </Typography>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
          {scenarios.map(scenario => (
            <Chip
              key={scenario.id}
              label={scenario.name}
              onClick={() => handleOpenScenarioDrawer(scenario)}
              onDelete={() => handleDeleteScenario(scenario.id)}
            />
          ))}
          {scenarios.length === 0 && (
            <Typography variant="body2" color="text.secondary">
              No scenarios defined yet
            </Typography>
          )}
        </Box>
      </Paper>

      {/* Test Cases Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Test Case Name</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Policy</TableCell>
              <TableCell>Last Result</TableCell>
              <TableCell>Created</TableCell>
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
            ) : testCases.map(testCase => (
              <TableRow key={testCase.id} hover>
                <TableCell>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {testCase.name}
                  </Typography>
                </TableCell>
                <TableCell>{testCase.description}</TableCell>
                <TableCell>
                  {testCase.policyId
                    ? policies.find(p => p.id === testCase.policyId)?.name || 'Unknown'
                    : 'All Policies'}
                </TableCell>
                <TableCell>
                  {testCase.result ? (
                    <Chip
                      label={testCase.result.decision}
                      size="small"
                      color={
                        testCase.result.decision === 'Permit' ? 'success' :
                        testCase.result.decision === 'Deny' ? 'error' :
                        testCase.result.decision === 'NotApplicable' ? 'default' : 'warning'
                      }
                    />
                  ) : (
                    <Typography variant="caption" color="text.secondary">Not run</Typography>
                  )}
                </TableCell>
                <TableCell>{format(new Date(testCase.createdAt), 'PP')}</TableCell>
                <TableCell align="right">
                  <IconButton size="small" onClick={() => handleOpenTestCaseDrawer(testCase)} aria-label="edit test case">
                    <Edit fontSize="small" />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleDeleteTestCase(testCase.id)} aria-label="delete test case">
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

      {/* Test Case Drawer */}
      <SlideOutDrawer
        open={drawerOpen}
        onClose={handleCloseTestCaseDrawer}
        title={editingTestCase ? 'Edit Test Case' : 'Create Test Case'}
        onSave={handleSaveTestCase}
        saveDisabled={!testCaseForm.name.trim() || saving}
      >
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <TextField
            label="Test Case Name"
            value={testCaseForm.name}
            onChange={e => setTestCaseForm({ ...testCaseForm, name: e.target.value })}
            fullWidth required
          />

          <TextField
            label="Description"
            value={testCaseForm.description}
            onChange={e => setTestCaseForm({ ...testCaseForm, description: e.target.value })}
            fullWidth multiline rows={2}
          />

          <FormControl fullWidth>
            <InputLabel>Test Policy</InputLabel>
            <Select
              value={testCaseForm.policyId}
              label="Test Policy"
              onChange={e => setTestCaseForm({ ...testCaseForm, policyId: e.target.value })}
            >
              <MenuItem value="all">All Policies</MenuItem>
              {policies.map(policy => (
                <MenuItem key={policy.id} value={policy.id}>{policy.name}</MenuItem>
              ))}
            </Select>
          </FormControl>

          <Autocomplete
            options={scenarios}
            getOptionLabel={option => option.name}
            value={scenarios.find(s => s.id === testCaseForm.scenarioId) || null}
            onChange={(_, newValue) => handleScenarioSelect(newValue?.id || '')}
            renderInput={params => <TextField {...params} label="Select ABAC Scenario (Optional)" />}
          />

          <Box>
            <Typography variant="subtitle2" gutterBottom>Input Data (JSON)</Typography>
            <Paper variant="outlined" sx={{ height: 300 }}>
              <Editor
                height="300px"
                defaultLanguage="json"
                value={testCaseForm.inputData}
                onChange={value => setTestCaseForm({ ...testCaseForm, inputData: value || '{}' })}
                options={{ minimap: { enabled: false }, lineNumbers: 'on' }}
              />
            </Paper>
          </Box>

          <Button
            variant="contained"
            startIcon={running ? <CircularProgress size={16} color="inherit" /> : <PlayArrow />}
            onClick={handleRunTest}
            disabled={!testCaseForm.inputData || running}
          >
            {running ? 'Running...' : 'Run Test'}
          </Button>

          {testResult && (
            <Alert severity={testResult.decision === 'Permit' ? 'success' : testResult.decision === 'Deny' ? 'error' : 'warning'} sx={{ mt: 2 }}>
              <Typography variant="subtitle2" gutterBottom>
                Test Result: {testResult.decision}
              </Typography>
              <Typography variant="body2">
                Matched Policies: {testResult.matchedPolicies.join(', ') || 'None'}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Executed at: {format(new Date(testResult.executedAt), 'PPpp')}
              </Typography>
            </Alert>
          )}
        </Box>
      </SlideOutDrawer>

      {/* Scenario Drawer */}
      <SlideOutDrawer
        open={scenarioDrawerOpen}
        onClose={handleCloseScenarioDrawer}
        title={editingScenario ? 'Edit Scenario' : 'Create Scenario'}
        onSave={handleSaveScenario}
        saveDisabled={!scenarioForm.name.trim()}
      >
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          <TextField
            label="Scenario Name"
            value={scenarioForm.name}
            onChange={e => setScenarioForm({ ...scenarioForm, name: e.target.value })}
            fullWidth required
          />

          <TextField
            label="Description"
            value={scenarioForm.description}
            onChange={e => setScenarioForm({ ...scenarioForm, description: e.target.value })}
            fullWidth multiline rows={2}
          />

          <Box>
            <Typography variant="subtitle2" gutterBottom>Scenario Value (JSON)</Typography>
            <Paper variant="outlined" sx={{ height: 400 }}>
              <Editor
                height="400px"
                defaultLanguage="json"
                value={scenarioForm.value}
                onChange={value => setScenarioForm({ ...scenarioForm, value: value || '{}' })}
                options={{ minimap: { enabled: false }, lineNumbers: 'on' }}
              />
            </Paper>
          </Box>
        </Box>
      </SlideOutDrawer>
    </Box>
  );
}
