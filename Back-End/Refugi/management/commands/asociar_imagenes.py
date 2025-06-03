import os
import requests
import json
import unicodedata
from bs4 import BeautifulSoup
from django.core.files.base import ContentFile
from django.core.management.base import BaseCommand
from Refugi.models import Refugi

class Command(BaseCommand):
    help = 'Busca y descarga imágenes para los 10 primeros refugios, sobrescribiendo y limpiando nombres'

    def limpiar_nombre_archivo(self, texto):
        # Normalizar tildes y eliminar caracteres no alfanuméricos
        texto = unicodedata.normalize('NFKD', texto).encode('ascii', 'ignore').decode('utf-8')
        texto = texto.replace(' ', '_').replace('/', '_').replace('\\', '_')
        return ''.join(c for c in texto if c.isalnum() or c in ['_', '-'])

    def handle(self, *args, **kwargs):
        headers = {
            "User-Agent": "Mozilla/5.0"
        }

        refugios = Refugi.objects.all() #Nomes les 10 priemres en local

        for refugio in refugios:
            query = f"{refugio.nombre or ''} {refugio.institucion or ''} Barcelona"
            url = f"https://www.bing.com/images/search?q={query.replace(' ', '+')}&form=HDRSC2"

            try:
                res = requests.get(url, headers=headers)
                soup = BeautifulSoup(res.text, "html.parser")
                img_tag = soup.find("a", {"class": "iusc"})

                if img_tag and 'm' in img_tag.attrs:
                    meta = json.loads(img_tag['m'])
                    image_url = meta.get("murl")

                    if image_url:
                        print(f"🔗 Imagen para {refugio.nombre}: {image_url}")
                        img_response = requests.get(image_url, headers=headers)

                        if img_response.status_code == 200:
                            nombre_archivo = self.limpiar_nombre_archivo(f"{refugio.nombre}_{refugio.institucion}") + ".jpg"
                            refugio.imagen_local.save(
                                nombre_archivo,
                                ContentFile(img_response.content),
                                save=True
                            )
                            self.stdout.write(self.style.SUCCESS(f"✅ Imagen guardada para {refugio.nombre}"))
                        else:
                            self.stdout.write(self.style.WARNING(f"⚠️ No se pudo descargar la imagen para {refugio.nombre}"))
                    else:
                        self.stdout.write(self.style.WARNING(f"❌ No se encontró imagen URL válida para {refugio.nombre}"))
                else:
                    self.stdout.write(self.style.WARNING(f"❌ No se encontró imagen para {refugio.nombre}"))

            except Exception as e:
                self.stdout.write(self.style.ERROR(f"❌ Error con {refugio.nombre}: {e}"))
