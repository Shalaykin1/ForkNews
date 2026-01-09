# Инструкция по сборке и установке ForkNews

## Подготовка окружения

### 1. Установка Android Studio
1. Скачайте и установите [Android Studio](https://developer.android.com/studio)
2. Запустите Android Studio
3. Установите Android SDK 34 через SDK Manager
4. Установите JDK 17 (включен в Android Studio)

### 2. Установка Android SDK Command Line Tools (опционально)
Если вы хотите собирать проект из командной строки:
```bash
# Через Android Studio SDK Manager установите "Android SDK Command-line Tools"
```

## Сборка проекта

### Через Android Studio

1. **Открытие проекта**
   ```
   File → Open → Выберите папку ForkNews
   ```

2. **Синхронизация Gradle**
   - Android Studio автоматически предложит синхронизировать Gradle
   - Или нажмите: `File → Sync Project with Gradle Files`

3. **Сборка APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   APK будет создан в: `app/build/outputs/apk/debug/app-debug.apk`

4. **Сборка Release версии**
   - Сначала создайте keystore для подписи:
     ```
     Build → Generate Signed Bundle / APK → APK
     → Create new → Заполните данные → Next → Release → Finish
     ```

### Через командную строку

1. **Сборка Debug версии**
   ```bash
   cd ForkNews
   ./gradlew assembleDebug
   ```
   APK: `app/build/outputs/apk/debug/app-debug.apk`

2. **Сборка Release версии (неподписанная)**
   ```bash
   ./gradlew assembleRelease
   ```
   APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

3. **Очистка проекта**
   ```bash
   ./gradlew clean
   ```

4. **Проверка зависимостей**
   ```bash
   ./gradlew dependencies
   ```

## Установка на устройство

### Через Android Studio

1. Подключите Android устройство через USB
2. Включите "Отладка по USB" на устройстве:
   ```
   Настройки → О телефоне → 7 раз нажать на "Номер сборки"
   Настройки → Система → Для разработчиков → Отладка по USB
   ```
3. В Android Studio выберите устройство и нажмите "Run" (Shift+F10)

### Через ADB (Android Debug Bridge)

1. Установите APK:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. Переустановка (с сохранением данных):
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. Удаление приложения:
   ```bash
   adb uninstall com.forknews
   ```

### Через файл APK

1. Скопируйте APK на устройство
2. Откройте файл через файловый менеджер
3. Разрешите установку из неизвестных источников
4. Установите приложение

## Отладка

### Просмотр логов

```bash
# Все логи приложения
adb logcat | grep ForkNews

# Только ошибки
adb logcat *:E

# Очистка логов
adb logcat -c
```

### Подключение отладчика

1. В Android Studio: `Run → Attach Debugger to Android Process`
2. Выберите процесс `com.forknews`

### Проверка базы данных

```bash
# Войти в shell устройства
adb shell

# Перейти к базе данных
cd /data/data/com.forknews/databases

# Открыть базу данных
sqlite3 forknews_database

# Посмотреть таблицы
.tables

# Посмотреть содержимое
SELECT * FROM repositories;

# Выход
.exit
```

## Решение проблем

### Gradle sync failed

```bash
# Очистите кэш Gradle
./gradlew clean
rm -rf .gradle
rm -rf build

# Пересоздайте проект
./gradlew build
```

### SDK not found

1. Откройте `local.properties` (создайте если нет)
2. Добавьте путь к SDK:
   ```
   sdk.dir=/путь/к/Android/Sdk
   ```

### Build failed - Memory issues

Увеличьте память для Gradle в `gradle.properties`:
```
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### Устройство не видно в ADB

```bash
# Перезапустите ADB
adb kill-server
adb start-server

# Проверьте подключенные устройства
adb devices
```

## Тестирование

### Запуск на эмуляторе

1. Создайте AVD (Android Virtual Device):
   ```
   Tools → Device Manager → Create Device
   ```
2. Выберите устройство с API 33+
3. Запустите эмулятор и установите приложение

### Тестирование уведомлений

1. Убедитесь, что разрешения на уведомления предоставлены
2. Добавьте тестовый репозиторий
3. Установите короткий интервал проверки (30 минут)
4. Проверьте WorkManager:
   ```bash
   adb shell dumpsys jobscheduler | grep forknews
   ```

## Публикация

### Подготовка к релизу

1. Обновите версию в `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2
   versionName = "1.1"
   ```

2. Создайте signed APK с keystore
3. Протестируйте на нескольких устройствах
4. Создайте release на GitHub

### Создание keystore (первый раз)

```bash
keytool -genkey -v -keystore forknews.keystore \
  -alias forknews -keyalg RSA -keysize 2048 -validity 10000
```

Сохраните пароль в безопасном месте!

## Полезные команды

```bash
# Список установленных пакетов
adb shell pm list packages | grep forknews

# Информация о приложении
adb shell dumpsys package com.forknews

# Очистка данных приложения
adb shell pm clear com.forknews

# Скриншот
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png

# Запись экрана
adb shell screenrecord /sdcard/demo.mp4
# Ctrl+C для остановки
adb pull /sdcard/demo.mp4
```

## Ресурсы

- [Android Developers](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Material Design 3](https://m3.material.io/)
- [Gradle Documentation](https://docs.gradle.org/)
