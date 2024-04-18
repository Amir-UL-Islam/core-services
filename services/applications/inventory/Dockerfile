FROM openjdk:17

LABEL authors="amir"

COPY build/libs/inventory-1.0-SNAPSHOT.jar inventory.jar

ENTRYPOINT ["java", "-jar", "inventory.jar"]