# Testing Standards

## Что покрывать

| Слой | Инструмент | Обязательно |
|---|---|---|
| Service | JUnit 5 + Mockito | Да — весь бизнес-слой |
| Controller | `@WebMvcTest` | Да — happy path + основные ошибки |
| Repository | `@DataJpaTest` / `@JdbcTest` + TestContainers | Для кастомных запросов |

---

## Именование тестов

Формат: `methodName_whenCondition_thenExpected`

```java
block_whenUserIsNotBlocked_thenBlockUser()
block_whenUserDoesNotExist_thenReturnNotFound()
block_whenUserIsAlreadyBlocked_thenReturnConflict()
searchAdminUsers_whenUserIsAdmin_thenReturnUsers()
searchAdminUsers_whenUserIsNotAdmin_thenReturnForbidden()
```

---

## Unit-тесты сервисов (JUnit 5 + Mockito)

Структура теста — `given / when / then`.

```java
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final UUID USER_ID = UserTestFactory.DEFAULT_ID;

    @Mock
    private UserRepository userRepository;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository);
    }

    @Test
    void block_whenUserIsNotBlocked_thenBlockUser() {
        // given
        User user = UserTestFactory.createUser(false);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when
        User result = adminUserService.block(USER_ID);

        // then
        assertThat(result.isBlocked()).isTrue();
        assertThat(result).isSameAs(user);
    }

    @Test
    void block_whenUserDoesNotExist_thenReturnNotFound() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminUserService.block(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void block_whenUserIsAlreadyBlocked_thenReturnConflict() {
        // given
        User user = UserTestFactory.createUser(true);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> adminUserService.block(USER_ID))
                .isInstanceOf(UserAlreadyBlockedException.class);
    }
}
```

---

## TestFactory

Тестовые данные создаются через `XxxTestFactory` — финальный класс с приватным конструктором в `test/java/.../domain/`.

```java
// user/domain/UserTestFactory.java (в test/)
public final class UserTestFactory {

    public static final UUID DEFAULT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final String DEFAULT_EMAIL = "user@example.com";
    public static final String DEFAULT_NICKNAME = "Tatarin";

    private UserTestFactory() {}

    public static User createUser() {
        return createUser(UserRole.USER, false);
    }

    public static User createUser(boolean blocked) {
        return createUser(UserRole.USER, blocked);
    }

    public static User createUser(UserRole role, boolean blocked) {
        User user = new User(DEFAULT_ID, DEFAULT_EMAIL, "hash", DEFAULT_NICKNAME, role);
        if (blocked) user.block();
        return user;
    }
}
```

Все константы (`DEFAULT_ID`, `DEFAULT_EMAIL`) — публичные, используются и в тестах контроллеров для проверки `jsonPath`.

---

## Тесты контроллеров (@WebMvcTest)

```java
@WebMvcTest(AdminUserController.class)
@Import({
        TestSecurityConfig.class,        // заглушка SecurityFilterChain для тестов
        GlobalExceptionHandler.class,    // проверяем маппинг HTTP-статусов
        AdminUserMapper.class,           // реальный маппер, не мок
})
class AdminUserControllerTest {

    private static final UUID USER_ID = UserTestFactory.DEFAULT_ID;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchAdminUsers_whenUserIsAdmin_thenReturnUsers() throws Exception {
        User user = UserTestFactory.createUser();
        given(adminUserService.search(any(), any()))
                .willReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/users/search")
                        .param("nickname", "Tatarin")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.items[0].nickname").value(UserTestFactory.DEFAULT_NICKNAME))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void searchAdminUsers_whenUserIsNotAdmin_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/search"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockAdminUser_whenUserDoesNotExist_thenReturnNotFound() throws Exception {
        given(adminUserService.block(USER_ID))
                .willThrow(UserNotFoundException.byId(USER_ID));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/block", USER_ID)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void blockAdminUser_whenUserIsAlreadyBlocked_thenReturnConflict() throws Exception {
        given(adminUserService.block(USER_ID))
                .willThrow(new UserAlreadyBlockedException(USER_ID));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/block", USER_ID)
                        .with(csrf()))
                .andExpect(status().isConflict());
    }
}
```

**Ключевые детали:**
- `@MockitoBean` (Spring 6.2+) вместо устаревшего `@MockBean`
- `GlobalExceptionHandler` импортируется явно — иначе тест не проверит HTTP-статусы
- Маппер импортируется как реальный компонент — не нужен мок
- `@WithMockUser(roles = "ADMIN")` имитирует авторизованного пользователя
- CSRF: `.with(csrf())` для POST/PATCH/DELETE запросов
- Мутирующие запросы проверяются и на happy path, и на ошибки (404, 409)

---

## Правила

- Тесты независимы — не зависят от порядка запуска, не оставляют состояния в БД
- `@BeforeEach` — только для setup (создание сервиса, общие mock-настройки), не для создания тестовых данных
- Не мокировать то, что не нужно — лишние моки усложняют понимание теста
- AssertJ везде (`assertThat`, `assertThatThrownBy`) — не JUnit `assertEquals`
