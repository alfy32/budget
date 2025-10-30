FROM eclipse-temurin:21-jre-jammy

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV SERVER_PORT=8080
ENV DB_NAME=budget_prod

ENTRYPOINT ["java","-jar","/app.jar"]