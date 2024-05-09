#!/bin/bash

# The number of concurrent requests you want to make
CONCURRENT_REQUESTS=10

for i in $(seq 1 $CONCURRENT_REQUESTS)
do
   curl http://localhost:8080/index.html &  # Replace with your server details
done

wait
echo "All requests sent."
