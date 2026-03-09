from web3 import Web3
import os
import json
from dotenv import load_dotenv

load_dotenv()

class Web3Connector:
    def __init__(self):
        self.rpc_url = os.getenv("POLYGON_RPC_URL", "https://rpc-mumbai.maticvigil.com")
        self.w3 = Web3(Web3.HTTPProvider(self.rpc_url))
        self.contract_address = self.w3.to_checksum_address(os.getenv("CONTRACT_ADDRESS"))
        self.private_key = os.getenv("PRIVATE_KEY")
        self.account = self.w3.eth.account.from_key(self.private_key)
        
        # Load ABI
        abi_path = os.path.join(os.path.dirname(__file__), "abi.json")
        with open(abi_path, "r") as f:
            self.abi = json.load(f)
            
        self.contract = self.w3.eth.contract(address=self.contract_address, abi=self.abi)

    def is_connected(self):
        return self.w3.is_connected()

    def get_contract(self):
        return self.contract

    def sign_and_send_transaction(self, function_call, value=0):
        # Build transaction
        nonce = self.w3.eth.get_transaction_count(self.account.address)
        
        # Estimate gas for this specific call
        gas_estimate = function_call.estimate_gas({
            'from': self.account.address,
            'value': value
        })
        
        transaction = function_call.build_transaction({
            'from': self.account.address,
            'nonce': nonce,
            'gas': int(gas_estimate * 1.2), # Add 20% gas buffer
            'gasPrice': self.w3.eth.gas_price,
            'value': value
        })
        
        # Sign transaction
        signed_txn = self.w3.eth.account.sign_transaction(transaction, private_key=self.private_key)
        
        # Send transaction
        tx_hash = self.w3.eth.send_raw_transaction(signed_txn.raw_transaction)
        
        # Return transaction hash
        return self.w3.to_hex(tx_hash)

    def wait_for_receipt(self, tx_hash):
        return self.w3.eth.wait_for_transaction_receipt(tx_hash)
