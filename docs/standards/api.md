# API Standards

## Spec-first подход

Контракт описывается в `openapi.yaml` до написания реализации. Интерфейсы генерируются автоматически через Maven-плагин.

**Процесс:**
1. Согласовать эндпоинты с фронтендом
2. Описать в `openapi.yaml`
3. Сгенерировать интерфейсы: `mvn generate-sources`
4. Реализовать интерфейсы в контроллерах

**Maven-плагин** в `pom.xml`:

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/openapi.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <configOptions>
                    <useSpringBoot3>true</useSpringBoot3>
                    <interfaceOnly>true</interfaceOnly>
                    <useResponseEntity>true</useResponseEntity>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Реализация сгенерированного интерфейса:**

```java
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdminUserController implements AdminUsersApi {

    private final AdminUserService adminUserService;
    private final AdminUserMapper adminUserMapper;

    @Override
    public ResponseEntity<AdminUserModerationPage> searchAdminUsers(
            String nickname, String email, UserRole role, Boolean blocked,
            OffsetDateTime createdFrom, OffsetDateTime createdTo,
            Integer page, Integer size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        UserSearchCriteria criteria = new UserSearchCriteria(
                nickname, email, adminUserMapper.toDomainRole(role),
                blocked, createdFrom, createdTo
        );
        return ResponseEntity.ok(adminUserMapper.toPage(adminUserService.search(criteria, pageable)));
    }

    @Override
    public ResponseEntity<AdminUserModerationResponse> blockAdminUser(UUID id) {
        return ResponseEntity.ok(adminUserMapper.toResponse(adminUserService.block(id)));
    }
}
```

Контроллер не содержит логики — только маппинг параметров и делегирование в сервис.

---

## Mapper-компоненты

Маппинг между доменными объектами и OpenAPI DTO — в отдельном `@Component`-классе в слое `presentation`.

```java
@Component
public class AdminUserMapper {

    public AdminUserModerationResponse toResponse(User user) {
        return new AdminUserModerationResponse()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(toApiRole(user.getRole()))
                .blocked(user.isBlocked())
                .createdAt(user.getCreatedAt());
    }

    public AdminUserModerationPage toPage(Page<User> page) {
        List<AdminUserModerationResponse> items = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new AdminUserModerationPage()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements());
    }

    public ru.tbank.tmap.user.domain.UserRole toDomainRole(UserRole role) {
        return role == null ? null : ru.tbank.tmap.user.domain.UserRole.valueOf(role.name());
    }
}
```

---

## Именование эндпоинтов

- Ресурсы во множественном числе, `kebab-case`
- Версия в пути: `/api/v1/...`
- Глаголов в URL нет — действие передаётся через HTTP-метод
- Подресурсы admin: `/api/v1/admin/...`, business owner: `/api/v1/business/...`

```
GET    /api/v1/venues                          — публичный список заведений
GET    /api/v1/venues/{id}                     — конкретное заведение
GET    /api/v1/heatmap                         — тепловая карта
GET    /api/v1/admin/users/search              — поиск пользователей (admin)
PATCH  /api/v1/admin/users/{id}/block          — заблокировать пользователя
PATCH  /api/v1/admin/users/{id}/unblock        — разблокировать
GET    /api/v1/admin/venues                    — список заведений на модерации
POST   /api/v1/business/venues                 — создать заведение (business owner)
PATCH  /api/v1/business/venues/{id}            — обновить заведение
```

---

## HTTP-коды ответов

| Ситуация | Код |
|---|---|
| Успешное получение данных | `200 OK` |
| Успешное создание | `201 Created` |
| Успешное удаление (без тела) | `204 No Content` |
| Ошибка валидации / неверный формат | `400 Bad Request` |
| Не аутентифицирован | `401 Unauthorized` |
| Нет прав | `403 Forbidden` |
| Ресурс не найден | `404 Not Found` |
| Конфликт состояния (уже заблокирован и т.п.) | `409 Conflict` |
| Файл слишком большой | `413 Payload Too Large` |
| Внутренняя ошибка сервера | `500 Internal Server Error` |

---

## Формат ответов

**Успешный ответ** — DTO напрямую, без обёртки:

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "nickname": "Tatarin",
  "email": "user@example.com",
  "role": "USER",
  "blocked": false
}
```

**Ошибка** — единый формат для всех эндпоинтов:

```json
{
  "code": "NOT_FOUND",
  "message": "User not found"
}
```

**`ErrorResponse`** и **`ErrorCode`** в `shared/error/`:

```java
public record ErrorResponse(ErrorCode code, String message) {}

public enum ErrorCode {
    BAD_REQUEST, VALIDATION_ERROR, NOT_FOUND, CONFLICT,
    INTERNAL_ERROR, UNAUTHORIZED, FORBIDDEN, INVALID_FILE
}
```

Конкретный `ErrorCode` выбирается в `GlobalExceptionHandler` под каждый тип исключения.
