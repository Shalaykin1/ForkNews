# 🔨 Инструкция по сборке Release APK

## ⚠️ Важно

В текущей dev container среде не установлен Android SDK. Для сборки APK необходимо:

1. Клонировать проект на локальную машину с установленным Android Studio
2. Собрать проект локально
3. Подписать APK
4. Создать релиз

---

## 📦 Сборка на локальной машине

### Шаг 1: Клонируйте проект

```bash
git clone https://github.com/Shalaykin1/ForkNews.git
cd ForkNews
```

### Шаг 2: Откройте в Android Studio

```bash
studio .
# Или через File → Open → выберите папку ForkNews
```

### Шаг 3: Соберите Release APK

#### Вариант А: Через Android Studio

1. **Build → Generate Signed Bundle / APK**
2. Выберите **APK**
3. **Create new keystore** (первый раз):
   - Key store path: `~/forknews.keystore`
   - Password: [ваш пароль]
   - Alias: `forknews`
   - Key password: [ваш пароль]
   - Validity: 25 years
   - Certificate: укажите данные
4. Нажмите **Next**
5. Выберите **release**
6. Нажмите **Finish**

APK будет в: `app/build/outputs/apk/release/app-release.apk`

#### Вариант Б: Через командную строку

**Создайте keystore (первый раз):**

```bash
keytool -genkey -v -keystore forknews.keystore \
  -alias forknews \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YourPassword \
  -keypass YourPassword \
  -dname "CN=ForkNews, OU=Development, O=ForkNews, L=City, ST=State, C=RU"
```

**Настройте подпись в `app/build.gradle.kts`:**

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../forknews.keystore")
            storePassword = "YourPassword"
            keyAlias = "forknews"
            keyPassword = "YourPassword"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

**Соберите:**

```bash
./gradlew clean assembleRelease
```

APK будет в: `app/build/outputs/apk/release/app-release.apk`

---

## 🔐 Подпись APK (если собрали unsigned)

Если у вас есть неподписанный APK:

```bash
# Выровнять APK
zipalign -v -p 4 app-release-unsigned.apk app-release-unsigned-aligned.apk

# Подписать
apksigner sign --ks forknews.keystore \
  --ks-key-alias forknews \
  --out app-release.apk \
  app-release-unsigned-aligned.apk

# Проверить подпись
apksigner verify app-release.apk
```

---

## 🚀 Создание релиза на GitHub

### Вариант 1: Через веб-интерфейс

1. Перейдите на https://github.com/Shalaykin1/ForkNews
2. **Releases** → **Create a new release**
3. **Choose a tag** → создайте новый тег: `v1.0.0`
4. **Release title**: `ForkNews v1.0.0`
5. **Description**: скопируйте из [CHANGELOG.md](CHANGELOG.md)
6. **Attach binaries**: перетащите `app-release.apk`
7. Нажмите **Publish release**

### Вариант 2: Через GitHub CLI

```bash
# Установите GitHub CLI (если еще нет)
# https://cli.github.com/

# Авторизуйтесь
gh auth login

# Создайте релиз
gh release create v1.0.0 \
  app/build/outputs/apk/release/app-release.apk \
  --title "ForkNews v1.0.0" \
  --notes "$(cat CHANGELOG.md | sed -n '/## \[1.0.0\]/,/## \[Unreleased\]/p' | head -n -1)"
```

### Вариант 3: Через Git и curl

```bash
# Создайте и запушьте тег
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Создайте релиз через API
RELEASE_ID=$(curl -X POST \
  -H "Authorization: token YOUR_GITHUB_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.github.com/repos/Shalaykin1/ForkNews/releases \
  -d '{
    "tag_name": "v1.0.0",
    "name": "ForkNews v1.0.0",
    "body": "См. CHANGELOG.md",
    "draft": false,
    "prerelease": false
  }' | jq -r '.id')

# Загрузите APK
curl -X POST \
  -H "Authorization: token YOUR_GITHUB_TOKEN" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary @app/build/outputs/apk/release/app-release.apk \
  "https://uploads.github.com/repos/Shalaykin1/ForkNews/releases/$RELEASE_ID/assets?name=ForkNews-v1.0.0.apk"
```

---

## 📝 Шаблон описания релиза

```markdown
# ForkNews v1.0.0

Первый стабильный релиз приложения для мониторинга обновлений GitHub репозиториев!

## ✨ Основные возможности

- 🔔 Автоматические уведомления о новых релизах
- 📦 Мониторинг GitHub репозиториев
- 🎮 Поддержка GameHub
- ⏰ Настраиваемая частота проверки (30 мин - 12 часов)
- 🕐 Проверка в заданное время (например, в 9:00)
- 🎨 Светлая и темная тема
- 📱 Material Design 3

## 📥 Установка

1. Скачайте `ForkNews-v1.0.0.apk`
2. Установите на устройство Android 13+
3. Разрешите установку из неизвестных источников

## 📖 Документация

- [README.md](../README.md) - полная документация
- [QUICKSTART.md](../QUICKSTART.md) - быстрый старт
- [FAQ.md](../FAQ.md) - часто задаваемые вопросы

## 🔒 Безопасность

- APK подписан официальным ключом
- SHA-256: `[будет добавлено после подписи]`

## 🐛 Известные проблемы

Нет критических проблем.

## 📞 Поддержка

- [Issues](https://github.com/Shalaykin1/ForkNews/issues)
- [Discussions](https://github.com/Shalaykin1/ForkNews/discussions)

---

**Полный changelog:** [CHANGELOG.md](../CHANGELOG.md)
```

---

## 🔍 Проверка APK

После сборки проверьте APK:

```bash
# Информация об APK
aapt dump badging app-release.apk | grep -E "package|sdkVersion|targetSdkVersion"

# Размер APK
ls -lh app-release.apk

# Подпись
apksigner verify --verbose app-release.apk

# SHA-256
sha256sum app-release.apk
```

---

## 📊 Чеклист перед релизом

- [ ] Обновлена версия в `app/build.gradle.kts`
- [ ] Обновлен `CHANGELOG.md`
- [ ] Протестировано на реальном устройстве
- [ ] APK собран с release конфигурацией
- [ ] APK подписан валидным keystore
- [ ] Проверена подпись APK
- [ ] Создан Git тег
- [ ] Написано описание релиза
- [ ] APK загружен в релиз
- [ ] Релиз опубликован

---

## 🎯 Альтернатива: GitHub Actions

Создайте файл `.github/workflows/release.yml`:

```yaml
name: Release APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Build Release APK
      run: ./gradlew assembleRelease
    
    - name: Sign APK
      uses: r0adkll/sign-android-release@v1
      with:
        releaseDirectory: app/build/outputs/apk/release
        signingKeyBase64: ${{ secrets.SIGNING_KEY }}
        alias: ${{ secrets.ALIAS }}
        keyStorePassword: ${{ secrets.KEY_STORE_PASSWORD }}
        keyPassword: ${{ secrets.KEY_PASSWORD }}
    
    - name: Create Release
      uses: softprops/action-gh-release@v1
      with:
        files: app/build/outputs/apk/release/*.apk
        body_path: CHANGELOG.md
```

---

## 💡 Полезные советы

1. **Храните keystore в безопасном месте** - потеря keystore = невозможность обновить приложение
2. **Делайте backup keystore** - сохраните копию в безопасном месте
3. **Не коммитьте keystore** - добавьте `*.keystore` в `.gitignore`
4. **Используйте разные keystore** для debug и release
5. **Документируйте пароли** - сохраните пароли в безопасном месте

---

## 📞 Помощь

Если возникли проблемы:
- 📖 [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)
- ❓ [FAQ.md](FAQ.md)
- 🐛 [Create Issue](https://github.com/Shalaykin1/ForkNews/issues)

---

**Успешной сборки!** 🚀
