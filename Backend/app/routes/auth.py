from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database.neon_db import SessionLocal
from app.models.db_models import User
from app.models.auth_schemas import UserRegister, UserLogin, Token, UserProfile
from app.auth.auth_handler import get_password_hash, verify_password, create_access_token
from app.auth.role_checker import get_current_user
from datetime import timedelta

router = APIRouter()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@router.post("/register", response_model=Token)
async def register(user: UserRegister, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.email == user.email).first()
    if db_user:
        raise HTTPException(status_code=400, detail="Email already registered")
        
    new_user = User(
        name=user.name,
        email=user.email,
        password_hash=get_password_hash(user.password),
        role=user.role,
        wallet_address=user.wallet_address
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    
    access_token = create_access_token(data={"sub": new_user.email, "role": new_user.role})
    return {"access_token": access_token, "token_type": "bearer", "role": new_user.role}

@router.post("/login", response_model=Token)
async def login(credentials: UserLogin, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == credentials.email).first()
    if not user or not verify_password(credentials.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    access_token = create_access_token(data={"sub": user.email, "role": user.role})
    return {"access_token": access_token, "token_type": "bearer", "role": user.role}

@router.get("/me", response_model=UserProfile)
async def get_me(user: User = Depends(get_current_user)):
    return user
