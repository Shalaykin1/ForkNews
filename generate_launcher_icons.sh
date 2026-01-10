#!/bin/bash

# Создаём простую PNG иконку с помощью ImageMagick (если доступен)
# Если ImageMagick недоступен, создаём минимальные иконки

echo "Генерация иконок лаунчера..."

# Проверяем наличие convert (ImageMagick)
if command -v convert &> /dev/null; then
    echo "ImageMagick найден, генерируем качественные иконки..."
    
    # mdpi (48x48)
    convert -size 48x48 xc:"#0049B8" \
            -fill white -draw "roundrectangle 8,7 40,41 2,2" \
            -fill "#0049B8" -draw "rectangle 11,12 35,14" \
            -fill "#0049B8" -draw "rectangle 11,17 35,19" \
            -fill "#0049B8" -draw "rectangle 11,22 28,24" \
            -fill "#0049B8" -draw "circle 24,10 24,12" \
            -fill "#0049B8" -draw "circle 17,34 17,36" \
            -fill "#0049B8" -draw "circle 31,34 31,36" \
            -fill "#0049B8" -draw "line 24,12 24,26" \
            -fill "#0049B8" -draw "line 17,26 31,26" \
            -fill "#0049B8" -draw "line 17,26 17,34" \
            -fill "#0049B8" -draw "line 31,26 31,34" \
            app/src/main/res/mipmap-mdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-mdpi/ic_launcher.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png
    
    # hdpi (72x72)
    convert -size 72x72 xc:"#0049B8" \
            -fill white -draw "roundrectangle 12,10 60,62 3,3" \
            -fill "#0049B8" -draw "rectangle 17,18 53,21" \
            -fill "#0049B8" -draw "rectangle 17,26 53,29" \
            -fill "#0049B8" -draw "rectangle 17,34 42,37" \
            -fill "#0049B8" -draw "circle 36,15 36,18" \
            -fill "#0049B8" -draw "circle 26,51 26,54" \
            -fill "#0049B8" -draw "circle 46,51 46,54" \
            -fill "#0049B8" -draw "line 36,18 36,40" \
            -fill "#0049B8" -draw "line 26,40 46,40" \
            -fill "#0049B8" -draw "line 26,40 26,51" \
            -fill "#0049B8" -draw "line 46,40 46,51" \
            app/src/main/res/mipmap-hdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-hdpi/ic_launcher.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png
    
    # xhdpi (96x96)
    convert -size 96x96 xc:"#0049B8" \
            -fill white -draw "roundrectangle 16,14 80,82 4,4" \
            -fill "#0049B8" -draw "rectangle 23,24 71,28" \
            -fill "#0049B8" -draw "rectangle 23,34 71,38" \
            -fill "#0049B8" -draw "rectangle 23,44 56,48" \
            -fill "#0049B8" -draw "circle 48,20 48,24" \
            -fill "#0049B8" -draw "circle 35,68 35,72" \
            -fill "#0049B8" -draw "circle 61,68 61,72" \
            -fill "#0049B8" -draw "line 48,24 48,53" \
            -fill "#0049B8" -draw "line 35,53 61,53" \
            -fill "#0049B8" -draw "line 35,53 35,68" \
            -fill "#0049B8" -draw "line 61,53 61,68" \
            app/src/main/res/mipmap-xhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xhdpi/ic_launcher.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
    
    # xxhdpi (144x144)
    convert -size 144x144 xc:"#0049B8" \
            -fill white -draw "roundrectangle 24,21 120,123 6,6" \
            -fill "#0049B8" -draw "rectangle 34,36 106,42" \
            -fill "#0049B8" -draw "rectangle 34,51 106,57" \
            -fill "#0049B8" -draw "rectangle 34,66 84,72" \
            -fill "#0049B8" -draw "circle 72,30 72,36" \
            -fill "#0049B8" -draw "circle 52,102 52,108" \
            -fill "#0049B8" -draw "circle 92,102 92,108" \
            -fill "#0049B8" -draw "line 72,36 72,80" \
            -fill "#0049B8" -draw "line 52,80 92,80" \
            -fill "#0049B8" -draw "line 52,80 52,102" \
            -fill "#0049B8" -draw "line 92,80 92,102" \
            app/src/main/res/mipmap-xxhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xxhdpi/ic_launcher.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
    
    # xxxhdpi (192x192)
    convert -size 192x192 xc:"#0049B8" \
            -fill white -draw "roundrectangle 32,28 160,164 8,8" \
            -fill "#0049B8" -draw "rectangle 45,48 141,56" \
            -fill "#0049B8" -draw "rectangle 45,68 141,76" \
            -fill "#0049B8" -draw "rectangle 45,88 112,96" \
            -fill "#0049B8" -draw "circle 96,40 96,48" \
            -fill "#0049B8" -draw "circle 69,136 69,144" \
            -fill "#0049B8" -draw "circle 123,136 123,144" \
            -fill "#0049B8" -draw "line 96,48 96,107" \
            -fill "#0049B8" -draw "line 69,107 123,107" \
            -fill "#0049B8" -draw "line 69,107 69,136" \
            -fill "#0049B8" -draw "line 123,107 123,136" \
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
