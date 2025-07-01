#!/bin/bash

# Aurora Transaction Test Script
# Usage: ./scripts/test-transaction.sh [host] [port]

HOST=${1:-localhost}
PORT=${2:-8080}
BASE_URL="http://${HOST}:${PORT}"

echo "🚀 Testing Aurora Transaction Ingest API at ${BASE_URL}"
echo "=================================================="

# Generate a sample transaction
TXN_ID=$(uuidgen)
ACCOUNT="ACC$(date +%s)"
AMOUNT=$(echo "scale=2; $(($RANDOM % 100000)) / 100" | bc)
CURRENCY="USD"
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

echo "📝 Generated transaction:"
echo "  Transaction ID: ${TXN_ID}"
echo "  Account: ${ACCOUNT}"
echo "  Amount: ${AMOUNT} ${CURRENCY}"
echo "  Timestamp: ${TIMESTAMP}"
echo

# Create JSON payload
PAYLOAD=$(cat <<EOF
{
  "txnId": "${TXN_ID}",
  "account": "${ACCOUNT}",
  "amount": ${AMOUNT},
  "currency": "${CURRENCY}",
  "timestamp": "${TIMESTAMP}"
}
EOF
)

echo "📤 Sending transaction..."
RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST "${BASE_URL}/v1/transactions" \
  -H "Content-Type: application/json" \
  -d "${PAYLOAD}")

HTTP_CODE=$(echo "${RESPONSE}" | tail -n1)
BODY=$(echo "${RESPONSE}" | head -n -1)

if [ "${HTTP_CODE}" = "202" ]; then
  echo "✅ Transaction accepted successfully!"
  echo "📥 Response: ${BODY}"
  echo
  
  # Wait a moment for processing
  echo "⏳ Waiting 2 seconds for processing..."
  sleep 2
  
  # Try to retrieve the transaction
  echo "🔍 Retrieving transaction..."
  RETRIEVE_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X GET "${BASE_URL}/v1/transactions/${TXN_ID}")
  
  RETRIEVE_HTTP_CODE=$(echo "${RETRIEVE_RESPONSE}" | tail -n1)
  RETRIEVE_BODY=$(echo "${RETRIEVE_RESPONSE}" | head -n -1)
  
  if [ "${RETRIEVE_HTTP_CODE}" = "200" ]; then
    echo "✅ Transaction retrieved successfully!"
    echo "📄 Transaction details: ${RETRIEVE_BODY}"
  else
    echo "⚠️  Transaction not found (may still be processing)"
    echo "📄 Response code: ${RETRIEVE_HTTP_CODE}"
  fi
  
else
  echo "❌ Transaction failed!"
  echo "📄 HTTP Code: ${HTTP_CODE}"
  echo "📄 Response: ${BODY}"
fi

echo
echo "🏥 Checking service health..."
HEALTH_RESPONSE=$(curl -s "${BASE_URL}/v1/transactions/health")
echo "📊 Health status: ${HEALTH_RESPONSE}"

echo
echo "📈 Checking metrics endpoint..."
METRICS_RESPONSE=$(curl -s "${BASE_URL}/actuator/health")
echo "📊 Actuator health: ${METRICS_RESPONSE}"

echo
echo "🎉 Test complete!" 