# Задача
Проблема следующего характера, ИИ не можеи запустить Gradle при работе, и использует хак с явным указанием JAVA_HOME, вот что указывает сама ИИ:
- Нужны JDK и Android SDK (путь в `local.properties`, не коммитится). Gradle не работает с Java 25 (JBR из Android Studio) — запускайте с JDK 17, например `JAVA_HOME=~/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home`.

1. Необходимо исправить что-бы любая работа с Gradle что в терминале, что в ИИ проходила без указания JAVA_HOME
2. Поискать и узнать какая актуальная JAVA версия для `sourceCompatibility` и `targetCompatibility`, если выше `11` - Исправить на новую  