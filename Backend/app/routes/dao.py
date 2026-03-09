from fastapi import APIRouter, Depends, HTTPException
from app.models.schemas import ProposalCreate, ProposalVote, ProposalInfo
from app.services.blockchain_service import BlockchainService
from app.blockchain.web3_connector import Web3Connector
from app.services.database_service import DatabaseService
from app.auth.role_checker import RoleChecker, get_current_user
from app.models.db_models import User
from app.database.neon_db import SessionLocal
import os

router = APIRouter()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def get_blockchain_service():
    return BlockchainService(Web3Connector())

# Move these to BlockchainService later for better architecture
def get_dao_contract(connector: Web3Connector):
    address = os.getenv("DAO_CONTRACT_ADDRESS")
    if not address:
        raise HTTPException(status_code=500, detail="DAO contract address not configured")
    return connector.get_contract(address=address, abi_filename="dao_abi.json")

@router.post("/proposals", status_code=201)
async def create_proposal(
    proposal: ProposalCreate,
    user: User = Depends(get_current_user),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """Create a new DAO proposal."""
    connector = service.web3
    dao = get_dao_contract(connector)
    
    # Enforce roles for specific communities
    if proposal.community == 1 and user.role != "hospital":
        raise HTTPException(status_code=403, detail="Only hospitals can create hospital proposals")
    if proposal.community == 2 and user.role != "transport":
        raise HTTPException(status_code=403, detail="Only transporters can create transport proposals")
        
    try:
        function_call = dao.functions.createProposal(
            connector.w3.to_checksum_address(user.wallet_address),
            proposal.title,
            proposal.description,
            proposal.community,
            proposal.durationDays
        )
        tx_hash = connector.sign_and_send_transaction(function_call)
        return {"tx_hash": tx_hash}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.get("/proposals")
async def get_proposals(
    community: int = 0, # 0=GENERAL, 1=HOSPITAL, 2=TRANSPORT
    user: User = Depends(get_current_user),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """List proposals for a community."""
    connector = service.web3
    dao = get_dao_contract(connector)
    
    try:
        count = dao.functions.proposalCounter().call()
        proposals = []
        
        community_names = {0: "General", 1: "Hospital", 2: "Transport"}
        
        # In a real app, we would paginate and use an indexer. 
        # For this demo, we iterate backwards.
        for i in range(count, 0, -1):
            p_data = dao.functions.proposals(i).call()
            # Proposal struct: id, proposer, title, description, votesFor, votesAgainst, community, executed, endTime
            if p_data[6] == community:
                has_voted = dao.functions.hasVoted(i, connector.w3.to_checksum_address(user.wallet_address)).call()
                proposals.append(ProposalInfo(
                    id=p_data[0],
                    proposer=p_data[1],
                    title=p_data[2],
                    description=p_data[3],
                    votesFor=p_data[4],
                    votesAgainst=p_data[5],
                    community=community_names.get(p_data[6], "Unknown"),
                    endTime=p_data[8],
                    hasVoted=has_voted
                ))
            if len(proposals) >= 20: break
                
        return proposals
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/proposals/vote")
async def vote_proposal(
    vote: ProposalVote,
    user: User = Depends(get_current_user),
    service: BlockchainService = Depends(get_blockchain_service)
):
    """Submit a vote to a proposal."""
    connector = service.web3
    dao = get_dao_contract(connector)
    
    # Check proposal community and user role
    p_data = dao.functions.proposals(vote.proposalId).call()
    community = p_data[6]
    
    if community == 1 and user.role != "hospital":
        raise HTTPException(status_code=403, detail="Only hospitals can vote in this DAO")
    if community == 2 and user.role != "transport":
        raise HTTPException(status_code=403, detail="Only transporters can vote in this DAO")
        
    try:
        function_call = dao.functions.vote(
            vote.proposalId,
            connector.w3.to_checksum_address(user.wallet_address),
            vote.support
        )
        tx_hash = connector.sign_and_send_transaction(function_call)
        return {"tx_hash": tx_hash}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
