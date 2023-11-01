FROM amazoncorretto:17.0.7-alpine
EXPOSE 8080
ARG JAR_FILE=build/libs/java-stellar-pp-bot-*.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","app.jar"]