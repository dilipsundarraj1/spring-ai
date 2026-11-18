#!/bin/bash

BASE_URL="http://localhost:8080/springai"
DURATION=60
END=$((SECONDS + DURATION))

echo "Starting tool calling load test (with errors) for ${DURATION} seconds..."
echo "Target: ${BASE_URL}"
echo "Valid and invalid requests will be mixed to generate error metrics."
echo ""

ITERATION=0

while [ $SECONDS -lt $END ]; do
  ITERATION=$((ITERATION + 1))
  echo "=== Iteration ${ITERATION} ==="

  # --- weather: valid city ---
  curl -s -o /dev/null -w "weather   [valid]   London       %{http_code}  (%{time_total}s)\n" \
    --max-time 30 --request POST \
    --url "${BASE_URL}/v1/tool_calling" \
    --header 'Content-Type: application/json' \
    --header 'USER_ID: user-123' \
    --data '{"prompt": "What is the current weather in London?"}'

  # --- weather: invalid city (triggers 400 from weather API → error counter) ---
  curl -s -o /dev/null -w "weather   [invalid] INVALIDCITY  %{http_code}  (%{time_total}s)\n" \
    --max-time 30 --request POST \
    --url "${BASE_URL}/v1/tool_calling" \
    --header 'Content-Type: application/json' \
    --header 'USER_ID: user-123' \
    --data '{"prompt": "What is the current weather in INVALIDCITY_XYZ_DOESNOTEXIST?"}'

  echo ""
  sleep 2
done

echo "Load test complete."
