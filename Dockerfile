# Resolve and pin registry digests before a production release; see docs/en/RELEASE.md.
FROM node:24.22.0-bookworm-slim AS web
WORKDIR /src/frontend
COPY frontend/package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi
COPY frontend/ ./
RUN npm run typecheck && npm test && npm run build

FROM eclipse-temurin:25-jdk-noble AS build
RUN apt-get update && apt-get install --no-install-recommends -y ca-certificates curl && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY mvnw pom.xml ./
COPY domain/ domain/
COPY server/ server/
COPY deploy/Healthcheck.java /tmp/Healthcheck.java
COPY --from=web /src/frontend/dist/ server/src/main/resources/static/
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress verify     && javac -d /tmp /tmp/Healthcheck.java

FROM eclipse-temurin:25-jre-noble AS runtime
RUN groupadd --gid 10001 belegflow && useradd --uid 10001 --gid 10001 --no-create-home belegflow
WORKDIR /app
COPY --from=build --chown=10001:10001 /src/server/target/belegflow-server-0.2.2.jar app.jar
COPY --from=build --chown=10001:10001 /tmp/Healthcheck.class /opt/belegflow-health/Healthcheck.class
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=8s --start-period=60s --retries=4 CMD ["java","-cp","/opt/belegflow-health","Healthcheck"]
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
