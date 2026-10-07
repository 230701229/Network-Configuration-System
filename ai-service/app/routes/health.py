from fastapi import APIRouter
from app.models.schemas import HealthResponse
from app.services.llm_client import llm_client
from app.config import settings

router = APIRouter(tags=["Health"])

@router.get("/health", response_model=HealthResponse)
async def health_check():
    return HealthResponse(
        status="healthy",
        ai_available=llm_client.is_available(),
        llm_provider=settings.llm_provider
    )
