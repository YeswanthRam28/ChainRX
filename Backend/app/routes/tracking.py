from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from typing import Dict, List
import json

router = APIRouter()

# Store active connections: {shipment_id: [websocket1, websocket2]}
active_tracking: Dict[int, List[WebSocket]] = {}

@router.websocket("/ws/tracking/{shipment_id}")
async def websocket_tracking(websocket: WebSocket, shipment_id: int):
    await websocket.accept()
    
    if shipment_id not in active_tracking:
        active_tracking[shipment_id] = []
    
    active_tracking[shipment_id].append(websocket)
    
    try:
        while True:
            # Receive location update from Transporter
            data = await websocket.receive_text()
            location_data = json.loads(data)
            
            # Broadcast to everyone else (Hospitals) tracking this shipment
            for client in active_tracking[shipment_id]:
                if client != websocket:
                    try:
                        await client.send_text(json.dumps(location_data))
                    except Exception:
                        # Handle stale connections
                        active_tracking[shipment_id].remove(client)
                        
    except WebSocketDisconnect:
        active_tracking[shipment_id].remove(websocket)
        if not active_tracking[shipment_id]:
            del active_tracking[shipment_id]
