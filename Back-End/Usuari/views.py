from rest_framework import generics, status
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.decorators import api_view, permission_classes, parser_classes
from rest_framework.parsers import MultiPartParser, FormParser
from django.shortcuts import get_object_or_404
from drf_yasg import openapi
from drf_yasg.utils import swagger_auto_schema
from .serializers import CustomUserDetailsSerializer, GoogleAuthSerializer
from dj_rest_auth.views import UserDetailsView

from django.contrib.auth import logout as django_logout
from rest_framework.views import APIView
from rest_framework import status, permissions
from django.conf import settings

from google.oauth2 import id_token
from google.auth.transport import requests
from rest_framework.views import APIView
from rest_framework.response import Response
from django.contrib.auth import get_user_model
from rest_framework_simplejwt.tokens import RefreshToken
from django.core.files.base import ContentFile
import requests as external_requests
import base64


from Refugi.models import Refugi
from Refugi.serializers import RefugiSerializer
from .serializers import (
    UsuariSerializer,
    UsuariDetailSerializer,
    UserProfilePhotoSerializer,
    IdiomaSerializer,
)

Usuari = get_user_model()

class CustomUserDetailsView(UserDetailsView):
    serializer_class = CustomUserDetailsSerializer

class ListCreateUsuariView(generics.ListCreateAPIView):
    """
    GET  /api/usuarios/         -> Lista todos los usuarios
    POST /api/usuarios/crear/   -> Crea un nuevo usuario (con contraseña)
    """
    queryset = Usuari.objects.all()
    permission_classes = [AllowAny]
    serializer_class = UsuariSerializer


class UsuariDetailView(generics.RetrieveAPIView):
    """
    GET /api/usuarios/<pk>/  -> Detalle de un usuario (incluye favorites)
    """
    queryset = Usuari.objects.all()
    serializer_class = UsuariDetailSerializer
    permission_classes = [IsAuthenticated]


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def list_favorite_shelters(request):
    user = request.user
    favoritos = user.favorites.all()
    serializer = RefugiSerializer(favoritos, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def add_favorite_shelter(request, refugi_id):
    user = request.user
    refugi = get_object_or_404(Refugi, id=refugi_id)

    if refugi not in user.favorites.all():
        user.favorites.add(refugi)
        return Response({"message": "Refugio añadido a favoritos"})
    else:
        return Response({"message": "El refugio ya está en favoritos"})


@api_view(['DELETE'])
@permission_classes([IsAuthenticated])
def remove_favorite_shelter(request, refugi_id):
    user = request.user
    refugi = get_object_or_404(Refugi, id=refugi_id)

    if refugi in user.favorites.all():
        user.favorites.remove(refugi)
        return Response({"message": "Refugio eliminado de favoritos"})
    else:
        return Response({"message": "El refugio no estaba en favoritos"})


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_user_photo(request, user_id):
    try:
        user = Usuari.objects.get(pk=user_id)
    except Usuari.DoesNotExist:
        return Response({"detail": "Usuario no encontrado."}, status=status.HTTP_404_NOT_FOUND)

    serializer = UserProfilePhotoSerializer(user, context={'request': request})
    return Response(serializer.data)


@api_view(['DELETE'])
@permission_classes([IsAuthenticated])
def delete_user_photo(request):
    user = request.user
    # Elimina la foto del sistema de archivos y del campo
    if user.photo:
        user.photo.delete(save=True)
        user.photo = None
        user.save()
        return Response({"detail": "Foto eliminada correctamente."}, status=status.HTTP_200_OK)
    return Response({"detail": "No había foto para eliminar."}, status=status.HTTP_400_BAD_REQUEST)


@swagger_auto_schema(
    method='put',
    manual_parameters=[
        openapi.Parameter(
            'photo',
            in_=openapi.IN_FORM,
            type=openapi.TYPE_FILE,
            required=True,
            description="Foto del usuario"
        )
    ]
)
@api_view(['PUT'])
@permission_classes([IsAuthenticated])
@parser_classes([MultiPartParser, FormParser])
def update_user_photo(request):
    user = request.user
    serializer = UserProfilePhotoSerializer( user, data=request.data, partial=True, context={'request': request} )
    if serializer.is_valid():
        serializer.save()
        return Response(serializer.data)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@api_view(['PATCH'])
@permission_classes([IsAuthenticated])
def update_idioma(request, idioma):
    """
    PATCH /api/usuarios/cambiar_idioma/{idioma}/
    Cambia el idioma del usuario autenticado al valor pasado en la URL.
    """
    # simple validation opcional
    if not isinstance(idioma, str) or not idioma:
        return Response(
            {"detail": "El parámetro de URL 'idioma' no es válido."},
            status=status.HTTP_400_BAD_REQUEST
        )

    user = request.user
    user.idioma = idioma
    user.save(update_fields=['idioma'])
    return Response({"idioma": user.idioma}, status=status.HTTP_200_OK)


class CustomUserDetailsView(UserDetailsView):
    serializer_class = CustomUserDetailsSerializer


class LogoutNoRefreshView(APIView):
    """
    Logout simplificado: limpia la sesión de Django y borra
    las cookies de access + refresh sin pedir nada en el body.
    """
    permission_classes = [permissions.IsAuthenticated]

    def post(self, request):
        # 1) Limpia la sesión de Django (por si acaso)
        django_logout(request)

        # 2) Construye respuesta y borra cookies
        resp = Response({"detail": "Sesión cerrada correctamente."},
                        status=status.HTTP_200_OK)
        # Nombre de la cookie de access token
        access_cookie = settings.REST_AUTH.get('JWT_AUTH_COOKIE')
        # Nombre de la cookie de refresh token
        refresh_cookie = settings.REST_AUTH.get('JWT_AUTH_REFRESH_COOKIE')

        if access_cookie:
            resp.delete_cookie(access_cookie)
        if refresh_cookie:
            resp.delete_cookie(refresh_cookie)

        return resp

GOOGLE_CLIENT_ID = "344007678371-6bfbcu57ls87urkrnppvgordvdh0sati.apps.googleusercontent.com"

class GoogleAuthView(APIView):
    permission_classes = [AllowAny]

    @swagger_auto_schema(request_body=GoogleAuthSerializer)
    def post(self, request):
        token = request.data.get("id_token")
        if not token:
            return Response({"error": "No se proporcionó el id_token"}, status=400)

        try:
            idinfo = id_token.verify_oauth2_token(token, requests.Request(), GOOGLE_CLIENT_ID)

            sub = idinfo["sub"]
            email = idinfo["email"]
            name = idinfo.get("name", "")
            picture_url = idinfo.get("picture")

            user, created = Usuari.objects.get_or_create(
                email=email,
                defaults={
                    "username": email.split('@')[0],
                    "first_name": name,
                    "descripcion": "Cuenta creada con Google"
                }
            )

            # Si es nuevo, descarga imagen de perfil
            if created and picture_url:
                try:
                    image_response = external_requests.get(picture_url)
                    if image_response.status_code == 200:
                        user.photo.save(
                            f"{user.username}_google.jpg",
                            ContentFile(image_response.content),
                            save=True
                        )
                except Exception as e:
                    print("Error descargando imagen:", e)

            refresh = RefreshToken.for_user(user)

            return Response({
                "user_id": user.id,
                "access": str(refresh.access_token),
                "refresh": str(refresh),
            })

        except ValueError:
            return Response({"error": "id_token inválido"}, status=400)

