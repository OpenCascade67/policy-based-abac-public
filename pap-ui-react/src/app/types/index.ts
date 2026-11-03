// Types for the ABAC PAP application

export type UserType = 'predefined' | 'email';

export type SystemRole = 'System Admin' | 'Domain Admin' | 'Policy Author' | 'Policy Viewer';

export interface User {
  id: string;
  username: string;
  password?: string;
  email?: string;
  userType: UserType;
  status: 'active' | 'inactive';
  domainRoles: DomainRole[];
  defaultDomainId?: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface DomainRole {
  domainId: string;
  roles: string[];
}

export interface Role {
  id: string;
  name: string;
  description: string;
  isSystemRole: boolean;
  permissions: string[];
  createdAt: string;
  lastModifiedAt: string;
}

export interface Domain {
  id: string;
  name: string;
  description: string;
  status: 'active' | 'inactive';
  createdAt: string;
  lastModifiedAt: string;
}

export interface Policy {
  id: string;
  name: string;
  description: string;
  policyDefinition: PolicyDefinition;
  createdAt: string;
  lastModifiedAt: string;
}

export interface PolicyDefinition {
  version: string;
  policyId: string;
  ruleCombiningAlgorithm: 'permit-overrides' | 'deny-overrides' | 'first-applicable';
  rules: PolicyRule[];
  ruleReferences?: PolicyRuleReference[]; // New: support shared and inline rules
  obligations?: Obligation[]; // XACML Obligations
}

export interface PolicyRule {
  ruleId: string;
  effect: 'Permit' | 'Deny';
  description?: string;
  target?: PolicyTarget;
  condition?: PolicyCondition;
}

export interface PolicyTarget {
  subjects?: AttributeMatch[];
  resources?: AttributeMatch[];
  actions?: AttributeMatch[];
  environments?: AttributeMatch[];
}

export interface AttributeMatch {
  attributeId: string;
  matchFunction: 'equals' | 'contains' | 'greater-than' | 'less-than' | 'regex';
  value: string | number | boolean;
}

export interface PolicyCondition {
  operator: 'AND' | 'OR';
  conditions: (PolicyCondition | AttributeMatch)[];
}

export interface ABACScenario {
  id: string;
  name: string;
  description: string;
  value: string; // JSON string
  testCases: string[];
  createdAt: string;
  lastModifiedAt: string;
}

export interface TestCase {
  id: string;
  name: string;
  description: string;
  policyId?: string; // undefined means "all policies"
  scenarioId?: string;
  inputData: string; // JSON string
  result?: TestResult;
  createdAt: string;
  lastModifiedAt: string;
}

export interface TestResult {
  decision: 'Permit' | 'Deny' | 'NotApplicable' | 'Indeterminate';
  matchedPolicies: string[];
  executedAt: string;
}

export interface SharedRule {
  id: string;
  name: string;
  description: string;
  category: string;
  effect: 'Permit' | 'Deny';
  target?: PolicyTarget;
  condition?: PolicyCondition;
  createdAt: string;
  lastModifiedAt: string;
  usedByPolicies?: string[]; // Track which policies use this rule
}

export interface PolicyRuleReference {
  type: 'shared' | 'inline';
  sharedRuleId?: string; // For type: 'shared'
  inlineRule?: PolicyRule; // For type: 'inline'
}

export interface ObligationAttributeAssignment {
  attributeId: string;
  value: string | number | boolean;
  dataType?: string;
}

export interface Obligation {
  obligationId: string;
  fulfillOn: 'Permit' | 'Deny' | 'Both';
  description?: string;
  attributeAssignments: ObligationAttributeAssignment[];
}

export interface AppSettings {
  theme: 'light' | 'dark';
  colorScheme: string;
}

export interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  currentDomainId: string | null;
}