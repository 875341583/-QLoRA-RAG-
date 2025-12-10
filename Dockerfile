# Use OpenJDK 17 slim image as base for smaller footprint
FROM openjdk:17-jdk-slim

# Set maintainer information
LABEL maintainer="navigation-system-team@company.com" \
      version="1.0.0" \
      description="Navigation System Application"

# Set working directory
WORKDIR /app

# Set timezone to Shanghai
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Install necessary tools and clean up apt cache in single layer
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

# Create non-root user to run application (security best practice)
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy Maven built JAR file to container with specific naming pattern
COPY target/navigation-application-*.jar app.jar

# Create logs directory and set permissions
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose application port (Spring Boot default port)
EXPOSE 8080

# Set JVM parameters for optimal performance
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1ReservePercent=25 -Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8"

# Set health check endpoint with proper intervals
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start application with shell expansion for environment variables
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]


# 内容由AI生成，仅供参考