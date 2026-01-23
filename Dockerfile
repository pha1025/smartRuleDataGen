# 第一阶段：构建环境 (使用 Maven 打包)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
# 开始打包 (跳过测试以加快速度)
RUN mvn clean package -DskipTests

# 第二阶段：运行环境 (使用轻量级 JDK 运行)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# 从第一阶段复制生成的 jar 包
COPY --from=build /app/target/*.jar app.jar

# 声明端口 (Render 会自动检测到这个端口)
EXPOSE 8080

# 启动命令 (动态读取 Render 分配的端口，如果没读到则默认 8080)
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]