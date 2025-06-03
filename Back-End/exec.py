#!/usr/bin/env python3
import requests
import random

# —————— CONFIGURACIÓN —————— Alonso
API_KEY    = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzUwOTQ4NTM0LCJpYXQiOjE3NDgzNTY1MzQsImp0aSI6IjdmMGQwYmE4OWQ4YjRmMGI5NDEzY2UwNzJkNTllODM3IiwidXNlcl9pZCI6Nn0.LC7zCmCSsJexHiysY1FhVs5runTI8iJ1MJhd472CFFc"      # ← Pon aquí tu token (sin comillas extra)
START_ID   = 1                  # ID inicial de refugio
END_ID     = 399                # ID final de refugio
BASE_URL   = "http://nattech.fib.upc.edu:40430"
TAGS       = [
    "Aigua Freskita", "Bon Ombra", "Aire Condicionat", "Bon Rotllo",
    "Animal Friendly", "Gratuït", "Family Friendly", "Bany Refrescant"
]
# ——————————————————————————————

def main():
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }

    for refugio_id in range(START_ID, END_ID + 1):
        # 1) Valoración aleatoria
        puntuacion = random.randint(1, 5)
        url_valorar = f"{BASE_URL}/api/refugios/{refugio_id}/valorar/{puntuacion}/"
        resp_val = requests.post(url_valorar, headers=headers, json={})
        status_val = "✅" if resp_val.ok else f"❌ {resp_val.status_code}"

        # 2) Asignación de tags aleatoria
        num_tags = random.randint(1, len(TAGS))
        seleccion = random.sample(TAGS, num_tags)
        url_tags = f"{BASE_URL}/api/refugios/{refugio_id}/tags/"
        resp_tags = requests.post(url_tags, headers=headers, json={"tags": seleccion})
        status_tags = "✅" if resp_tags.ok else f"❌ {resp_tags.status_code}"

        print(f"Refugio {refugio_id}: puntuación {puntuacion} {status_val}; "
              f"tags {seleccion} {status_tags}")

if __name__ == "__main__":
    main()
