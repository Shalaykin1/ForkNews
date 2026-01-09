# Архитектура ForkNews

## Обзор

ForkNews построен на основе **MVVM (Model-View-ViewModel)** архитектуры с использованием современных Android компонентов и best practices.

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  ┌─────────────────┐           ┌──────────────────┐         │
│  │   MainActivity  │           │ SettingsActivity │         │
│  │                 │           │                  │         │
│  │  - RecyclerView │           │  - Preferences   │         │
│  │  - SwipeRefresh │           │  - Theme Toggle  │         │
│  │  - FAB          │           │  - Intervals     │         │
│  └────────┬────────┘           └────────┬─────────┘         │
└───────────┼─────────────────────────────┼───────────────────┘
            │                             │
            │ observes                    │ reads/writes
            ▼                             ▼
┌─────────────────────────────────────────────────────────────┐
│                      ViewModel Layer                         │
│  ┌─────────────────┐           ┌──────────────────┐         │
│  │  MainViewModel  │           │ PreferencesManager│        │
│  │                 │           │                  │         │
│  │  - LiveData     │           │  - DataStore     │         │
│  │  - StateFlow    │           │  - Flow          │         │
│  └────────┬────────┘           └──────────────────┘         │
└───────────┼─────────────────────────────────────────────────┘
            │
            │ uses
            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                          │
│  ┌──────────────────────────────────────────────┐           │
│  │         RepositoryRepository                 │           │
│  │                                              │           │
│  │  - Координирует данные из разных источников │           │
│  │  - Проверяет обновления                     │           │
│  │  - Управляет кэшем                          │           │
│  └───────────┬──────────────┬───────────────────┘           │
└─────────────┼──────────────┼─────────────────────────────────┘
              │              │
              │              │
      ┌───────▼──────┐   ┌───▼────────┐
      │ Local Data   │   │ Remote Data│
      │              │   │            │
┌─────┴──────────────┴───┴────────────┴─────────────────────┐
│                    Data Layer                              │
│                                                            │
│  Local Storage          │    Remote Sources               │
│  ┌─────────────────┐    │    ┌────────────────┐          │
│  │  Room Database  │    │    │  GitHub API    │          │
│  │                 │    │    │  (Retrofit)    │          │
│  │  - RepositoryDao│    │    └────────────────┘          │
│  │  - AppDatabase  │    │    ┌────────────────┐          │
│  └─────────────────┘    │    │ GameHub Parser │          │
│                         │    │  (Jsoup)       │          │
│  ┌─────────────────┐    │    └────────────────┘          │
│  │  DataStore      │    │                                 │
│  │  Preferences    │    │                                 │
│  └─────────────────┘    │                                 │
└─────────────────────────┴─────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Background Work                           │
│  ┌──────────────────────────────────────────────┐           │
│  │         UpdateCheckWorker                    │           │
│  │         (WorkManager)                        │           │
│  │                                              │           │
│  │  - Периодическая проверка обновлений        │           │
│  │  - Отправка уведомлений                     │           │
│  └──────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

## Слои приложения

### 1. UI Layer (Presentation)

**Компоненты:**
- `MainActivity` - главный экран со списком репозиториев
- `SettingsActivity` - экран настроек
- `RepositoryAdapter` - адаптер для RecyclerView

**Ответственность:**
- Отображение данных пользователю
- Обработка пользовательских действий
- Навигация между экранами

**Технологии:**
- ViewBinding для доступа к View
- RecyclerView с DiffUtil для списков
- SwipeRefreshLayout для обновления
- Material Design 3 компоненты

### 2. ViewModel Layer

**Компоненты:**
- `MainViewModel` - бизнес-логика главного экрана
- `PreferencesManager` - управление настройками

**Ответственность:**
- Хранение и обработка UI состояния
- Взаимодействие с репозиторием
- Управление жизненным циклом данных

**Технологии:**
- `ViewModel` для сохранения состояния при смене конфигурации
- `LiveData` / `Flow` для реактивных данных
- `viewModelScope` для корутин

### 3. Repository Layer

**Компоненты:**
- `RepositoryRepository` - единая точка доступа к данным

**Ответственность:**
- Координация между локальными и удаленными источниками
- Логика кэширования
- Проверка обновлений
- Обработка ошибок

**Паттерны:**
- Single Source of Truth (SSOT)
- Repository Pattern

### 4. Data Layer

#### Local Data Sources

**Room Database:**
```kotlin
@Entity(tableName = "repositories")
data class Repository(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val owner: String,
    // ... другие поля
)
```

**Преимущества Room:**
- Compile-time проверка SQL
- Автоматическая миграция схемы
- Поддержка Flow для реактивных запросов
- Встроенный кэш

**DataStore Preferences:**
- Замена SharedPreferences
- Типобезопасное API
- Поддержка Flow
- Асинхронные операции

#### Remote Data Sources

**GitHub API Service:**
```kotlin
interface GitHubApiService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRelease>
}
```

**GameHub Service:**
- HTML парсинг с Jsoup
- Извлечение имени APK файла

### 5. Background Work

**UpdateCheckWorker:**
- Наследуется от `CoroutineWorker`
- Запускается WorkManager
- Проверяет обновления для всех репозиториев
- Отправляет уведомления

**Типы работы:**
- Periodic Work - регулярные проверки
- OneTime Work - проверка в заданное время

## Потоки данных

### 1. Добавление репозитория

```
User Action (FAB Click)
    ↓
MainActivity.showAddRepositoryDialog()
    ↓
MainViewModel.addRepository(url)
    ↓
RepositoryRepository.addRepository(repository)
    ↓
RepositoryDao.insertRepository(repository)
    ↓
Room Database
    ↓
Flow<List<Repository>> emits new list
    ↓
RepositoryAdapter updates RecyclerView
```

### 2. Проверка обновлений

```
WorkManager triggers UpdateCheckWorker
    ↓
UpdateCheckWorker.doWork()
    ↓
RepositoryRepository.getRepositoriesWithNotifications()
    ↓
For each repository:
    RepositoryRepository.checkForUpdates(repo)
        ↓
        GitHub API / GameHub Service
        ↓
        Compare with current release
        ↓
        If new release:
            RepositoryDao.updateRelease()
            NotificationManager.notify()
```

### 3. Просмотр релиза

```
User clicks on Repository card
    ↓
MainActivity receives click event
    ↓
MainViewModel.markReleaseAsViewed(repoId)
    ↓
RepositoryDao.markReleaseAsViewed(repoId)
    ↓
Open browser with release URL
```

## Управление состоянием

### UI State

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

### State Flow vs LiveData

**StateFlow используется для:**
- Состояние загрузки
- Простые значения

**Flow используется для:**
- Список репозиториев (Room автоматически эмитит изменения)
- Настройки из DataStore

## Dependency Injection (Manual)

В текущей версии используется ручное создание зависимостей:

```kotlin
val database = Room.databaseBuilder(context, AppDatabase::class.java, "db").build()
val repository = RepositoryRepository(database.repositoryDao())
val viewModel = MainViewModel(repository)
```

**Для масштабирования можно добавить:**
- Hilt / Dagger для DI
- Модульная структура зависимостей

## Обработка ошибок

### Сетевые ошибки
```kotlin
try {
    val response = api.getLatestRelease(owner, repo)
    if (response.isSuccessful) {
        // Success
    } else {
        // HTTP error
    }
} catch (e: IOException) {
    // Network error
} catch (e: Exception) {
    // Other errors
}
```

### Database ошибки
```kotlin
try {
    dao.insertRepository(repo)
} catch (e: SQLiteConstraintException) {
    // Constraint violation
}
```

## Threading Model

### Корутины

```kotlin
viewModelScope.launch {
    // Выполняется на Main dispatcher
    _isLoading.value = true
    
    withContext(Dispatchers.IO) {
        // Выполняется на IO dispatcher
        repository.checkForUpdates()
    }
    
    _isLoading.value = false
}
```

**Dispatchers:**
- `Main` - UI операции
- `IO` - сеть, база данных
- `Default` - вычисления

## Testing Strategy

### Unit Tests
- ViewModels
- Repository
- Utilities

### Integration Tests
- Database
- API Service

### UI Tests
- Activity scenarios
- Navigation

## Performance Optimizations

1. **DiffUtil** в RecyclerView для эффективного обновления списков
2. **Flow** для реактивных запросов без лишних обновлений
3. **WorkManager** оптимизирует фоновые задачи
4. **Room** кэширует запросы
5. **Lazy initialization** для тяжелых объектов

## Security

1. **Network Security Config** для HTTPS
2. **ProGuard** для обфускации кода
3. **Room** защита от SQL injection
4. **DataStore** зашифрованное хранение (можно добавить)

## Scalability

Для масштабирования можно добавить:
1. **Multi-module architecture**
2. **Hilt/Dagger** для DI
3. **Paging 3** для больших списков
4. **Navigation Component** для сложной навигации
5. **Clean Architecture** слои

## Технический стек

- **Language**: Kotlin
- **Min SDK**: 33 (Android 13)
- **Architecture**: MVVM
- **DI**: Manual (можно добавить Hilt)
- **Async**: Coroutines + Flow
- **Network**: Retrofit + OkHttp
- **Database**: Room
- **Preferences**: DataStore
- **Background**: WorkManager
- **UI**: Material Design 3
- **Parser**: Jsoup
