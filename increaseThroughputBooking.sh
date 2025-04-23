#!/bin/bash

CONCURRENCY=30

URL="http://localhost:8080/bookings/check-availability/6808ebad0663ca405bcb62e9?startDate=2025-01-01&endDate=2025-01-05"

START_TIME=$(date +%s)
END_TIME=$((START_TIME + 30))

make_request() {
    RESPONSE_TIME=$(curl -s -o /dev/null -w "%{time_total}\n" "$URL")
    echo "$(date '+%H:%M:%S') - Response time: ${RESPONSE_TIME}s"
}

while [ $(date +%s) -lt $END_TIME ]; do
    for ((i=1; i<=CONCURRENCY; i++)); do
        make_request &
    done
    
    sleep 0.5
done

echo "Load test completed!"