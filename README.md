# Spring Telegram Bot — Webhook + Redis (RU menu flow)

Готовый шаблон бота на **Spring Boot 3.5**, **Webhook**, **Redis** для сессий и заказов.
- Инлайн-меню на русском (по схеме: Услуги → Выгул/Передержка/Няня → описание → связь с диспетчером)
- «Черновик заявки» (`DRAFT`) создаётся при выборе услуги/подтипа, затем превращается в `NEW`
- Админ-команды: `/orders [N]`, `/setstatus <id> <STATUS>`

## Быстрый старт
1. Redis: `docker run -p 6379:6379 redis:7`
2. Среда (IntelliJ → Run Config → Environment variables):
   ```
   TELEGRAM_BOT_TOKEN=123456:ABC...
   TELEGRAM_BOT_USERNAME=your_bot
   TELEGRAM_WEBHOOK_URL=https://<public-host>/webhook/telegram
   TELEGRAM_WEBHOOK_SECRET=supersecret
   ```
3. Запусти: `mvn spring-boot:run`
4. Проверка локально:
   ```bash
   curl -X POST "http://localhost:8080/webhook/telegram"      -H "Content-Type: application/json"      -H "X-Telegram-Bot-Api-Secret-Token: supersecret"      -d '{"update_id":1,"message":{"message_id":1,"date":0,"chat":{"id":123,"type":"private"},"text":"/start"}}'
   ```
