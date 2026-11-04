FROM eclipse-temurin:25-jre-noble

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV SERVER_PORT=8080
# The SQLite database lives on a mounted folder so it survives new containers.
ENV DB_PATH=/data/budget.db
VOLUME /data

ENTRYPOINT ["java","--enable-native-access=ALL-UNNAMED","-jar","/app.jar"]
