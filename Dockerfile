# Этап 1: Сборка проекта через Maven
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Этап 2: Запуск готового приложения
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Указываем команду запуска
ENTRYPOINT ["java", "-jar", "app.jar"]