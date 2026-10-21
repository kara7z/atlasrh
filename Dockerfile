ARG JAVA_VERSION=21

FROM eclipse-temurin:${JAVA_VERSION}-jdk-jammy AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
  && ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package \
  && cp target/*.jar /workspace/app.jar

FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S -g 10001 app \
  && adduser -S -D -H -u 10001 -G app app
COPY --from=build /workspace/app.jar ./app.jar

USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
