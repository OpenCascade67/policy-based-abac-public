// API DTO types — mirrors the .NET backend DTOs

export interface DomainRoleDto {
  domainId: string;
  roles: string[];
}

export interface UserDto {
  id: string;
  username: string;
  email?: string;
  userType: 'local' | 'email';
  status: 'active' | 'inactive';
  domainRoles: DomainRoleDto[];
  defaultDomainId?: string;
  createdById?: string;
  lastModifiedById?: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresAt: string;
  user: UserSummaryDto;
  currentDomainId?: string;
}

export interface UserSummaryDto {
  id: string;
  username: string;
  email?: string;
  userType: string;
  defaultDomainId?: string;
  domainRoles: DomainRoleDto[];
}

export interface RoleDto {
  id: string;
  name: string;
  description: string;
  isSystemRole: boolean;
  permissions: string[];
  createdById?: string;
  lastModifiedById?: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface DomainDto {
  id: string;
  name: string;
  description: string;
  status: 'active' | 'inactive';
  createdById?: string;
  lastModifiedById?: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface AttributeMatchDto {
  attributeId: string;
  matchFunction: 'Equals' | 'Contains' | 'GreaterThan' | 'LessThan' | 'Regex';
  value: string | number | boolean;
}

export interface PolicyTargetDto {
  subjects?: AttributeMatchDto[];
  resources?: AttributeMatchDto[];
  actions?: AttributeMatchDto[];
  environments?: AttributeMatchDto[];
}

export interface PolicyConditionDto {
  operator: 'AND' | 'OR';
  conditions: (PolicyConditionDto | AttributeMatchDto)[];
}

export interface PolicyRuleDto {
  ruleId: string;
  effect: 'Permit' | 'Deny';
  description?: string;
  target?: PolicyTargetDto;
  condition?: PolicyConditionDto;
}

export interface PolicyRuleReferenceDto {
  type: 'inline' | 'shared';
  sharedRuleId?: string;
  inlineRule?: PolicyRuleDto;
}

export interface ObligationAttributeAssignmentDto {
  attributeId: string;
  value: string | number | boolean;
  dataType?: string;
}

export interface ObligationDto {
  obligationId: string;
  fulfillOn: 'Permit' | 'Deny' | 'Both';
  description?: string;
  attributeAssignments: ObligationAttributeAssignmentDto[];
}

export interface PolicyDefinitionDto {
  version: string;
  policyId: string;
  ruleCombiningAlgorithm: string;
  rules: PolicyRuleDto[];
  ruleReferences?: PolicyRuleReferenceDto[];
  obligations?: ObligationDto[];
}

export interface PolicyDto {
  id: string;
  name: string;
  description: string;
  policyDefinition: PolicyDefinitionDto;
  createdById?: string;
  lastModifiedById?: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface SharedRuleDto {
  id: string;
  name: string;
  description: string;
  category: string;
  effect: 'Permit' | 'Deny';
  target?: PolicyTargetDto;
  condition?: PolicyConditionDto;
  usedByPolicies: string[];
  createdById?: string;
  lastModifiedById?: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface ScenarioDto {
  id: string;
  name: string;
  description: string;
  value: string;
  testCases: string[];
  createdById?: string;
  lastModifiedById?: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface TestResultDto {
  decision: 'Permit' | 'Deny' | 'NotApplicable' | 'Indeterminate';
  matchedPolicies: string[];
  executedAt: string;
}

export interface TestCaseDto {
  id: string;
  name: string;
  description: string;
  policyId?: string;
  scenarioId?: string;
  inputData: string;
  result?: TestResultDto;
  createdById?: string;
  lastModifiedById?: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface AppSettingsDto {
  theme: string;
  colorScheme: string;
}

export interface PagedResult<T> {
  items: T[];
  totalCount: number;
  page: number;
  pageSize: number;
}
