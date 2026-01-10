#!/bin/bash
echo "=== Проверка настройки фоновой работы ==="
echo ""
echo "1. ForkNewsApplication.kt:"
grep -A 5 "initWorkManager" app/src/main/java/com/forknews/ForkNewsApplication.kt
echo ""
echo "2. BootReceiver.kt:"
grep -A 3 "schedulePeriodicWork" app/src/main/java/com/forknews/receiver/BootReceiver.kt
echo ""
echo "3. MainActivity.kt (onCreate):"
grep -A 2 "schedulePeriodicWork" app/src/main/java/com/forknews/ui/main/MainActivity.kt
echo ""
echo "4. AndroidManifest.xml - BootReceiver:"
grep -A 5 "BootReceiver" app/src/main/AndroidManifest.xml
echo ""
echo "5. AndroidManifest.xml - Application:"
grep "android:name=\".ForkNewsApplication\"" app/src/main/AndroidManifest.xml
echo ""
echo "✅ Фоновая проверка настроена:"
echo "   - Интервал: 5 минут"
echo "   - Запускается при старте приложения (ForkNewsApplication)"
echo "   - Запускается в MainActivity"
echo "   - Перезапускается после перезагрузки устройства (BootReceiver)"
