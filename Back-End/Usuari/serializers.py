from django.contrib.auth import get_user_model
from rest_framework import serializers
from Refugi.serializers import RefugiSerializer
from dj_rest_auth.serializers import UserDetailsSerializer
from .models import Usuari


Usuari = get_user_model()
User = get_user_model()

class UsuariSerializer(serializers.ModelSerializer):
    """
    Para creación y listado de usuarios.
    Incluye el campo 'password' como write_only.
    """
    password = serializers.CharField(write_only=True)

    class Meta:
        model = Usuari
        fields = ['id', 'username', 'email', 'telefono', 'direccion', 'password', 'photo', 'descripcion', 'idioma']

    def create(self, validated_data):
        # Quizá podría venir el password en validated_data
        pwd = validated_data.pop('password')
        user = Usuari(**validated_data)
        user.set_password(pwd)
        user.save()
        return user

class UsuariDetailSerializer(UsuariSerializer):
    """
    Para el detalle de usuario: añade sus favoritos.
    """
    favorites = RefugiSerializer(many=True, read_only=True)

    class Meta(UsuariSerializer.Meta):
        fields = UsuariSerializer.Meta.fields + ['favorites']

class AddFavoriteSerializer(serializers.Serializer):
    refugi_id = serializers.IntegerField()

class UserProfilePhotoSerializer(serializers.ModelSerializer):
    photo = serializers.ImageField(use_url=True)
    class Meta:
        model = Usuari
        fields = ['photo']

class IdiomaSerializer(serializers.ModelSerializer):
    class Meta:
        model = Usuari
        fields = ['idioma']


class CustomUserDetailsSerializer(UserDetailsSerializer):
    """
    Extiende al serializer por defecto de dj-rest-auth añadiendo 'idioma'.
    """
    idioma = serializers.CharField(allow_blank=True, required=False)

    class Meta(UserDetailsSerializer.Meta):
        model = User
        # hereda todos los campos por defecto (+ first_name, last_name…)
        fields = UserDetailsSerializer.Meta.fields + ("idioma",)

class GoogleAuthSerializer(serializers.Serializer):
    id_token = serializers.CharField()