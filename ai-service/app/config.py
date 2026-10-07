from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    llm_provider: str = "gemini"  # "gemini" or "openai"
    gemini_api_key: str = ""
    gemini_model: str = "gemini-1.5-flash"
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"
    ai_service_port: int = 8000
    ai_service_host: str = "0.0.0.0"
    backend_api_key: str = "internal-secret"
    
    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()
