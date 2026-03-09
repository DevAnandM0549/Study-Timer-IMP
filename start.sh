#!/bin/sh

# 1. Start Tor in the background
echo "Starting Tor service..."
tor -f /etc/tor/torrc &
TOR_PID=$!

# 2. Wait for Tor to initialize and create the hostname file
echo "Waiting for Tor to generate onion address..."
sleep 15

# 3. Print the Onion Address
echo "=================================================="
echo "YOUR PERMANENT ONION ADDRESS IS:"
if [ -f /var/lib/tor/hidden_service/hostname ]; then
    cat /var/lib/tor/hidden_service/hostname
else
    echo "ERROR: Hostname file not found!"
fi
echo "=================================================="

# 4. Start your Spring Boot App in foreground
echo "Starting Spring Boot application..."
exec java -jar /app/app.jar