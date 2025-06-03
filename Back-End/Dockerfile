FROM python:3.12.2

WORKDIR /app

COPY requirements.txt /app

RUN pip install --upgrade pip
RUN pip install -r requirements.txt

# Copiar todo el código
COPY . /app

# Copiar y dar permisos al script wait-for-it.sh
RUN chmod +x /app/wait-for-it.sh
