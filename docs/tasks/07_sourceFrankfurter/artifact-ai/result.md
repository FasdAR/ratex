# Результат: источник Frankfurter (задача 07)

Ветка `feature/07-sourceFrankfurter` от `develop`. Дата сессии: 2026-10-07.

## Что сделано
- `FrankfurterRateSource` + `FrankfurterJsonParser` (`currency/data/source/frankfurter/`): `GET {baseUrl}/v2/rates?base=EUR`, база снимка EUR, `id = "frankfurter"`.
- `RateSourcePriority` содержит только `FRANKFURTER`. ECB, CBR, FED, Treasury (классы, парсеры, тесты, `network_security_config`, сертификаты) оставлены в проекте, но не подключены. Чтобы вернуть источник, добавьте константу в enum.
- Базовый URL вынесен в `BuildConfig.FRANKFURTER_BASE_URL` для обоих flavor (`dev`, `prod`). Значение по умолчанию `https://api.frankfurter.dev` (без версии: путь `/v2/rates` — константа в коде). Переопределение по приоритету: env `RATEX_FRANKFURTER_BASE_URL` → gradle-свойство `frankfurterBaseUrl` → значение flavor. Хвостовой `/` обрезается и в gradle, и в источнике.
- Тесты: `FrankfurterJsonParserTest`, `FrankfurterRateSourceTest` (MockEngine), `RateSourcePriorityTest` (теперь `["frankfurter"]`). Из `MergedCurrencyRateRepoTest` удалён тест порядка enum старых четырёх источников (порядок проверяет `RateSourcePriorityTest`).

## Что выяснено про API (проверено вживую 2026-10-07)
- Актуальная версия `v2`; `v1/latest` помечен deprecated (заголовки `deprecation`, `link: rel="successor-version"`).
- `/v2/rates?base=EUR` отдаёт массив `{date, base, quote, rate}`, около 160 валют. Дата у каждой пары своя (выходные и праздники центробанков), датой снимка берём максимальную.
- Минимальный срок обновления в документации (`llms.txt`, OpenAPI) не назван. Ориентир — заголовок ответа `cache-control: public, max-age=71640, stale-if-error=86400` (≈19,9 ч): запрос чаще возвращает тот же ответ из кэша Cloudflare. Поэтому `refreshInterval = 20 часов`. Если сервер поменяет `max-age`, значение нужно пересмотреть.
- Ключ не нужен, квоты и лимиты в документации не заявлены.

## Решения и ограничения
- Frankfurter единственный активный источник: если он недоступен, а кэша нет, приложение курсов не покажет (остальные источники не подключены). Старые снимки других источников в Room остаются, но не читаются.
- Self-hosted инстанс по `http://` потребует cleartext-разрешения в `network_security_config` — не делалось (YAGNI).
- TLS: `api.frankfurter.dev` стоит за Cloudflare, отдельных trust anchors не добавлялось. На эмуляторе API 34 вживую не проверялось.

## Проверки
`./gradlew test ktlintCheck assembleDevDebug assembleProdDebug :app:lintDevRelease` — успешно (136 unit-тестов, 0 падений). Переопределение URL проверено через `generateDevDebugBuildConfig`.
