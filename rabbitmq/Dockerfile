# Wersja obrazu bazowego - wartość przychodzi z .env (RABBITMQ_VERSION) przez 
# docker compose build args. Jedno miejsce na upgrade RabbitMQ bez edycji Dockerfile.
ARG RABBITMQ_VERSION

# Oficjalny obraz RabbitMQ. Suffix -management = wbudowany panel webowy (:15672).
# Suffix -alpine = mniejszy obraz (Alpine Linux).
FROM rabbitmq:${RABBITMQ_VERSION}