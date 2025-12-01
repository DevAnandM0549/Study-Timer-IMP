#!/bin/sh

# 1. Start Tor in the background
tor -f /etc/tor/torrc &

# 2. Start your Spring Boot App
# We use a wildcard (*) so it finds your jar file automatically
java -jar /app/app.jar &

# 3. Wait a moment for Tor to create the secret key
sleep 10

# 4. Print the Onion Address so you can see it
echo "=================================================="
echo "YOUR PERMANENT ONION ADDRESS IS:"
cat /var/lib/tor/hidden_service/hostname
echo "=================================================="

# 5. Keep running forever
wait -n