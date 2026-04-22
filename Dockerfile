FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests \
    && find target -maxdepth 1 -type f -name "*.jar" ! -name "*.original" -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN useradd --system --create-home --uid 1001 appuser
USER appuser

COPY --from=builder /workspace/app.jar ./app.jar

EXPOSE 8081
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

