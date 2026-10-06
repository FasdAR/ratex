# Проблема
- Источники курсов сейчас: ЕЦБ (EUR) и, после задачи 03, ЦБ РФ (RUB). Нет источника из США (база USD)
- Для USD и ряда валют курс получается только через EUR или RUB. Источник из США даст независимое подтверждение и расширит набор валют

# Решение
- Выбрать первичный источник из США (не агрегатор и не обёртка над чужими данными), реализовать его за контрактом `CurrencyRateSource`
- Кандидаты (не проверялись, в начале задачи изучить документацию и сравнить):
  - ФРС США, отчёт H.10 (Foreign Exchange Rates): курсы к USD, около 20 валют, формат и частота обновления неизвестны
  - Минфин США, Treasury Reporting Rates of Exchange (fiscaldata.treasury.gov API): много валют, но, насколько известно, обновляется раз в квартал, для актуальных курсов может не подойти
- Критерии выбора: актуальность (ежедневно), число валют, формат (JSON/XML/CSV), отсутствие ключа и лимитов, условия использования
- Если ни один кандидат не подходит (например, только квартальные данные), зафиксировать это в артефакте и не реализовывать источник

# Итог
- Описано сравнение кандидатов и выбор с обоснованием
- Реализован выбранный источник (`UsaRateSource` или по названию источника) с тестами на фикстуре
- Подключён в DI вместе с остальными источниками

# Открытые вопросы (решить в брейнсторме)
- Источник от США считает в USD: как он вписывается в схему нескольких источников и в мост между источниками (см. задачу 03)
- Приоритет при пересечении валют с ЕЦБ и ЦБ РФ
- Проверить TLS-цепочку на эмуляторе: у ЕЦБ была проблема с корнем Sectigo E46, см. `docs-ai/artifact/2026-10-06-source-data.md`

# Решение брейнсторма (2026-10-06)
Статус: реализовано в ветке `feature/04-source-usa` (не закоммичено), результат — `docs-ai/artifact/2026-10-06-source-usa.md`. Не проверено на эмуляторе.

Решение пользователя: реализовать **оба** источника США, оба с самым низким приоритетом. Правило «не реализовывать, если данные неактуальны» не применяем.
- Приоритет: ЕЦБ → ЦБ РФ → ФРС H.10 (раз в неделю) → Treasury (раз в квартал).
- По документации (страницы не открывались через живой API): H.10 публикуется по понедельникам за прошлую неделю, DDP выводится из эксплуатации, замена FRED, вероятно, с ключом. Treasury обновляется раз в квартал (последнее 05.10.2026, следующее ожидается 15.10.2026), ключ не нужен.

## Дизайн
Всё в `currency/data/source/<fed|treasury>/`: источник, парсер, тесты на фикстурах. Каждый реализует `CurrencyRateSource`, база USD. Domain, UI и схема Room не меняются.
- **`FedRateSource`** (`id = "fed"`, интервал 12 ч): часть валют котируется как USD за единицу (EUR, GBP, AUD, NZD), часть как единиц за USD, приводим к «валют за USD». `ND` (праздники) пропускаем, берём последнее значение и его дату. Формат и URL выгрузки зависят от проверки.
- **`TreasuryRateSource`** (`id = "treasury"`, интервал 24 ч): JSON `rates_of_exchange`, значения строками, берём записи с максимальной `record_date`. ISO-кодов в данных нет (`country_currency_desc` вида «Euro Zone-Euro»), нужна таблица «описание → ISO»; записи без соответствия пропускаем.
- **`RateSourcePriority`**: `ECB`, `CBR`, `FED`, `TREASURY`.
- **DI (`CurrencyModule`)**: ещё два `CurrencyRateRepoImpl` (кэш по `sourceId`), оба источника в `MergedCurrencyRateRepo`. Мост через USD (у ЕЦБ он есть). Не первые источники уже получают дедлайн 5 с (`secondaryTimeout`).
- **Принимаем:** курсы Treasury могут быть старше трёх месяцев; низкоприоритетные источники только добавляют валюты, которых нет выше; дата снимка максимальная.
- **Тесты:** парсеры на фикстурах из настоящих ответов (норма, `ND`, кривые значения, пустой ответ), поведение источников, пара кейсов приоритета в `MergedCurrencyRateRepoTest`. Проверка: `./gradlew test assembleDebug ktlintCheck`.
- Ветка `feature/04-source-usa`; результат записать в `docs-ai/artifact/2026-10-06-source-usa.md`.

## Что проверить до реализации
Команды запускать в обычном терминале. Файлы складываются в `$D`; сохранить результаты и принести их в сессию (или вставить вывод).

```bash
D=/tmp/ratex-usa; mkdir -p $D
```

1. **Treasury: доступность и формат** (ожидание: `200`, `application/json`):
   ```bash
   curl -gsS -m 30 -D $D/treasury.headers -o $D/treasury.json \
     "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange?sort=-record_date&page[size]=400&fields=record_date,country,currency,country_currency_desc,exchange_rate,effective_date"
   head -n1 $D/treasury.headers; head -c 900 $D/treasury.json
   
   result: 
   HTTP/1.1 200 OK
   {"data":[{"record_date":"2026-09-30","country":"Afghanistan","currency":"Afghani","country_currency_desc":"Afghanistan-Afghani","exchange_rate":"65.15","effective_date":"2026-09-30"},{"record_date":"2026-09-30","country":"Albania","currency":"Lek","country_currency_desc":"Albania-Lek","exchange_rate":"80.75","effective_date":"2026-09-30"},{"record_date":"2026-09-30","country":"Algeria","currency":"Dinar","country_currency_desc":"Algeria-Dinar","exchange_rate":"133.126","effective_date":"2026-09-30"},{"record_date":"2026-09-30","country":"Angola","currency":"Kwanza","country_currency_desc":"Angola-Kwanza","exchange_rate":"913.0","effective_date":"2026-09-30"},{"record_date":"2026-09-30","country":"Antigua & Barbuda","currency":"East Caribbean Dollar","country_currency_desc":"Antigua & Barbuda-East Caribbean Dollar","exchange_rate":"2.7","effective_date":"2026-09-30"},{"record_date":"2026-%
   ```
   Смотрим: сколько записей на одну `record_date`, как выглядят `country_currency_desc` и `exchange_rate`, есть ли `null`/пустые строки, нужна ли пагинация (`meta.total-pages`).
2. **Treasury: свежая дата и полный набор описаний** (для таблицы «описание → ISO»):
   ```bash
   curl -gsS -m 30 "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange?sort=-record_date&page[size]=1&fields=record_date"
   curl -gsS -m 30 -o $D/treasury-last.json "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange?filter=record_date:eq:<ДАТА_ИЗ_ПРЕДЫДУЩЕГО>&page[size]=500&fields=country_currency_desc,exchange_rate,effective_date"
   
   result:
   {"data":[{"record_date":"2026-09-30"}],"meta":{"count":1,"labels":{"record_date":"Record Date"},"dataTypes":{"record_date":"DATE"},"dataFormats":{"record_date":"YYYY-MM-DD"},"total-count":103,"total-pages":103},"links":{"self":"&page%5Bnumber%5D=1&page%5Bsize%5D=1","first":"&page%5Bnumber%5D=1&page%5Bsize%5D=1","prev":null,"next":"&page%5Bnumber%5D=2&page%5Bsize%5D=1","last":"&page%5Bnumber%5D=103&page%5Bsize%5D=1"}}% 
   ```
3. **ФРС H.10: страница релиза и ссылки на выгрузку:**
   ```bash
   curl -sSL -m 30 -D $D/h10.headers -o $D/h10-current.html "https://www.federalreserve.gov/releases/h10/current/"
   head -n1 $D/h10.headers; wc -c $D/h10-current.html
   grep -oiE 'href="[^"]*(datadownload|\.csv|\.xml|\.json)[^"]*"' $D/h10-current.html | sort -u
   
   result:
   HTTP/2 200 
   85787 /tmp/ratex-usa/h10-current.html
   href="/datadownload"
   href="https://www.federalreserve.gov/datadownload/Choose.aspx?rel=H10"
   ```
4. **ФРС H.10: DDP-выгрузка** (ссылку с хэшем серии взять со страницы релиза, ссылка «Data Download Program»; ожидание: CSV или XML со всеми валютами, не пустое тело и не HTML):
   ```bash
   curl -sSL -m 30 -D $D/ddp.headers -o $D/ddp.out "<URL_DDP_СО_СТРАНИЦЫ>"
   head -n1 $D/ddp.headers; grep -i content-type $D/ddp.headers; wc -c $D/ddp.out; head -c 1200 $D/ddp.out
   
   result:
   curl: (3) URL rejected: Bad hostname
   wc: /tmp/ratex-usa/ddp.out: open: No such file or directory
   head: /tmp/ratex-usa/ddp.out: No such file or directory
   ```
   Смотрим: есть ли все ~20 валют, как записан `ND`, как указано направление котировки (за единицу или за USD), дата в каждой строке.
5. **ФРС H.10: запасной вариант FRED без ключа** (одна серия на валюту, поэтому ~20 запросов; проверяем, что отдаётся без ключа):
   ```bash
   curl -sS -m 30 -o $D/fred-eur.csv -w "%{http_code} %{content_type}\n" "https://fred.stlouisfed.org/graph/fredgraph.csv?id=DEXUSEU"
   tail -n 5 $D/fred-eur.csv
   
   result:
   200 application/csv
    2026-09-28,1.1367
    2026-09-29,1.1336
    2026-09-30,1.1345
    2026-10-01,1.1232
    2026-10-02,1.1259
   ```
6. **TLS на эмуляторе API 34** (у ЕЦБ была проблема с корнем Sectigo E46, см. `docs-ai/artifact/2026-10-06-source-data.md`). После реализации открыть оба хоста (`api.fiscaldata.treasury.gov`, хост H.10) на эмуляторе. Заранее цепочку можно посмотреть так:
   ```bash
   openssl s_client -connect api.fiscaldata.treasury.gov:443 -servername api.fiscaldata.treasury.gov -showcerts </dev/null 2>/dev/null | grep -E "s:|i:"
   openssl s_client -connect www.federalreserve.gov:443 -servername www.federalreserve.gov -showcerts </dev/null 2>/dev/null | grep -E "s:|i:"
   
   result:
   -E "s:|i:"
    0 s:C=US, ST=District of Columbia, O=Department of Treasury, CN=fiscaldata.treasury.gov
    i:C=CA, O=Entrust Limited, CN=Entrust OV TLS Issuing RSA CA 2
    1 s:C=CA, O=Entrust Limited, CN=Entrust OV TLS Issuing RSA CA 2
    i:C=GB, O=Sectigo Limited, CN=Sectigo Public Server Authentication Root R46
    2 s:C=GB, O=Sectigo Limited, CN=Sectigo Public Server Authentication Root R46
    i:C=GB, O=Sectigo Limited, CN=Sectigo Public Server Authentication Root R46
    0 s:CN=federalreserve.gov
    i:C=US, O=Google Trust Services, CN=WR1
    1 s:C=US, O=Google Trust Services, CN=WR1
    i:C=US, O=Google Trust Services LLC, CN=GTS Root R1
    2 s:C=US, O=Google Trust Services LLC, CN=GTS Root R1
    i:C=BE, O=GlobalSign nv-sa, OU=Root CA, CN=GlobalSign Root CA
   ```

## Выводы по результатам (2026-10-06)
- **Treasury работает без ключа** (`200`, JSON). Последняя запись `record_date = 2026-09-30`, формат поля `exchange_rate` — строка (`"913.0"`), направление «валюты за USD» (65.15 AFN за USD). ISO-кодов нет, только `country`, `currency` и `country_currency_desc` (`"Afghanistan-Afghani"`), а `currency` неоднозначен (`"Dinar"`, `"Dollar"`), поэтому таблица «описание → ISO» строится по `country_currency_desc`.
- **Не ясно по Treasury:** `total-count = 103` при запросе без фильтра. Либо в датасете всего 103 записи (одна дата), либо счётчик считается иначе. Вторая половина команды 2 (выгрузка по дате) не запускалась, поэтому полный список описаний и наличие пустых/`null` курсов не проверены.
- **H.10: прямой выгрузки на странице релиза нет.** Есть только ссылки `/datadownload` и `https://www.federalreserve.gov/datadownload/Choose.aspx?rel=H10`. Команда 4 не проверила DDP: в неё подставилась буква-заглушка `<URL_DDP_СО_СТРАНИЦЫ>` (`Bad hostname`), так что состояние DDP остаётся неизвестным.
- **FRED без ключа работает** (`200 application/csv`), формат `дата,значение`. Данные H.10 свежие: последняя точка `2026-10-02` при проверке во вторник 06.10. `DEXUSEU` = USD за 1 EUR (1.1259), то есть для EUR, GBP, AUD, NZD направление обратное, как и ожидалось.
- **TLS:** цепочка Treasury заканчивается корнем `Sectigo Public Server Authentication Root R46`, это RSA-вариант корня Sectigo, который уже ломал ЕЦБ на эмуляторе (там E46). Риск высокий, проверять на эмуляторе API 34 в первую очередь. Цепочка ФРС (Google Trust Services → GlobalSign Root CA) — риск низкий. Хост FRED проверить так же.

## Следующая проверка
```bash
D=/tmp/ratex-usa
# 7. Treasury: полный набор последней даты (для таблицы описание -> ISO, поиска null)
curl -gsS -m 30 -o $D/treasury-last.json \
  "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange?filter=record_date:eq:2026-09-30&page[size]=500&fields=country_currency_desc,exchange_rate,effective_date"
head -c 300 $D/treasury-last.json; grep -o '"total-count":[0-9]*' $D/treasury-last.json
grep -o '"exchange_rate":"[^"]*"' $D/treasury-last.json | sort | uniq -c | sort -rn | head -5

result:
{"data":[{"country_currency_desc":"Afghanistan-Afghani","exchange_rate":"65.15","effective_date":"2026-09-30"},{"country_currency_desc":"Albania-Lek","exchange_rate":"80.75","effective_date":"2026-09-30"},{"country_currency_desc":"Algeria-Dinar","exchange_rate":"133.126","effective_date":"2026-09-30"total-count":165
  10 "exchange_rate":"1.0"
   8 "exchange_rate":"573.75"
   6 "exchange_rate":"577.72"
   4 "exchange_rate":"16.393"
   3 "exchange_rate":"2.7"

# 8. FRED: несколько серий одним запросом (идентификаторы серий H.10 даны по памяти, не проверены)
curl -sS -m 30 -o $D/fred-multi.csv -w "%{http_code} %{content_type}\n" \
  "https://fred.stlouisfed.org/graph/fredgraph.csv?id=DEXUSEU,DEXUSUK,DEXJPUS,DEXCHUS,DEXCAUS,DEXSZUS"
head -n 2 $D/fred-multi.csv; tail -n 3 $D/fred-multi.csv

result:
200 application/csv
observation_date,DEXUSEU,DEXUSUK,DEXJPUS,DEXCHUS,DEXCAUS,DEXSZUS
1971-01-04,,2.3938,357.73,,1.0109,4.3180
2026-09-30,1.1345,1.3267,157.25,6.7045,1.4210,0.8348
2026-10-01,1.1232,1.3196,157.63,6.7038,1.4253,0.8314
2026-10-02,1.1259,1.3234,157.81,6.7038,1.4253,0.8293

# 9. DDP: что внутри страницы выбора пакета H.10 (ищем series=... и Output.aspx)
curl -sSL -m 30 -o $D/ddp-choose.html "https://www.federalreserve.gov/datadownload/Choose.aspx?rel=H10"
wc -c $D/ddp-choose.html
grep -oiE '(Output\.aspx|Build\.aspx|series=)[^"'"'"' <]*' $D/ddp-choose.html | sort -u | head -20

result:
   83548 /tmp/ratex-usa/ddp-choose.html
Output.aspx?rel=H10&amp;filetype=zip
series=122e3bcb627e8e53f1bf72a1a09cfb81&amp;lastobs=10&amp;from=&amp;to=&amp;filetype=csv&amp;label=include&amp;layout=seriescolumn&amp;type=package
series=60f32914ab61dfab590e0e470153e3ae&amp;lastobs=10&amp;from=&amp;to=&amp;filetype=csv&amp;label=include&amp;layout=seriescolumn&amp;type=package
series=847be2166a425bda9b4d92465f797544&amp;lastobs=120&amp;from=&amp;to=&amp;filetype=csv&amp;label=include&amp;layout=seriescolumn&amp;type=package
series=c5d6e0edf324b2fb28d73bcacafaaa02&amp;lastobs=120&amp;from=&amp;to=&amp;filetype=csv&amp;label=include&amp;layout=seriescolumn&amp;type=package

# 10. TLS FRED
openssl s_client -connect fred.stlouisfed.org:443 -servername fred.stlouisfed.org -showcerts </dev/null 2>/dev/null | grep -E "s:|i:"

result:
 0 s:C=US, ST=Missouri, L=St. Louis, O=Federal Reserve Bank of St Louis, CN=research.stlouisfed.org
   i:C=US, O=DigiCert Inc, CN=DigiCert Global G3 TLS ECC SHA384 2020 CA1
 1 s:C=US, O=DigiCert Inc, CN=DigiCert Global G3 TLS ECC SHA384 2020 CA1
   i:C=US, O=DigiCert Inc, OU=www.digicert.com, CN=DigiCert Global Root G3
```

## Выводы по командам 7–10 (2026-10-06)
**H.10: берём DDP CSV, FRED не нужен.** Ссылки пакетов со страницы `Choose.aspx?rel=H10` я проверил отдельно (через sandbox, ключ не нужен):
- `https://www.federalreserve.gov/datadownload/Output.aspx?rel=H10&series=60f32914ab61dfab590e0e470153e3ae&lastobs=10&from=&to=&filetype=csv&label=include&layout=seriescolumn&type=package` → `200 text/csv`, 3.7 КБ, 23 валюты, 10 последних дат (2026-09-21 … 2026-10-02). Второй пакет с `lastobs=10` (`122e3bcb…`) — индексы доллара, не наш.
- Формат: заголовочные строки в кавычках (`"Series Description"`, `"Unit:"`, `"Multiplier:"`, `"Currency:"`, `"Unique Identifier:"`, `"Time Period"`), затем строки `дата,значения…`. **ISO-коды лежат в строке `"Currency:"`**, поэтому таблица «название → ISO» для ФРС не нужна.
- **Направление котировки определяет идентификатор серии:** `RXI$US_N.B.*` (AUD, EUR, NZD, GBP) — USD за единицу, `RXI_N.B.*` (остальные) — единиц за USD.
- **Ловушка:** венесуэльский боливар в строке `"Currency:"` помечен `VEB`, хотя идентификатор серии `…VES`, а актуальный код `VES`. Нужен явный маппинг `VEB → VES`.
- Не проверено: как записан пропуск (`ND`) — в окне `lastobs=10` его нет. Ожидается `ND`, парсер должен пропускать любое нечисловое значение и брать для каждой валюты последнюю числовую.
- FRED отброшен: это обёртка над H.10, а в задаче источник должен быть первичным. Он остаётся запасным вариантом (`fredgraph.csv?id=…&cosd=…`, ключ не нужен, TLS DigiCert без проблем), но не реализуется (YAGNI). Риск выбора DDP: релиз «выводится из эксплуатации» и хэш пакета в URL может смениться. Тогда `FedRateSource` перестанет работать, остальные источники продолжат (дедлайн 5 с).

**Treasury: детали для парсера.**
- На `2026-09-30` 165 записей, одна страница, курсов `null`/пустых нет. В первой выгрузке (400 записей) три `record_date`: `2026-09-30` (165), `2026-06-30` (174), `2026-03-31` (61); 61 — это обрезка выгрузки на 400 записях (165 + 174 + 61), а не неполный квартал. (Исправлено при реализации: изначально здесь был неверный вывод «берём последнюю запись по каждому описанию».) В прошлом квартале есть закрытые валюты (`Bulgaria-Lev New`, `Cyprus-Euro`), поэтому парсер берёт **только записи самой поздней даты**.
- Часть записей к USD не относится или дублируется:
  - `1.0` у 10 описаний: `Bahamas-Dollar`, `Bermuda-Dollar`, `Ecuador-Dolares`, `El Salvador-Dollar`, `Panama-Dolares`, `Palau-Dollar`, `Marshall Islands-U.S. Dollar`, `Micronesia-U.S. Dollar`, `Cuba-Chavito`, `Timor-Leste-Dili`. Это привязка к доллару, USD в таблицу не нужен, записи пропускаем (для Bahamas/Bermuda и подобных ISO-код был бы BSD/BMD, но курс «1.0» к USD выдумывать не будем, пропускаем и их).
  - Одна валюта у нескольких стран: CFA-франк `573.75` (8 стран, XOF) и `577.72` (6 стран, XAF); `16.393` у Eswatini, Lesotho, Namibia и South Africa (SZL, LSL, NAD, ZAR); `2.7` у трёх стран XCD. Парсер должен схлопывать дубли по ISO.
  - Редкие описания: `Zimbabwe-Gold` (ZWG), `Euro Zone-Euro`, `Switzerland-Franc`, `United Kingdom-Pound`. Точность низкая, 3 знака (`0.835` CHF).
- Таблица «`country_currency_desc` → ISO» нужна для ~165 описаний, `currency` (`Dollar`, `Dinar`, `Franc`, `Pound`) для ключа не подходит. Неизвестные описания пропускаем.
- Курс Treasury — «валюты за USD» для всех записей.

**TLS (проверять на эмуляторе API 34):**
- `api.fiscaldata.treasury.gov`: лист → `Entrust OV TLS Issuing RSA CA 2` → корень `Sectigo Public Server Authentication Root R46`. Скорее всего повторится история с ЕЦБ (корня Sectigo нет в системном хранилище образа). Решение как для ЕЦБ: добавить промежуточный `Entrust OV TLS Issuing RSA CA 2` в `network_security_config` (по образцу `res/raw/sectigo_ov_e36.pem`), см. команду 11.
- `www.federalreserve.gov`: Google Trust Services (WR1) → GTS Root R1 → GlobalSign Root CA. Риск низкий.
- `fred.stlouisfed.org`: DigiCert Global G3, риск низкий (источник не используем).

## Последние проверки (до реализации)
```bash
D=/tmp/ratex-usa
# 11. PEM промежуточного сертификата Treasury (для network_security_config)
openssl s_client -connect api.fiscaldata.treasury.gov:443 -servername api.fiscaldata.treasury.gov -showcerts </dev/null 2>/dev/null \
  | awk '/BEGIN CERT/{n++} n==2{print} /END CERT/{if(n==2) exit}' > $D/entrust_ov_rsa_ca2.pem
openssl x509 -in $D/entrust_ov_rsa_ca2.pem -noout -subject -issuer -dates -fingerprint -sha256

result:
openssl x509 -in $D/entrust_ov_rsa_ca2.pem -noout -subject -issuer -dates -fingerprint -sha256
subject=C=CA, O=Entrust Limited, CN=Entrust OV TLS Issuing RSA CA 2
issuer=C=GB, O=Sectigo Limited, CN=Sectigo Public Server Authentication Root R46
notBefore=Dec 11 00:00:00 2024 GMT
notAfter=Dec 10 23:59:59 2027 GMT
sha256 Fingerprint=1F:92:7F:37:47:03:06:AF:C3:01:8A:B0:49:E6:BE:1D:3C:0A:3A:45:CA:20:7F:64:F5:32:51:2B:A9:69:EB:94

# 12. Как в DDP записан пропуск данных (ND): длинная история
curl -sSL -m 60 -o $D/ddp-long.csv "https://www.federalreserve.gov/datadownload/Output.aspx?rel=H10&series=60f32914ab61dfab590e0e470153e3ae&lastobs=500&from=&to=&filetype=csv&label=include&layout=seriescolumn&type=package"
wc -l $D/ddp-long.csv; grep -c ',ND' $D/ddp-long.csv; grep -m3 ',ND' $D/ddp-long.csv | cut -c1-200

result:
     505 /tmp/ratex-usa/ddp-long.csv
21
2024-11-11,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND
2024-11-28,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND
2024-12-25,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND,ND
```
Что должно получиться в п. 11: `subject = … Entrust OV TLS Issuing RSA CA 2`, `issuer = … Sectigo Public Server Authentication Root R46`, срок действия не истёк.

## Критерии решения по итогам проверки
- Если у H.10 нет выгрузки без ключа: выбираем FRED без ключа (много запросов) или отказываемся от H.10 и фиксируем это в артефакте.
- Если Treasury отдаёт `null` или пустые курсы в последней записи: берём последнюю запись, где курс есть.
- Из ответов сохранить фикстуры в `app/src/test/resources/` (урезанные, ~10 валют, плюс случаи `ND`/`null`).

# Требования
- Выполнять после задачи 03 (нужна схема из нескольких источников)
- Изменения делать в отдельной ветке
- В конце записать результат в `docs-ai/artifact`
