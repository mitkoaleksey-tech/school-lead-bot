FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .

RUN sed -i 's/\r$//' mvnw
RUN chmod +x mvnw
# Полностью отключаем тесты и их компиляцию
RUN ./mvnw clean package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]