import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routes import analyze, policy_assist, health
from app.config import settings

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)

app = FastAPI(
    title="NMS AI Analysis Service",
    description="""AI-powered analysis service for the Network Configuration Compliance System.
    
    IMPORTANT: This service provides EXPLANATIONS and RECOMMENDATIONS only.
    Compliance scores are always determined by the deterministic rule engine in the backend.
    This service never overrides compliance decisions.""",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://backend:8080", "http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"]
)

app.include_router(health.router)
app.include_router(analyze.router)
app.include_router(policy_assist.router)

@app.get("/")
async def root():
    return {"message": "NMS AI Analysis Service", "version": "1.0.0", "docs": "/docs"}
