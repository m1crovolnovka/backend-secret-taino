# Taino Backend

Production API для «Тайно» — анонимный Тайный Санта в Telegram Mini App.

**Стек:** Java 21, Spring Boot 3.4, Maven, Spring Security, JPA, Flyway, PostgreSQL, ImgBB, Spring Scheduling, Springdoc OpenAPI, Bucket4j, Testcontainers.

## Быстрый старт

```bash
cp .env.example .env
# TELEGRAM_BOT_TOKEN, JWT_SECRET (>=32), IMGBB_API_KEY

docker compose up -d postgres
mvn spring-boot:run
```

- API: http://localhost:8080  
- OpenAPI UI: http://localhost:8080/api/swagger  
- Health: http://localhost:8080/actuator/health  

Полный стек (JAR собирается внутри Docker, кэш Maven ускоряет повторные сборки):

```bash
docker compose up --build
```

## Аутентификация

Единственный вход — Telegram Mini App `initData`:

```http
POST /api/auth/telegram
{"initData":"<raw initData>"}
```

Бэкенд проверяет HMAC-SHA256 по токену бота и `auth_date` (TTL ~1ч). Ответ — JWT (`accessToken`).  
Клиентскому `user.id` без подписи не верим. Username обновляется при каждом входе (может быть `null`).

## Картинки (ImgBB)

`ImageStorage` → `ImgbbImageStorage` (`IMGBB_API_KEY`).  
Перед upload: sniff MIME, ресайз ≤1600px, JPEG re-encode (EXIF снимается), имя UUID.

## Фазы комнаты

`REGISTRATION` → `DRAW_DONE` → `DELIVERY` → `FINISHED`

Авто-раздача (Spring Scheduling): derangement (случайный цикл), минимум 3 участника, идемпотентно под `SELECT FOR UPDATE`.  
Повторная раздача — только админ с `confirm=true&redraw=true`.

## Режимы админа

| Режим | Участвует | Админ-вкладка (цвета/формы) | Настройки |
|-------|-----------|-----------------------------|-----------|
| `ADMIN_PARTICIPATES` | да | да | да |
| `ADMIN_NOT_PARTICIPATES` | нет | да | да |
| `ADMIN_AS_USER` | да | нет | да |

Связки «кто→кому» **не отдаются** ни одному API (включая админа).

## Основные эндпоинты

- `GET/POST /api/rooms`, `POST /api/rooms/join`, `GET/PUT /api/rooms/{id}`
- `GET/PUT /api/rooms/{id}/me/form`, `.../wishlist`, `POST .../images`
- `GET /api/rooms/{id}/assignment` — анонимный вишлист
- `POST /api/rooms/{id}/me/change-requests`
- `/api/admin/rooms/{id}/**` — participants, forms, wishlist, draw, kick, change-requests
- `POST /api/bot/webhook` — `/start` + deep-link `room_<code>`

## BotFather

1. Создай бота, получи токен → `TELEGRAM_BOT_TOKEN`.
2. Menu Button / Web App URL → URL фронта (Vercel).
3. Домен Mini App должен совпадать с CORS (`CORS_ALLOWED_ORIGINS`).
4. Deep-link: `https://t.me/<bot>?start=room_CODE` или `startapp=room_CODE`.

## Тесты

```bash
mvn test
```

Unit: initData, JWT, derangement, ImageProcessor, sanitizer.  
Integration (Testcontainers): требует Docker Desktop.

## Безопасность

- Admin endpoints проверяют владельца + режим на сервере (не клиентские флаги).
- IDOR: membership на каждый запрос.
- Bean Validation, sanitizer текста, только http/https ссылки.
- Rate limit на `/api/auth/**`.
- Секреты только из env; initData/JWT/assignments не логируются.
- Audit log админских действий.

## Env

См. `.env.example`. Файл `.env` не коммитить.
let's go