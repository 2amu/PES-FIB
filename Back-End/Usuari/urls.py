from django.urls import path
from .views import (
    ListCreateUsuariView,
    UsuariDetailView,
    add_favorite_shelter,
    remove_favorite_shelter, list_favorite_shelters, get_user_photo, update_user_photo, delete_user_photo,
    update_idioma,
)

urlpatterns = [
    path('', ListCreateUsuariView.as_view(), name='usuarios-list-create'),
    path('<int:pk>/', UsuariDetailView.as_view(), name='detalle_usuario'),
    path('favorites/add/<int:refugi_id>/', add_favorite_shelter, name='add_favorite'),
    path('favorites/remove/<int:refugi_id>/', remove_favorite_shelter, name='remove_favorite'),
    path('favorites/', list_favorite_shelters, name='list_favorites'),
    path('<int:user_id>/photo/', get_user_photo, name='get_user_photo'),
    path('photo/update/', update_user_photo, name='update_user_photo'),
    path('photo/delete/', delete_user_photo, name='delete_user_photo'),
    path('cambiar_idioma/<str:idioma>/',update_idioma,name='update_idioma')
]
