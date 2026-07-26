# Multi-stage Dockerfile for the Trendly Spring Boot backend (OPS-1.2).
#
# NOTE: BE-1.1 (tasks/backend/BE-1.1.md) -- the real Spring Boot skeleton --
# has not landed yet. Per OPS-1.2's own task notes, this Dockerfile is
# written against a minimal PLACEHOLDER Maven project
# (backend/pom.xml + backend/src/...), not a real Spring Boot app. It still
# exercises the real build mechanics (Maven dependency layer caching,
# `mvn package`, copying the built fat JAR into a slim JRE runtime) so the
# pattern is proven ahead of BE-1.1 landing.
#
# Once BE-1.1 lands with the real `pom.xml` (same Java 21 / Maven / JAR
# coordinates expected -- groupId com.trendly), this file should need at
# most a smoke-check, not a rewrite: delete backend/pom.xml and
# backend/src/ (placeholder) and confirm `docker build` still succeeds
# against the real project. See tasks/devops/OPS-1.2.md implementation
# notes for the flagged follow-up.

# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy only the POM first so dependency resolution is cached in its own
# Docker layer and only re-runs when dependencies actually change.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy source and build the fat JAR.
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- runtime stage ---
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /build/target/app.jar app.jar

# Reasonable JVM defaults, overridable at runtime via either var:
#   docker run -e JAVA_OPTS="-Xmx512m" ...
#   docker run -e JDK_JAVA_OPTIONS="-Xmx512m" ...   (picked up natively by the JVM)
ENV JAVA_OPTS=""

USER app
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
