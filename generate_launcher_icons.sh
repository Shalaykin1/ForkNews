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
            -draw "rectangle 16,12 18,22" \
            -draw "rectangle 20,12 22,22" \
            -draw "rectangle 24,12 26,22" \
            -draw "rectangle 28,12 30,22" \
            -draw "rectangle 15,11 31,12" \
            -draw "rectangle 21,22 23,24" \
            -draw "rectangle 22,24 23,31" \
            -font DejaVu-Sans-Bold -pointsize 7 \
            -gravity South -annotate +0+5 "ForkNews" \
            app/src/main/res/mipmap-mdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-mdpi/ic_launcher.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png
    
    # hdpi (72x72)
    convert -size 72x72 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 24,17 30,33" \
            -draw "rectangle 30,17 36,33" \
            -draw "rectangle 36,17 42,33" \
            -draw "rectangle 42,17 48,33" \
            -draw "rectangle 22,16 50,18" \
            -draw "rectangle 31,33 35,36" \
            -draw "rectangle 33,36 34,46" \
            -font DejaVu-Sans-Bold -pointsize 10 \
            -gravity South -annotate +0+7 "ForkNews" \
            app/src/main/res/mipmap-hdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-hdpi/ic_launcher.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png
    
    # xhdpi (96x96)
    convert -size 96x96 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 32,23 36,43" \
            -draw "rectangle 40,23 44,43" \
            -draw "rectangle 48,23 52,43" \
            -draw "rectangle 56,23 60,43" \
            -draw "rectangle 30,21 62,24" \
            -draw "rectangle 42,43 46,47" \
            -draw "rectangle 44,47 45,61" \
            -font DejaVu-Sans-Bold -pointsize 13 \
            -gravity South -annotate +0+10 "ForkNews" \
            app/src/main/res/mipmap-xhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xhdpi/ic_launcher.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
    
    # xxhdpi (144x144)
    convert -size 144x144 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 48,35 54,65" \
            -draw "rectangle 60,35 66,65" \
            -draw "rectangle 72,35 78,65" \
            -draw "rectangle 84,35 90,65" \
            -draw "rectangle 45,32 93,37" \
            -draw "rectangle 63,65 69,71" \
            -draw "rectangle 66,71 68,92" \
            -font DejaVu-Sans-Bold -pointsize 19 \
            -gravity South -annotate +0+15 "ForkNews" \
            app/src/main/res/mipmap-xxhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xxhdpi/ic_launcher.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
    
    # xxxhdpi (192x192)
    convert -size 192x192 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 64,47 72,87" \
            -draw "rectangle 80,47 88,87" \
            -draw "rectangle 96,47 104,87" \
            -draw "rectangle 112,47 120,87" \
            -draw "rectangle 60,43 124,49" \
            -draw "rectangle 84,87 92,95" \
            -draw "rectangle 88,95 90,122" \
            -font DejaVu-Sans-Bold -pointsize 25 \
            -gravity South -annotate +0+20 "ForkNews" \
            -font DejaVu-Sans-Bold -pointsize 26 \
            -gravity South -annotate +0+24 "ForkNews" \
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
