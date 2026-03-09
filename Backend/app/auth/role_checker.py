from fastapi import Request, HTTPException, Depends, Security
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.auth.auth_handler import decode_token
from app.database.neon_db import SessionLocal
from app.models.db_models import User
from typing import List

security = HTTPBearer()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

async def get_current_user(auth: HTTPAuthorizationCredentials = Security(security), db: SessionLocal = Depends(get_db)):
    payload = decode_token(auth.credentials)
    if not payload:
        raise HTTPException(status_code=403, detail="Invalid token or expired token")
    
    email = payload.get("sub")
    if not email:
        raise HTTPException(status_code=403, detail="Could not validate credentials")
    
    user = db.query(User).filter(User.email == email).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    if user.account_status == "suspended":
        raise HTTPException(status_code=403, detail="Your account has been suspended")
        
    return user

class RoleChecker:
    def __init__(self, allowed_roles: List[str]):
        self.allowed_roles = allowed_roles

    def __call__(self, user: User = Depends(get_current_user)):
        if user.role not in self.allowed_roles:
            raise HTTPException(
                status_code=403, 
                detail=f"Access denied: This action requires one of the following roles: {self.allowed_roles}"
            )
        return user
