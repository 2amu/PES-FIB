from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static
from rest_framework import permissions
from drf_yasg.views import get_schema_view
from Usuari.views import CustomUserDetailsView, LogoutNoRefreshView, GoogleAuthView
from drf_yasg import openapi
from dj_rest_auth.views import PasswordResetView, PasswordResetConfirmView


schema_view = get_schema_view(
    openapi.Info(
        title="API Docs.",
        default_version="v1",
        description="Lorem Ipsum …",
    ),
    public=True,
    permission_classes=(permissions.AllowAny,),
)

urlpatterns = [
    path("admin/", admin.site.urls),
    path('auth/logout/', LogoutNoRefreshView.as_view(), name='logout'),
    path("auth/google/", GoogleAuthView.as_view(), name="google-login"),
    path("auth/user/", CustomUserDetailsView.as_view(), name="custom_user_details"),
    # Auth / JWT
    path("auth/", include("dj_rest_auth.urls")),
    path("auth/registration/", include("dj_rest_auth.registration.urls")),
    path("auth/password/reset/", PasswordResetView.as_view(), name="password_reset"),
    path("auth/password/reset/confirm/<str:uidb64>/<str:token>",
         PasswordResetConfirmView.as_view(), name="password_reset_confirm"),

    # Swagger / Redoc
    path("swagger<format>/", schema_view.without_ui(cache_timeout=0), name="schema-json"),
    path("swagger/",   schema_view.with_ui('swagger',   cache_timeout=0), name="schema-swagger-ui"),
    path("redoc/",     schema_view.with_ui('redoc',     cache_timeout=0), name="schema-redoc"),

    # APIs
    path('api/refugios/', include('Refugi.urls')),
    path('api/usuarios/', include('Usuari.urls')),
    path('api/', include('chat.urls')),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
