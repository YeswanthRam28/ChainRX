from pydantic import BaseModel, EmailStr
from typing import Optional, List

class UserRegister(BaseModel):
    name: str
    email: EmailStr
    password: str
    role: str # admin, hospital, transport
    wallet_address: Optional[str] = None

class UserLogin(BaseModel):
    email: EmailStr
    password: str

class Token(BaseModel):
    access_token: str
    token_type: str
    role: str

class UserProfile(BaseModel):
    id: int
    name: str
    email: str
    role: str
    wallet_address: Optional[str]
    reputation_score: float
    account_status: str
