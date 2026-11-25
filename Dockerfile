FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends openssl python3 \
    && rm -rf /var/lib/apt/lists/* \
    && install -d -o 10001 -g 10001 /app/var/jwt

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/clientes-cuentas-microservicio-0.1.1.jar ./app.jar
COPY --chown=10001:10001 scripts/local-jwt.py ./local-jwt.py
COPY --chown=10001:10001 docker-entrypoint.sh ./docker-entrypoint.sh
RUN chmod 755 /app/docker-entrypoint.sh
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
