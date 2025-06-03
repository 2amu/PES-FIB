import json
from channels.generic.websocket import AsyncWebsocketConsumer
from channels.db import database_sync_to_async
from .models import Mensaje
from Refugi.models import Refugi
from django.contrib.auth import get_user_model

User = get_user_model()

class ChatConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        self.refugio_id = self.scope['url_route']['kwargs']['refugio_id']
        self.room_group_name = f'chat_{self.refugio_id}'

        await self.channel_layer.group_add(self.room_group_name, self.channel_name)
        await self.accept()
        print(f"[WS] Conectado al chat de refugio {self.refugio_id}")

    async def disconnect(self, close_code):
        await self.channel_layer.group_discard(self.room_group_name, self.channel_name)
        print(f"[WS] Desconectado del chat de refugio {self.refugio_id}")

    async def receive(self, text_data):
        print(f"[WS] Texto recibido: {repr(text_data)}")
        try:
            data = json.loads(text_data)
        except json.JSONDecodeError:
            await self.send(text_data=json.dumps({
                'error': 'Formato JSON inválido'
            }))
            print("[WS] Error: Formato JSON inválido")
            return

        mensaje = data.get('mensaje')
        if not mensaje:
            await self.send(text_data=json.dumps({
                'error': 'El campo "mensaje" es obligatorio'
            }))
            print("[WS] Error: Campo 'mensaje' no encontrado o vacío")
            return

        user = self.scope.get("user")
        if not user or user.is_anonymous:
            await self.send(text_data=json.dumps({
                'error': 'Usuario no autenticado'
            }))
            print("[WS] Error: Usuario no autenticado")
            return

        # Guardar mensaje en base de datos
        mensaje_obj = await self.save_mensaje(self.refugio_id, user.id, mensaje)
        print(f"[WS] Mensaje guardado: {mensaje_obj}")

        # Enviar mensaje al grupo
        await self.channel_layer.group_send(
            self.room_group_name,
            {
                'type': 'chat_message',
                'mensaje': mensaje,
                'user': mensaje_obj.user.email,
                'user_id': mensaje_obj.user.id,
                'username': mensaje_obj.user.username,
                'timestamp': str(mensaje_obj.timestamp)
            }
        )

    async def chat_message(self, event):
        print(f"[WS] Enviando mensaje a cliente: {event}")
        await self.send(text_data=json.dumps(event))

    @database_sync_to_async
    def save_mensaje(self, refugio_id, user_id, contenido):
        refugio = Refugi.objects.get(id=refugio_id)
        user = User.objects.get(id=user_id)
        return Mensaje.objects.create(refugio=refugio, user=user, contenido=contenido)
