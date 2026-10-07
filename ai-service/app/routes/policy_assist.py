from fastapi import APIRouter
from app.models.schemas import PolicyAssistRequest, PolicyAssistResponse
from app.services.llm_client import llm_client
import logging

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/policy-assist", tags=["Policy Assistant"])

@router.post("", response_model=PolicyAssistResponse)
async def assist_policy_creation(request: PolicyAssistRequest):
    """Convert natural language policy requirements to structured rule drafts.
    
    Returns a DRAFT that must be reviewed and approved by an administrator
    before becoming an active policy.
    """
    logger.info(f"Policy assist request for device type: {request.device_type}")
    result = llm_client.assist_policy_creation(request)
    return result
