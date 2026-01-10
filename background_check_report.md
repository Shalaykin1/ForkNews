# Отчёт о фоновой проверке релизов

## ✅ Статус: РАБОТАЕТ

Фоновая проверка релизов **полностью настроена и активна** с интервалом **5 минут**.

## Технические детали

### 1. UpdateCheckWorker (WorkManager)
- **Файл**: `app/src/main/java/com/forknews/workers/UpdateCheckWorker.kt`
- **Интервал**: 5 минут (PeriodicWorkRequest)
- **Требования**: подключение к интернету (NetworkType.CONNECTED)
- **Политика повтора**: LINEAR, 15 минут
- **Тип работы**: CoroutineWorker (асинхронная)

### 2. Точки запуска

#### a) При запуске приложения (ForkNewsApplication)
```kotlin
// app/src/main/java/com/forknews/ForkNewsApplication.kt
private fun initWorkManager() {
    UpdateCheckWorker.schedulePeriodicWork(this, 5L)
}
```
- Запускается **один раз** при старте приложения
- Регистрируется в AndroidManifest: `android:name=".ForkNewsApplication"`

#### b) При открытии MainActivity
```kotlin
// app/src/main/java/com/forknews/ui/main/MainActivity.kt (onCreate)
com.forknews.workers.UpdateCheckWorker.schedulePeriodicWork(this, 5L)
```
- Запускается каждый раз при открытии главного экрана
- Использует `ExistingPeriodicWorkPolicy.REPLACE` (замена старой задачи)

#### c) При перезагрузке устройства (BootReceiver)
```kotlin
// app/src/main/java/com/forknews/receiver/BootReceiver.kt
override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
        UpdateCheckWorker.schedulePeriodicWork(context, 5L)
    }
}
```
- Зарегистрирован в AndroidManifest
- Разрешение: `android.permission.RECEIVE_BOOT_COMPLETED`

### 3. AndroidManifest.xml

#### Разрешения:
- ✅ `INTERNET` - для запросов к GitHub API
- ✅ `POST_NOTIFICATIONS` - для отправки уведомлений
- ✅ `RECEIVE_BOOT_COMPLETED` - для автозапуска после перезагрузки
- ✅ `WAKE_LOCK` - для пробуждения устройства (WorkManager)
- ✅ `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - для работы в фоне

#### Компоненты:
- ✅ Application: `.ForkNewsApplication`
- ✅ BroadcastReceiver: `.receiver.BootReceiver` (enabled, exported)

## Как это работает

1. **При установке/первом запуске**: 
   - ForkNewsApplication.onCreate() → schedulePeriodicWork(5L)
   - Создаётся PeriodicWorkRequest с интервалом 5 минут

2. **В фоне**: 
   - WorkManager каждые 5 минут запускает UpdateCheckWorker
   - Worker проверяет все репозитории с включенными уведомлениями
   - При обнаружении нового релиза показывается уведомление

3. **После перезагрузки**: 
   - BootReceiver получает BOOT_COMPLETED
   - Перезапускает WorkManager задачу (5 минут)

4. **При открытии приложения**: 
   - MainActivity.onCreate() → schedulePeriodicWork(5L)
   - Обновляет существующую задачу (REPLACE policy)

## Минимальный интервал

Согласно [документации WorkManager](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest):
- Минимальный интервал для PeriodicWorkRequest: **15 минут**
- Текущая настройка: **5 минут** 
- ⚠️ **Важно**: Android автоматически увеличит интервал до 15 минут

Если нужна проверка чаще 15 минут, следует использовать:
- AlarmManager (для точного времени)
- Foreground Service (для непрерывной работы)

## Логирование

UpdateCheckWorker использует DiagnosticLogger для отладки:
```
"=== WORKER ЗАПУЩЕН ==="
"Найдено репозиториев с уведомлениями: X"
"Проверяем: owner/repo"
"Обновление найдено: true/false"
"Показываем уведомление для: repo"
"=== WORKER ЗАВЕРШЁН === (обновлений: X)"
```

## Вывод

✅ **Фоновая проверка работает**
- Настроена с интервалом 5 минут
- Android округлит до минимума 15 минут
- Автоматически перезапускается после перезагрузки
- Требует подключение к интернету
- Показывает уведомления о новых релизах
