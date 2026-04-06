# Testing Standards

## Что покрывать

| Слой | Инструмент | Обязательно |
|---|---|---|
| Service | JUnit 5 + Mockito | Да — весь бизнес-слой |
| Controller | `@WebMvcTest` | Да — happy path + основные ошибки |
| Repository | TestContainers | Для кастомных `@Query` |

---

## Именование тестов

Формат: `methodName_whenCondition_thenExpected`

```java
getHeatmap_whenZoneExists_thenReturnHeatmapData()
getHeatmap_whenZoneNotFound_thenThrowNotFoundException()
createLocation_whenNameAlreadyExists_thenThrowConflictException()
calculateIntensity_whenNoRecentTransactions_thenReturnZero()
```

---

## Unit-тесты (JUnit 5 + Mockito)

Структура теста — `given / when / then`.

```java
@ExtendWith(MockitoExtension.class)
class HeatmapServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private HeatmapMapper heatmapMapper;

    @InjectMocks
    private HeatmapServiceImpl heatmapService;

    @Test
    void getHeatmap_whenZoneExists_thenReturnHeatmapData() {
        // given
        String zoneId = "zone-42";
        List<TransactionEntity> transactions = List.of(buildTransaction(zoneId, BigDecimal.valueOf(850)));
        HeatmapResponse expected = new HeatmapResponse(zoneId, 0.87, 850);

        given(transactionRepository.findRecentByZone(eq(zoneId), any())).willReturn(transactions);
        given(heatmapMapper.toResponse(transactions)).willReturn(expected);

        // when
        HeatmapResponse result = heatmapService.getHeatmap(zoneId);

        // then
        assertThat(result.zoneId()).isEqualTo(zoneId);
        assertThat(result.intensity()).isEqualTo(0.87);
        verify(transactionRepository).findRecentByZone(eq(zoneId), any());
    }

    @Test
    void getHeatmap_whenZoneNotFound_thenThrowNotFoundException() {
        // given
        given(transactionRepository.findRecentByZone(any(), any())).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> heatmapService.getHeatmap("unknown-zone"))
            .isInstanceOf(ZoneNotFoundException.class);
    }
}
```

---

## Правила

- Тесты независимы — не зависят от порядка запуска, не оставляют состояния в БД
- Не мокировать то, что не нужно — лишние моки усложняют понимание теста
- `@BeforeEach` для повторяющегося setup, не копипастить данные между тестами