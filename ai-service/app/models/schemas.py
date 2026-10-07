from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum

class Severity(str, Enum):
    CRITICAL = "CRITICAL"
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"
    INFO = "INFO"

class ViolationItem(BaseModel):
    parameter: str
    expected_value: str = Field(alias="expectedValue")
    actual_value: Optional[str] = Field(default=None, alias="actualValue")
    severity: Severity
    description: Optional[str] = None
    remediation: Optional[str] = None
    
    model_config = {"populate_by_name": True}

class AnalyzeRequest(BaseModel):
    device_hostname: str = Field(alias="deviceHostname")
    device_type: str = Field(alias="deviceType")
    compliance_score: float = Field(alias="complianceScore")
    total_violations: int = Field(alias="totalViolations")
    violations: List[ViolationItem]
    
    model_config = {"populate_by_name": True}

class AnalyzeResponse(BaseModel):
    summary: str
    analysis: str
    recommendations: List[str]
    priority: str  # "CRITICAL", "HIGH", "MEDIUM", "LOW"
    impact: str
    ai_available: bool = True
    model_used: Optional[str] = None
    prompt_tokens: Optional[int] = None
    completion_tokens: Optional[int] = None

class PolicyRuleDraft(BaseModel):
    parameter: str
    expected_value: str
    operator: str = "EQUALS"
    severity: Severity = Severity.MEDIUM
    description: Optional[str] = None
    remediation: Optional[str] = None

class PolicyAssistRequest(BaseModel):
    natural_language_input: str = Field(alias="naturalLanguageInput")
    device_type: str = Field(alias="deviceType")
    
    model_config = {"populate_by_name": True}

class PolicyAssistResponse(BaseModel):
    suggested_policy_name: str
    description: str
    rules: List[PolicyRuleDraft]
    disclaimer: str = "This is an AI-generated draft. Review and approve before making it active."
    ai_available: bool = True

class HealthResponse(BaseModel):
    status: str
    ai_available: bool
    llm_provider: str
    version: str = "1.0.0"
