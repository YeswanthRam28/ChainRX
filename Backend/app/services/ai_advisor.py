import google.generativeai as genai
import os
import json
from typing import List, Dict

class AIAdvisor:
    def __init__(self):
        api_key = os.getenv("GEMINI_API_KEY")
        if not api_key:
            raise ValueError("GEMINI_API_KEY not found in environment")
        genai.configure(api_key=api_key)
        self.model = genai.GenerativeModel('gemini-1.5-flash')

    async def get_best_bid(self, shipment: Dict, bids: List[Dict]) -> Dict:
        """
        Takes shipment details and a list of bids, and returns the best bid with reasoning.
        """
        if not bids:
            return {"error": "No bids available for analysis"}

        # Prepare the prompt for Gemini
        prompt = f"""
        You are an expert medical logistics coordinator for ChainRX. 
        Your task is to analyze multiple delivery bids and pick the most suitable transporter for a hospital's requirement.

        Shipment Requirement:
        - ID: {shipment['id']}
        - Cargo Type: {shipment['cargo_type']}
        - Route: {shipment['pickup_location']} to {shipment['delivery_location']}
        - Budget: {shipment['escrow_amount']} SHM

        Available Bids:
        {json.dumps(bids, indent=2)}

        Analysis Criteria:
        1. Reliability: If the cargo is 'Urgent' or 'Cold Chain', prioritize speed and transporter reputation.
        2. Cost Efficiency: If multiple transporters have similar times, pick the cheaper one.
        3. Risk: Avoid transporters with 0 reputation if better options exist.

        Return your decision in the following JSON format ONLY:
        {{
            "best_bid_id": <bid_id>,
            "reasoning": "<A brief 1-2 sentence explanation of why this bid was chosen>",
            "confidence_score": <A value from 0 to 100>
        }}
        """

        try:
            response = self.model.generate_content(prompt)
            # Use raw text or JSON parsing depending on how Gemini responds
            # Adding JSON cleaning in case Gemini adds markdown ticks
            cleaned_text = response.text.replace('```json', '').replace('```', '').strip()
            return json.loads(cleaned_text)
        except Exception as e:
            return {"error": f"AI Analysis failed: {str(e)}"}
