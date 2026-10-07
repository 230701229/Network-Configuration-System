from fastapi import APIRouter, HTTPException
from app.models.schemas import AnalyzeRequest, AnalyzeResponse
from app.services.llm_client import llm_client
import logging

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/analyze", tags=["Analysis"])

@router.post("", response_model=AnalyzeResponse)
async def analyze_violations(request: AnalyzeRequest):
    """Analyze configuration violations and provide AI-powered explanations.
    
    IMPORTANT: The compliance score is determined by the backend rule engine.
    This endpoint only provides explanations, impact analysis, and recommendations.
    It does NOT change or override the compliance score.
    """
    logger.info(f"Analyzing violations for device: {request.device_hostname}, violations: {len(request.violations)}")
    
    if not request.violations:
        raise HTTPException(status_code=400, detail="No violations provided for analysis")
    
    result = llm_client.analyze_violations(request)
    logger.info(f"Analysis complete for {request.device_hostname}: priority={result.priority}, ai_available={result.ai_available}")
    return result
