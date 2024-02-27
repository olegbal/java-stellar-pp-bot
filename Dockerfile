FROM amazoncorretto:17.0.7-alpine
EXPOSE 8080
EXPOSE 9010
ARG JAR_FILE=build/libs/java-stellar-pp-bot-*.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-Dcom.sun.management.jmxremote=true", "-Dcom.sun.management.jmxremote.port=9010", "-Dcom.sun.management.jmxremote.local.only=false", "-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.rmi.port=9010", "-Djava.rmi.server.hostname=localhost", "-jar" , "app.jar" ]