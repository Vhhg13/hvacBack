FROM eclipse-temurin:23

COPY app/build/libs/app-all.jar app-all.jar

CMD [ "java", "-jar", "app-all.jar" ]