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
            -draw "rectangle 20,10 28,30" \
            -draw "rectangle 15,10 17,20" \
            -draw "rectangle 18,10 20,20" \
            -draw "rectangle 21,10 23,20" \
            -draw "rectangle 24,10 26,20" \
            -draw "rectangle 14,9 27,11" \
            -font DejaVu-Sans-Bold -pointsize 8 \
            -gravity South -annotate +0+5 "ForkNews" \
            app/src/main/res/mipmap-mdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-mdpi/ic_launcher.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png
    
    # hdpi (72x72)
    convert -size 72x72 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 30,15 42,45" \
            -draw "rectangle 22,15 26,30" \
            -draw "rectangle 27,15 31,30" \
            -draw "rectangle 32,15 36,30" \
            -draw "rectangle 37,15 41,30" \
            -draw "rectangle 21,13 42,17" \
            -font DejaVu-Sans-Bold -pointsize 11 \
            -gravity South -annotate +0+8 "ForkNews" \
            app/src/main/res/mipmap-hdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-hdpi/ic_launcher.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png
    
    # xhdpi (96x96)
    convert -size 96x96 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 40,20 56,60" \
            -draw "rectangle 30,20 34,40" \
            -draw "rectangle 36,20 40,40" \
            -draw "rectangle 43,20 47,40" \
            -draw "rectangle 50,20 54,40" \
            -draw "rectangle 28,18 56,22" \
            -font DejaVu-Sans-Bold -pointsize 14 \
            -gravity South -annotate +0+12 "ForkNews" \
            app/src/main/res/mipmap-xhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xhdpi/ic_launcher.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
    
    # xxhdpi (144x144)
    convert -size 144x144 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 60,30 84,90" \
            -draw "rectangle 45,30 51,60" \
            -draw "rectangle 54,30 60,60" \
            -draw "rectangle 64,30 70,60" \
            -draw "rectangle 74,30 80,60" \
            -draw "rectangle 42,27 84,33" \
            -font DejaVu-Sans-Bold -pointsize 20 \
            -gravity South -annotate +0+18 "ForkNews" \
            app/src/main/res/mipmap-xxhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xxhdpi/ic_launcher.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
    
    # xxxhdpi (192x192)
    convert -size 192x192 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 80,40 112,120" \
            -draw "rectangle 60,40 68,80" \
            -draw "rectangle 72,40 80,80" \
            -draw "rectangle 85,40 93,80" \
            -draw "rectangle 98,40 106,80" \
            -draw "rectangle 56,36 112,44" \
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
