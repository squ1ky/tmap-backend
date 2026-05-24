# Database Standards

## Именование

| Объект | Формат | Пример |
|---|---|---|
| Таблицы | `snake_case`, множественное число | `users`, `venues`, `transactions` |
| Колонки | `snake_case` | `created_at`, `password_hash`, `photo_object_key` |
| Индексы | `idx_<таблица>_<колонки>` | `idx_venues_h3_res9`, `idx_transactions_occurred_at` |
| Внешние ключи | `fk_<таблица>_<ссылаемая>` (в именовании ограничений) | `fk_venues_users` |

---

## Domain Entity

`@Entity`-класс живёт в слое `domain`. Бизнес-логика, которая касается только полей самой сущности, — уместна здесь.

```java
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    @ToString.Include
    private String email;

    @Column(name = "blocked", nullable = false)
    private boolean blocked;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public User(UUID id, String email, String passwordHash, String nickname, UserRole role) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = Objects.requireNonNull(email, "email");
        ...
    }

    public static User create(String email, String passwordHash, String nickname) {
        return new User(UUID.randomUUID(), email, passwordHash, nickname, UserRole.USER);
    }

    public void block() {
        if (blocked) throw new UserAlreadyBlockedException(id);
        blocked = true;
    }

    public void unblock() {
        if (!blocked) throw new UserNotBlockedException(id);
        blocked = false;
    }
}
```

**Правила оформления:**
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — JPA требует, но прямое создание через него запрещено
- `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` + `@EqualsAndHashCode.Include` только на `id`
- Всегда явно `@Column(name = "...")` и `@Table(name = "...")`
- `insertable = false, updatable = false` на колонках, которыми управляет БД (`created_at`)
- Все enum-колонки: `@Enumerated(EnumType.STRING)`

**Правило:** если методу нужен репозиторий, другой сервис или внешние данные — он принадлежит Service, не Entity.

---

## Repository Pattern

Домен определяет **интерфейс** репозитория без Spring-зависимостей. Инфраструктура реализует его.

```java
// user/domain/UserRepository.java — чистый интерфейс в домене
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> search(UserSearchCriteria criteria, Pageable pageable);
}

// user/infrastructure/db/JpaUserRepository.java — адаптер в инфраструктуре
public interface JpaUserRepository
        extends UserRepository, JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    @Override
    default Page<User> search(UserSearchCriteria criteria, Pageable pageable) {
        return findAll(toSpecification(criteria), pageable);
    }

    private static Specification<User> toSpecification(UserSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.nickname() != null && !criteria.nickname().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("nickname")),
                        "%" + criteria.nickname().toLowerCase(Locale.ROOT) + "%"
                ));
            }
            if (criteria.blocked() != null) {
                predicates.add(cb.equal(root.get("blocked"), criteria.blocked()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
```

Сервис работает только с интерфейсом `UserRepository` — инфраструктурный адаптер ему не виден.

---

## JDBC для read-запросов

Сложные read-only выборки (viewport, поиск, агрегация) реализуются через `NamedParameterJdbcTemplate` с `DataClassRowMapper` на read-проекцию. JPA для этого не используется.

```java
// venue/infrastructure/db/jdbc/JdbcVenueQueryRepository.java
@Repository
public class JdbcVenueQueryRepository implements VenueQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataClassRowMapper<VenueProjection> rowMapper =
            new DataClassRowMapper<>(VenueProjection.class);

    @Override
    public List<VenueProjection> findActiveInViewport(BoundingBox bbox, List<VenueCategory> categories) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, name, address, lat, lng, category,
                       photo_object_key AS photoObjectKey
                FROM venues
                WHERE status = :status
                  AND lat BETWEEN :swLat AND :neLat
                  AND lng BETWEEN :swLng AND :neLng
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", VenueStatus.ACTIVE.name())
                .addValue("swLat", bbox.swLat())
                .addValue("neLat", bbox.neLat())
                .addValue("swLng", bbox.swLng())
                .addValue("neLng", bbox.neLng());

        if (categories != null && !categories.isEmpty()) {
            sql.append(" AND category IN (:categories)");
            params.addValue("categories", categories.stream().map(Enum::name).toList());
        }
        sql.append(" ORDER BY name, id");

        return jdbcTemplate.query(sql.toString(), params, rowMapper);
    }
}
```

`VenueProjection` — `record` в `application/query/`, колонки маппятся через алиасы (`AS photoObjectKey`).

---

## Optional в репозиториях

Методы поиска возвращают `Optional<T>`. В сервисе — бросают доменное исключение:

```java
User user = userRepository.findById(id)
        .orElseThrow(() -> UserNotFoundException.byId(id));
```

---

## Миграции (Flyway)

Файлы в `src/main/resources/db/migration/`.

### Именование файлов

```
V001__create_core_tables.sql
V004__init_districts_and_h3.sql
V006__make_loyalty_discounts_integer.sql
V007__rename_venues_photo_url_to_photo_object_key.sql
V008__create_venue_pending_updates.sql
```

Формат: `V<версия>__<описание>.sql` — два подчёркивания, описание через `_`. Версия должна только расти — пропуски допустимы.

### Пример миграции

```sql
CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    email         varchar(255) UNIQUE NOT NULL,
    password_hash varchar(255) NOT NULL,
    nickname      varchar(255) NOT NULL,
    role          varchar(32)  NOT NULL,
    blocked       boolean      NOT NULL DEFAULT false,
    created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE venues (
    id         uuid             PRIMARY KEY,
    owner_id   uuid             NOT NULL REFERENCES users (id),
    name       varchar(255)     NOT NULL,
    status     varchar(32)      NOT NULL DEFAULT 'PENDING',
    created_at timestamptz      NOT NULL DEFAULT now(),
    updated_at timestamptz      NOT NULL DEFAULT now()
);

CREATE INDEX idx_venues_status ON venues (status);
```

### Правила

- Применённый файл — никогда не редактировать и не удалять: Flyway упадёт с checksum-ошибкой
- Один файл — одно логическое изменение
- Версии только возрастают

**Добавление колонки в таблицу с данными:**

```sql
-- Хорошо — с DEFAULT, иначе миграция упадёт на непустой таблице
ALTER TABLE venues ADD COLUMN reject_reason text;

-- Хорошо — NOT NULL только с дефолтом
ALTER TABLE loyalty_rules
    ADD COLUMN discount_percent integer NOT NULL DEFAULT 0;
```

**Деструктивные операции** (`DROP TABLE`, `DROP COLUMN`):
- Обсудить с командой до написания миграции
- Два PR: сначала убрать использование в коде → потом дропать в БД
- Не делать в конце спринта перед демо
