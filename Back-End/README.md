# FreskitoBCN Back-End

Un projecte Django amb Docker per gestionar dades de refugis i usuaris.

---

## Prerequisits

- Docker (versió ≥ 20.10)  
- Docker Compose (versió ≥ 1.29)  
- Git  

---

## Clonar el repositori

```bash
git clone https://github.com/el-teu-usuari/FreskitoBCN_Back-End.git
cd FreskitoBCN_Back-End
```

---

## Configuració

1. Crea un archiu `.env`.  
2. Omple les variables d’entorn necessàries (claus secrets, connexió a la base de dades, etc.).  
3. Assegura’t que no hi ha cap servei ocupant els ports:
   - **8000** (API)  
   - **5432** (PostgreSQL)  
   - **6379** (Redis)  

---

## Arrencada amb Docker

```bash
sudo docker-compose up -d --build
```

Aquest comandament:

- Construeix les imatges de l’API, Postgres i Redis.  
- Inicia els contenidors en segon pla.  

---

## Migracions de base de dades

Quan el contenidor `api` estigui en execució, crea i aplica les migracions:

```bash
sudo docker-compose exec api python manage.py makemigrations
sudo docker-compose exec api python manage.py migrate
```

---

## Omplir dades de refugis

Per a sincronitzar la base de dades amb l’origen de dades de refugis (agafar les dades de la API):

```bash
docker exec -it api python manage.py actualizar_refugios 
```

> **Nota:** Substitueix `api` pel nom real del teu contenidor si és diferent.

---

## Comandes útils

- **Aturar tot**  
  ```bash
  sudo docker-compose down
  ```

- **Veure logs de l’API**  
  ```bash
  sudo docker-compose logs -f api
  ```

- **Executar tests**  
  ```bash
  sudo docker-compose exec api pytest
  ```

---

## Accés a la documentació

- **Swagger UI:**  
  [http://localhost:8000/swagger/](http://localhost:8000/swagger/)  

---


---

FreskitoBCN © 2025 
