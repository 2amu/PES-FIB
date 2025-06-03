import io
import pytest
from django.urls import reverse
from rest_framework.test import APIClient
from django.core.files.uploadedfile import SimpleUploadedFile
from Usuari.models import Usuari
from Refugi.models import Refugi
from rest_framework import status

@pytest.fixture
def api_client():
    return APIClient()

@pytest.fixture
def create_user(db):
    def _create(**kwargs):
        return Usuari.objects.create_user(
            username=kwargs.get('username', 'user'),
            email=kwargs.get('email', 'user@example.com'),
            password=kwargs.get('password', 'pass123'),
            telefono=kwargs.get('telefono', ''),
            direccion=kwargs.get('direccion', ''),
            descripcion=kwargs.get('descripcion', ''),
            idioma=kwargs.get('idioma', ''),
        )
    return _create

@pytest.fixture
def create_refugi(db):
    def _create(**kwargs):
        return Refugi.objects.create(
            nombre=kwargs.get('nombre', 'Test'),
            latitud=kwargs.get('latitud', 41.0),
            longitud=kwargs.get('longitud', 2.0),
        )
    return _create

# User creation and listing
@pytest.mark.django_db
class TestUserEndpoints:
    def test_create_user_success(self, api_client):
        url = reverse('usuarios-list-create')
        data = {
            'username': 'newuser',
            'email': 'new@example.com',
            'password': 'strongpass',
        }
        response = api_client.post(url, data)
        assert response.status_code == status.HTTP_201_CREATED
        assert response.data['username'] == 'newuser'
        assert 'password' not in response.data

    def test_create_user_missing_password(self, api_client):
        url = reverse('usuarios-list-create')
        data = {'username': 'nouser', 'email': 'nouser@example.com'}
        response = api_client.post(url, data)
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert 'password' in response.data

    def test_list_users(self, api_client, create_user):
        create_user(username='u1')
        create_user(username='u2')
        url = reverse('usuarios-list-create')
        response = api_client.get(url)
        assert response.status_code == status.HTTP_200_OK
        assert len(response.data) >= 2

    def test_user_detail_unauth(self, api_client, create_user):
        user = create_user()
        url = reverse('detalle_usuario', args=[user.id])
        response = api_client.get(url)
        assert response.status_code == status.HTTP_401_UNAUTHORIZED

    def test_user_detail_auth(self, api_client, create_user):
        user = create_user()
        api_client.force_authenticate(user=user)
        url = reverse('detalle_usuario', args=[user.id])
        response = api_client.get(url)
        assert response.status_code == status.HTTP_200_OK
        assert response.data['username'] == user.username

# Favorites endpoints
@pytest.mark.django_db
class TestFavorites:
    def test_add_list_remove_favorites(self, api_client, create_user, create_refugi):
        user = create_user()
        refugi = create_refugi()
        api_client.force_authenticate(user=user)

        # Add
        add_url = reverse('add_favorite', args=[refugi.id])
        resp1 = api_client.post(add_url)
        assert resp1.status_code == status.HTTP_200_OK
        assert 'añadido' in resp1.data['message'].lower()

        # List
        list_url = reverse('list_favorites')
        resp2 = api_client.get(list_url)
        assert resp2.status_code == status.HTTP_200_OK
        assert any(item['id'] == refugi.id for item in resp2.data)

        # Remove
        remove_url = reverse('remove_favorite', args=[refugi.id])
        resp3 = api_client.delete(remove_url)
        assert resp3.status_code == status.HTTP_200_OK
        assert 'eliminado' in resp3.data['message'].lower()

        # Remove non-existent
        resp4 = api_client.delete(remove_url)
        assert resp4.status_code == status.HTTP_200_OK
        assert 'no estaba' in resp4.data['message'].lower()


# Idioma endpoint
@pytest.mark.django_db
class TestIdiomaEndpoint:
    def test_update_idioma_valid(self, api_client, create_user):
        user = create_user()
        api_client.force_authenticate(user=user)
        url = reverse('update_idioma', args=['es'])
        resp = api_client.patch(url)
        assert resp.status_code == status.HTTP_200_OK
        assert resp.data['idioma'] == 'es'

# Logout endpoint
@pytest.mark.django_db
class TestLogoutEndpoint:
    def test_logout_unauth(self, api_client):
        url = reverse('logout')
        resp = api_client.post(url)
        assert resp.status_code == status.HTTP_401_UNAUTHORIZED

    def test_logout_auth(self, api_client, create_user):
        user = create_user()
        api_client.force_authenticate(user=user)
        url = reverse('logout')
        resp = api_client.post(url)
        assert resp.status_code == status.HTTP_200_OK
        assert 'detail' in resp.data

# Google auth endpoint
@pytest.mark.django_db
class TestGoogleAuth:
    def test_google_no_token(self, api_client):
        url = reverse('google-login')
        resp = api_client.post(url, {})
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_google_invalid_token(self, api_client, monkeypatch):
        from google.oauth2 import id_token
        monkeypatch.setattr(id_token, 'verify_oauth2_token', lambda t, r, c: (_ for _ in ()).throw(ValueError()))
        url = reverse('google-login')
        resp = api_client.post(url, {'id_token': 'bad'})
        assert resp.status_code == status.HTTP_400_BAD_REQUEST

    def test_google_success(self, api_client, monkeypatch, tmp_path):
        fake_info = {
            'sub': '123',
            'email': 'g@example.com',
            'name': 'G User',
            'picture': 'http://example.com/pic.jpg'
        }
        # patch verify
        from google.oauth2 import id_token
        monkeypatch.setattr(id_token, 'verify_oauth2_token', lambda token, req, cid: fake_info)
        # patch external get
        class FakeResp: status_code = 200

        monkeypatch.setattr('Usuari.views.external_requests.get', lambda url: FakeResp())

        url = reverse('google-login')
        resp = api_client.post(url, {'id_token': 'token'})
        assert resp.status_code == status.HTTP_200_OK
        assert 'access' in resp.data and 'refresh' in resp.data and 'user_id' in resp.data

import pytest
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIClient
from django.contrib.auth import get_user_model
from Refugi.models import Refugi

User = get_user_model()

@pytest.fixture
def api_client():
    return APIClient()

@pytest.fixture
def create_user(db):
    def _create(**kwargs):
        return User.objects.create_user(
            username=kwargs.get('username', 'u'),
            email=kwargs.get('email', 'u@example.com'),
            password=kwargs.get('password', 'pass1234'),
        )
    return _create

@pytest.fixture
def create_refugi(db):
    def _create(**kwargs):
        return Refugi.objects.create(
            nombre=kwargs.get('nombre', 'R'),
            latitud=0,
            longitud=0
        )
    return _create

@pytest.mark.django_db
def test_mass_user_creation_and_listing(api_client):
    """Crear 500 usuarios seguidos y luego listarlos."""
    url = reverse('usuarios-list-create')
    for i in range(500):
        resp = api_client.post(url, {
            'username': f'user{i}',
            'email': f'user{i}@ex.com',
            'password': 'p123456'
        })
        assert resp.status_code == status.HTTP_201_CREATED
    resp = api_client.get(url)
    assert resp.status_code == status.HTTP_200_OK
    # Debe haber al menos 500 usuarios
    assert len(resp.data) >= 500

@pytest.mark.django_db
def test_add_favorite_nonexistent_refugio(api_client, create_user):
    """Intentar añadir un favorito con ID inexistente → 404."""
    user = create_user()
    api_client.force_authenticate(user=user)
    url = reverse('add_favorite', args=[999999])
    resp = api_client.post(url)
    assert resp.status_code == status.HTTP_404_NOT_FOUND

@pytest.mark.django_db
def test_logout_endpoint(api_client, create_user):
    """Logout debe funcionar con usuario autenticado y fallar sin."""
    user = create_user()
    api_client.force_authenticate(user=user)
    url = reverse('logout')
    resp = api_client.post(url)
    assert resp.status_code == status.HTTP_200_OK

    # Sin autenticar, debe responder 401
    api_client.force_authenticate(user=None)
    resp2 = api_client.post(url)
    assert resp2.status_code == status.HTTP_401_UNAUTHORIZED

@pytest.mark.django_db
def test_google_auth_flow(monkeypatch, api_client):
    """GoogleAuthView: missing token, invalid token y flujo exitoso."""
    url = reverse('google-login')

    # Sin id_token
    resp = api_client.post(url, {})
    assert resp.status_code == status.HTTP_400_BAD_REQUEST

    # id_token inválido
    monkeypatch.setattr('Usuari.views.id_token.verify_oauth2_token',
                        lambda token, request, client: (_ for _ in ()).throw(ValueError()))
    resp2 = api_client.post(url, {'id_token': 'bad'})
    assert resp2.status_code == status.HTTP_400_BAD_REQUEST

    # id_token válido y sin picture
    fake_info = {'sub': '123', 'email': 'g@ex.com', 'name': 'G', 'picture': None}
    monkeypatch.setattr('Usuari.views.id_token.verify_oauth2_token',
                        lambda token, request, client: fake_info)
    resp3 = api_client.post(url, {'id_token': 'good'})
    assert resp3.status_code == status.HTTP_200_OK
    assert 'access' in resp3.data and 'refresh' in resp3.data and 'user_id' in resp3.data

@pytest.mark.django_db
def test_tags_bulk_extremes(api_client, create_user, create_refugi):
    """Tags bulk: sin body, lista vacía, asignación y desasignación múltiple."""
    user = create_user()
    refugio = create_refugi()
    api_client.force_authenticate(user=user)
    url = reverse('tags-bulk', args=[refugio.id])

    # Sin 'tags' → 400
    resp = api_client.post(url, {}, format='json')
    assert resp.status_code == status.HTTP_400_BAD_REQUEST

    # Lista vacía → 400
    resp2 = api_client.post(url, {'tags': []}, format='json')
    assert resp2.status_code == status.HTTP_400_BAD_REQUEST

    # Asignar 100 etiquetas únicas a la vez
    muchas = [f'tag{i}' for i in range(100)]
    resp3 = api_client.post(url, {'tags': muchas}, format='json')
    assert resp3.status_code == status.HTTP_201_CREATED
    assert len(resp3.data['results']) == 100

    # Eliminar la mitad de las etiquetas
    mitad = muchas[:50]
    resp4 = api_client.delete(url, {'tags': mitad}, format='json')
    assert resp4.status_code == status.HTTP_200_OK
    # 50 resultados de desasignación
    assert len(resp4.data['results']) == 50
