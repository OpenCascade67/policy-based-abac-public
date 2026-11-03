package com.pap.service.export;

import com.pap.domain.model.Policy;
import com.pap.domain.model.PolicyDefinition;
import com.pap.domain.model.PolicyRule;
import com.pap.json.EnumNames;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Locale;

/**
 * Renders a policy as a XACML 3.0 document (XML) or its JSON profile. The output is byte-compatible with the
 * .NET implementation, including System.Text.Json's indentation and escaping.
 */
@Component
public class XacmlExporter {

    private static final String NL = System.lineSeparator();
    private static final String XACML_NAMESPACE = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17";

    /** @param format "xml" (case-insensitive) for XML; anything else produces JSON */
    public ExportedFile export(Policy policy, String format) {
        if ("xml".equalsIgnoreCase(format)) {
            return new ExportedFile(toXml(policy), MediaType.APPLICATION_XML, policy.getName() + ".xml");
        }
        return new ExportedFile(toJson(policy), MediaType.APPLICATION_JSON, policy.getName() + ".json");
    }

    String toXml(Policy policy) {
        PolicyDefinition definition = policy.getPolicyDefinition();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(NL);
        xml.append("<Policy xmlns=\"").append(XACML_NAMESPACE).append('"').append(NL);
        xml.append("        PolicyId=\"").append(definition.policyId()).append('"').append(NL);
        xml.append("        Version=\"").append(definition.version()).append('"').append(NL);
        xml.append("        RuleCombiningAlgId=\"").append(definition.ruleCombiningAlgorithm().urn()).append("\">").append(NL);
        xml.append("  <Description>").append(escapeXml(policy.getDescription())).append("</Description>").append(NL);
        xml.append("  <Target/>").append(NL);
        for (PolicyRule rule : definition.rules()) {
            xml.append("  <Rule RuleId=\"").append(rule.ruleId())
                    .append("\" Effect=\"").append(EnumNames.pascal(rule.effect())).append("\">").append(NL);
            if (rule.description() != null && !rule.description().isEmpty()) {
                xml.append("    <Description>").append(escapeXml(rule.description())).append("</Description>").append(NL);
            }
            xml.append("    <Target/>").append(NL);
            xml.append("  </Rule>").append(NL);
        }
        xml.append("</Policy>").append(NL);
        return xml.toString();
    }

    String toJson(Policy policy) {
        PolicyDefinition definition = policy.getPolicyDefinition();
        StringBuilder json = new StringBuilder();
        json.append('{').append(NL);
        json.append("  \"Policy\": {").append(NL);
        json.append("    \"PolicyId\": ").append(jsonString(definition.policyId())).append(',').append(NL);
        json.append("    \"Version\": ").append(jsonString(definition.version())).append(',').append(NL);
        json.append("    \"RuleCombiningAlgId\": ").append(jsonString(definition.ruleCombiningAlgorithm().urn())).append(',').append(NL);
        json.append("    \"Description\": ").append(jsonString(policy.getDescription())).append(',').append(NL);
        json.append("    \"Target\": {},").append(NL);
        if (definition.rules().isEmpty()) {
            json.append("    \"Rule\": []").append(NL);
        } else {
            json.append("    \"Rule\": [").append(NL);
            for (Iterator<PolicyRule> it = definition.rules().iterator(); it.hasNext(); ) {
                PolicyRule rule = it.next();
                json.append("      {").append(NL);
                json.append("        \"RuleId\": ").append(jsonString(rule.ruleId())).append(',').append(NL);
                json.append("        \"Effect\": ").append(jsonString(EnumNames.pascal(rule.effect()))).append(',').append(NL);
                json.append("        \"Description\": ").append(jsonString(rule.description())).append(NL);
                json.append("      }").append(it.hasNext() ? "," : "").append(NL);
            }
            json.append("    ]").append(NL);
        }
        json.append("  }").append(NL);
        json.append('}');
        return json.toString();
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    /** JSON string literal escaped like System.Text.Json's default (HTML-safe, ASCII-only) encoder. */
    private static String jsonString(String value) {
        if (value == null) return "null";
        StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    boolean htmlSensitive = "\"&'+<>`".indexOf(c) >= 0;
                    if (c < 0x20 || c > 0x7E || htmlSensitive) {
                        sb.append("\\u").append(String.format(Locale.ROOT, "%04X", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
