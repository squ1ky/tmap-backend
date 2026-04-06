# Security Standards

## Секреты и конфигурация

Никаких секретов в коде или в `application.yaml` в репозитории.

```yaml
# Хорошо — значения из переменных окружения
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  expiration-ms: ${JWT_EXPIRATION_MS}

# Плохо — секреты в файле
jwt:
  secret: my-super-secret-key-12345
```

`.env`-файл с реальными значениями — в `.gitignore`. В репозитории держать только `.env.example` с примерами для данных:

```
DB_URL=jdbc:postgresql://localhost:5432/citypulse
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=my-super-secret-key-12345
JWT_EXPIRATION_MS=86400000
```

---

## JWT



**Правила:**
- JWT-секрет минимум 256 бит (32 байта), сгенерированный случайно
- Срок жизни access-токена — не более 24 часов

---

## Авторизация

Глобальные правила доступа — в `SecurityFilterChain`. Точечный контроль — через `@PreAuthorize`.

```java
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}

// Точечный контроль на уровне метода
@PreAuthorize("hasRole('ADMIN')")
public void deleteLocation(Long id) { ... }
```

---

## Обработка ошибок

Централизованная обработка через `@RestControllerAdvice`. Стектрейс в ответе клиенту — никогда.

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex); // стектрейс только в логах
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("INTERNAL_ERROR", "Something went wrong"));
    }
}
```

Коды ошибок — `UPPER_SNAKE_CASE`: `ZONE_NOT_FOUND`, `TOKEN_EXPIRED`, `ACCESS_DENIED`.

---

## Логирование

Использовать `@Slf4j` (Lombok). Никогда не логировать чувствительные данные.

```java
// Хорошо
log.info("User authenticated: userId={}", userId);
log.warn("Zone not found: zoneId={}", zoneId);
log.error("Failed to process transaction: txId={}", txId, ex);

// Плохо — токен в логах
log.info("User authenticated with token: {}", token);
```

| Уровень | Когда использовать |
|---|---|
| `ERROR` | Неожиданные ошибки, требующие внимания |
| `WARN` | Ожидаемые проблемы: 404, бизнес-ограничения |
| `INFO` | Ключевые события: аутентификация, создание, удаление |
| `DEBUG` | Детали для отладки, не нужные в продакшене |

**Никогда не логировать:**
- Пароли, JWT-токены, API-ключи
- Персональные данные пользователей