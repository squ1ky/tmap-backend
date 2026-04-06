# Coding Standards

Стандарты бэкенд-команды. Читай целиком один раз — потом обращайся к нужному разделу.

> Если что-то не описано здесь — ориентируйся на [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

---

## Структура проекта

Package by layer:

```
com.company.citypulse
├── controller        # REST-контроллеры
├── service           # бизнес-логика (интерфейсы + реализации)
├── repository        # Spring Data JPA репозитории
├── entity            # JPA-сущности
├── dto               # Request / Response объекты
├── mapper            # маппинг entity ↔ dto
├── config            # Spring-конфигурации
└── exception         # кастомные исключения + GlobalExceptionHandler
```

---

## Ключевые правила

**Spring**
- Инъекция только через конструктор — `@RequiredArgsConstructor`, никакого `@Autowired` на поле
- `@Transactional` — на сервисном слое; `readOnly = true` для методов чтения
- Секреты — только через переменные окружения, не хардкодить в `application.yml`

**Именование**
- Классы: `UserController`, `UserService`, `UserServiceImpl`, `UserRepository`, `UserEntity`, `CreateUserRequest`, `UserResponse`
- Таблицы и колонки БД: `snake_case`, таблицы во множественном числе (`transactions`, `locations`)
- Методы тестов: `methodName_whenCondition_thenExpected`

**Код**
- `Optional<T>` из репозиториев — не возвращать `null`
- Логирование через `@Slf4j` — не `System.out.println`
- Никаких стектрейсов в ответах API клиенту

**API**
- Spec-first: контракт описывается в `openapi.yaml`, интерфейсы генерируются через OpenAPI Generator
- Все эндпоинты версионируются: `/api/v1/...`
- Единый формат ошибок: `ErrorResponse` с полями `code` и `message`

---

## Детальные стандарты

| Тема | Документ |
|---|---|
| REST API, OpenAPI, формат ответов | [standards/api.md](standards/api.md) |
| JPA, PostgreSQL, миграции | [standards/database.md](standards/database.md) |
| JUnit 5, Mockito, TestContainers | [standards/testing.md](standards/testing.md) |
| JWT, секреты, обработка ошибок | [standards/security.md](standards/security.md) |