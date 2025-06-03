import requests
from django.core.management.base import BaseCommand
from django.conf import settings
from Refugi.models import Refugi

class Command(BaseCommand):
    help = "Obtiene los refugios desde la API y los guarda en la base de datos."

    def obtener_datos_api(self):
        """Hace múltiples peticiones a la API para obtener todos los datos paginados."""
        headers = {"Authorization": f"Bearer {settings.API_TOKEN}"}
        base_url = "https://opendata-ajuntament.barcelona.cat"  # Base principal
        url = settings.API_URL  # URL inicial
        todos_los_datos = []  # Lista para acumular todas las URIs

        try:
            while url:  # Mientras haya una página siguiente
                self.stdout.write(f"📌 Consultando API: {url}")
                response = requests.get(url, headers=headers)
                response.raise_for_status()

                data = response.json()

                # Si no hay registros, salimos del bucle
                if not data["result"]["records"]:
                    break

                # Agregar los registros obtenidos
                todos_los_datos.extend(data["result"]["records"])

                # Obtener la URL para la siguiente página
                next_url = data["result"].get("_links", {}).get("next", "")

                if next_url:
                    if not next_url.startswith("http"):
                        if next_url.startswith("/api/"):
                            url = f"{base_url}/data{next_url}"
                        else:
                            next_url = next_url.lstrip("/")
                            url = f"{base_url}/{next_url}"
                    else:
                        # Si es absoluta, asegurarse de que tiene la ruta correcta
                        if "/api/action/" in next_url and "/data/api/" not in next_url:
                            url = next_url.replace("https://opendata-ajuntament.barcelona.cat/api/", "https://opendata-ajuntament.barcelona.cat/data/api/")
                        else:
                            url = next_url
                else:
                    break

        except requests.exceptions.RequestException as e:
            self.stderr.write(f"❌ Error al obtener datos: {str(e)}")
            return []

        return todos_los_datos

    def handle(self, *args, **kwargs):
        """Ejecuta la actualización de datos en la base de datos."""
        datos = self.obtener_datos_api()

        if not datos:
            self.stdout.write("❌ No se han encontrado datos nuevos.")
            return

        guardados = 0
        for item in datos:
            nombre = item.get("name", "Desconocido")
            latitud = item.get("geo_epgs_4326_lat", None)
            longitud = item.get("geo_epgs_4326_lon", None)
            direccion = item.get("addresses_road_name", "No disponible")
            numero_calle = item.get("addresses_start_street_number", "No disponible")
            distrito = item.get("addresses_district_name", None)
            vecindario = item.get("addresses_neighborhood_name", "No disponible")
            codigo_postal = item.get("addresses_zip_code", None)
            institucion = item.get("institution_name", "Sin institución")
            ultima_modificacion = item.get("modified", None)

            # Evitar duplicados por nombre + institución
            if not Refugi.objects.filter(nombre=nombre, institucion=institucion).exists():
                Refugi.objects.create(
                    nombre=nombre,
                    latitud=latitud,
                    longitud=longitud,
                    direccion=direccion,
                    numero_calle=numero_calle,
                    distrito=distrito,
                    vecindario=vecindario,
                    codigo_postal=codigo_postal,
                    institucion=institucion,
                    ultima_modificacion=ultima_modificacion,
                )
                guardados += 1

        self.stdout.write(f"✅ {guardados} nuevos refugios guardados en la base de datos.")

