# Coding Standards

Стандарты бэкенд-команды. Читай целиком один раз — потом обращайся к нужному разделу.

> Если что-то не описано здесь — ориентируйся на [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

---

## Структура проекта

Package by feature. Каждый модуль самодостаточен и содержит свои слои внутри.

```
ru.tbank.tmap
├── user
│   ├── api              # публичные интерфейсы модуля (Facade) для использования другими модулями
│   ├── application      # сервисы, команды (command/), проекции (query/), порты (port/)
│   ├── domain           # сущности, репозиторные интерфейсы, доменные исключения
│   ├── infrastructure   # JPA/JDBC-адаптеры, хранилища, внешние клиенты
│   └── presentation     # контроллеры, маперы presentation → domain
├── venue
│   └── ...              # та же структура
├── auth
│   └── ...
├── shared               # кросс-модульное: ErrorResponse, GeoPoint, SecurityUtils
└── infrastructure       # глобальные Spring-бины: SecurityConfig, KafkaConfig, MinioConfig
```

Слои внутри модуля:

| Слой | Что лежит |
|---|---|
| `api` | Facade-интерфейсы для вызова модуля из других модулей |
| `application` | `@Service`-классы, команды (record), проекции (record), порты (interface) |
| `domain` | `@Entity`-классы, интерфейсы репозиториев, бизнес-исключения |
| `infrastructure` | `JpaXxx`, `JdbcXxx`, адаптеры к внешним системам (Minio, Kafka) |
| `presentation` | `@RestController`-классы, mapper-компоненты |

---

## Ключевые правила

**Spring**
- Инъекция только через конструктор — `@RequiredArgsConstructor`, никакого `@Autowired` на поле
- `@Transactional` — на сервисном слое; `readOnly = true` для методов чтения
- Секреты — только через переменные окружения, не хардкодить в `application.yml`

**Именование**
- Классы: `AdminUserController`, `AdminUserService`, `UserRepository` (интерфейс), `JpaUserRepository` (адаптер)
- Mapper-компоненты: `AdminUserMapper`, `VenueMapper` — `@Component`, без интерфейса
- Таблицы и колонки БД: `snake_case`, таблицы во множественном числе (`users`, `venues`, `transactions`)
- Методы тестов: `methodName_whenCondition_thenExpected`

**Код**
- `Optional<T>` из репозиториев — не возвращать `null`
- Логирование через `@Slf4j` — не `System.out.println`
- Никаких стектрейсов в ответах API клиенту

**API**
- Spec-first: контракт описывается в `openapi.yaml`, интерфейсы генерируются через OpenAPI Generator
- Все эндпоинты версионируются: `/api/v1/...`
- Единый формат ошибок: `ErrorResponse(ErrorCode code, String message)`

---

## Детальные стандарты

| Тема | Документ |
|---|---|
| REST API, OpenAPI, формат ответов | [standards/api.md](standards/api.md) |
| JPA, JDBC, PostgreSQL, миграции | [standards/database.md](standards/database.md) |
| JUnit 5, Mockito, WebMvcTest | [standards/testing.md](standards/testing.md) |
| JWT, секреты, обработка ошибок | [standards/security.md](standards/security.md) |
