import requests
from django.core.files.base import ContentFile
from django.core.files.storage import default_storage
from django.http import JsonResponse
from django.conf import settings
from .models import Refugi, Valoracion, Tag, RefugiTag
from rest_framework.decorators import api_view, permission_classes
from math import radians, sin, cos, sqrt, atan2
from rest_framework.response import Response
from .serializers import RefugiSerializer, RefugiListadoSerializer, RefugiConDistanciaSerializer, ValoracionDetailSerializer,TagSerializer
from rest_framework.permissions import AllowAny, IsAuthenticated, IsAuthenticatedOrReadOnly
from django.shortcuts import get_object_or_404
from rest_framework import status
from django.db.models import Count
from django.utils.timezone import now
from drf_yasg.utils import swagger_auto_schema
from django.db.models import Q, Avg
from drf_yasg import openapi
import unicodedata
import re
from .utils import calcular_distancia
from rest_framework import generics, permissions



@api_view(['GET'])
@permission_classes([AllowAny])
def listar_refugios(request):
    """
    Devuelve una lista de refugios, con opciones de filtrado por nombre, institucion o distrito.
    ex: http://localhost:8000/api/refugios/?nombre=Barcelona
        http://localhost:8000/api/refugios/listar/
    """
    nombre = request.GET.get("nombre")
    distrito = request.GET.get("distrito")
    institucion = request.GET.get("institucion")

    refugios = Refugi.objects.all()

    if nombre:
        refugios = refugios.filter(nombre__icontains=nombre)

    if distrito:
        refugios = refugios.filter(distrito__icontains=distrito)

    if institucion:
        refugios = refugios.filter(institucion__icontains=institucion)

    refugios = refugios.order_by('id')
    serializer = RefugiSerializer(refugios, many=True, context={'request': request})
    return Response(serializer.data)


@swagger_auto_schema(
    method='get',
    operation_description="Listado de refugios con todos sus campos y distancia desde la posición dada",
    responses={200: RefugiConDistanciaSerializer(many=True)},
    manual_parameters=[
        openapi.Parameter(
            'lat',
            openapi.IN_PATH,
            description="Latitud del punto de referencia",
            type=openapi.TYPE_NUMBER
        ),
        openapi.Parameter(
            'lon',
            openapi.IN_PATH,
            description="Longitud del punto de referencia",
            type=openapi.TYPE_NUMBER
        ),
    ]
)
@api_view(['GET'])
@permission_classes([AllowAny])
def listar_refugios_cercania(request, lat, lon):
    """
    GET /api/refugios/listar_cercania/<lat>/<lon>/
    Devuelve todos los refugios con todos sus campos + imagen_local_url + distancia (m).
    """
    try:
        user_lat = float(lat)
        user_lon = float(lon)
    except ValueError:
        return Response({'error': 'lat y lon deben ser números'}, status=400)

    qs = Refugi.objects.all().order_by('id')
    todos = list(qs)

    def clave_dist(r):
        return calcular_distancia(
            user_lat, user_lon,
            float(r.latitud), float(r.longitud)
        )

    ordenados = sorted(todos, key=clave_dist)

    serializer = RefugiConDistanciaSerializer(
        ordenados,
        many=True,
        context={
            'request': request,
            'user_lat': user_lat,
            'user_lon': user_lon,
        }
    )
    return Response(serializer.data)

@swagger_auto_schema(
    method='put',
    operation_description="Permite actualizar un refugio específico por su ID.",
    request_body=RefugiSerializer,
    responses={
        200: RefugiSerializer(),
        400: "Error de validación",
        404: "Refugio no encontrado",
    },
    manual_parameters=[
        openapi.Parameter(
            'id',
            openapi.IN_PATH,
            description="ID del refugio a actualizar",
            type=openapi.TYPE_INTEGER
        )
    ]
)

@api_view(['PUT'])
@permission_classes([AllowAny])
def edit_shelter(request, id):
    """
   Permite actualizar un refugio específico
   """

    try:
        refugio = Refugi.objects.get(id=id)
    except Refugi.DoesNotExist:
        return Response({"error": "Refugio no encontrado"}, status=404)

    serializer = RefugiSerializer(refugio, data=request.data, partial=True, context={'request': request})     # `partial=True` permite actualizar solo algunos campos
    if serializer.is_valid():
        serializer.save()  # Guardar los cambios
        return Response(RefugiSerializer(refugio, context={'request': request}).data)
    return Response(serializer.errors, status=400)

@swagger_auto_schema(
    method='post',
    operation_description="Crea uno o varios refugios de golpe",
    request_body=RefugiSerializer(many=True),  # indicamos que puede venir una lista
    responses={
        201: RefugiSerializer(many=True),
        400: "Solicitud inválida"
    }
)
@api_view(['POST'])
@permission_classes([AllowAny])
def add_shelters(request):
    """
    POST /api/refugios/add_batch/
    Si el body es un objeto, crea un refugio.
    Si el body es una lista, crea todos los refugios de la lista.
    """
    data = request.data

    # añadimos timestamp a cada item
    if isinstance(data, list):
        for obj in data:
            obj.setdefault("ultima_modificacion", now())
    else:
        data.setdefault("ultima_modificacion", now())

    many = isinstance(data, list)
    serializer = RefugiSerializer(data=data, many=many, context={'request': request})
    if serializer.is_valid():
        # .save() en modo many=True invoca create() por cada elemento
        created = serializer.save()
        # serializamos de nuevo para devolver URLs y campos calculados
        out = RefugiSerializer(created, many=many, context={'request': request})
        return Response(out.data, status=status.HTTP_201_CREATED)

    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


@swagger_auto_schema(
    method='post',
    request_body=RefugiSerializer,
    responses={201: 'Refugio creado exitosamente', 400: 'Solicitud inválida'}
)

@api_view(['POST'])
@permission_classes([AllowAny])
def add_shelter(request):
    """
    Permite crear un nuevo refugio.
    """
    data = request.data.copy()
    data["ultima_modificacion"] = now()

    serializer = RefugiSerializer(data=data, context={'request': request})
    if serializer.is_valid():
        serializer.save()
        return Response(RefugiSerializer(serializer.instance, context={'request': request}).data)

    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@swagger_auto_schema(
    method='delete',
    operation_description="Elimina un refugio por su ID.",
    responses={
        200: openapi.Response("Refugio eliminado correctamente"),
        404: openapi.Response("Refugio no encontrado"),
    },
    manual_parameters=[
        openapi.Parameter(
            'id',
            openapi.IN_PATH,
            description="ID del refugio a eliminar",
            type=openapi.TYPE_INTEGER
        )
    ]
)

@api_view(['DELETE'])
@permission_classes([AllowAny])
def delete_shelter(request, id):
    """
    Permite eliminar un refugio específico buscándolo por su ID.
    """
    refugio = get_object_or_404(Refugi, id=id)
    refugio.delete()
    return Response({"mensaje": f"Refugio con ID {id} eliminado correctamente."}, status=status.HTTP_200_OK)

@swagger_auto_schema(
    method='get',
    operation_description="Obtener un refugio por su ID",
    responses={200: RefugiSerializer, 404: 'Refugio no encontrado'},
    manual_parameters=[
        openapi.Parameter(
            'id',
            openapi.IN_PATH,
            description="ID del refugio a obtener",
            type=openapi.TYPE_INTEGER
        )
    ]
)

@api_view(['GET'])
@permission_classes([AllowAny])
def get_shelter(request, id):
    """
    Obtiene un refugio por su ID.
    """
    try:
        shelter = Refugi.objects.get(id=id)
        serializer = RefugiSerializer(shelter, context={'request': request})
        return Response(serializer.data, status=status.HTTP_200_OK)
    except Refugi.DoesNotExist:
        return Response({"error": "Refugio no encontrado"}, status=status.HTTP_404_NOT_FOUND)


@api_view(['GET'])
@permission_classes([AllowAny])
def buscar_refugios(request, palabras):
    """
    Busca refugios por palabras clave en el nombre o institución desde la URL.
    Ej: /api/refugios/buscar/Barcelona-Cruz-Roja
    """
    if not palabras:
        return Response({"error": "No se proporcionó ninguna palabra clave."}, status=400)

    palabras_lista = palabras.strip().split('-')  # convertimos el string a lista

    filtros = Q()
    for palabra in palabras_lista:
        filtros |= Q(nombre__icontains=palabra) | Q(institucion__icontains=palabra)

    resultados = Refugi.objects.filter(filtros)
    serializer = RefugiSerializer(resultados, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def valorar_refugio(request, refugio_id, puntuacion):
    """
    POST /api/refugios/{refugio_id}/valorar/{puntuacion}/
    Guarda o actualiza la valoración del usuario, leyendo el comentario desde JSON.
    """
    refugio = get_object_or_404(Refugi, id=refugio_id)
    try:
        puntuacion = float(puntuacion)
        if not 1.0 <= puntuacion <= 5.0:
            raise ValueError
    except ValueError:
        return Response({'error': 'Puntuación inválida (1.0 a 5.0)'}, status=400)

    comentario = request.data.get('comentario', '').strip()

    obj, creado = Valoracion.objects.update_or_create(
        refugio=refugio,
        user=request.user,
        defaults={
            'puntuacion': puntuacion,
            'comentario': comentario,
        }
    )

    promedio = refugio.valoraciones.aggregate(
        avg=Avg('puntuacion')
    )['avg'] or 0
    refugio.valoracion = promedio
    refugio.save()

    return Response({
        'mensaje': 'Valoración registrada' if creado else 'Valoración actualizada',
        'nueva_media': promedio
    }, status=200)

def limpiar_nombre_archivo(nombre):
    nombre = unicodedata.normalize('NFKD', nombre).encode('ascii', 'ignore').decode('utf-8')
    nombre = re.sub(r'\s+', '_', nombre)  # espacios a "_"
    nombre = re.sub(r'[^\w.-]', '', nombre)  # solo alfanumérico, guiones y puntos
    return nombre

@api_view(['POST'])
@permission_classes([AllowAny])
def renombrar_imagen_refugio(request, refugio_id, nuevo_nombre):
    """
    Cambia el nombre del archivo de imagen asociado a un refugio.
    El nuevo nombre viene en la URL: /renombrar-imagen/<nuevo_nombre>/
    """
    refugio = get_object_or_404(Refugi, id=refugio_id)
    nuevo_nombre = limpiar_nombre_archivo(nuevo_nombre)
    if not refugio.imagen_local:
        return Response({"error": "Este refugio no tiene imagen asociada."}, status=400)

    try:
        with refugio.imagen_local.open('rb') as archivo:
            contenido = archivo.read()

        default_storage.delete(refugio.imagen_local.name)

        refugio.imagen_local.save(nuevo_nombre, ContentFile(contenido), save=True)

        return Response({
            "mensaje": "Imagen renombrada con éxito.",
            "nueva_url": refugio.imagen_local.url
        })

    except Exception as e:
        return Response({"error": f"No se pudo renombrar la imagen: {str(e)}"}, status=500)



@api_view(['GET'])
@permission_classes([AllowAny])
def detalles_refugios(request, lat, lon):
    """
    GET /api/refugios/detalles_refugios/41.38/2.17/
    """
    try:
        user_lat = float(lat)
        user_lon = float(lon)
    except ValueError:
        return Response({'error': 'lat y lon deben ser números'}, status=400)
    todos = list(Refugi.objects.all())
    def distancia_al_usuario(r):
        return calcular_distancia(
            user_lat, user_lon,
            float(r.latitud),
            float(r.longitud)
        )
    ordenados = sorted(todos, key=distancia_al_usuario)
    serializer = RefugiListadoSerializer(
        ordenados,
        many=True,
        context={
            'request': request,
            'user_lat': user_lat,
            'user_lon': user_lon,
        }
    )
    return Response(serializer.data)

@api_view(['GET'])
@permission_classes([AllowAny])
def listar_valoraciones_refugio(request, refugio_id):
    """
    GET /api/refugios/{refugio_id}/valoraciones/
    Devuelve todas las valoraciones de ese refugio, con usuario, puntuación, fecha y foto completa del usuario.
    """
    refugio = get_object_or_404(Refugi, id=refugio_id)
    qs = refugio.valoraciones.select_related('user').all()
    serializer = ValoracionDetailSerializer(qs, many=True, context={'request': request})
    return Response(serializer.data)


@api_view(['POST'])
@permission_classes([AllowAny])
def valorar_refugio_anonimo(request, refugio_id, puntuacion):
    """
    POST /api/refugios/valoracion-anonima/<refugio_id>/<puntuacion>/
    Permite valorar un refugio (1.0–5.0) sin necesidad de estar autenticado.
    """
    refugio = get_object_or_404(Refugi, id=refugio_id)
    try:
        puntuacion = float(puntuacion)
        if not (1.0 <= puntuacion <= 5.0):
            raise ValueError
    except ValueError:
        return Response({'error': 'Puntuación inválida (1.0 a 5.0)'}, status=400)

    Valoracion.objects.create(refugio=refugio, puntuacion=puntuacion)

    promedio = refugio.valoraciones.aggregate(Avg('puntuacion'))['puntuacion__avg']
    refugio.valoracion = promedio
    refugio.save()

    return Response({
        'mensaje': 'Valoración registrada (anónima)',
        'nueva_media': promedio
    }, status=201)

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def listar_refugios_cercania_usuario(request, lat, lon):
    """
    GET /api/refugios/listar_cercania_usuario/<lat>/<lon>/
    Lista todos los refugios, con toda su info, ordenados por distancia
    Añade a cada uno:
      - distancia (m)
      - isFavorite (bool) si está en favoritos del usuario autenticado
      - tags: dict { tag_name: total_asignaciones }
    """
    try:
        user_lat = float(lat)
        user_lon = float(lon)
    except ValueError:
        return Response({'error': 'lat y lon deben ser números'}, status=400)

    user = request.user

    # Ordenar refugios por distancia
    refugios = list(Refugi.objects.all())
    refugios.sort(key=lambda r: calcular_distancia(
        user_lat, user_lon,
        float(r.latitud), float(r.longitud)
    ))

    # Serializar datos básicos + distancia
    serializer = RefugiConDistanciaSerializer(
        refugios,
        many=True,
        context={'request': request, 'user_lat': user_lat, 'user_lon': user_lon}
    )
    data = serializer.data

    # Favoritos del usuario actual
    fav_ids = set(user.favorites.values_list('id', flat=True))

    # Etiquetas existentes
    todas_etiquetas = list(Tag.objects.values_list('name', flat=True))

    # Recuento de asignaciones por (refugio, tag)
    qs = RefugiTag.objects.values('refugi_id', 'tag__name') \
        .annotate(count=Count('id'))
    recuentos = {
        (item['refugi_id'], item['tag__name']): item['count']
        for item in qs
    }

    # Añadir campos isFavorite y tags a cada refugio
    for obj in data:
        rid = obj['id']
        obj['isFavorite'] = rid in fav_ids

        tags_info = {}
        for tag_name in todas_etiquetas:
            tags_info[tag_name] = recuentos.get((rid, tag_name), 0)
        obj['tags'] = tags_info

    return Response(data)


###################################

class TagListCreateAPIView(generics.ListCreateAPIView):
    """
    GET  /api/refugios/tags/    → lista todas las tags
    POST /api/refugios/tags/    → crea una nueva tag
    """
    queryset = Tag.objects.all()
    serializer_class = TagSerializer
    permission_classes = [IsAuthenticatedOrReadOnly]

    @swagger_auto_schema(
        operation_summary="Crear Tag global",
        operation_id="createGlobalTag",
        request_body=TagSerializer,
        responses={201: TagSerializer()}
    )
    def post(self, request, *args, **kwargs):
        return super().post(request, *args, **kwargs)


class TagRetrieveDestroyAPIView(generics.RetrieveDestroyAPIView):
    """
    DELETE /api/refugios/tags/{pk}/  → borra la tag global con id=pk
    """
    queryset = Tag.objects.all()
    serializer_class = TagSerializer
    permission_classes = [IsAuthenticatedOrReadOnly]

    @swagger_auto_schema(
        operation_summary="Eliminar etiqueta global",
        operation_id="deleteGlobalTag",
        responses={204: openapi.Response("Etiqueta global eliminada")}
    )
    def delete(self, request, *args, **kwargs):
        return super().delete(request, *args, **kwargs)


@swagger_auto_schema(
    methods=['post'],
    operation_summary="Asignar múltiples etiquetas a un refugio",
    operation_id="assignMultipleTagsToRefugi",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        required=['tags'],
        properties={
            'tags': openapi.Schema(
                type=openapi.TYPE_ARRAY,
                items=openapi.Schema(type=openapi.TYPE_STRING),
                description="Lista de nombres de etiquetas a asignar"
            )
        }
    ),
    responses={201: openapi.Response("Etiquetas asignadas"), 400: openapi.Response("tags obligatorio")}
)
@swagger_auto_schema(
    methods=['delete'],
    operation_summary="Quitar múltiples etiquetas de un refugio",
    operation_id="unassignMultipleTagsFromRefugi",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        required=['tags'],
        properties={
            'tags': openapi.Schema(
                type=openapi.TYPE_ARRAY,
                items=openapi.Schema(type=openapi.TYPE_STRING),
                description="Lista de nombres de etiquetas a desasignar"
            )
        }
    ),
    responses={200: openapi.Response("Etiquetas desasignadas"), 400: openapi.Response("tags obligatorio")}
)
@api_view(['POST', 'DELETE'])
@permission_classes([IsAuthenticated])
def tags_bulk(request, refugio_id):
    """
    POST   /api/refugios/{refugio_id}/tags/   → {"tags": [...]}
    DELETE /api/refugios/{refugio_id}/tags/   → {"tags": [...]}
    """
    refugi = get_object_or_404(Refugi, id=refugio_id)
    tags = request.data.get('tags')
    if not isinstance(tags, list) or not tags:
        return Response({'error': 'tags obligatorio (lista de strings)'}, status=400)

    resultados = []
    if request.method == 'POST':
        for name in tags:
            nm = name.strip()
            if not nm:
                continue
            tag_obj, _ = Tag.objects.get_or_create(name=nm)
            _, created = RefugiTag.objects.get_or_create(
                refugi=refugi, user=request.user, tag=tag_obj
            )
            resultados.append({'tag': nm, 'status': 'asignada' if created else 'ya existía'})
        return Response({'results': resultados}, status=201)

    # DELETE
    for name in tags:
        nm = name.strip()
        if not nm:
            continue
        try:
            tag_obj = Tag.objects.get(name=nm)
            rt = RefugiTag.objects.get(refugi=refugi, user=request.user, tag=tag_obj)
            rt.delete()
            resultados.append({'tag': nm, 'status': 'desasignada'})
        except (Tag.DoesNotExist, RefugiTag.DoesNotExist):
            resultados.append({'tag': nm, 'status': 'no existía'})
    return Response({'results': resultados}, status=200)

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def my_tags_in_shelter(request, refugio_id):
    refugi_tags = RefugiTag.objects.filter(refugi_id=refugio_id, user=request.user)
    tags = [rt.tag for rt in refugi_tags]
    serializer = TagSerializer(tags, many=True)
    return Response(serializer.data)

