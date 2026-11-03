// XACML export utilities for converting policies to XACML XML and JSON formats

import type { Policy, PolicyDefinition, PolicyRule, AttributeMatch, Obligation } from '../types';

/**
 * Convert a policy to XACML 3.0 JSON format
 */
export function toXACMLJSON(policy: Policy): object {
  const policyDef = policy.policyDefinition;
  
  const policyObj: any = {
    Policy: {
      PolicyId: policyDef.policyId,
      Version: policyDef.version,
      RuleCombiningAlgId: `urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:${policyDef.ruleCombiningAlgorithm}`,
      Description: policy.description,
      Target: {},
      Rule: policyDef.rules.map(rule => ruleToXACMLJSON(rule)),
    },
  };

  // Add obligations if present
  if (policyDef.obligations && policyDef.obligations.length > 0) {
    policyObj.Policy.ObligationExpressions = {
      ObligationExpression: policyDef.obligations.map(obl => obligationToXACMLJSON(obl)),
    };
  }

  return policyObj;
}

function ruleToXACMLJSON(rule: PolicyRule): object {
  const ruleObj: any = {
    RuleId: rule.ruleId,
    Effect: rule.effect,
  };

  if (rule.description) {
    ruleObj.Description = rule.description;
  }

  if (rule.target) {
    ruleObj.Target = {
      AnyOf: [],
    };

    // Add target constraints
    if (rule.target.subjects && rule.target.subjects.length > 0) {
      ruleObj.Target.AnyOf.push({
        AllOf: rule.target.subjects.map(attr => attributeToXACMLJSON(attr, 'subject')),
      });
    }

    if (rule.target.resources && rule.target.resources.length > 0) {
      ruleObj.Target.AnyOf.push({
        AllOf: rule.target.resources.map(attr => attributeToXACMLJSON(attr, 'resource')),
      });
    }

    if (rule.target.actions && rule.target.actions.length > 0) {
      ruleObj.Target.AnyOf.push({
        AllOf: rule.target.actions.map(attr => attributeToXACMLJSON(attr, 'action')),
      });
    }

    if (rule.target.environments && rule.target.environments.length > 0) {
      ruleObj.Target.AnyOf.push({
        AllOf: rule.target.environments.map(attr => attributeToXACMLJSON(attr, 'environment')),
      });
    }
  }

  return ruleObj;
}

function attributeToXACMLJSON(attr: AttributeMatch, category: string): object {
  return {
    Match: {
      MatchId: `urn:oasis:names:tc:xacml:1.0:function:${attr.matchFunction}`,
      AttributeValue: {
        DataType: 'http://www.w3.org/2001/XMLSchema#string',
        Value: attr.value,
      },
      AttributeDesignator: {
        AttributeId: attr.attributeId,
        Category: `urn:oasis:names:tc:xacml:3.0:attribute-category:${category}`,
        DataType: 'http://www.w3.org/2001/XMLSchema#string',
        MustBePresent: true,
      },
    },
  };
}

/**
 * Data type mapping between short names and XACML URIs
 */
export const XACML_DATA_TYPES = {
  string: 'http://www.w3.org/2001/XMLSchema#string',
  integer: 'http://www.w3.org/2001/XMLSchema#integer',
  boolean: 'http://www.w3.org/2001/XMLSchema#boolean',
  double: 'http://www.w3.org/2001/XMLSchema#double',
  date: 'http://www.w3.org/2001/XMLSchema#date',
  time: 'http://www.w3.org/2001/XMLSchema#time',
  dateTime: 'http://www.w3.org/2001/XMLSchema#dateTime',
  anyURI: 'http://www.w3.org/2001/XMLSchema#anyURI',
};

/**
 * Convert full URI to short data type name
 */
export function toShortDataType(uri?: string): string {
  if (!uri) return 'string';
  
  const s = String(uri);
  
  // Direct matches
  if (s in XACML_DATA_TYPES) return s;
  
  // URI matches (case-insensitive and partial)
  const lowerUri = s.toLowerCase();
  
  if (lowerUri.includes('string')) return 'string';
  if (lowerUri.includes('integer')) return 'integer';
  if (lowerUri.includes('boolean')) return 'boolean';
  if (lowerUri.includes('double')) return 'double';
  if (lowerUri.includes('datetime')) return 'dateTime';
  if (lowerUri.includes('date')) return 'date';
  if (lowerUri.includes('time')) return 'time';
  if (lowerUri.includes('anyuri')) return 'anyURI';

  return 'string'; 
}

/**
 * Convert short name to full XACML URI
 */
export function toFullDataType(name?: string): string {
  if (!name) return XACML_DATA_TYPES.string;
  return (XACML_DATA_TYPES as any)[name] || name;
}

/**
 * Convert name or URI to PascalCase enum string for the backend
 */
export function toEnumDataType(val?: string): string {
  const short = toShortDataType(val);
  // Map to PascalCase enum names expected by .NET
  const mapping: Record<string, string> = {
    string: 'String',
    integer: 'Integer',
    boolean: 'Boolean',
    double: 'Double',
    date: 'Date',
    time: 'Time',
    dateTime: 'DateTime',
    anyURI: 'AnyURI',
  };
  return mapping[short] || 'String';
}

function obligationToXACMLJSON(obl: Obligation): object {
  return {
    ObligationId: obl.obligationId,
    FulfillOn: obl.fulfillOn,
    AttributeAssignmentExpression: obl.attributeAssignments.map(attr => ({
      AttributeId: attr.attributeId,
      DataType: toFullDataType(attr.dataType),
      Expression: {
        AttributeValue: {
          DataType: toFullDataType(attr.dataType),
          Value: attr.value,
        },
      },
    })),
  };
}

/**
 * Convert a policy to XACML 3.0 XML format
 */
export function toXACMLXML(policy: Policy): string {
  const policyDef = policy.policyDefinition;
  
  let xml = '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n';
  xml += `<Policy xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\"\n`;
  xml += `        PolicyId=\"${policyDef.policyId}\"\n`;
  xml += `        Version=\"${policyDef.version}\"\n`;
  xml += `        RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:${policyDef.ruleCombiningAlgorithm}\">\n`;
  
  xml += `  <Description>${escapeXML(policy.description)}</Description>\n`;
  xml += `  <Target/>\n`;
  
  policyDef.rules.forEach(rule => {
    xml += ruleToXACMLXML(rule, 2);
  });

  // Add obligations if present
  if (policyDef.obligations && policyDef.obligations.length > 0) {
    xml += '  <ObligationExpressions>\n';
    policyDef.obligations.forEach(obl => {
      xml += obligationToXACMLXML(obl, 3);
    });
    xml += '  </ObligationExpressions>\n';
  }
  
  xml += '</Policy>';
  
  return xml;
}

function ruleToXACMLXML(rule: PolicyRule, indent: number): string {
  const spaces = '  '.repeat(indent);
  let xml = `${spaces}<Rule RuleId="${rule.ruleId}" Effect="${rule.effect}">\n`;
  
  if (rule.description) {
    xml += `${spaces}  <Description>${escapeXML(rule.description)}</Description>\n`;
  }
  
  xml += `${spaces}  <Target/>\n`;
  xml += `${spaces}</Rule>\n`;
  
  return xml;
}

function obligationToXACMLXML(obl: Obligation, indent: number): string {
  const spaces = '  '.repeat(indent);
  let xml = `${spaces}<ObligationExpression ObligationId="${obl.obligationId}" FulfillOn="${obl.fulfillOn}">\n`;
  
  obl.attributeAssignments.forEach(attr => {
    const dataType = toFullDataType(attr.dataType);
    xml += `${spaces}  <AttributeAssignmentExpression AttributeId="${attr.attributeId}" DataType="${dataType}">\n`;
    xml += `${spaces}    <AttributeValue DataType="${dataType}">${escapeXML(String(attr.value))}</AttributeValue>\n`;
    xml += `${spaces}  </AttributeAssignmentExpression>\n`;
  });
  
  xml += `${spaces}</ObligationExpression>\n`;
  
  return xml;
}

function escapeXML(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

/**
 * Download policy as JSON file
 */
export function downloadPolicyAsJSON(policy: Policy): void {
  const json = JSON.stringify(toXACMLJSON(policy), null, 2);
  downloadFile(`${policy.name}.json`, json, 'application/json');
}

/**
 * Download policy as XML file
 */
export function downloadPolicyAsXML(policy: Policy): void {
  const xml = toXACMLXML(policy);
  downloadFile(`${policy.name}.xml`, xml, 'application/xml');
}

function downloadFile(filename: string, content: string, mimeType: string): void {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}