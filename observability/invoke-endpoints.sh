#!/bin/bash

END=$((SECONDS + 60))

echo "Starting load test for 60 seconds..."

while [ $SECONDS -lt $END ]; do
  curl -s -o /dev/null -w "prompts: %{http_code}\n" --request POST \
    --url http://localhost:8080/springai/v1/prompts \
    --header 'Content-Type: application/json' \
    --data '{"prompt": "Build a rest client in Python"}'

  curl -s -o /dev/null -w "tool_calling: %{http_code}\n" --request POST \
    --url http://localhost:8080/springai/v1/tool_calling \
    --header 'Content-Type: application/json' \
    --data '{"prompt": "Write a function to generate a sum of 2 numbers"}'

  sleep 1
done

echo "Load test complete."
