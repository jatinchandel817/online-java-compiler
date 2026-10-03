

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jdk
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Verify that both Java and the compiler are installed
RUN java -version && javac -version && which javac

EXPOSE 9090

CMD ["java", "-jar", "app.jar"]
