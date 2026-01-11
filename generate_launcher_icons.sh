#!/bin/bash

# Создаём простую PNG иконку с помощью ImageMagick (если доступен)
# Если ImageMagick недоступен, создаём минимальные иконки

echo "Генерация иконок лаунчера..."

# Проверяем наличие convert (ImageMagick)
if command -v convert &> /dev/null; then
    echo "ImageMagick найден, генерируем качественные иконки..."
    
    # mdpi (48x48)
    convert -size 48x48 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 6,15 7,19" -draw "rectangle 8,15 9,19" -draw "rectangle 10,15 11,19" -draw "rectangle 12,15 13,19" \
            -draw "rectangle 5,15 13,16" -draw "rectangle 8,19 11,24" \
            -font DejaVu-Sans-Bold -pointsize 9 -gravity West -annotate +14+0 "ForkNews" \
            -fill none -stroke white -strokewidth 1.5 -draw "path 'M 14,33 Q 20,32 26,33 Q 32,34 38,33'" \
            -fill white -draw "path 'M 38,33 L 40,29 L 41,34 Z'" \
            app/src/main/res/mipmap-mdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-mdpi/ic_launcher.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png
    
    # hdpi (72x72)
    convert -size 72x72 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 9,23 11,28" -draw "rectangle 12,23 14,28" -draw "rectangle 15,23 17,28" -draw "rectangle 18,23 20,28" \
            -draw "rectangle 8,22 21,24" -draw "rectangle 12,28 17,36" \
            -font DejaVu-Sans-Bold -pointsize 13 -gravity West -annotate +21-1 "ForkNews" \
            -fill none -stroke white -strokewidth 2 -draw "path 'M 21,49 Q 30,47 39,49 Q 48,51 57,49'" \
            -fill white -draw "path 'M 57,49 L 60,43 L 62,50 Z'" \
            app/src/main/res/mipmap-hdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-hdpi/ic_launcher.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png
    
    # xhdpi (96x96)
    convert -size 96x96 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 12,31 15,37" -draw "rectangle 16,31 19,37" -draw "rectangle 20,31 23,37" -draw "rectangle 24,31 27,37" \
            -draw "rectangle 11,30 28,32" -draw "rectangle 16,37 23,48" \
            -font DejaVu-Sans-Bold -pointsize 17 -gravity West -annotate +28-1 "ForkNews" \
            -fill none -stroke white -strokewidth 2.5 -draw "path 'M 28,66 Q 40,63 52,66 Q 64,69 76,66'" \
            -fill white -draw "path 'M 76,66 L 80,58 L 83,67 Z'" \
            app/src/main/res/mipmap-xhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xhdpi/ic_launcher.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
    
    # xxhdpi (144x144)
    convert -size 144x144 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 18,46 22,56" -draw "rectangle 24,46 28,56" -draw "rectangle 30,46 34,56" -draw "rectangle 36,46 40,56" \
            -draw "rectangle 17,45 41,48" -draw "rectangle 24,56 34,72" \
            -font DejaVu-Sans-Bold -pointsize 26 -gravity West -annotate +42-2 "ForkNews" \
            -fill none -stroke white -strokewidth 3.5 -draw "path 'M 42,99 Q 60,95 78,99 Q 96,103 114,99'" \
            -fill white -draw "path 'M 114,99 L 120,87 L 124,100 Z'" \
            app/src/main/res/mipmap-xxhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xxhdpi/ic_launcher.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
    
    # xxxhdpi (192x192)
    convert -size 192x192 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 24,62 30,74" -draw "rectangle 32,62 38,74" -draw "rectangle 40,62 46,74" -draw "rectangle 48,62 54,74" \
            -draw "rectangle 23,60 55,64" -draw "rectangle 32,74 46,96" \
            -font DejaVu-Sans-Bold -pointsize 34 -gravity West -annotate +56-3 "ForkNews" \
            -fill none -stroke white -strokewidth 4.5 -draw "path 'M 56,132 Q 80,127 104,132 Q 128,137 152,132'" \
            -fill white -draw "path 'M 152,132 L 160,116 L 165,133 Z'" \
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
