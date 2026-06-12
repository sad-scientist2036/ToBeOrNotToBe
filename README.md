# Theatre Booking System

Веб-сервис для бронирования мест на спектакль с гарантией отсутствия двойного бронирования, real-time обновлением схемы зала и возможностью отмены бронирования.

Проект показывает реализацию конкурентного бронирования с использованием пессимистической блокировки в PostgreSQL, SSE (Server-Sent Events) для обновления схемы в реальном времени и полным циклом регистрации/аутентификации пользователей.

## Что делает приложение

Приложение запускает:

1. **Веб-интерфейс (Thymeleaf)** с интерактивной схемой зала.
2. **Регистрацию и аутентификацию пользователей** через Spring Security.
3. **Схему зала 10×20 мест** с отображением статуса каждого места:
   - 🟢 **Зеленый** — свободно
   - 🟡 **Желтый** — временно забронировано (HOLD, 5 минут)
   - 🔴 **Красный** — занято (продано)
4. **REST API** для всех операций с местами и билетами.
5. **Swagger UI** для просмотра и тестирования API.
6. **Базу данных PostgreSQL**, в которой сохраняются залы, места, билеты и пользователи.
7. **Real-time обновления схемы** через SSE (Server-Sent Events) — при бронировании места у всех открытых клиентов схема обновляется автоматически.
8. **Блокировку мест** при одновременных запросах через `SELECT FOR UPDATE`.

В начале работы (при первом запуске) автоматически создаётся зал с 200 местами (10 рядов × 20 мест). Каждый пользователь может зарегистрироваться, выбрать свободное место, ввести свои данные (имя и телефон) и оформить билет. Место временно резервируется на 5 минут после нажатия кнопки "Hold".

## Модель данных

### Сущности

**Hall (Зал)**
- `id`
- `name` (название зала)
- `rows` (количество рядов)
- `seatsPerRow` (мест в ряду)

**Seat (Место)**
- `id`
- `rowNum` (номер ряда)
- `seatNum` (номер места)
- `status` (`FREE`, `HOLD`, `BOOKED`)
- `holdExpiresAt` (время до которого зарезервировано)
- `hall` (связь с залом)

**Ticket (Билет)**
- `id`
- `seat` (связь с местом, уникально)
- `user` (связь с пользователем, опционально)
- `customerName` (имя покупателя)
- `customerPhone` (телефон покупателя)
- `bookedAt` (время покупки)

**User (Пользователь)**
- `id`
- `email` (уникально)
- `password` (зашифрован)
- `name`
- `phone` (уникально)
- `role` (`USER` или `ADMIN`)
- `createdAt`

### Связи между сущностями

- Зал имеет много мест (`One-to-Many`)
- Место может иметь один билет (`One-to-One`, уникальность)
- Пользователь может иметь много билетов (`One-to-Many`)

В БД данные хранятся в четырёх таблицах:
- `halls`
- `seats`
- `tickets`
- `users`

Связи организованы через внешние ключи:
- `seats.hall_id` → `halls.id`
- `tickets.seat_id` → `seats.id` 
- `tickets.user_id` → `users.id`

## Технологии

- **Java 21**
- **Maven**
- **Spring Boot 3.2.5**
- **Spring Web MVC**
- **Spring Security 6.2.4**
- **Spring Data JPA / Hibernate**
- **PostgreSQL**
- **springdoc-openapi + Swagger UI**
- **Testcontainers** (интеграционные тесты)
- **JUnit 5 + Mockito** (unit-тесты)
- **Thymeleaf** (шаблонизатор)
- **JavaScript + SSE** (real-time обновления)

## Сборка и запуск

### Требования
- Java 21+
- PostgreSQL 15+
- Maven 3.8+

## Подготовка базы данных

### Создать базу данных
```bash
createdb -U postgres theatre_db
```
### Сборка jar-файла:
```bash
mvn clean package
```
### Запуск собранного jar-файла:
```bash
java -jar target/ToBeOrNotToBe-1.0.0.jar
```

## REST API

### Получить схему зала (все места):

```bash
curl -X GET http://localhost:8080/api/seats
```
### Забронировать место (Hold):

```bash
curl -X POST http://localhost:8080/api/seats/1/hold
```
### Подтвердить бронирование (Confirm):

```bash
curl -X POST http://localhost:8080/api/seats/1/confirm \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Иван Петров","customerPhone":"89220000000"}'
```
### Получить статистику зала:

```bash
curl -X GET http://localhost:8080/api/stats
```
### Получить мои билеты:

```bash
curl -X GET http://localhost:8080/api/my/bookings
```
### Отменить бронирование:

```bash
curl -X POST http://localhost:8080/api/seats/1/cancel-hold
```
### Освободить место (отмена билета):

```bash
curl -X POST http://localhost:8080/api/seats/1/release
```

## Swagger UI
Swagger UI доступен по адресу: http://localhost:8080/swagger-ui/index.html

## Тестирование
### Запуск всех тестов:

```bash
mvn test
```
### Запуск только unit-тестов:

```bash
mvn test -Dtest=*ServiceTest
```
### Запуск только интеграционных тестов:

```bash
mvn test -Dtest=*IntegrationTest
```
###Запуск конкретного теста:

```bash
mvn test -Dtest=BookingServiceTest
mvn test -Dtest=RepositoryTest#seatRepository_ShouldSaveAndFind
```

## Unit‑тесты

| Тест | Что проверяет |
|------|-------------|
| `holdSeat_ShouldChangeStatusToHold` | Бронирование свободного места → статус HOLD |
| `holdSeat_WhenSeatAlreadyBooked_ShouldThrowException` | Попытка бронирования занятого места → ошибка |
| `confirmBooking_ShouldCreateTicket` | Подтверждение бронирования → создание билета |
| `getAllSeats_ShouldReturnSortedSeats` | Получение всех мест → сортировка по рядам |
| `seatRepository_ShouldSaveAndFind` | Сохранение и поиск места в БД |
| `userRepository_ShouldSaveAndFindByEmail` | Поиск пользователя по email |

## Интеграционные тесты

| Тест | Endpoint | Что проверяет |
|------|----------|-------------|
| `getSeats_ShouldReturnSeatsList` | `GET /api/seats` | API возвращает список мест |
| `holdSeat_ShouldReturnSuccess` | `POST /api/seats/{id}/hold` | API бронирования возвращает успех |
| `confirmBooking_ShouldCreateTicket` | `POST /api/seats/{id}/confirm` | API подтверждения создаёт билет |

## Пример SQL‑запросов в psql

### Подключиться к БД

```bash
psql -U postgres -d theatre_db
```

### Посмотреть все места с их статусом:

```bash
SELECT id, row_num, seat_num, status, hold_expires_at 
FROM seats 
ORDER BY row_num, seat_num;
```
### Посмотреть всех пользователей:

```bash
SELECT id, email, name, phone, role, created_at FROM users;
```
### Посмотреть все билеты с данными места:

```bash
SELECT 
    t.id,
    t.customer_name,
    t.customer_phone,
    t.booked_at,
    s.row_num,
    s.seat_num,
    u.email as user_email
FROM tickets t
LEFT JOIN seats s ON s.id = t.seat_id
LEFT JOIN users u ON u.id = t.user_id
ORDER BY t.booked_at DESC;
```
### Посмотреть занятость зала по рядам:

```bash
SELECT 
    row_num,
    COUNT(*) as total_seats,
    SUM(CASE WHEN status = 'BOOKED' THEN 1 ELSE 0 END) as booked
FROM seats
GROUP BY row_num
ORDER BY row_num;
```

## Дополнительный функционал

- **Временное резервирование мест** — место блокируется на 5 минут после нажатия кнопки «Hold». В это время место недоступно для других пользователей. Если в течение 5 минут бронирование не подтверждено, статус автоматически меняется на `FREE`.
- **Статистика занятости зала** — отображается процент занятости зала в целом и по рядам. Данные обновляются в реальном времени.
- **Отмена бронирования** — пользователь может отменить временное бронирование (HOLD) или полностью аннулировать купленный билет (BOOKED). При отмене:
  - статус места меняется на `FREE` (если было в HOLD);
  - билет удаляется из системы (если был BOOKED);
  - все подключённые клиенты получают обновление через SSE.
- **Просмотр своих билетов** — авторизованный пользователь видит список всех своих оформленных билетов с подробной информацией:
  - номер ряда и места;
  - имя и телефон покупателя;
  - дата и время бронирования;
  - текущий статус билета (`HOLD` или `BOOKED`).
- **Real‑time обновления** — через SSE (Server‑Sent Events) схема зала обновляется у всех пользователей одновременно. При любом изменении статуса места:
  - все открытые клиенты получают уведомление;
  - интерфейс автоматически перерисовывает схему зала;
  - цветовая индикация меняется в соответствии с новым статусом (зелёный/жёлтый/красный).


Дата: 2026-06-12
Версия: 1.0.0
