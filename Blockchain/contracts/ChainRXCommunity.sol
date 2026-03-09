// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

contract ChainRXCommunity {
    enum CommunityType { GENERAL, HOSPITAL, TRANSPORT }
    
    struct Proposal {
        uint256 id;
        address proposer;
        string title;
        string description;
        uint256 votesFor;
        uint256 votesAgainst;
        CommunityType community;
        bool executed;
        uint256 endTime;
    }

    mapping(uint256 => Proposal) public proposals;
    mapping(uint256 => mapping(address => bool)) public hasVoted;
    uint256 public proposalCounter;

    event ProposalCreated(uint256 indexed proposalId, address proposer, string title, CommunityType community);
    event Voted(uint256 indexed proposalId, address voter, bool support, uint256 currentFor, uint256 currentAgainst);

    address public owner;

    modifier onlyOwner() {
        require(msg.sender == owner, "Only owner can call this");
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    /**
     * @dev Create a new proposal in a specific community
     */
    function createProposal(
        address _proposer,
        string memory _title,
        string memory _description,
        CommunityType _community,
        uint256 _durationInDays
    ) public onlyOwner returns (uint256) {
        proposalCounter++;
        proposals[proposalCounter] = Proposal({
            id: proposalCounter,
            proposer: _proposer,
            title: _title,
            description: _description,
            votesFor: 0,
            votesAgainst: 0,
            community: _community,
            executed: false,
            endTime: block.timestamp + (_durationInDays * 1 days)
        });

        emit ProposalCreated(proposalCounter, _proposer, _title, _community);
        return proposalCounter;
    }

    /**
     * @dev Vote on a proposal. Relayer (owner) must verify user's role before calling.
     */
    function vote(uint256 _proposalId, address _voter, bool _support) public onlyOwner {
        Proposal storage p = proposals[_proposalId];
        require(block.timestamp < p.endTime, "Voting ended");
        require(!hasVoted[_proposalId][_voter], "Already voted");

        hasVoted[_proposalId][_voter] = true;

        if (_support) {
            p.votesFor++;
        } else {
            p.votesAgainst++;
        }

        emit Voted(_proposalId, _voter, _support, p.votesFor, p.votesAgainst);
    }

    function getProposal(uint256 _proposalId) public view returns (
        string memory title,
        string memory description,
        uint256 votesFor,
        uint256 votesAgainst,
        CommunityType community,
        uint256 endTime
    ) {
        Proposal memory p = proposals[_proposalId];
        return (p.title, p.description, p.votesFor, p.votesAgainst, p.community, p.endTime);
    }
}
