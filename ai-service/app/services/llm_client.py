import json
import logging
from typing import Optional
from tenacity import retry, stop_after_attempt, wait_exponential

from app.config import settings
from app.models.schemas import AnalyzeRequest, AnalyzeResponse, PolicyAssistRequest, PolicyAssistResponse
from app.prompts.violation_analysis import build_violation_analysis_prompt
from app.prompts.policy_assistant import build_policy_assist_prompt

logger = logging.getLogger(__name__)

class LLMClient:
    def __init__(self):
        self.provider = settings.llm_provider
        self._setup_client()
    
    def _setup_client(self):
        if self.provider == "gemini":
            import google.generativeai as genai
            genai.configure(api_key=settings.gemini_api_key)
            self.gemini_model = genai.GenerativeModel(settings.gemini_model)
        elif self.provider == "openai":
            from openai import OpenAI
            self.openai_client = OpenAI(api_key=settings.openai_api_key)
    
    def is_available(self) -> bool:
        if self.provider == "gemini":
            return bool(settings.gemini_api_key)
        elif self.provider == "openai":
            return bool(settings.openai_api_key)
        return False
    
    @retry(stop=stop_after_attempt(2), wait=wait_exponential(multiplier=1, min=2, max=10))
    def _call_gemini(self, prompt: str) -> tuple[str, int, int]:
        response = self.gemini_model.generate_content(prompt)
        text = response.text
        # Estimate tokens (Gemini SDK doesn't always return usage)
        prompt_tokens = len(prompt.split())
        completion_tokens = len(text.split())
        return text, prompt_tokens, completion_tokens
    
    @retry(stop=stop_after_attempt(2), wait=wait_exponential(multiplier=1, min=2, max=10))
    def _call_openai(self, prompt: str) -> tuple[str, int, int]:
        response = self.openai_client.chat.completions.create(
            model=settings.openai_model,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            max_tokens=1000
        )
        text = response.choices[0].message.content
        prompt_tokens = response.usage.prompt_tokens
        completion_tokens = response.usage.completion_tokens
        return text, prompt_tokens, completion_tokens
    
    def _generate(self, prompt: str) -> tuple[str, int, int]:
        if self.provider == "gemini":
            return self._call_gemini(prompt)
        elif self.provider == "openai":
            return self._call_openai(prompt)
        raise ValueError(f"Unknown LLM provider: {self.provider}")
    
    def _parse_json_response(self, text: str) -> dict:
        # Clean up markdown code blocks if present
        text = text.strip()
        if text.startswith("```json"):
            text = text[7:]
        if text.startswith("```"):
            text = text[3:]
        if text.endswith("```"):
            text = text[:-3]
        return json.loads(text.strip())
    
    def analyze_violations(self, request: AnalyzeRequest) -> AnalyzeResponse:
        """Analyze compliance violations and provide explanations.
        The compliance score is already determined — AI only explains.
        """
        if not self.is_available():
            return self._fallback_analysis(request)
        
        try:
            prompt = build_violation_analysis_prompt(
                request.device_hostname,
                request.device_type,
                request.compliance_score,
                request.violations
            )
            text, prompt_tokens, completion_tokens = self._generate(prompt)
            data = self._parse_json_response(text)
            
            return AnalyzeResponse(
                summary=data.get("summary", ""),
                analysis=data.get("analysis", ""),
                impact=data.get("impact", ""),
                recommendations=data.get("recommendations", []),
                priority=data.get("priority", "MEDIUM"),
                ai_available=True,
                model_used=f"{self.provider}/{settings.gemini_model if self.provider == 'gemini' else settings.openai_model}",
                prompt_tokens=prompt_tokens,
                completion_tokens=completion_tokens
            )
        except Exception as e:
            logger.error(f"LLM call failed: {e}")
            return self._fallback_analysis(request)
    
    def assist_policy_creation(self, request: PolicyAssistRequest) -> PolicyAssistResponse:
        """Convert natural language to structured policy rules draft."""
        if not self.is_available():
            return PolicyAssistResponse(
                suggested_policy_name="AI-Generated Policy Draft",
                description="AI service unavailable. Please define rules manually.",
                rules=[],
                ai_available=False
            )
        
        try:
            prompt = build_policy_assist_prompt(
                request.natural_language_input,
                request.device_type
            )
            text, _, _ = self._generate(prompt)
            data = self._parse_json_response(text)
            
            from app.models.schemas import PolicyRuleDraft, Severity
            rules = [
                PolicyRuleDraft(
                    parameter=r["parameter"],
                    expected_value=str(r["expected_value"]),
                    operator=r.get("operator", "EQUALS"),
                    severity=Severity(r.get("severity", "MEDIUM")),
                    description=r.get("description"),
                    remediation=r.get("remediation")
                )
                for r in data.get("rules", [])
            ]
            
            return PolicyAssistResponse(
                suggested_policy_name=data.get("suggested_policy_name", "AI Draft Policy"),
                description=data.get("description", ""),
                rules=rules,
                ai_available=True
            )
        except Exception as e:
            logger.error(f"Policy assist failed: {e}")
            return PolicyAssistResponse(
                suggested_policy_name="Draft Policy",
                description="AI generation failed. Please define rules manually.",
                rules=[],
                ai_available=False
            )
    
    def _fallback_analysis(self, request: AnalyzeRequest) -> AnalyzeResponse:
        """Provide rule-based fallback analysis when AI is unavailable."""
        critical_violations = [v for v in request.violations if v.severity == "CRITICAL"]
        high_violations = [v for v in request.violations if v.severity == "HIGH"]
        
        priority = "LOW"
        if critical_violations:
            priority = "CRITICAL"
        elif high_violations:
            priority = "HIGH"
        elif request.violations:
            priority = "MEDIUM"
        
        recommendations = []
        for v in request.violations:
            if v.remediation:
                recommendations.append(v.remediation)
            else:
                recommendations.append(f"Fix {v.parameter}: set to {v.expected_value}")
        
        return AnalyzeResponse(
            summary=f"{request.device_hostname} has {request.total_violations} configuration violation(s) with a compliance score of {request.compliance_score:.1f}%.",
            analysis=f"The device has {len(critical_violations)} critical and {len(high_violations)} high severity violations that require attention.",
            impact="Configuration deviations may expose the network to security risks and operational issues.",
            recommendations=recommendations or ["Review all flagged parameters and align with the approved policy."],
            priority=priority,
            ai_available=False,
            model_used="fallback-rule-based"
        )

llm_client = LLMClient()
