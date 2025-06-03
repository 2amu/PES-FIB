from django.core.management.base import BaseCommand
from Refugi.models import Refugi

class Command(BaseCommand):
    help = 'Muestra todos los refugios guardados en la base de datos, con su valoración'

    def handle(self, *args, **kwargs):
        refugios = Refugi.objects.all()
        total = refugios.count()

        if not refugios.exists():
            self.stdout.write(self.style.WARNING("⚠️ No hay refugios en la base de datos."))
            return

        self.stdout.write(self.style.SUCCESS(f"🔍 Se encontraron {total} refugios:\n"))

        for refugio in refugios:
            nombre = refugio.nombre or "Sin nombre"
            institucion = refugio.institucion or "Sin institución"
            lat = refugio.latitud
            lon = refugio.longitud
            direccion = refugio.direccion or "Sin dirección"
            valoracion = refugio.valoracion or 0

            self.stdout.write(
                f"🏠 {nombre}, {institucion} | 📍 {lat}, {lon} | 📌 {direccion} | ⭐ Valoración: {valoracion:.2f}"
            )

        self.stdout.write(self.style.SUCCESS(f"\n✅ Hay un total de {total} refugios. Fin de la lista de refugios."))
