#!/bin/bash
# ============================================================
# NoiseMap Demo Script
# Прогоняет полный цикл: регистрация → устройство →
# загрузка записи → геймификация → статистика → карта →
# уведомления → лидерборд
# ============================================================

set -e

BASE_URL="http://localhost"
USER_SVC="$BASE_URL:8081"
REC_SVC="$BASE_URL:8082"
MAP_SVC="$BASE_URL:8083"
STATS_SVC="$BASE_URL:8084"
GAME_SVC="$BASE_URL:8085"
NOTIF_SVC="$BASE_URL:8086"
MOD_SVC="$BASE_URL:8087"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║       NoiseMap — Демонстрация API            ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# ---- Health checks ----
echo -e "${YELLOW}[1/9] Проверка health endpoints...${NC}"
for port in 8081 8082 8083 8084 8085 8086 8087; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL:$port/actuator/health 2>/dev/null || echo "000")
  if [ "$STATUS" = "200" ]; then
    echo -e "  ✅ localhost:$port — ${GREEN}UP${NC}"
  else
    echo -e "  ❌ localhost:$port — ${RED}DOWN (HTTP $STATUS)${NC}"
  fi
done
echo ""

# ---- 1. Register ----
echo -e "${YELLOW}[2/9] Регистрация пользователя...${NC}"
REGISTER_RESPONSE=$(curl -s -X POST "$USER_SVC/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo_'$RANDOM'@noisemap.kz",
    "password": "DemoPass123!",
    "displayName": "Demo User",
    "language": "ru"
  }')

echo "  Response: $REGISTER_RESPONSE"
ACCESS_TOKEN=$(echo $REGISTER_RESPONSE | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
  echo -e "  ${RED}Ошибка: не удалось получить токен${NC}"
  exit 1
fi

echo -e "  ${GREEN}✅ Токен получен: ${ACCESS_TOKEN:0:30}...${NC}"
echo ""

# Decode JWT to get user ID (base64 decode middle part)
USER_ID=$(echo $ACCESS_TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | grep -o '"sub":"[^"]*"' | cut -d'"' -f4)
echo -e "  ${GREEN}✅ User ID: $USER_ID${NC}"
echo ""

# ---- 2. Profile ----
echo -e "${YELLOW}[3/9] Получение профиля...${NC}"
PROFILE=$(curl -s "$USER_SVC/api/v1/users/me" \
  -H "X-User-Id: $USER_ID")
echo "  $PROFILE"
echo ""

# ---- 3. Device calibration справочник ----
echo -e "${YELLOW}[4/9] Калибровка устройства из справочника...${NC}"
DEVICE_CALIBRATION=$(curl -s "$USER_SVC/api/v1/devices/calibration?model=Samsung%20Galaxy%20S24" \
  -H "X-User-Id: $USER_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "  Samsung Galaxy S24: $DEVICE_CALIBRATION"

DEVICE_CALIBRATION2=$(curl -s "$USER_SVC/api/v1/devices/calibration?model=iPhone%2013%20Pro" \
  -H "X-User-Id: $USER_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "  iPhone 13 Pro: $DEVICE_CALIBRATION2"
echo ""

# ---- 4. Check gamification (should be empty) ----
echo -e "${YELLOW}[5/9] Геймификация (до записей)...${NC}"
GAME_BEFORE=$(curl -s "$GAME_SVC/api/v1/gamification/me" \
  -H "X-User-Id: $USER_ID")
echo "  $GAME_BEFORE"
echo ""

# ---- 5. City stats (public) ----
echo -e "${YELLOW}[6/9] Статистика города (публичный endpoint)...${NC}"
CITY_STATS=$(curl -s "$STATS_SVC/api/v1/stats/city")
echo "  $CITY_STATS"
echo ""

# ---- 6. Heatmap tiles (public) — Almaty center ----
echo -e "${YELLOW}[7/9] Тепловая карта Алматы (публичный endpoint)...${NC}"
MAP_RESPONSE=$(curl -s "$MAP_SVC/api/v1/map/tiles?minLat=43.20&minLng=76.85&maxLat=43.30&maxLng=76.99")
echo "  $MAP_RESPONSE"
echo ""

# ---- 7. Notifications ----
echo -e "${YELLOW}[8/9] Уведомления...${NC}"
NOTIF_COUNT=$(curl -s "$NOTIF_SVC/api/v1/notifications/unread-count" \
  -H "X-User-Id: $USER_ID")
echo "  Непрочитанных: $NOTIF_COUNT"
echo ""

# ---- 8. Leaderboard ----
echo -e "${YELLOW}[9/9] Лидерборд (топ-5)...${NC}"
LEADERBOARD=$(curl -s "$GAME_SVC/api/v1/gamification/leaderboard?limit=5")
echo "  $LEADERBOARD"
echo ""

# ---- Summary ----
echo -e "${CYAN}╔═════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║              Демо завершено!                    ║${NC}"
echo -e "${CYAN}╠═════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║                                                 ║${NC}"
echo -e "${CYAN}║  Swagger UI (агрегированный через Gateway):     ║${NC}"
echo -e "${CYAN}║  http://localhost:8080/swagger-ui.html          ║${NC}"
echo -e "${CYAN}║                                                 ║${NC}"
echo -e "${CYAN}║  Swagger UI каждого сервиса напрямую:           ║${NC}"
echo -e "${CYAN}║  • User:         localhost:8081/swagger-ui.html ║${NC}"
echo -e "${CYAN}║  • Recording:    localhost:8082/swagger-ui.html ║${NC}"
echo -e "${CYAN}║  • Mapping:      localhost:8083/swagger-ui.html ║${NC}"
echo -e "${CYAN}║  • Statistics:   localhost:8084/swagger-ui.html ║${NC}"
echo -e "${CYAN}║  • Gamification: localhost:8085/swagger-ui.html ║${NC}"
echo -e "${CYAN}║  • Notification: localhost:8086/swagger-ui.html ║${NC}"
echo -e "${CYAN}║  • Moderation:   localhost:8087/swagger-ui.html ║${NC}"
echo -e "${CYAN}║  • Comment:      localhost:8088/swagger-ui.html ║${NC}"
echo -e "${CYAN}║                                                 ║${NC}"
echo -e "${CYAN}║  RabbitMQ UI:    localhost:15672 (guest/guest)  ║${NC}"
echo -e "${CYAN}║  Prometheus:     localhost:9090                 ║${NC}"
echo -e "${CYAN}║  Grafana:        localhost:3000 (admin/admin)   ║${NC}"
echo -e "${CYAN}║                                                 ║${NC}"
echo -e "${CYAN}╚═════════════════════════════════════════════════╝${NC}"