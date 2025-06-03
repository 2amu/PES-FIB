#!/usr/bin/env python3
import requests

# —————— CONFIGURACIÓN —————— Aleix
API_KEY      = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzUwOTQ5ODg1LCJpYXQiOjE3NDgzNTc4ODUsImp0aSI6IjcxN2U0ZmQyMDVlMTRiMjRiNjBmNTA5MjgyMmM2MjZiIiwidXNlcl9pZCI6N30.42aDHJfimIRbjq9txGJUjpsHBLltqwYRe1h0qaER27c"
BASE_URL     = "http://nattech.fib.upc.edu:40430"
REFUGIO_ID   = 178                          # ← ID del refugio
PUNTUACION   = 4.0                        # ← Puntuación del 1.0 al 5.0
COMENTARIO   = "Espai molt agradable!"   # ← Comentario que quieres enviar
# ——————————————————————————————

def main():
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }

    url = f"{BASE_URL}/api/refugios/{REFUGIO_ID}/valorar/{PUNTUACION}/"
    data = {
        "comentario": COMENTARIO
    }

    response = requests.post(url, headers=headers, json=data)

    if response.ok:
        print("✅ Valoración registrada correctamente:")
        print(response.json())
    else:
        print(f"❌ Error al registrar valoración: {response.status_code}")
        print(response.text)

if __name__ == "__main__":
    main()
