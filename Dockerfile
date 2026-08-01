# ===== Stage 1: 构建 =====
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先拷贝 pom.xml，利用 Docker 缓存加速依赖下载
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 拷贝源码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===== Stage 2: 运行 =====
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# 生产环境通过环境变量注入配置，不开启远程调试
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
