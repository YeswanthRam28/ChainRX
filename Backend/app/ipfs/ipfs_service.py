import requests
import os
from dotenv import load_dotenv

load_dotenv()

class IPFSService:
    def __init__(self):
        self.pinata_api_key = os.getenv("PINATA_API_KEY")
        self.pinata_secret_api_key = os.getenv("PINATA_SECRET_API_KEY")
        self.base_url = "https://api.pinata.cloud/pinning/pinFileToIPFS"

    def upload_file(self, file_path: str):
        """Upload a file to IPFS using Pinata."""
        try:
            with open(file_path, "rb") as file:
                headers = {
                    "pinata_api_key": self.pinata_api_key,
                    "pinata_secret_api_key": self.pinata_secret_api_key,
                }
                files = {"file": (os.path.basename(file_path), file)}
                response = requests.post(self.base_url, files=files, headers=headers)
                
                if response.status_code == 200:
                    ipfs_hash = response.json()["IpfsHash"]
                    return ipfs_hash
                else:
                    raise Exception(f"Failed to upload to IPFS: {response.text}")
        except Exception as e:
            print(f"IPFS Upload Error: {str(e)}")
            raise e

    def upload_content(self, file_obj, filename: str):
        """Upload an in-memory file content to IPFS."""
        try:
            headers = {
                "pinata_api_key": self.pinata_api_key,
                "pinata_secret_api_key": self.pinata_secret_api_key,
            }
            files = {"file": (filename, file_obj)}
            response = requests.post(self.base_url, files=files, headers=headers)
            
            if response.status_code == 200:
                ipfs_hash = response.json()["IpfsHash"]
                return ipfs_hash
            else:
                raise Exception(f"Failed to upload to IPFS: {response.text}")
        except Exception as e:
            print(f"IPFS Upload Error: {str(e)}")
            raise e
