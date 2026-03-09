# --- Part 1: Build the App ---
FROM maven:3-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
# This builds your app into a JAR file
RUN mvn clean package -DskipTests

# --- Part 2: Run the App ---
FROM eclipse-temurin:17-jdk-alpine
# Install Tor
RUN apk add --no-cache tor

WORKDIR /app

# Copy the built JAR file from Part 1
COPY --from=builder /build/target/*.jar app.jar

# Copy your configuration files
COPY torrc /etc/tor/torrc
COPY start.sh /start.sh

# Give permission to run the script
RUN chmod +x /start.sh
RUN mkdir -p /var/lib/tor/hidden_service
RUN chmod 700 /var/lib/tor/hidden_service

# Start everything
CMD ["/start.sh"]