# Open GWM Split

Открытый аналог и минимальная база для разделения экрана и плавающих панелей управления на автомобилях Great Wall Motors (Tank 300, Tank 500, Haval).

## Результаты анализа оригинального APK (`com.altaysoft.t500_split`)

1. **Архитектура связи с автомобилем**:
   - Внутри APK **нет** прямых классов `com.gwm.*`.
   - Взаимодействие с телеметрией машины идет через нативные библиотеки (`libas_ipc0.so`, `libas_ipc1.so`), использующие **[FDBus](https://github.com/jeremyczhen/fdbus)** (Fast Distributed Bus) поверх локальных сокетов к системным демонам GWM.
   - Используются системные свойства: `persist.gwm.vehicle.vin`, `persist.vendor.gwm.cfg.project.code`, `gwm_hvac_panel_states`.
   - Присутствует встроенный SSH-клиент (JSch) для сервисных команд на ГУ.

2. **Архитектура интерфейса**:
   - Плавающий виджет через `WindowManager` (`TYPE_APPLICATION_OVERLAY`).
   - Перехват и эмуляция системных кнопок ("Назад", "Домой", переключение сплита) через `AccessibilityService` (`GLOBAL_ACTION_BACK`, `GLOBAL_ACTION_HOME`, `GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN`).

## Структура проекта

- `app/src/main/java/org/opengwm/split/service/FloatingControlsService.kt` — плавающая панель поверх всех окон с кнопками управления и мини-спидометром.
- `app/src/main/java/org/opengwm/split/service/CarAccessibilityService.kt` — служба доступности для эмуляции кнопок и переключения мульти-оконного режима Android.
- `app/src/main/java/org/opengwm/split/vehicle/VehicleContract.kt` — интерфейс телеметрии авто.
- `app/src/main/java/org/opengwm/split/vehicle/MockVehicleProvider.kt` — генератор фейковых данных для тестов на эмуляторе без автомобиля.
- `app/src/main/java/org/opengwm/split/MainActivity.kt` — экран настроек и выдачи разрешений.

## Запуск и тестирование

1. Открыть проект в Android Studio: `C:\Users\ttt79\.gemini\antigravity\scratch\open-gwm-split`
2. Запустить эмулятор с кастомным разрешением экрана под авто.
3. Выдать разрешение на оверлей и активировать службу доступности.
