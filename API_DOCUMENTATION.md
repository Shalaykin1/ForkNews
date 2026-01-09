# API Documentation

## GitHub API

ForkNews использует GitHub REST API v3 для получения информации о релизах.

### Endpoints

#### Get Latest Release
```
GET https://api.github.com/repos/{owner}/{repo}/releases/latest
```

**Параметры:**
- `owner` - владелец репозитория
- `repo` - название репозитория

**Пример запроса:**
```bash
curl https://api.github.com/repos/microsoft/vscode/releases/latest
```

**Пример ответа:**
```json
{
  "id": 123456789,
  "tag_name": "1.85.0",
  "name": "January 2024",
  "html_url": "https://github.com/microsoft/vscode/releases/tag/1.85.0",
  "published_at": "2024-01-10T10:00:00Z",
  "body": "Release notes..."
}
```

### Rate Limits

GitHub API имеет ограничения:
- **Без авторизации**: 60 запросов/час
- **С авторизацией**: 5000 запросов/час

Для увеличения лимита можно добавить токен в заголовок:
```kotlin
"Authorization: Bearer YOUR_GITHUB_TOKEN"
```

### Добавление токена (опционально)

1. Создайте Personal Access Token на GitHub:
   - Settings → Developer settings → Personal access tokens → Generate new token
   - Права: `public_repo` (только для публичных репозиториев)

2. Добавьте токен в `RetrofitClient.kt`:
```kotlin
private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer YOUR_TOKEN_HERE")
            .build()
        chain.proceed(request)
    }
    .build()
```

## GameHub API

GameHub не имеет официального API, поэтому используется парсинг HTML.

### Endpoint
```
GET https://gamehub.xiaoji.com/download/
```

### Парсинг

Приложение ищет элемент `<a download="...">` для получения имени APK файла:

```kotlin
val doc = Jsoup.connect("https://gamehub.xiaoji.com/download/").get()
val downloadButton = doc.select("a[download]").firstOrNull()
val apkName = downloadButton?.attr("download")
```

**Пример HTML:**
```html
<a download="gamehub_v2.5.apk" href="/files/gamehub_v2.5.apk">Download</a>
```

## Internal Database API

### Repository Entity

```kotlin
data class Repository(
    val id: Long = 0,
    val name: String,
    val owner: String,
    val url: String,
    val latestRelease: String? = null,
    val latestReleaseUrl: String? = null,
    val hasNewRelease: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val type: RepositoryType = RepositoryType.GITHUB,
    val lastChecked: Long = 0
)
```

### DAO Operations

```kotlin
interface RepositoryDao {
    // Get all repositories
    fun getAllRepositories(): Flow<List<Repository>>
    
    // Get repository by ID
    suspend fun getRepositoryById(id: Long): Repository?
    
    // Get repositories with notifications enabled
    suspend fun getRepositoriesWithNotifications(): List<Repository>
    
    // Insert repository
    suspend fun insertRepository(repository: Repository): Long
    
    // Update repository
    suspend fun updateRepository(repository: Repository)
    
    // Delete repository
    suspend fun deleteRepository(repository: Repository)
    
    // Mark release as viewed
    suspend fun markReleaseAsViewed(id: Long)
    
    // Update release information
    suspend fun updateRelease(id: Long, release: String, url: String, timestamp: Long)
}
```

## DataStore Preferences

### Settings Keys

```kotlin
// Check interval (minutes)
CHECK_INTERVAL_KEY: Long = 60

// Theme settings
USE_SYSTEM_THEME_KEY: Boolean = true
DARK_THEME_KEY: Boolean = false

// Notifications
NOTIFICATIONS_ENABLED_KEY: Boolean = true

// Custom time
CUSTOM_TIME_ENABLED_KEY: Boolean = false
CUSTOM_TIME_HOUR_KEY: Int = 9
CUSTOM_TIME_MINUTE_KEY: Int = 0
```

### Usage Example

```kotlin
// Get value
PreferencesManager.getCheckInterval().collect { interval ->
    println("Check interval: $interval minutes")
}

// Set value
PreferencesManager.setCheckInterval(120) // 2 hours
```

## WorkManager

### Periodic Work

Запускается с заданным интервалом:

```kotlin
UpdateCheckWorker.schedulePeriodicWork(context, intervalMinutes = 60)
```

### Custom Time Work

Запускается в определенное время каждый день:

```kotlin
UpdateCheckWorker.scheduleCustomTimeWork(context, hour = 9, minute = 0)
```

### Work Constraints

```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED) // Требуется интернет
    .build()
```

## Notifications

### Notification Channel

```kotlin
Channel ID: "forknews_updates"
Channel Name: "Repository Updates"
Importance: IMPORTANCE_DEFAULT
```

### Notification Structure

```kotlin
Title: "{repo_name}: новый релиз"
Content: "{release_name}"
Action: Открыть URL релиза в браузере
```

### Creating Notification

```kotlin
val notification = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle("$repoName: новый релиз")
    .setContentText(releaseName)
    .setContentIntent(pendingIntent)
    .setAutoCancel(true)
    .build()
```

## Error Handling

### Network Errors

```kotlin
try {
    val response = githubApi.getLatestRelease(owner, repo)
    if (response.isSuccessful) {
        // Process data
    } else {
        // Handle HTTP error
        when (response.code()) {
            404 -> // Repository not found
            403 -> // Rate limit exceeded
            else -> // Other error
        }
    }
} catch (e: Exception) {
    // Network error or parsing error
}
```

### Database Errors

```kotlin
try {
    repository.addRepository(newRepo)
} catch (e: SQLiteConstraintException) {
    // Duplicate entry
} catch (e: Exception) {
    // Other database error
}
```

## Testing APIs

### Test GitHub API

```bash
# Test with curl
curl -H "Accept: application/vnd.github.v3+json" \
     https://api.github.com/repos/microsoft/vscode/releases/latest

# Check rate limit
curl https://api.github.com/rate_limit
```

### Test GameHub

```bash
curl https://gamehub.xiaoji.com/download/ | grep "download="
```

### Test Notifications

```kotlin
// In your activity/fragment
val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
val notification = NotificationCompat.Builder(this, "forknews_updates")
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle("Test")
    .setContentText("Test notification")
    .build()
notificationManager.notify(1, notification)
```

## Security Considerations

1. **GitHub Token**: Никогда не коммитьте токен в репозиторий
2. **HTTPS Only**: Все запросы через HTTPS
3. **Input Validation**: Валидация URL перед парсингом
4. **SQL Injection**: Room автоматически защищает от SQL injection
5. **Network Security**: Используйте Network Security Configuration для Android 9+

## Performance Tips

1. **Caching**: Room автоматически кэширует запросы
2. **Background Work**: Все сетевые запросы в фоновых потоках
3. **Pagination**: Для большого количества репозиториев используйте Paging 3
4. **Image Loading**: Используйте Coil или Glide для загрузки изображений (если добавите)
5. **WorkManager**: Автоматически оптимизирует фоновые задачи
