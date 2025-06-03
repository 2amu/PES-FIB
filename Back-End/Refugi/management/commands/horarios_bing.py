import time
import json
import unicodedata
import urllib.parse
import requests
from bs4 import BeautifulSoup
from django.core.management.base import BaseCommand
from Refugi.models import Refugi
import re


class Command(BaseCommand):
    help = 'Busca y asocia el horario de los miércoles desde Bing para los refugios'

    def limpiar_nombre_archivo(self, texto):
        texto = unicodedata.normalize('NFKD', texto).encode('ascii', 'ignore').decode('utf-8')
        texto = texto.replace(' ', '_').replace('/', '_').replace('\\', '_')
        return ''.join(c for c in texto if c.isalnum() or c in ['_', '-'])

    def handle(self, *args, **kwargs):
        headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36"
        }

        refugios = Refugi.objects.filter(horario__isnull=True)  # solo los que no tienen horario

        for refugio in refugios:
            query = f"{refugio.nombre or ''} {refugio.direccion or ''} Barcelona horario"
            url = f"https://www.bing.com/search?q={urllib.parse.quote_plus(query)}"

            try:
                res = requests.get(url, headers=headers)
                soup = BeautifulSoup(res.text, "html.parser")

                # Buscar bloques de texto que contengan "miércoles"
                posibles = soup.find_all("li") + soup.find_all("div")
                horario_miercoles = None

                for bloque in posibles:
                    texto = bloque.get_text(" ", strip=True)
                    if "miércoles" in texto.lower():
                        match = re.search(r"[Mm]iércoles\s*[:\-]?\s*(\d{1,2}:\d{2}\s*[–-]\s*\d{1,2}:\d{2})", texto)
                        if match:
                            horario_miercoles = match.group(1)
                            break

                if horario_miercoles:
                    refugio.horario = horario_miercoles  # solo el tramo horario
                    self.stdout.write(self.style.SUCCESS(f"✅ {refugio.nombre} -> {horario_miercoles}"))
                else:
                    horario_miercoles = "09:00 - 17:00"
                    refugio.horario = horario_miercoles
                    self.stdout.write(self.style.WARNING(f"⚠️ No se encontró horario de miércoles para {refugio.nombre}, se asignó por defecto: {horario_miercoles}"))

                refugio.save()

            except Exception as e:
                self.stdout.write(self.style.ERROR(f"❌ Error con {refugio.nombre}: {e}"))

            time.sleep(1)  # evitar bloqueos
