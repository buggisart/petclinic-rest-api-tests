# Найденные дефекты

Ожидания взяты из OpenAPI-спецификации (Swagger UI: `http://localhost:9966/petclinic/swagger-ui.html`),
а не из фактического поведения контроллеров.

Проверено на образе `springcommunity/spring-petclinic-rest:4.0.2`
(`sha256:d092779e874fd680357763a6f219d97afb419dca13f6a750ccfb8ac63c610986`)

| # | Endpoint | Описание                                                 | Где баг?   |
|---|----------|----------------------------------------------------------|------------|
| 1 | `PUT /api/owners/{ownerId}` | статус код 204 без тела вместо 200 + `Owner` тело ответа | реализация |
| 2 | `DELETE /api/owners/{ownerId}` | статус код 204 без тела вместо 200 + `Owner` тело ответа | контракт   |
| 3 | `POST /api/owners` | `telephone` в схеме 1..20 цифр, принимается только 10    | контракт   |
| 4 | `POST /api/owners` | 500 на телефон недопустимой длины вместо 400             | реализация |
| 5 | все ручки с `ProblemDetail` | один пример на 400, 404 и 500: везде `status` 500        | контракт   |
| 6 | `GET /api/owners/{ownerId}` | 404 с пустым телом вместо `ProblemDetail` с причиной     | реализация |
| 7 | `GET`, `PUT`, `DELETE /api/owners/{ownerId}` | 500 на невалидный `ownerId` вместо 400                   | реализация |
| 8 | все ответы с `ProblemDetail` | в ответах есть поля, которых нет в схеме                 | контракт   |
| 9 | `POST /api/owners` | `true` и число в строковом поле создают owner вместо 400 | реализация |
| 10 | `POST /api/owners` | нечитаемое тело запроса даёт 500 вместо 400              | реализация |
| 11 | `POST /api/owners` | неподдерживаемый `Content-Type` даёт 500 вместо 415      | реализация |
| 12 | `POST /api/owners` | `address` из одного символа принимается как валидный     | контракт   |

## Как воспроизводить

Все шаги выполняются на запущенном приложении:

```bash
docker compose up -d --wait
```

Базовый адрес — `http://localhost:9966/petclinic`. Валидное тело owner, которое используется
в шагах ниже:

```json
{"firstName":"Ann","lastName":"Lee","address":"Main 1","city":"Moscow","telephone":"1234567890"}
```

---

## BUG-1. `PUT /api/owners/{ownerId}` — 204 без тела вместо 200 и `Owner` в ответе

**Тип:** дефект реализации.

**Шаги воспроизведения:**

1. Создать owner и запомнить `id` из ответа:

   ```bash
   curl -s -X POST http://localhost:9966/petclinic/api/owners \
     -H 'Content-Type: application/json' \
     -d '{"firstName":"Ann","lastName":"Lee","address":"Main 1","city":"Moscow","telephone":"1234567890"}'
   # {"firstName":"Ann",...,"id":78}
   ```

2. Обновить этого owner валидным телом:

   ```bash
   curl -i -X PUT http://localhost:9966/petclinic/api/owners/78 \
     -H 'Content-Type: application/json' \
     -d '{"firstName":"Ann","lastName":"Lee","address":"Bauman 10","city":"Kazan","telephone":"8435550199"}'
   ```

3. Посмотреть на статус ответа и на наличие тела.

**Ожидаемый результат:** 200 и обновлённый `Owner` в теле.

**Фактический результат:** 204, `Content-Length: 0`. Изменения при этом сохраняются — следующий
`GET` возвращает новые значения.

```
HTTP/1.1 204
Content-Length: 0
```

**Почему реализация, а не контракт:** возврат обновлённого ресурса на `PUT` — заявленное
поведение, и оно полезно клиенту: без него после каждого обновления нужен дополнительный `GET`,
чтобы узнать актуальное состояние (например, изменились ли поля на сервере).

**Тест:** `UpdateOwnerHappyPathTest.updateOwner` — падает с `expected: 200 but was: 204`.

---

## BUG-2. `DELETE /api/owners/{ownerId}` — 204 без тела вместо 200 и `Owner` в ответе

**Тип:** скорее всего дефект контракта.

**Шаги воспроизведения:**

1. Создать owner и запомнить `id` из ответа (см. шаг 1 в BUG-1).
2. Удалить его:

   ```bash
   curl -i -X DELETE http://localhost:9966/petclinic/api/owners/78
   ```

3. Посмотреть на статус ответа и на наличие тела.

**Ожидаемый результат:** 200 и удалённый `Owner` в теле.

**Фактический результат:** 204, `Content-Length: 0`. Owner при этом действительно удалён —
повторный `GET` отвечает 404.

```
HTTP/1.1 204
Content-Length: 0
```

**Почему контракт, а не реализация:** 204 на удаление — общепринятый ответ, возвращать тело
удалённого объекта клиенту незачем. Исправлять следует спецификацию.

**Тест:** `DeleteOwnerHappyPathTest.deleteOwner` — падает с `expected: 200 but was: 204`.

---

## BUG-3. `POST /api/owners` — границы длины `telephone` в схеме не совпадают с реализацией

**Тип:** скорее всего дефект контракта.

**Шаги воспроизведения:**

1. Создать owner с телефоном из 10 цифр — статус код 201:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:9966/petclinic/api/owners \
     -H 'Content-Type: application/json' \
     -d '{"firstName":"Ann","lastName":"Lee","address":"Main 1","city":"Moscow","telephone":"1234567890"}'
   # 201
   ```

2. Повторить с телефоном из 9 цифр (`123456789`) — 500.
3. Повторить с телефоном из 11 цифр (`12345678901`) — 500.
4. Сверить со схемой, где заявлены другие границы:

   ```bash
   curl -s http://localhost:9966/petclinic/v3/api-docs \
     | python3 -c "import json,sys; print(json.load(sys.stdin)['components']['schemas']['OwnerFields']['properties']['telephone'])"
   # {'type': 'string', ..., 'maxLength': 20, 'minLength': 1, 'pattern': '^[0-9]*$'}
   ```

**Ожидаемый результат:** `telephone` — строка `[1, 20] characters` с паттерном `^[0-9]*$`,
то есть валидны и одна цифра, и двадцать.

**Фактический результат:** принимаются только 10 цифр, всё остальное отклоняется.

**Почему контракт, а не реализация:** телефон фиксированной длины — осмысленное требование
предметной области, а `1..20` в схеме выглядит как заглушка. Ожидаемое исправление —
`minLength: 10`, `maxLength: 10` (либо паттерн `^[0-9]{10}$`).

**Тест:** `CreateOwnerTelephoneLengthTest` — draft-версия, тег `draft`. Ожидания в нём написаны
под предполагаемое требование «ровно 10 цифр» (10 → 201, 9 и 11 → 400), а не под текущую схему.
Финализируется после подтверждения смены требования.

---

## BUG-4. `POST /api/owners` — 500 на телефон недопустимой длины вместо 400

**Тип:** дефект реализации.

**Шаги воспроизведения:**

1. Отправить создание owner с телефоном из 3 цифр:

   ```bash
   curl -s -X POST http://localhost:9966/petclinic/api/owners \
     -H 'Content-Type: application/json' \
     -d '{"firstName":"Ann","lastName":"Lee","address":"Main 1","city":"Moscow","telephone":"123"}'
   ```

2. Посмотреть статус и тело ответа.
3. Для сравнения отправить телефон с буквами (`not-a-number`) — там ответ корректный, 400
   с описанием поля. То есть нарушение паттерна обрабатывается правильно, а нарушение длины — нет.

**Ожидаемый результат:** ошибка валидации входных данных — 400 и `ProblemDetail` с описанием поля.

**Фактический результат:** 500 с `title: ConstraintViolationException`, пустым
`schemaValidationErrors` и текстом без пользы для клиента:

```json
{
  "detail": "An unexpected error occurred while processing your request",
  "status": 500,
  "title": "ConstraintViolationException",
  "schemaValidationErrors": []
}
```

**Тест:** `CreateOwnerTelephoneLengthTest.rejectTelephoneAroundRequiredLength` (9 и 11 цифр) —
падает с `expected: 400 but was: 500`.

**Корректная обработка паттерна закреплена кейсом `non numeric telephone`
в `CreateOwnerValidationTest` — он проходит.**

---

## BUG-5. Все ручки с `ProblemDetail` — один пример ошибки на 400, 404 и 500, везде `status` 500

**Тип:** дефект контракта (документации).

**Шаги воспроизведения:**

1. Открыть Swagger UI: `http://localhost:9966/petclinic/swagger-ui.html`.
2. Раскрыть `POST /api/owners`, в разделе Responses открыть **400 Bad request** → Example Value.
3. Раскрыть `GET /api/owners/{ownerId}`, открыть **404 Owner not found.** → Example Value.
   Пример тот же самый, включая `status: 500`.
4. Убедиться, что источник примера — схема, а не ответы:

   ```bash
   curl -s http://localhost:9966/petclinic/v3/api-docs \
     | python3 -c "import json,sys; p=json.load(sys.stdin)['components']['schemas']['ProblemDetail']['properties']; print({k: v.get('example') for k, v in p.items()})"
   # {'type': 'http://localhost:9966/petclinic/api/owner', 'title': 'NoResourceFoundException',
   #  'status': 500, 'detail': 'No static resource api/owner.', ...}
   ```

**Где:** в схеме `ProblemDetail` примеры заданы на уровне полей (`example` у `type`, `title`,
`status`, `detail`), а не на уровне ответов. Swagger UI подставляет один и тот же Example Value
во все ответы, которые ссылаются на эту схему — в спецификации таких операций 36.
На тестируемых ручках:

| Операция | Ответы с этим примером |
|----------|------------------------|
| `POST /api/owners` | 400 `Bad request.`, 500 `Server error.` |
| `GET /api/owners/{ownerId}` | 400 `Bad request.`, 404 `Owner not found.`, 500 `Server error.` |
| `PUT /api/owners/{ownerId}` | 400 `Bad request.`, 404 `Owner not found.`, 500 `Server error.` |
| `DELETE /api/owners/{ownerId}` | 400 `Bad request.`, 404 `Owner  not found.`, 500 `Server error.` |

**Ожидаемый результат:** пример задан у каждого ответа и согласован с ним. Для 400 — `status: 400`
и ошибка валидации в `schemaValidationErrors`. Для 404 `Owner not found.` — `status: 404`,
читаемый `detail` вида `Owner 999999 not found.` и пустой `schemaValidationErrors`: к схеме
запрос претензий не имеет.

**Фактический результат в примере (одинаковый для 400, 404 и 500):**

```json
{
  "type": "http://localhost:9966/petclinic/api/owner",
  "title": "NoResourceFoundException",
  "status": 500,
  "detail": "No static resource api/owner.",
  "timestamp": "2024-11-23T13:59:21.382040700Z",
  "schemaValidationErrors": [
    {
      "message": "[Path '/lastName'] Instance type (null) does not match any allowed primitive type (allowed: ['string'])"
    }
  ]
}
```

**Что не так:** `status` равен 500 независимо от кода ответа, а `title` и `detail` описывают
совершенно другую ошибку — ненайденный статический ресурс (`api/owner` вместо `api/owners`),
при этом `schemaValidationErrors` в том же примере говорит про `/lastName`. Похоже, значения
скопировали из случайного прогона.

**Влияние:** пример вводит в заблуждение при генерации клиентов и написании тестов — по нему
можно заложить проверку `status = 500` для ответа 400 или 404. Отдельно он скрывает BUG-6:
из документации нельзя понять, каким должен быть текст ошибки «owner не найден».

**Заодно:** в описании 404 у `DELETE /api/owners/{ownerId}` двойной пробел —
`Owner  not found.` вместо `Owner not found.`.

**Тест:** не покрывается — дефект в тексте спецификации, а не в поведении API.
Реальный ответ 400 при этом консистентен (`status: 400`), что закреплено
в `CreateOwnerValidationTest`.

---

## BUG-6. `GET /api/owners/{ownerId}` — 404 приходит с пустым телом вместо `ProblemDetail`

**Тип:** дефект реализации.

**Шаги воспроизведения:**

1. Запросить owner с идентификатором, которого нет в базе:

   ```bash
   curl -i http://localhost:9966/petclinic/api/owners/999999
   ```

2. Посмотреть на статус и наличие тела.
3. Повторить на удалённом owner: создать его, удалить, снова запросить `GET` — результат тот же.
4. Проверить, что то же самое происходит на `PUT` и `DELETE` с неизвестным `ownerId`.

**Ожидаемый результат:** 404 и `ProblemDetail` в теле с читаемой причиной в `detail` —
например `Owner 999999 not found.`, как и заявлено описанием ответа `Owner not found.`.

**Фактический результат:** 404, `Content-Length: 0`.

```
HTTP/1.1 404
Content-Length: 0
```

**Влияние:** клиент не получает ни причины, ни идентификатора запроса — по логам на стороне
клиента невозможно отличить «нет такого owner» от «ручка отвечает 404 по другой причине».

**Тесты:** падают на пустом теле (`OpenAPI declares ProblemDetail in the body of this response`)
`GetOwnerNotFoundTest` (неизвестный и удалённый owner), `UpdateOwnerNotFoundTest`,
`DeleteOwnerNotFoundTest`, `DeleteOwnerTwiceTest`. Сам код 404 при этом корректный.

---

## BUG-7. `GET`, `PUT`, `DELETE /api/owners/{ownerId}` — 500 на невалидный `ownerId` вместо 400

**Тип:** дефект реализации.

**Шаги воспроизведения:**

1. Запросить owner, подставив в путь значение, которое не является неотрицательным целым:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' http://localhost:9966/petclinic/api/owners/abc   # 500
   curl -s -o /dev/null -w '%{http_code}\n' http://localhost:9966/petclinic/api/owners/1.5   # 500
   curl -s -o /dev/null -w '%{http_code}\n' http://localhost:9966/petclinic/api/owners/-1    # 500
   curl -s -o /dev/null -w '%{http_code}\n' http://localhost:9966/petclinic/api/owners/%20   # 500
   ```

2. Посмотреть `title` в теле каждого ответа — он у всех разный:

   | `ownerId` | `title` в ответе |
   |-----------|------------------|
   | `abc`, `1.5` | `MethodArgumentTypeMismatchException` |
   | `-1` | `ConstraintViolationException` |
   | `%20` (пробел) | `MissingPathVariableException` |

3. Повторить всю матрицу на `PUT` и `DELETE` — ответы те же самые, обработчик пути общий.
4. Отдельно проверить запрос без идентификатора — путь `/api/owners/` отвечает 500 с `title`
   `NoResourceFoundException` на всех трёх методах:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' http://localhost:9966/petclinic/api/owners/
   curl -s -o /dev/null -w '%{http_code}\n' -X DELETE http://localhost:9966/petclinic/api/owners/
   ```

5. Для контроля запросить корректные идентификаторы, которых нет в базе (`/api/owners/0`
   и `/api/owners/999999`) — они отвечают 404, как и должны.

**Ожидаемый результат:** 400 и `ProblemDetail`. В схеме `ownerId` — `int32` с `minimum: 0`,
и у всех трёх операций объявлен ответ `400 Bad request`, то есть значение, не являющееся
неотрицательным целым, — это ошибка запроса, а не сбой сервера. Для пути без идентификатора
ожидания другие: `PUT` и `DELETE` на коллекции не описаны, поэтому корректен 404 или 405,
а `GET` на коллекции описан, поэтому корректен и список владельцев, и 404 — но не 500.

**Фактический результат:** 500 с техническим `title` и `detail`
`An unexpected error occurred while processing your request`.

**Влияние:** 500 в мониторинге выглядит как авария сервиса, хотя виноват запрос клиента.
Клиент по такому ответу не понимает, что исправить, и обычно уходит в ретраи, которые
бессмысленны: ответ не изменится.

**Тесты:** `GetOwnerInvalidIdTest`, `UpdateOwnerInvalidIdTest` и `DeleteOwnerInvalidIdTest` —
у каждого одна и та же матрица из пяти кейсов (слово, пробел, отрицательное, дробное,
восклицательный знак) из общего источника `OwnerTestBase#invalidOwnerIds` плюс отдельный кейс
без идентификатора. Матрица падает с `expected: 400 but was: 500`, кейсы без идентификатора —
на 500 вместо 404 или 405. Итого 18 падений.

---

## BUG-8. Все ответы с `ProblemDetail` — в теле есть поля, которых нет в схеме

**Тип:** дефект контракта (документации).

**Шаги воспроизведения:**

1. Получить ошибку валидации, отправив создание owner без обязательного поля `city`:

   ```bash
   curl -s -X POST http://localhost:9966/petclinic/api/owners \
     -H 'Content-Type: application/json' \
     -d '{"firstName":"Ann","lastName":"Lee","address":"Main 1","telephone":"1234567890"}'
   ```

2. Посмотреть схему элемента списка ошибок:

   ```bash
   curl -s http://localhost:9966/petclinic/v3/api-docs \
     | python3 -c "import json,sys; print(json.dumps(json.load(sys.stdin)['components']['schemas']['ValidationMessage'], indent=2))"
   ```

   В `ProblemDetail` объявлены `type`, `title`, `status`, `detail`, `timestamp` и
   `schemaValidationErrors`, в `ValidationMessage` — `message` и `additionalProperties`.

**Ожидаемый результат:** тело ответа описано схемой полностью.

**Фактический результат:** в `ProblemDetail` приходит поле `instance` (путь запроса), которого
в схеме нет, а в элементах `schemaValidationErrors` — `field`, `defaultMessage` и `rejectedValue`,
хотя `ValidationMessage` документирует только `message` и `additionalProperties`:

```json
{
  "instance": "/petclinic/api/owners",
  "status": 400,
  "title": "MethodArgumentNotValidException",
  "schemaValidationErrors": [
    {
      "message": "Field 'city' must not be null (rejected value: null)",
      "field": "city",
      "defaultMessage": "must not be null",
      "rejectedValue": "null"
    }
  ]
}
```

**`additionalProperties` в схеме `ValidationMessage`:** разрешает произвольные поля, а обычное свойство с таким именем — оно лежит внутри `properties`
и имеет тип `object`. То есть контракт предлагает складывать дополнительные данные внутрь объекта
`additionalProperties`, но в реальных ответах это поле не встречается ни разу, а `field`,
`defaultMessage` и `rejectedValue` приходят на верхнем уровне.

**Оговорка про строгость:** ни `ProblemDetail`, ни `ValidationMessage` не объявляют
`additionalProperties: false`, а по умолчанию JSON Schema незадокументированные поля разрешает.
Формально валидатор такой ответ примет, поэтому это неполнота документации, а не нарушение схемы —
отсюда и низкий приоритет дефекта.

**Влияние:** самые полезные клиенту данные — имя поля и отклонённое значение — контрактом
не описаны, поэтому клиенты, сгенерированные из спецификации инструментами вроде OpenAPI Generator,
этих полей не получают. По этой же причине негативные тесты
разбирают текст `message` вместо чтения `field`.

**Тест:** не покрывается — расхождение в тексте спецификации. Тесты ассертят только
задокументированные поля.

---

## BUG-9. `POST /api/owners` — JSON-тип поля не проверяется, `true` и число создают owner

**Тип:** дефект реализации.

**Шаги воспроизведения:**

1. Отправить `true` в строковом поле `firstName`:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:9966/petclinic/api/owners \
     -H 'Content-Type: application/json' \
     -d '{"firstName":true,"lastName":"Lee","address":"Main 1","city":"Rome","telephone":"1234567890"}'
   ```

2. Повторить с числом в `telephone` и в `address`:

   ```bash
   curl -s -X POST http://localhost:9966/petclinic/api/owners \
     -H 'Content-Type: application/json' \
     -d '{"firstName":"Ann","lastName":"Lee","address":42,"city":"Rome","telephone":1234567890}'
   ```

**Ожидаемый результат:** 400 и `ProblemDetail`. Все поля схемы `OwnerFields` объявлены как
`type: string`, а у операции описан ответ `400 Bad request`.

**Фактический результат:** 201 Created. Значение приводится к строке и сохраняется: в базе
появляется owner с `"firstName": "true"` или с адресом `"42"`.

**Влияние:** контракт де-факто не соблюдается — клиент на строго типизированном языке ожидает,
что сервер отвергнет такой запрос, а вместо этого получает созданную запись с мусором в данных.
Ошибку никто не заметит: ответ 201 выглядит успешным.

**Тесты:** `CreateOwnerFieldTypeTest` — кейсы `boolean in firstName (BUG-9)`,
`number in address (BUG-9)`, `number in telephone (BUG-9)`. Падают с `expected: 400 but was: 201`.

Замечание для разработчика: приведение делает Jackson, у него по умолчанию разрешено
преобразование скаляров в строку. Число в `firstName` и `lastName` до сохранения не доходит
только потому, что случайно не проходит проверку по паттерну.

---

## BUG-10. `POST /api/owners` — нечитаемое тело запроса даёт 500 вместо 400

**Тип:** дефект реализации.

**Шаги воспроизведения:**

1. Отправить массив в поле `firstName`:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:9966/petclinic/api/owners \
     -H 'Content-Type: application/json' \
     -d '{"firstName":["Ann"],"lastName":"Lee","address":"Main 1","city":"Rome","telephone":"1234567890"}'
   ```

2. Повторить с объектом в поле `city` — результат тот же.
3. Отправить тело, которое вообще не разбирается как объект `OwnerFields` — пустое, обрезанный
   JSON, массив, строку. Все четыре тоже отвечают 500:

   ```bash
   for body in '' '{' '[]' '"owner"'; do
     curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:9966/petclinic/api/owners \
       -H 'Content-Type: application/json' -d "$body"
   done
   ```

**Ожидаемый результат:** 400 и `ProblemDetail`: тело запроса не соответствует схеме, у операции
объявлен ответ `400 Bad request`. Там, где виновато конкретное поле, ответ должен его называть.

**Фактический результат:** 500 с `title` `HttpMessageNotReadableException` и техническим
`detail` `An unexpected error occurred while processing your request`.

**Влияние:** то же, что у BUG-7: ошибка клиента выглядит как авария сервиса, попадает в
мониторинг как 5xx и провоцирует бессмысленные ретраи.

**Тесты:** `CreateOwnerFieldTypeTest` — кейсы `array in firstName (BUG-10)` и
`object in city (BUG-10)`; `CreateOwnerMalformedBodyTest.createOwnerWithUnreadableBody` — четыре
кейса нечитаемого тела. Итого 6 падений с `expected: 400 but was: 500`.

---

## BUG-11. `POST /api/owners` — неподдерживаемый `Content-Type` даёт 500 вместо 415

**Тип:** дефект реализации.

**Шаги воспроизведения:**

1. Отправить корректный payload, объявив тип `text/plain`:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:9966/petclinic/api/owners \
     -H 'Content-Type: text/plain' \
     -d '{"firstName":"Ann","lastName":"Lee","address":"Main 1","city":"Rome","telephone":"1234567890"}'
   ```

2. Повторить запрос вообще без заголовка `Content-Type` — результат тот же.

**Ожидаемый результат:** 415 Unsupported Media Type. Операция принимает только
`application/json`, и это стандартный ответ на неподдерживаемый тип содержимого.

**Фактический результат:** 500 с `title` `HttpMediaTypeNotSupportedException`, то есть нужный
код ответа сервер вычислил, но отдал его как внутреннюю ошибку.

**Влияние:** клиент не понимает, что ему нужно поправить всего один заголовок, и получает
сигнал об аварии сервиса вместо инструкции.

**Тесты:** `CreateOwnerMalformedBodyTest.createOwnerWithUnsupportedMediaType`.
Падает с `expected: 415 but was: 500`.

---

## BUG-12. `POST /api/owners` — нижняя граница `address` в 1 символ выглядит нелогично

**Тип:** скорее всего дефект контракта.

**Шаги воспроизведения:**

1. Создать owner с адресом из одного символа — статус код 201:

   ```bash
   curl -s -X POST http://localhost:9966/petclinic/api/owners \
     -H 'Content-Type: application/json' \
     -d '{"firstName":"Ann","lastName":"Lee","address":"a","city":"Moscow","telephone":"1234567890"}'
   ```

2. Запросить созданного owner по `id` из ответа — адрес `a` сохранён и отдаётся клиенту как есть.
3. Сверить со схемой, где такая длина разрешена:

   ```bash
   curl -s http://localhost:9966/petclinic/v3/api-docs \
     | python3 -c "import json,sys; print(json.load(sys.stdin)['components']['schemas']['OwnerFields']['properties']['address'])"
   ```

**Ожидаемый результат:** адрес из одного символа отклоняется с 400. Адрес — это данные, по которым
клиника ездит к животному, и одна буква таким данным быть не может.

**Фактический результат:** 201, в базе появляется owner с адресом `a`.

**Почему контракт, а не реализация:** приложение ведёт себя ровно так, как написано в схеме
(`minLength: 1`), то есть претензия к самой схеме. Единица здесь выглядит не продуманным
требованием, а значением по умолчанию, поставленным только чтобы поле было обязательным, — та же
природа, что у границ `telephone` в BUG-3. Ожидаемое исправление — осмысленный минимум
(например `minLength: 5`) либо паттерн, требующий улицу и номер дома. Тот же вопрос стоит задать
про `city`: одна буква не является названием города.

**Тест:** `CreateOwnerFieldLengthTest.createOwnerWithShortestValue` — проходит, потому что
написан по текущей схеме и фиксирует её как есть. Этот дефект найден чтением спецификации,
а не падением теста; после уточнения требования ожидание в кейсе `address` меняется на 400.

