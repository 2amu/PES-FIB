# Refugi/tests_api.py
import pytest
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIClient
from Refugi.models import Refugi, Valoracion, Tag, RefugiTag
from Usuari.models import Usuari

@pytest.fixture
def api_client():
    return APIClient()

@pytest.fixture
def create_user(db):
    def _create(**kwargs):
        return Usuari.objects.create_user(
            username=kwargs.get("username", "testuser"),
            email=kwargs.get("email", "test@example.com"),
            password=kwargs.get("password", "secret123")
        )
    return _create

@pytest.fixture
def auth_client(api_client, create_user):
    user = create_user()
    api_client.force_authenticate(user=user)
    return api_client, user

@pytest.fixture
def create_refugi(db):
    def _create(**kwargs):
        return Refugi.objects.create(
            nombre=kwargs.get("nombre", "R"),
            latitud=kwargs.get("latitud", 0.0),
            longitud=kwargs.get("longitud", 0.0),
            institucion=kwargs.get("institucion", "I"),
            distrito=kwargs.get("distrito", "D")
        )
    return _create

@pytest.mark.django_db
class TestShelterCRUD:
    def test_listar_empty(self, api_client):
        resp = api_client.get(reverse("listar_refugios"))
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data == []

    def test_add_and_get_and_delete(self, api_client):
        # Crear
        data = {"nombre": "X", "latitud": 1.1, "longitud": 2.2}
        resp = api_client.post(reverse("add_shelter"), data, format="json")
        assert resp.status_code == status.HTTP_200_OK
        rid = resp.data["id"]

        # Obtener
        resp2 = api_client.get(reverse("get_shelter", args=[rid]))
        assert resp2.status_code == status.HTTP_200_OK
        assert resp2.data["nombre"] == "X"

        # Borrar
        resp3 = api_client.delete(reverse("delete_shelter", args=[rid]))
        assert resp3.status_code == status.HTTP_200_OK
        assert "eliminado" in resp3.data["mensaje"].lower()

        # Ya no existe
        resp4 = api_client.get(reverse("get_shelter", args=[rid]))
        assert resp4.status_code == status.HTTP_404_NOT_FOUND

    def test_edit_not_found_and_validation(self, api_client, create_refugi):
        # Edit inexistente
        resp = api_client.put(reverse("edit_shelter", args=[999]), {"nombre": "Y"}, format="json")
        assert resp.status_code == status.HTTP_404_NOT_FOUND

        # Edit existente con datos inválidos
        refugio = create_refugi()
        resp2 = api_client.put(
            reverse("edit_shelter", args=[refugio.id]),
            {"latitud": "no-numero"},
            format="json"
        )
        assert resp2.status_code == status.HTTP_400_BAD_REQUEST

    def test_buscar_and_filter(self, api_client, create_refugi):
        create_refugi(nombre="FooBar", institucion="ZZZ")
        create_refugi(nombre="Other", institucion="Fooing")

        # Buscar por nombre
        resp = api_client.get(reverse("buscar_refugios", args=["FooBar"]))
        assert resp.status_code == status.HTTP_200_OK
        assert len(resp.data) == 1

        # URL mal formada: no hace match → 404
        resp2 = api_client.get("/api/refugios/buscar//")
        assert resp2.status_code == status.HTTP_404_NOT_FOUND

@pytest.mark.django_db
class TestProximityAndDetails:
    @pytest.mark.parametrize("lat,lon,expected_code", [
        ("a", "b", status.HTTP_400_BAD_REQUEST),
        ("1", "2", status.HTTP_200_OK),
    ])
    def test_listar_cercania(self, api_client, create_refugi, lat, lon, expected_code):
        first  = create_refugi(latitud=0, longitud=0)
        second = create_refugi(latitud=1, longitud=1)

        resp = api_client.get(reverse("listar_refugios_cercania", args=[lat, lon]))
        assert resp.status_code == expected_code

        if expected_code == status.HTTP_200_OK:
            assert resp.data[0]["id"] == second.id
            assert resp.data[1]["id"] == first.id

    def test_detalles_refugios_invalid_and_valid(self, api_client, create_refugi):
        create_refugi(latitud=0, longitud=0)

        # inválido
        resp1 = api_client.get("/api/refugios/detalles_refugios/x/y/")
        assert resp1.status_code == status.HTTP_400_BAD_REQUEST

        # válido
        resp2 = api_client.get(reverse("detalles_refugios", args=["0", "0"]))
        assert resp2.status_code == status.HTTP_200_OK
        assert isinstance(resp2.data, list)

@pytest.mark.django_db
class TestValoraciones:
    def test_valorar_anonimo_and_invalid(self, api_client, create_refugi):
        refugio = create_refugi()
        # válido
        resp1 = api_client.post(reverse("valorar_refugio_anonimo", args=[refugio.id, "5"]))
        assert resp1.status_code == status.HTTP_201_CREATED
        # inválido
        resp2 = api_client.post(reverse("valorar_refugio_anonimo", args=[refugio.id, "6"]))
        assert resp2.status_code == status.HTTP_400_BAD_REQUEST

    def test_valorar_require_auth_and_listing(self, auth_client, create_refugi):
        client, user = auth_client
        refugio = create_refugi()
        url = reverse("valorar_refugio", args=[refugio.id, "4"])
        resp1 = client.post(url, format="json")
        assert resp1.status_code == status.HTTP_200_OK

        resp2 = client.get(reverse("listar_valoraciones_refugio", args=[refugio.id]))
        assert resp2.status_code == status.HTTP_200_OK
        assert len(resp2.data) == 1

    def test_listar_cercania_usuario_requires_auth(self, api_client):
        resp = api_client.get(reverse("listar_refugios_cercania_usuario", args=["0", "0"]))
        assert resp.status_code == status.HTTP_401_UNAUTHORIZED

@pytest.mark.django_db
class TestImageRename:
    def test_renombrar_imagen_no_image(self, api_client, create_refugi):
        refugio = create_refugi()
        url = reverse("renombrar_imagen", args=[refugio.id, "nuevo"])
        resp = api_client.post(url)
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

@pytest.mark.django_db
class TestTagsEndpoints:
    def test_global_tag_crud(self, api_client, create_user):
        # crear sin auth → 401
        resp0 = api_client.post(reverse("tag-list-create"), {"name": "T"}, format="json")
        assert resp0.status_code == status.HTTP_401_UNAUTHORIZED

        # con auth
        user = create_user()
        api_client.force_authenticate(user=user)
        resp1 = api_client.post(reverse("tag-list-create"), {"name": "T"}, format="json")
        assert resp1.status_code == status.HTTP_201_CREATED
        tid = resp1.data["id"]

        resp2 = api_client.get(reverse("tag-list-create"))
        assert any(t["id"] == tid for t in resp2.data)

        resp3 = api_client.delete(reverse("tag-detail", args=[tid]))
        assert resp3.status_code == status.HTTP_204_NO_CONTENT

    def test_tags_bulk_and_my_tags(self, auth_client, create_refugi):
        client, user = auth_client
        refugio = create_refugi()
        url = reverse("tags-bulk", args=[refugio.id])

        resp0 = client.post(url, {}, format="json")
        assert resp0.status_code == status.HTTP_400_BAD_REQUEST

        resp1 = client.post(url, {"tags": ["a", "b"]}, format="json")
        assert resp1.status_code == status.HTTP_201_CREATED
        assert len(resp1.data["results"]) == 2

        resp2 = client.get(reverse("my-tags-in-shelter", args=[refugio.id]))
        assert resp2.status_code == status.HTTP_200_OK
        assert {t["name"] for t in resp2.data} == {"a", "b"}

        resp3 = client.delete(url, {"tags": ["a", "c"]}, format="json")
        assert resp3.status_code == status.HTTP_200_OK
        statuses = {r["status"] for r in resp3.data["results"]}
        assert "desasignada" in statuses and "no existía" in statuses
