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

`.env`-файл с реальными значениями — в `.gitignore`. В репозитории держать только `.env.example`:

```
DB_URL=jdbc:postgresql://localhost:5432/tmap
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=change-me-at-least-32-chars-long
JWT_EXPIRATION_MS=3600000
```

---

## JWT

**Правила:**
- JWT-секрет минимум 256 бит (32 байта), сгенерированный случайно
- Access-токен передаётся в `Authorization: Bearer <token>` заголовке
- Refresh-токен — в HTTP-only cookie
- Refresh-токен хранится в БД в виде SHA-256 хэша (`refresh_tokens.token_hash`), не plaintext

---

## Авторизация

Глобальные правила — в `SecurityFilterChain`. Детальный контроль — через `@PreAuthorize`.

```java
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
    };
    private static final String[] PUBLIC_READ_ENDPOINTS = {
            "/api/v1/heatmap/**",
            "/api/v1/venues",
            "/api/v1/venues/**"
    };
    private static final String[] PUBLIC_INFRA_ENDPOINTS = {
            "/error", "/swagger-ui/**", "/api-docs/**", "/actuator/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exc -> exc
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_AUTH_ENDPOINTS).permitAll()
                        .requestMatchers(PUBLIC_READ_ENDPOINTS).permitAll()
                        .requestMatchers(PUBLIC_INFRA_ENDPOINTS).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/business/**").hasRole("BUSINESS_OWNER")
                        .anyRequest().authenticated())
                .build();
    }
}
```

Ролевые ограничения на уровне маршрутов — через `hasRole(...)` в `SecurityFilterChain`. `@PreAuthorize` — для дополнительных проверок на уровне метода.

---

## Обработка ошибок

Централизованная обработка через `@RestControllerAdvice` в `shared/error/GlobalExceptionHandler`. Отдельный `@ExceptionHandler` на каждый тип исключения. Стектрейс в ответе клиенту — никогда.

```java
// shared/error/GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: id={} email={}", ex.getId(), ex.getEmail());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorCode.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyBlockedException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyBlocked(UserAlreadyBlockedException ex) {
        log.warn("User already blocked: id={}", ex.getId());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR,
                        message.isBlank() ? "Validation failed" : message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex); // стектрейс только в логах
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(ErrorCode.INTERNAL_ERROR, "Something went wrong"));
    }
}
```

При добавлении нового доменного исключения — добавить отдельный `@ExceptionHandler` в `GlobalExceptionHandler`.

---

## Логирование

Использовать `@Slf4j` (Lombok). Никогда не логировать чувствительные данные.

```java
// Хорошо
log.info("User authenticated: userId={}", userId);
log.warn("User not found: id={} email={}", ex.getId(), ex.getEmail());
log.error("Unexpected error", ex);

// Плохо — токен в логах
log.info("User authenticated with token: {}", token);
```

| Уровень | Когда использовать |
|---|---|
| `ERROR` | Неожиданные ошибки, требующие внимания |
| `WARN` | Ожидаемые проблемы: 404, конфликты, бизнес-ограничения |
| `INFO` | Ключевые события: аутентификация, создание, удаление |
| `DEBUG` | Детали для отладки, не нужные в продакшене |

**Никогда не логировать:**
- Пароли, JWT-токены, API-ключи, refresh-токены
- Персональные данные пользователей
