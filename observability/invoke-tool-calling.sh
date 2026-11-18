#!/bin/bash

BASE_URL="http://localhost:8080/springai"
DURATION=60
END=$((SECONDS + DURATION))

echo "Starting tool calling load test for ${DURATION} seconds..."
echo "Target: ${BASE_URL}"
echo ""

while [ $SECONDS -lt $END ]; do

  curl -s -o /dev/null -w "weather:   %{http_code}  (%{time_total}s)\n" \
    --max-time 30 --request POST \
    --url "${BASE_URL}/v1/tool_calling" \
    --header 'Content-Type: application/json' \
    --header 'USER_ID: user-123' \
    --data '{"prompt": "What is the current weather in London?"}'

  curl -s -o /dev/null -w "currency:  %{http_code}  (%{time_total}s)\n" \
    --max-time 30 --request POST \
    --url "${BASE_URL}/v1/tool_calling" \
    --header 'Content-Type: application/json' \
    --header 'USER_ID: user-123' \
    --data '{"prompt": "Convert 100 USD to EUR and GBP"}'

  curl -s -o /dev/null -w "datetime:  %{http_code}  (%{time_total}s)\n" \
    --max-time 30 --request POST \
    --url "${BASE_URL}/v1/tool_calling" \
    --header 'Content-Type: application/json' \
    --header 'USER_ID: user-123' \
    --data '{"prompt": "What time is it in Tokyo right now?"}'

  echo "---"
  sleep 2
done

echo "Load test complete."
