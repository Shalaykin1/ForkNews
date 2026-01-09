# Contributing to ForkNews

Спасибо за ваш интерес к улучшению ForkNews! 🎉

## Как внести вклад

### Сообщения об ошибках (Bug Reports)

Если вы нашли ошибку:

1. Проверьте, что issue еще не создан в [Issues](https://github.com/Shalaykin1/ForkNews/issues)
2. Создайте новый issue с шаблоном "Bug Report"
3. Укажите:
   - Версию Android
   - Версию приложения
   - Шаги для воспроизведения
   - Ожидаемое поведение
   - Фактическое поведение
   - Скриншоты (если применимо)

### Предложения новых функций (Feature Requests)

1. Проверьте, что предложение еще не создано
2. Создайте issue с шаблоном "Feature Request"
3. Опишите:
   - Проблему, которую решает функция
   - Предлагаемое решение
   - Альтернативы
   - Примеры использования

### Pull Requests

1. **Fork** репозиторий
2. Создайте **ветку** для вашей функции:
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Commit** изменения:
   ```bash
   git commit -m 'Add amazing feature'
   ```
4. **Push** в ветку:
   ```bash
   git push origin feature/amazing-feature
   ```
5. Создайте **Pull Request**

## Стандарты кода

### Kotlin Style Guide

Следуйте [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

```kotlin
// Правильно
class RepositoryViewModel(
    private val repository: RepositoryRepository
) : ViewModel() {
    
    fun loadRepositories() {
        viewModelScope.launch {
            // Code here
        }
    }
}

// Неправильно
class repositoryViewModel(private val Repository:RepositoryRepository):ViewModel()
{
    fun loadRepositories()
    {
        // Bad formatting
    }
}
```

### Именование

- **Classes**: PascalCase (`MainActivity`, `RepositoryAdapter`)
- **Functions**: camelCase (`loadRepositories()`, `updateRelease()`)
- **Variables**: camelCase (`repositoryList`, `isLoading`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`, `DEFAULT_INTERVAL`)
- **Resources**: snake_case (`activity_main.xml`, `ic_notification.xml`)

### Комментарии

```kotlin
/**
 * Checks for updates in the specified repository.
 *
 * @param repository The repository to check
 * @return true if new release found, false otherwise
 */
suspend fun checkForUpdates(repository: Repository): Boolean {
    // Implementation
}
```

### Архитектура

Следуйте MVVM паттерну:

```
UI Layer → ViewModel → Repository → Data Sources
```

### Тестирование

- Добавляйте unit тесты для новой логики
- Проверяйте edge cases
- Тестируйте на разных версиях Android

```kotlin
@Test
fun `addRepository should insert valid repository`() = runTest {
    // Arrange
    val repository = Repository(name = "test", owner = "owner", url = "url")
    
    // Act
    viewModel.addRepository(repository)
    
    // Assert
    val result = viewModel.repositories.first()
    assertTrue(result.contains(repository))
}
```

## Структура коммитов

Используйте [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: Новая функция
- `fix`: Исправление ошибки
- `docs`: Изменения документации
- `style`: Форматирование кода
- `refactor`: Рефакторинг кода
- `test`: Добавление тестов
- `chore`: Обновление зависимостей и т.д.

**Примеры:**

```
feat(notifications): add custom time selection

Add ability to select custom time for daily checks.
Closes #123

---

fix(database): correct migration from v1 to v2

Fixed SQLite constraint violation when migrating.
Fixes #456

---

docs(readme): update build instructions

Added steps for M1 Macs.
```

## Процесс Review

1. **Автоматические проверки** должны пройти
2. **Code review** от maintainer
3. **Тестирование** на реальном устройстве
4. **Merge** после одобрения

## Окружение разработки

### Требования

- Android Studio Arctic Fox+
- JDK 17
- Android SDK 33+
- Git

### Настройка

```bash
# Клонировать репозиторий
git clone https://github.com/Shalaykin1/ForkNews.git
cd ForkNews

# Открыть в Android Studio
studio .

# Или собрать из командной строки
./gradlew assembleDebug
```

### Запуск тестов

```bash
# Unit тесты
./gradlew test

# UI тесты
./gradlew connectedAndroidTest

# Все тесты
./gradlew check
```

### Проверка стиля кода

```bash
# Ktlint
./gradlew ktlintCheck

# Detekt
./gradlew detekt
```

## Документация

При добавлении новых функций:

1. Обновите `README.md`
2. Обновите `CHANGELOG.md`
3. Добавьте KDoc комментарии к публичным API
4. Обновите `API_DOCUMENTATION.md` если необходимо

## Вопросы?

- Создайте [Discussion](https://github.com/Shalaykin1/ForkNews/discussions)
- Напишите в [Issues](https://github.com/Shalaykin1/ForkNews/issues)

## Code of Conduct

Будьте дружелюбны и уважительны к другим участникам.

## Лицензия

Внося вклад, вы соглашаетесь, что ваш код будет лицензирован под MIT License.

---

Спасибо за ваш вклад! 🚀
