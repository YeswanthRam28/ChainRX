from fastapi import APIRouter, HTTPException
from typing import List, Tuple
from pydantic import BaseModel
from app.services.route_optimizer import RouteOptimizer
import aiohttp
import urllib.parse

router = APIRouter()

class RouteRequest(BaseModel):
    locations: List[str] # List of addresses to visit

class OptimizedRoute(BaseModel):
    original_order: List[str]
    optimized_order: List[str]
    coordinates: List[Tuple[float, float]]

async def geocode(address: str):
    """Geocode address using Nominatim (OSM)."""
    url = f"https://nominatim.openstreetmap.org/search?q={urllib.parse.quote(address)}&format=json&limit=1"
    headers = {'User-Agent': 'ChainRX-Route-Optimizer'}
    async with aiohttp.ClientSession() as session:
        async with session.get(url, headers=headers) as response:
            if response.status == 200:
                data = await response.json()
                if data:
                    return float(data[0]['lat']), float(data[0]['lon'])
    return None

@router.post("/optimize", response_model=OptimizedRoute)
async def optimize_route(request: RouteRequest):
    if len(request.locations) < 2:
        raise HTTPException(status_code=400, detail="At least 2 locations are required")

    # 1. Geocode all locations
    coords_map = {}
    valid_coords = []
    ordered_addresses = []

    for addr in request.locations:
        coord = await geocode(addr)
        if coord:
            coords_map[coord] = addr
            valid_coords.append(coord)
        else:
            # If geocoding fails, we might want to skip or return error
            pass

    if len(valid_coords) < 2:
        raise HTTPException(status_code=400, detail="Could not geocode enough locations")

    # 2. Run Genetic Algorithm
    optimizer = RouteOptimizer(valid_coords)
    optimized_coords = optimizer.optimize()

    # 3. Map back to addresses
    optimized_order = [coords_map[c] for c in optimized_coords]

    return OptimizedRoute(
        original_order=request.locations,
        optimized_order=optimized_order,
        coordinates=optimized_coords
    )
