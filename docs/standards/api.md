# API Standards

## Spec-first подход

Контракт описывается в `openapi.yaml` до написания реализации. Интерфейсы генерируются автоматически.

**Процесс:**
1. Согласовать эндпоинты с фронтендом
2. Описать в `openapi.yaml`
3. Сгенерировать интерфейсы через Maven-плагин
4. Реализовать интерфейсы в контроллерах

**Maven-плагин** в `pom.xml`:

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>7.2.0</version>
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
@RequiredArgsConstructor
public class HeatmapController implements HeatmapApi {

    private final HeatmapService heatmapService;

    @Override
    public ResponseEntity<HeatmapResponse> getHeatmap(String cityId) {
        return ResponseEntity.ok(heatmapService.getHeatmap(cityId));
    }
}
```

---

## Именование эндпоинтов

- Ресурсы во множественном числе, `kebab-case`
- Версия в пути: `/api/v1/...`
- Глаголов в URL нет — действие передаётся через HTTP-метод

```
GET    /api/v1/heatmap                  — тепловая карта
GET    /api/v1/heatmap/zones/{zoneId}   — конкретная зона
GET    /api/v1/transactions/stats       — статистика транзакций
GET    /api/v1/locations                — список локаций
POST   /api/v1/locations               — создать локацию
PATCH  /api/v1/locations/{id}          — обновить локацию
DELETE /api/v1/locations/{id}          — удалить локацию
```

---

## HTTP-коды ответов

| Ситуация | Код |
|---|---|
| Успешное получение данных | `200 OK` |
| Успешное создание | `201 Created` |
| Успешное удаление (без тела) | `204 No Content` |
| Ошибка валидации | `400 Bad Request` |
| Не аутентифицирован | `401 Unauthorized` |
| Нет прав | `403 Forbidden` |
| Ресурс не найден | `404 Not Found` |
| Внутренняя ошибка сервера | `500 Internal Server Error` |

---

## Формат ответов

**Успешный ответ** — возвращается DTO напрямую, без обёртки:

```json
{
  "zoneId": "zone-42",
  "intensity": 0.87,
  "avgCheck": 850
}
```

**Ошибка** — единый формат для всех эндпоинтов:

```json
{
  "code": "ZONE_NOT_FOUND",
  "message": "Zone with id zone-42 not found"
}
```

**Реализация `ErrorResponse`:**

```java
public record ErrorResponse(String code, String message) {}
```

Коды ошибок — `UPPER_SNAKE_CASE`: `ZONE_NOT_FOUND`, `TOKEN_EXPIRED`, `VALIDATION_ERROR`.

---

## Валидация

Валидация входных данных — на уровне DTO через Spring Validation. В сервисах повторно не валидировать.

```java
// DTO
public record GetHeatmapRequest(
    @NotBlank(message = "cityId is required")
    String cityId,

    @NotNull(message = "radius is required")
    @Min(value = 100, message = "radius must be at least 100 meters")
    @Max(value = 5000, message = "radius must be at most 5000 meters")
    Integer radius
) {}

// Контроллер — обязателен @Valid
@GetMapping
public ResponseEntity<HeatmapResponse> getHeatmap(
    @Valid GetHeatmapRequest request
) { ... }
```