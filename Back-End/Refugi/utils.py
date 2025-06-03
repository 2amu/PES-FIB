# Refugi/utils.py

from math import radians, sin, cos, sqrt, atan2

def calcular_distancia(lat1, lon1, lat2, lon2):
    """
    Fórmula de Haversine para distancia en km entre dos puntos.
    """
    R = 6371
    dlat = radians(lat2 - lat1)
    dlon = radians(lon2 - lon1)
    a = sin(dlat/2)**2 + cos(radians(lat1))*cos(radians(lat2))*sin(dlon/2)**2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
