def build_policy_assist_prompt(natural_language: str, device_type: str) -> str:
    return f"""You are a network compliance policy expert. Convert the following natural language security requirement into structured policy rules.

Device Type: {device_type}
Requirement: "{natural_language}"

Return ONLY valid JSON in this exact format. No text outside the JSON:
{{
  "suggested_policy_name": "<descriptive policy name>",
  "description": "<policy description>",
  "rules": [
    {{
      "parameter": "<parameter_name_lowercase_with_underscores>",
      "expected_value": "<true|false|string|number>",
      "operator": "<EQUALS|NOT_EQUALS|CONTAINS|NOT_CONTAINS|GREATER_THAN|LESS_THAN>",
      "severity": "<CRITICAL|HIGH|MEDIUM|LOW|INFO>",
      "description": "<why this rule matters>",
      "remediation": "<how to fix if violated>"
    }}
  ]
}}

Common parameter names to use when applicable:
- ssh (boolean: true=enabled)
- telnet (boolean: false=disabled)
- ntp (boolean: true=enabled)
- snmp (boolean: true=enabled)
- ospf (boolean: true=enabled for routers)
- http (boolean: false=disabled)
- https (boolean: true=enabled)
- logging (boolean: true=enabled)
- password_encryption (boolean: true=enabled)
- spanning_tree (boolean: true=enabled for switches)
- port_security (boolean: true=enabled for switches)
- dhcp_snooping (boolean: true=enabled for switches)"""
