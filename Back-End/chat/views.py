from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from .models import Mensaje
from .serializers import MensajeSerializer

class ChatHistorialAPIView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request, refugio_id):
        mensajes = Mensaje.objects.filter(refugio_id=refugio_id).order_by('-timestamp')[:50]
        mensajes = reversed(mensajes)  # para que salgan de más antiguo a más nuevo
        serializer = MensajeSerializer(mensajes, many=True)
        return Response(serializer.data)
