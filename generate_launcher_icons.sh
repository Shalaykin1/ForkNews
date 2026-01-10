#!/bin/bash

# Создаём простую PNG иконку с помощью ImageMagick (если доступен)
# Если ImageMagick недоступен, создаём минимальные иконки

echo "Генерация иконок лаунчера..."

# Проверяем наличие convert (ImageMagick)
if command -v convert &> /dev/null; then
    echo "ImageMagick найден, генерируем качественные иконки..."
    
    # mdpi (48x48)
    convert -size 48x48 xc:"#0049B8" \
            -fill white -stroke white -strokewidth 1 \
            -draw "circle 24,14 24,18" \
            -draw "circle 16,36 16,40" \
            -draw "circle 32,36 32,40" \
            -draw "line 24,18 24,28" \
            -draw "line 16,28 32,28" \
            -draw "line 16,28 16,36" \
            -draw "line 32,28 32,36" \
            app/src/main/res/mipmap-mdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-mdpi/ic_launcher.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png
    
    # hdpi (72x72)
    convert -size 72x72 xc:"#0049B8" \
            -fill white -stroke white -strokewidth 2 \
            -draw "circle 36,21 36,27" \
            -draw "circle 24,54 24,60" \
            -draw "circle 48,54 48,60" \
            -draw "line 36,27 36,42" \
            -draw "line 24,42 48,42" \
            -draw "line 24,42 24,54" \
            -draw "line 48,42 48,54" \
            app/src/main/res/mipmap-hdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-hdpi/ic_launcher.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png
    
    # xhdpi (96x96)
    convert -size 96x96 xc:"#0049B8" \
            -fill white -stroke white -strokewidth 2 \
            -draw "circle 48,28 48,36" \
            -draw "circle 32,72 32,80" \
            -draw "circle 64,72 64,80" \
            -draw "line 48,36 48,56" \
            -draw "line 32,56 64,56" \
            -draw "line 32,56 32,72" \
            -draw "line 64,56 64,72" \
            app/src/main/res/mipmap-xhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xhdpi/ic_launcher.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
    
    # xxhdpi (144x144)
    convert -size 144x144 xc:"#0049B8" \
            -fill white -stroke white -strokewidth 3 \
            -draw "circle 72,42 72,54" \
            -draw "circle 48,108 48,120" \
            -draw "circle 96,108 96,120" \
            -draw "line 72,54 72,84" \
            -draw "line 48,84 96,84" \
            -draw "line 48,84 48,108" \
            -draw "line 96,84 96,108" \
            app/src/main/res/mipmap-xxhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xxhdpi/ic_launcher.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
    
    # xxxhdpi (192x192)
    convert -size 192x192 xc:"#0049B8" \
            -fill white -stroke white -strokewidth 4 \
            -draw "circle 96,56 96,72" \
            -draw "circle 64,144 64,160" \
            -draw "circle 128,144 128,160" \
            -draw "line 96,72 96,112" \
            -draw "line 64,112 128,112" \
            -draw "line 64,112 64,144" \
            -draw "line 128,112 128,144" \
            app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xxxhdpi/ic_launcher.png app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
    
    echo "✅ Иконки созданы с помощью ImageMagick"
else
    echo "⚠️  ImageMagick не найден"
    echo "Для генерации качественных иконок установите ImageMagick:"
    echo "  sudo apt-get install imagemagick"
    echo ""
    echo "Или используйте онлайн генераторы:"
    echo "  - https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html"
    echo "  - https://icon.kitchen/"
fi

echo ""
echo "Готово! Иконки созданы в mipmap-* директориях"
