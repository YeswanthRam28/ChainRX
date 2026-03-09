from sqlalchemy import Column, BigInteger, Text, TIMESTAMP, func, ForeignKey, Float, Numeric
import enum
from app.database.neon_db import Base, engine

class UserRole(str, enum.Enum):
    ADMIN = "admin"
    HOSPITAL = "hospital"
    TRANSPORT = "transport"

class UserStatus(str, enum.Enum):
    PENDING = "pending"
    ACTIVE = "active"
    SUSPENDED = "suspended"

class User(Base):
    __tablename__ = "users"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    name = Column(Text, nullable=False)
    email = Column(Text, unique=True, nullable=False)
    password_hash = Column(Text, nullable=False)
    role = Column(Text, nullable=False) # admin, hospital, transport
    wallet_address = Column(Text, nullable=True) # Nullable — not unique since not all users have wallets
    reputation_score = Column(Float, default=5.0)
    account_status = Column(Text, default="active")
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())

class DB_Shipment(Base):
    __tablename__ = "shipments"
    id = Column(BigInteger, primary_key=True)
    hospital_id = Column(BigInteger, ForeignKey("users.id"), nullable=True)
    transporter_id = Column(BigInteger, ForeignKey("users.id"), nullable=True)
    pickup_location = Column(Text, nullable=False)
    delivery_location = Column(Text, nullable=False)
    cargo_type = Column(Text, nullable=False)
    escrow_amount = Column(Numeric, nullable=False)
    status = Column(Text, nullable=False)
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())

class DB_Bid(Base):
    __tablename__ = "bids"
    id = Column(BigInteger, primary_key=True, autoincrement=True)
    shipment_id = Column(BigInteger, ForeignKey("shipments.id"))
    transporter_id = Column(BigInteger, ForeignKey("users.id"), nullable=True)
    bidder = Column(Text, nullable=True) # Wallet address or user name
    bid_amount = Column(Numeric, nullable=False)
    estimated_time = Column(Text, nullable=False)
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now())

# Create tables on startup
Base.metadata.create_all(bind=engine)
