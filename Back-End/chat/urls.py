from django.urls import path
from .views import ChatHistorialAPIView

urlpatterns = [
    path('chat/<int:refugio_id>/', ChatHistorialAPIView.as_view(), name='chat-historial'),
]
