@echo off
chcp 65001 >nul
echo ========================================================
echo  GWM Tank 500 / Haval - Android 9 Telemetry Auto-Setup
echo ========================================================
echo.

echo [1/4] Проверка подключения ADB...
adb devices
echo.

echo [2/4] Выдача системных прав Automotive для org.opengwm.split...
adb shell pm grant org.opengwm.split android.car.permission.CAR_SPEED 2>nul
adb shell pm grant org.opengwm.split android.car.permission.CAR_ENGINE_PROBING 2>nul
adb shell pm grant org.opengwm.split android.car.permission.CAR_INFO 2>nul
adb shell pm grant org.opengwm.split android.car.permission.CAR_POWERTRAIN 2>nul
adb shell pm grant org.opengwm.split android.car.permission.CAR_DYNAMICS_STATE 2>nul
adb shell pm grant org.opengwm.split android.car.permission.CAR_EXTERIOR_ENVIRONMENT 2>nul
adb shell pm grant org.opengwm.split android.car.permission.CAR_TIRES 2>nul
adb shell pm grant org.opengwm.split android.car.permission.CAR_IDENTIFICATION 2>nul
adb shell pm grant org.opengwm.split com.gwm.android.adapter.permission.GET_DATA 2>nul
echo [OK] Все разрешения запрошены.
echo.

echo [3/4] Тест прямого отклика шины через CarService:
echo --- Скорость (PROP 0x11600207):
adb shell cmd car_service get-property-value 0x11600207 0
echo --- Передача (PROP 0x11400401):
adb shell cmd car_service get-property-value 0x11400401 0
echo --- 9AT ступень GWM (PROP 0x214016c9):
adb shell cmd car_service get-property-value 0x214016c9 0
echo --- Обороты ДВС (PROP 0x11600305):
adb shell cmd car_service get-property-value 0x11600305 0
echo.

echo [4/4] Запуск приложения на ГУ...
adb shell am start -n org.opengwm.split/.MainActivity
echo.
echo ========================================================
echo  Готово! Мониторинг логов телеметрии (Ctrl+C для выхода):
echo ========================================================
adb logcat -s AASVehicleProvider:I
