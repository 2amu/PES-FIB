from django.db import models
from django.conf import settings
from Refugi.models import Refugi

class Mensaje(models.Model):
    refugio = models.ForeignKey(Refugi, on_delete=models.CASCADE, related_name='mensajes')
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='mensajes_chat')
    contenido = models.TextField()
    timestamp = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f'{self.user} en {self.refugio}: {self.contenido[:20]}...'
