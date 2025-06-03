import os
import django
from channels.routing import ProtocolTypeRouter, URLRouter
from django.core.asgi import get_asgi_application

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'FreskitoBCN.settings')
django.setup()

from chat.middleware import JWTAuthMiddleware  # <-- nuevo middleware
import chat.routing

application = ProtocolTypeRouter({
    "http": get_asgi_application(),
    "websocket": JWTAuthMiddleware(  # <-- reemplaza AuthMiddlewareStack
        URLRouter(
            chat.routing.websocket_urlpatterns
        )
    ),
})
