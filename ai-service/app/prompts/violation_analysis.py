def build_violation_analysis_prompt(device_hostname: str, device_type: str, 
                                     compliance_score: float, violations: list) -> str:
    """
    Build a focused prompt for analyzing network configuration violations.
    This prompt does NOT ask the AI to determine compliance.
    Compliance has already been determined deterministically.
    AI only provides explanation and recommendations.
    """
    violations_text = ""
    for i, v in enumerate(violations, 1):
        violations_text += f"""
{i}. Parameter: {v.parameter}
   Severity: {v.severity}
   Expected: {v.expected_value}
   Actual: {v.actual_value or 'NOT CONFIGURED'}
   Description: {v.description or 'N/A'}
"""

    prompt = f"""You are a network security expert analyzing configuration compliance violations for a network device.

IMPORTANT: The compliance score of {compliance_score:.1f}% has already been calculated by a deterministic rule engine. Do NOT recalculate or dispute this score. Your role is ONLY to explain, analyze impact, and recommend remediation.

Device Information:
- Hostname: {device_hostname}
- Device Type: {device_type}
- Compliance Score: {compliance_score:.1f}% (pre-calculated, do not change)
- Number of Violations: {len(violations)}

Detected Violations (from deterministic compliance engine):
{violations_text}

Provide your response in the following JSON format ONLY. Do not include any text outside the JSON:
{{
  "summary": "<1-2 sentence overview of what was found>",
  "analysis": "<Detailed explanation of why these violations are significant from a security and operational perspective. 3-5 sentences.>",
  "impact": "<What could go wrong if these violations are not addressed. 2-3 sentences.>",
  "recommendations": [
    "<Specific action 1>",
    "<Specific action 2>",
    "<Specific action N for each violation>"
  ],
  "priority": "<CRITICAL|HIGH|MEDIUM|LOW based on the highest severity violation>"
}}"""
    return prompt
