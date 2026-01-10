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
            -draw "rectangle 23,10 25,12" \
            -draw "path 'M 20,12 L 28,12 L 29,20 Q 29,22 27,23 L 21,23 Q 19,22 19,20 Z'" \
            -draw "rectangle 23,23 25,25" \
            -draw "path 'M 16,14 L 17,14 Q 18,16 17,18 L 16,18 Q 15,16 16,14 Z'" \
            -draw "path 'M 31,14 L 32,14 Q 33,16 32,18 L 31,18 Q 30,16 31,14 Z'" \
            -font DejaVu-Sans-Bold -pointsize 7 \
            -gravity South -annotate +0+5 "ForkNews" \
            app/src/main/res/mipmap-mdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-mdpi/ic_launcher.png app/src/main/res/mipmap-mdpi/ic_launcher_round.png
    
    # hdpi (72x72)
    convert -size 72x72 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 35,15 37,18" \
            -draw "path 'M 30,18 L 42,18 L 44,30 Q 44,33 41,35 L 31,35 Q 28,33 28,30 Z'" \
            -draw "rectangle 35,35 37,38" \
            -draw "path 'M 24,21 L 26,21 Q 27,24 26,27 L 24,27 Q 23,24 24,21 Z'" \
            -draw "path 'M 46,21 L 48,21 Q 49,24 48,27 L 46,27 Q 45,24 46,21 Z'" \
            -font DejaVu-Sans-Bold -pointsize 10 \
            -gravity South -annotate +0+7 "ForkNews" \
            app/src/main/res/mipmap-hdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-hdpi/ic_launcher.png app/src/main/res/mipmap-hdpi/ic_launcher_round.png
    
    # xhdpi (96x96)
    convert -size 96x96 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 46,20 50,24" \
            -draw "path 'M 40,24 L 56,24 L 59,40 Q 59,44 55,46 L 41,46 Q 37,44 37,40 Z'" \
            -draw "rectangle 46,46 50,50" \
            -draw "path 'M 32,28 L 34,28 Q 36,32 34,36 L 32,36 Q 30,32 32,28 Z'" \
            -draw "path 'M 62,28 L 64,28 Q 66,32 64,36 L 62,36 Q 60,32 62,28 Z'" \
            -font DejaVu-Sans-Bold -pointsize 13 \
            -gravity South -annotate +0+10 "ForkNews" \
            -font DejaVu-Sans-Bold -pointsize 13 \
            -gravity South -annotate +0+10 "ForkNews" \
            app/src/main/res/mipmap-xhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xhdpi/ic_launcher.png app/src/main/res/mipmap-xhdpi/ic_launcher_round.png
    
    # xxhdpi (144x144)
    convert -size 144x144 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 70,30 74,36" \
            -draw "path 'M 60,36 L 84,36 L 88,60 Q 88,66 82,69 L 62,69 Q 56,66 56,60 Z'" \
            -draw "rectangle 70,69 74,75" \
            -draw "path 'M 48,42 L 51,42 Q 54,48 51,54 L 48,54 Q 45,48 48,42 Z'" \
            -draw "path 'M 93,42 L 96,42 Q 99,48 96,54 L 93,54 Q 90,48 93,42 Z'" \
            -font DejaVu-Sans-Bold -pointsize 19 \
            -gravity South -annotate +0+15 "ForkNews" \
            app/src/main/res/mipmap-xxhdpi/ic_launcher.png
    
    cp app/src/main/res/mipmap-xxhdpi/ic_launcher.png app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png
    
    # xxxhdpi (192x192)
    convert -size 192x192 xc:"#0049B8" \
            -fill white \
            -draw "rectangle 93,40 99,48" \
            -draw "path 'M 80,48 L 112,48 L 117,80 Q 117,88 109,92 L 83,92 Q 75,88 75,80 Z'" \
            -draw "rectangle 93,92 99,100" \
            -draw "path 'M 64,56 L 68,56 Q 72,64 68,72 L 64,72 Q 60,64 64,56 Z'" \
            -draw "path 'M 124,56 L 128,56 Q 132,64 128,72 L 124,72 Q 120,64 124,56 Z'" \
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
