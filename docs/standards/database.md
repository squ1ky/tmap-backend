# Database Standards

## Именование

| Объект | Формат | Пример |
|---|---|---|
| Таблицы | `snake_case`, множественное число | `transactions`, `heat_zones` |
| Колонки | `snake_case` | `created_at`, `avg_check` |
| Индексы | `idx_<таблица>_<колонка>` | `idx_transactions_zone_id` |
| Внешние ключи | `fk_<таблица>_<ссылаемая>` | `fk_transactions_locations` |

---

## Entity

`@Entity`-класс отвечает за маппинг на таблицу. Простая логика которая касается **только полей самой сущности** — допустима и часто делает код чище.

```java
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_id", nullable = false)
    private String zoneId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "refunded", nullable = false)
    private boolean refunded = false;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private LocationEntity location;

    // Допустимо — работает только с полями этой сущности
    public void refund() {
        if (refunded) throw new IllegalStateException("Transaction already refunded");
        this.refunded = true;
    }

    public boolean isHighValue() {
        return amount.compareTo(BigDecimal.valueOf(5000)) > 0;
    }
}
```

**Правило:** если методу нужен репозиторий, другой сервис или внешние данные — он принадлежит Service, не Entity.

**Правила оформления:**
- Всегда явно указывать `@Table(name = "...")` и `@Column(name = "...")`
- ID — только `Long`, не примитив `long` (поддерживает `null` до сохранения)
- Все связи по умолчанию `FetchType.LAZY`

---

## N+1 проблема

`FetchType.EAGER` не использовать. Для загрузки связанных сущностей — `JOIN FETCH` или `@EntityGraph`.

```java
// Плохо — EAGER провоцирует N+1
@OneToMany(fetch = FetchType.EAGER)
private List<TransactionEntity> transactions;

// Хорошо — явная загрузка через JPQL
@Query("SELECT l FROM LocationEntity l JOIN FETCH l.transactions WHERE l.id = :id")
Optional<LocationEntity> findByIdWithTransactions(@Param("id") Long id);
```

---

## Optional в репозиториях

Методы поиска возвращают `Optional<T>`, не `null`.

```java
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByZoneIdAndOccurredAt(String zoneId, LocalDateTime occurredAt);

    @Query("SELECT t FROM TransactionEntity t WHERE t.zoneId = :zoneId AND t.occurredAt >= :from")
    List<TransactionEntity> findRecentByZone(@Param("zoneId") String zoneId,
                                              @Param("from") LocalDateTime from);
}

// В сервисе
TransactionEntity tx = transactionRepository.findById(id)
    .orElseThrow(() -> new TransactionNotFoundException(id));
```

---

## Миграции (Flyway)

Файлы миграций лежат в `src/main/resources/db/migration/`.

### Именование файлов

```
V001__create_transactions_table.sql
V002__create_locations_table.sql
V003__add_intensity_column_to_heat_zones.sql
V004__add_idx_transactions_zone_id.sql
```

Формат: `V<версия>__<описание>.sql` — две подчёркивания, описание через `_`, версия без пропусков.

### Пример миграции

```sql
-- V001__create_transactions_table.sql
CREATE TABLE transactions
(
    id          BIGSERIAL PRIMARY KEY,
    zone_id     VARCHAR(100)   NOT NULL,
    amount      DECIMAL(12, 2) NOT NULL,
    refunded    BOOLEAN        NOT NULL DEFAULT FALSE,
    occurred_at TIMESTAMP      NOT NULL,
    location_id BIGINT REFERENCES locations (id),
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_zone_id ON transactions (zone_id);
CREATE INDEX idx_transactions_occurred_at ON transactions (occurred_at);
```

### Правила

- Миграции необратимы — никогда не редактировать и не удалять применённый файл, Flyway упадёт с checksum-ошибкой
- Один файл — одно логическое изменение
- Версии без пропусков: `V001`, `V002`, `V003` — не `V001`, `V005`, `V010`

**Добавление колонки в таблицу с данными:**

```sql
-- Хорошо — с DEFAULT, иначе миграция упадёт на непустой таблице
ALTER TABLE transactions
    ADD COLUMN intensity DECIMAL(4, 3) NOT NULL DEFAULT 0.0;

-- Плохо — упадёт если в таблице есть строки
ALTER TABLE transactions
    ADD COLUMN intensity DECIMAL(4, 3) NOT NULL;
```

**Деструктивные операции** (`DROP TABLE`, `DROP COLUMN`):
- Обсудить с командой до написания миграции
- Делать в два шага: сначала убрать использование в коде → отдельный PR, потом дропать в БД → отдельный PR
- Не делать в конце спринта перед демо