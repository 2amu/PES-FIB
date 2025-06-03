from django.contrib.auth.models import AbstractUser, Group, Permission
from django.db import models



class Usuari(AbstractUser):
    telefono = models.CharField(max_length=15, blank=True, null=True)
    direccion = models.TextField(blank=True, null=True)
    descripcion = models.CharField(max_length=255, blank=True, null=True)
    photo = models.ImageField(upload_to='users_photos/', blank=True, null=True)
    idioma = models.CharField(max_length=255, blank=True, null=True)

    favorites = models.ManyToManyField(
        'Refugi.Refugi',
        related_name='favorited_by',
        blank=True,
    )

    groups = models.ManyToManyField(
        Group,
        related_name='usuari_groups',
        blank=True
    )
    user_permissions = models.ManyToManyField(
        Permission,
        related_name='usuari_permissions',
        blank=True
    )

    class Meta:
        verbose_name = "usuari"
        verbose_name_plural = "usuaris"

    def __str__(self):
        return self.username
