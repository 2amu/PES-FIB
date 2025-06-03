# serializers.py
from rest_framework import serializers
from .models import Refugi, Valoracion, Tag
from .utils import calcular_distancia

class RefugiSerializer(serializers.ModelSerializer):
    imagen_local_url = serializers.SerializerMethodField()

    class Meta:
        model = Refugi
        exclude = ['imagen_local']
        # O: fields = ['id', 'nombre', 'latitud', ..., 'imagen_local_url']

    def get_imagen_local_url(self, obj):
        request = self.context.get('request')
        if obj.imagen_local and hasattr(obj.imagen_local, 'url'):
            return request.build_absolute_uri(obj.imagen_local.url)
        return None

class RefugiListadoSerializer(serializers.ModelSerializer):
    imagen_principal = serializers.SerializerMethodField()
    valoracion_media = serializers.FloatField(source='valoracion')
    distancia = serializers.SerializerMethodField()

    class Meta:
        model = Refugi
        fields = [
            'id',
            'nombre',
            'latitud',
            'longitud',
            'horario',
            'valoracion_media',
            'imagen_principal',
            'distancia',
        ]

    def get_imagen_principal(self, obj):
        request = self.context.get('request')
        if obj.imagen_local and hasattr(obj.imagen_local, 'url') and request:
            return request.build_absolute_uri(obj.imagen_local.url)
        return None

    def get_distancia(self, obj):
        user_lat = self.context.get('user_lat')
        user_lon = self.context.get('user_lon')
        if user_lat is None or user_lon is None:
            return None
        distancia_km = calcular_distancia(
            user_lat,
            user_lon,
            float(obj.latitud),
            float(obj.longitud)
        )
        distancia_m = int(distancia_km * 1000)
        return distancia_m

class RefugiConDistanciaSerializer(serializers.ModelSerializer):
    imagen_local_url = serializers.SerializerMethodField()
    distancia = serializers.SerializerMethodField()

    class Meta:
        model = Refugi
        fields = [
            'id', 'nombre', 'latitud', 'longitud',
            'direccion', 'numero_calle', 'distrito',
            'vecindario', 'codigo_postal', 'institucion',
            'ultima_modificacion', 'valoracion',
            'imagen_local_url', 'distancia', 'horario'
        ]

    def get_imagen_local_url(self, obj):
        request = self.context.get('request')
        if obj.imagen_local and hasattr(obj.imagen_local, 'url'):
            return request.build_absolute_uri(obj.imagen_local.url)
        return None

    def get_distancia(self, obj):
        user_lat = self.context.get('user_lat')
        user_lon = self.context.get('user_lon')
        if user_lat is None or user_lon is None:
            return None
        km = calcular_distancia(
            user_lat, user_lon,
            float(obj.latitud), float(obj.longitud)
        )
        return int(km * 1000)

class ValoracionDetailSerializer(serializers.ModelSerializer):
    user = serializers.StringRelatedField(read_only=True)  # llamará a user.__str__ (p.ej. username)
    photo = serializers.SerializerMethodField()

    class Meta:
        model = Valoracion
        fields = [
            'id',
            'user',
            'photo',
            'puntuacion',
            'fecha',
            'comentario'
        ]

    def get_photo(self, obj):
        request = self.context.get('request')
        photo = obj.user.photo
        if photo and request:
            return request.build_absolute_uri(photo.url)
        return None


class TagSerializer(serializers.ModelSerializer):
    class Meta:
        model = Tag
        fields = ['id', 'name']