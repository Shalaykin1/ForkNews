#!/bin/bash

# Скрипт для автоматической сборки и релиза ForkNews
# Использование: ./release.sh <version>
# Пример: ./release.sh 1.0.0

set -e

VERSION=$1

if [ -z "$VERSION" ]; then
    echo "❌ Ошибка: Укажите версию релиза"
    echo "Использование: ./release.sh <version>"
    echo "Пример: ./release.sh 1.0.0"
    exit 1
fi

echo "🚀 Начинаю сборку релиза ForkNews v$VERSION"
echo ""

# Проверка Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "❌ Ошибка: Android SDK не найден"
    echo "Установите ANDROID_HOME или ANDROID_SDK_ROOT"
    exit 1
fi

# Проверка Java версии
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" != "17" ]; then
    echo "⚠️  Предупреждение: Требуется Java 17, у вас Java $JAVA_VERSION"
fi

# Очистка предыдущих сборок
echo "🧹 Очистка предыдущих сборок..."
./gradlew clean

# Сборка Release APK
echo "🔨 Сборка Release APK..."
./gradlew assembleRelease

APK_PATH="app/build/outputs/apk/release/app-release.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Ошибка: APK не собран"
    exit 1
fi

echo "✅ APK успешно собран: $APK_PATH"

# Информация об APK
echo ""
echo "📊 Информация об APK:"
ls -lh "$APK_PATH"

if command -v aapt &> /dev/null; then
    echo ""
    aapt dump badging "$APK_PATH" | grep -E "package|sdkVersion|targetSdkVersion"
fi

# Проверка подписи (если подписан)
if command -v apksigner &> /dev/null; then
    echo ""
    echo "🔐 Проверка подписи:"
    apksigner verify --verbose "$APK_PATH" || echo "⚠️  APK не подписан"
fi

# Копирование APK с версией
OUTPUT_APK="ForkNews-v$VERSION.apk"
cp "$APK_PATH" "$OUTPUT_APK"
echo ""
echo "📦 APK скопирован: $OUTPUT_APK"

# Вычисление SHA-256
if command -v sha256sum &> /dev/null; then
    echo ""
    echo "🔒 SHA-256:"
    sha256sum "$OUTPUT_APK"
fi

# Создание Git тега
echo ""
read -p "❓ Создать Git тег v$VERSION? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    git tag -a "v$VERSION" -m "Release version $VERSION"
    echo "✅ Тег v$VERSION создан"
    
    read -p "❓ Запушить тег в origin? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git push origin "v$VERSION"
        echo "✅ Тег запушен"
    fi
fi

# Создание релиза на GitHub (если установлен gh)
if command -v gh &> /dev/null; then
    echo ""
    read -p "❓ Создать релиз на GitHub? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # Извлечение changelog для версии
        CHANGELOG=$(sed -n "/## \[$VERSION\]/,/## \[/p" CHANGELOG.md | head -n -1)
        
        gh release create "v$VERSION" \
            "$OUTPUT_APK" \
            --title "ForkNews v$VERSION" \
            --notes "$CHANGELOG"
        
        echo "✅ Релиз создан на GitHub"
        echo "🌐 https://github.com/Shalaykin1/ForkNews/releases/tag/v$VERSION"
    fi
else
    echo ""
    echo "💡 Подсказка: Установите GitHub CLI (gh) для автоматического создания релиза"
    echo "   https://cli.github.com/"
fi

echo ""
echo "🎉 Готово!"
echo ""
echo "📝 Следующие шаги:"
echo "1. Протестируйте APK: $OUTPUT_APK"
echo "2. Если APK не подписан, подпишите его (см. RELEASE_BUILD.md)"
echo "3. Создайте релиз на GitHub (если еще не создан)"
echo "4. Загрузите APK в релиз"
echo "5. Опубликуйте релиз"
echo ""
echo "📖 Документация: RELEASE_BUILD.md"
