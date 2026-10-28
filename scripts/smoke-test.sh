#!/usr/bin/env bash
# End-to-end API smoke test through the real HTTP stack (session cookie + CSRF), as a browser would.
# Usage: scripts/smoke-test.sh [base_url]   (default http://localhost:8080; use http://localhost:4200 to go through the Angular proxy)
# Requires: curl, python3, a running backend with BLOOME_DEMO_ACCOUNTS=true.
set -euo pipefail
BASE="${1:-http://localhost:8080}/api/v1"
JAR_C=$(mktemp); JAR_A=$(mktemp); JAR_V=$(mktemp)
trap 'rm -f "$JAR_C" "$JAR_A" "$JAR_V"' EXIT
pass=0; fail=0
check() { if [ "$2" = "$3" ]; then echo "  ✔ $1"; pass=$((pass+1)); else echo "  ✘ $1 (expected $3, got $2)"; fail=$((fail+1)); fi; }
xsrf() { grep XSRF-TOKEN "$1" | awk '{print $7}' | tail -1; }
json() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)"; }
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

echo "Catalog"
check "search 'pivoine' finds Jardin de Pivoines" "$(curl -s "$BASE/products?q=pivoine" | json "d['content'][0]['name']")" "Jardin de Pivoines"
check "price filter respected" "$(curl -s "$BASE/products?maxPrice=300&size=48" | json "all(p['price']<=300 for p in d['content'])")" "True"
check "unknown product → 404" "$(code "$BASE/products/nexiste-pas")" "404"

echo "Security"
curl -s -c "$JAR_V" -b "$JAR_V" "$BASE/auth/csrf" > /dev/null
check "XSRF-TOKEN cookie issued" "$( [ -n "$(xsrf "$JAR_V")" ] && echo yes || echo no)" "yes"
check "visitor → /cart is 401" "$(code -b "$JAR_V" "$BASE/cart")" "401"
check "visitor → /admin/dashboard is 401" "$(code -b "$JAR_V" "$BASE/admin/dashboard")" "401"
check "POST without CSRF header is 403" "$(code -b "$JAR_V" -H 'Content-Type: application/json' -d '{}' "$BASE/auth/login")" "403"

echo "Session security"
JAR_R=$(mktemp)
curl -s -c "$JAR_R" -b "$JAR_R" "$BASE/auth/csrf" > /dev/null
OLD_TOKEN=$(xsrf "$JAR_R")
code -c "$JAR_R" -b "$JAR_R" -H "X-XSRF-TOKEN: $OLD_TOKEN" -H 'Content-Type: application/json' \
  -d '{"email":"client@bloome.local","password":"BloomeClient2026"}' "$BASE/auth/login" > /dev/null
NEW_TOKEN=$(xsrf "$JAR_R")
check "CSRF token rotated at login" "$( [ -n "$NEW_TOKEN" ] && [ "$NEW_TOKEN" != "$OLD_TOKEN" ] && echo yes || echo no)" "yes"
check "pre-login CSRF token rejected afterwards" "$(code -b "$JAR_R" -H "X-XSRF-TOKEN: $OLD_TOKEN" -H 'Content-Type: application/json' -d '{"productId":1,"quantity":1}' "$BASE/cart/items")" "403"
rm -f "$JAR_R"

echo "Customer journey"
curl -s -c "$JAR_A" -b "$JAR_A" "$BASE/auth/csrf" > /dev/null
check "admin login" "$(code -c "$JAR_A" -b "$JAR_A" -H "X-XSRF-TOKEN: $(xsrf "$JAR_A")" -H 'Content-Type: application/json' \
  -d '{"email":"admin@bloome.local","password":"BloomeAdmin2026"}' "$BASE/auth/login")" "200"
HA=(-c "$JAR_A" -b "$JAR_A" -H "X-XSRF-TOKEN: $(xsrf "$JAR_A")" -H 'Content-Type: application/json')
curl -s -c "$JAR_C" -b "$JAR_C" "$BASE/auth/csrf" > /dev/null
check "customer login" "$(code -c "$JAR_C" -b "$JAR_C" -H "X-XSRF-TOKEN: $(xsrf "$JAR_C")" -H 'Content-Type: application/json' \
  -d '{"email":"client@bloome.local","password":"BloomeClient2026"}' "$BASE/auth/login")" "200"
H=(-c "$JAR_C" -b "$JAR_C" -H "X-XSRF-TOKEN: $(xsrf "$JAR_C")" -H 'Content-Type: application/json')
PID=$(curl -s "$BASE/products/petit-bonheur" | json "d['id']")
stock() { curl -s -b "$JAR_A" "$BASE/admin/products/$PID" | json "d['stockQuantity']"; }
STOCK_BEFORE=$(stock)
check "customer → admin API is 403" "$(code -b "$JAR_C" "$BASE/admin/dashboard")" "403"
curl -s "${H[@]}" -X DELETE "$BASE/cart" > /dev/null
TOTAL=$(curl -s "${H[@]}" -d "{\"productId\":$PID,\"quantity\":2,\"price\":1}" "$BASE/cart/items" | json "d['total']")
check "server prices the cart (2 × 190 + 35 delivery)" "$TOTAL" "415.0"
DATE=$(python3 -c "import datetime;print(datetime.date.today()+datetime.timedelta(days=2))")
ORDER=$(curl -s "${H[@]}" -d "{\"customerName\":\"Yasmine Alaoui\",\"customerEmail\":\"client@bloome.local\",\"customerPhone\":\"0612345678\",\"addressLine1\":\"12 rue des Orangers\",\"city\":\"Rabat\",\"giftWrapping\":false,\"deliveryDate\":\"$DATE\",\"deliverySlot\":\"MORNING\",\"paymentMethod\":\"CASH_ON_DELIVERY\",\"idempotencyKey\":\"smoke-$(date +%s%N)\",\"expectedTotal\":415}" "$BASE/orders")
OID=$(echo "$ORDER" | json "d['id']")
check "COD order placed, status PENDING" "$(echo "$ORDER" | json "d['status']")" "PENDING"
check "stock decremented by 2" "$(stock)" "$((STOCK_BEFORE - 2))"
check "order visible in history" "$(curl -s -b "$JAR_C" "$BASE/orders/$OID" | json "d['id']")" "$OID"

echo "Administration"
check "invalid transition PENDING → DELIVERED refused" "$(code "${HA[@]}" -X PATCH -d '{"status":"DELIVERED"}' "$BASE/admin/orders/$OID/status")" "409"
check "PENDING → CONFIRMED accepted" "$(curl -s "${HA[@]}" -X PATCH -d '{"status":"CONFIRMED"}' "$BASE/admin/orders/$OID/status" | json "d['status']")" "CONFIRMED"
check "customer can still cancel a CONFIRMED order" "$(curl -s "${H[@]}" -d '{}' "$BASE/orders/$OID/cancel" | json "d['status']")" "CANCELLED"
check "cancellation returned the stock" "$(stock)" "$STOCK_BEFORE"
check "dashboard reachable for admin" "$(code -b "$JAR_A" "$BASE/admin/dashboard")" "200"

echo; echo "$pass passed, $fail failed"; [ "$fail" -eq 0 ]
