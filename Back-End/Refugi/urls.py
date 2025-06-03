from django.urls import path
from .views import listar_refugios, edit_shelter, add_shelter, delete_shelter, get_shelter, \
    buscar_refugios, valorar_refugio, renombrar_imagen_refugio, detalles_refugios, \
    listar_refugios_cercania, listar_valoraciones_refugio, add_shelters, valorar_refugio_anonimo, \
    listar_refugios_cercania_usuario, TagListCreateAPIView, TagRetrieveDestroyAPIView, \
    tags_bulk, my_tags_in_shelter
from drf_yasg.views import get_schema_view
from drf_yasg import openapi

# Configuración para Swagger
schema_view = get_schema_view(
    openapi.Info(
        title="API de Refugios",
        default_version='v1',
        description="Documentación de la API para gestionar refugios",
        contact=openapi.Contact(email="contact@refugios.com"),
    ),
    public=True,
)

urlpatterns = [
    path('', listar_refugios, name='listar_refugios'),
    path('detalles_refugios/<str:lat>/<str:lon>/',detalles_refugios,name='detalles_refugios'),
    path('listar/', listar_refugios, name='listar_refugios'),
    path('listar_cercania/<str:lat>/<str:lon>/', listar_refugios_cercania, name='listar_refugios_cercania'),
    path('listar_cercania_usuario/<str:lat>/<str:lon>/',listar_refugios_cercania_usuario,name='listar_refugios_cercania_usuario'),
    path('<int:id>/', get_shelter, name='get_shelter'),
    path('<int:id>/edit', edit_shelter, name='edit_shelter'),
    path('<int:id>/delete', delete_shelter, name='delete_shelter'),
    path('add', add_shelter, name='add_shelter'),
    path('add_batch/', add_shelters, name='add_shelters'),
    path('buscar/<str:palabras>/', buscar_refugios, name='buscar_refugios'),
    path('<int:refugio_id>/valorar/<str:puntuacion>/', valorar_refugio, name='valorar_refugio'),
    path('valoracion-anonima/<int:refugio_id>/<str:puntuacion>/',valorar_refugio_anonimo,name='valorar_refugio_anonimo'),
    path('<int:refugio_id>/renombrar-imagen/<str:nuevo_nombre>/', renombrar_imagen_refugio, name='renombrar_imagen'),
    path('swagger/', schema_view.with_ui('swagger', cache_timeout=0), name='schema-swagger-ui'),
    path('<int:refugio_id>/valoraciones/',listar_valoraciones_refugio,name='listar_valoraciones_refugio'),


    path('tags/',          TagListCreateAPIView.as_view(),      name='tag-list-create'),
    path('tags/<int:pk>/', TagRetrieveDestroyAPIView.as_view(), name='tag-detail'),
    path('<int:refugio_id>/my-tags/', my_tags_in_shelter, name='my-tags-in-shelter'),    path('<int:refugio_id>/tags/', tags_bulk, name='tags-bulk'),

]

