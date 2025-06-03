from django.core.validators import MinValueValidator, MaxValueValidator
from django.db import models
from django.conf import settings
from django.utils import timezone

class Tag(models.Model):
    """
    Una etiqueta global que pueden dar los usuarios.
    """
    name = models.CharField(max_length=100, unique=True)

    def __str__(self):
        return self.name

class RefugiTag(models.Model):
    """
    Asociación de un Tag a un Refugi por parte de un Usuario.
    Un usuario sólo puede dar cada etiqueta una vez a cada refugio.
    """
    refugi    = models.ForeignKey('Refugi', on_delete=models.CASCADE, related_name='refugi_tags')
    user      = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='user_tags')
    tag       = models.ForeignKey(Tag, on_delete=models.CASCADE, related_name='tag_assignments')
    timestamp = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('refugi','user','tag')
        indexes = [
            models.Index(fields=['refugi','tag']),
        ]

    def __str__(self):
        return f"{self.user} → {self.refugi}: {self.tag}"

class Refugi(models.Model):
    nombre = models.CharField(max_length=255)
    latitud = models.DecimalField(max_digits=9, decimal_places=6)
    longitud = models.DecimalField(max_digits=9, decimal_places=6)
    direccion = models.TextField(blank=True, null=True)
    numero_calle = models.CharField(max_length=20, blank=True, null=True)
    distrito = models.CharField(max_length=255, blank=True, null=True)
    vecindario = models.CharField(max_length=255, blank=True, null=True)
    codigo_postal = models.CharField(max_length=10, blank=True, null=True)
    institucion = models.CharField(max_length=255, blank=True, null=True)
    ultima_modificacion = models.DateTimeField(default=timezone.now, blank=True, null=True)

    valoracion = models.FloatField(default=0, blank=True, null=True)

    imagen_local = models.ImageField(
        upload_to='refugios/',
        blank=True,
        null=True
    )

    horario = models.CharField(
        max_length=255,
        blank=True,
        null=True,
    )


    def __str__(self):
        return self.nombre


class Valoracion(models.Model):
    refugio = models.ForeignKey('Refugi', on_delete=models.CASCADE, related_name='valoraciones')
    puntuacion = models.IntegerField(validators=[MinValueValidator(1), MaxValueValidator(5)])
    fecha = models.DateTimeField(auto_now_add=True)
    user = models.ForeignKey( settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='valoraciones', null = True, blank = True)
    comentario = models.CharField(max_length=255, blank=True, null=True)

    class Meta:
        unique_together = ('refugio', 'user')
        ordering = ['-fecha']

    def __str__(self):
        return f"{self.user} → {self.refugio}: {self.puntuacion}"

