# PetClinic REST API tests

Отдельный тестовый проект для REST API [Spring PetClinic REST](https://github.com/spring-petclinic/spring-petclinic-rest)

## Требования

- JDK 17 (проверено на 17.0.20)
- Docker с docker-compose — в нём поднимается тестируемое приложение
(проверено на Docker Desktop 4.89 и Colima)
- Maven 3.9.16 — только для запуска из терминала, в IDEA есть свой

## Шаг 1. Поднять тестируемое приложение

```bash
docker compose up -d --wait
```

Флаг `--wait` держит команду до тех пор, пока приложение не станет healthy: в
`docker-compose.yml` для этого описана проверка `actuator/health`. Первый запуск дольше, потому
что качается образ `springcommunity/spring-petclinic-rest:4.0.2` (~500 МБ), дальше около
10 секунд. Образ собран только под amd64, на Apple Silicon он идёт через эмуляцию — работает без
вмешательства.

Версия образа зафиксирована тегом, а не взята как `latest`: расхождения из [BUGS.md](BUGS.md)
и ожидания в тестах относятся к конкретной реализации, поэтому прогон должен быть воспроизводимым.
На момент написания `latest` указывает на тот же образ, что и `4.0.2`.

Проверка: [http://localhost:9966/petclinic/actuator/health](http://localhost:9966/petclinic/actuator/health)
Swagger: [http://localhost:9966/petclinic/swagger-ui.html](http://localhost:9966/petclinic/swagger-ui.html)

Если порт 9966 занят прошлым запуском:

```bash
docker ps -q --filter publish=9966 | xargs docker stop
```

Остановить приложение после работы: `docker compose down`.

## Шаг 2. Запустить тесты в IntelliJ IDEA

1. Проверить, что SDK проекта — Java 17 (File → Project Structure → Project).
2. Выбрать конфигурацию **All API tests** и нажать Run.

Конфигурации запуска лежат в репозитории (папка `.run`), IDEA подхватывает их сама. Весь набор
проходит меньше десяти секунд.

## Запуск тестов из терминала

```bash
mvn clean test
```

Прогон заканчивается `BUILD FAILURE`, и это ожидаемо: часть тестов написана по спецификации
и падает на расхождениях с реализацией. Список падений и их причины — в [BUGS.md](BUGS.md).

Адрес приложения по умолчанию — `http://localhost:9966/petclinic`, он задан в
`src/main/resources/application.yml`. Если приложение поднято на другом адресе, его передают
параметром:

```bash
mvn clean test -DbaseUrl=http://localhost:8080/petclinic
```

Каждый HTTP-запрос пишется в консоль (request + response) и вкладывается в Allure.

## Параллельный запуск

Тест-классы выполняются параллельно в четыре потока, настройка — в
`src/test/resources/junit-platform.properties`.

Выигрыш на локальном приложении небольшой. Медиана трёх прогонов: 3,6 секунды против 4,4
последовательных на весь `mvn clean test`, из которых около 1,5 секунды — это старт Maven
и компиляция, то есть на сами 89 тестов уходит примерно 2 секунды против 2,9. Причина простая:
запросы идут на localhost и стоят единицы миллисекунд, приложение поднимается до тестов,
а Spring-контекст создаётся один раз на весь прогон. Режим оставлен как задел — на медленном
окружении или на выросшем наборе он даёт заметно больше.

Методы внутри одного класса остаются последовательными. Причина в уборке: `OwnerTestBase`
держит очередь созданных owner'ов на экземпляр класса, а экземпляр при `@TestInstance(PER_CLASS)`
один на все методы — параллельные методы удаляли бы данные друг друга.

**Отключить** параллельность для одного прогона:

```bash
mvn clean test -Djunit.jupiter.execution.parallel.enabled=false
```



## Стек

- Java 17
- Maven 3.9.16
- Spring Boot 3.4.5
- JUnit 5
- RestAssured 5.5.1
- AssertJ
- Allure 2.29.1 (отчёт собирает CLI 2.30.0)
- docker-compose



## Allure

Allure — это SPA: отчёт нужно открывать **через HTTP**, не двойным кликом по `index.html`.
Иначе в деталях теста будет 404 (`Test result with uid ... not found`).

Каждый `mvn test` затирает старые results, чтобы в отчёт не попадали прошлые прогоны
(переименованные классы, упавший health, когда приложение ещё не было запущено).

```bash
mvn clean test
mvn allure:serve
```

`allure:serve` поднимает локальный сервер и сам открывает браузер. В IDEA то же самое делает
конфигурация **Allure report**.

Статический отчёт (тоже потом через локальный сервер, не как файл):

```bash
mvn allure:report
```

Папка: `target/allure-report`.

## Структура тестов

Жизненный цикл owner целиком проверяет `OwnerCrudFlowTest`: один владелец проходит через
создание, чтение, обновление и удаление, и каждый шаг подтверждается эффектом — владелец
доступен по `id`, после обновления отдаёт новые данные, после удаления отвечает 404. Статусы
там тоже сверяются со спецификацией, поэтому сейчас тест останавливается на 204 у `PUT`
(BUG-1) и доходит до шагов удаления только после исправления этого дефекта.

Остальные тесты разложены по операциям, один класс — один сценарий. В Allure та же разбивка
видна через `@Story`: CRUD flow, Create, Read, Update, Delete.

```
src/test/java/com/petclinic/api/
├── support/
│   └── ApiTestBase.java          Spring-контекст, ожидание готовности приложения
├── health/
│   └── HealthCheckTest.java      200 и status = UP
└── owners/
    ├── OwnerTestBase.java        создание owner как тестовых данных, удаление после теста,
    │                             общие проверки Owner и ProblemDetail
    ├── OwnerCrudFlowTest.java    сквозной сценарий: create → read → update → read → delete → read
    ├── create/
    │   ├── CreateOwnerHappyPathTest.java          happy path: 201 и тело ответа
    │   ├── CreateOwnerValidationTest.java         тело без полей, нечисловой телефон, по кейсу
    │   │                                          на каждое пропущенное и на каждое пустое
    │   │                                          обязательное поле
    │   ├── CreateOwnerDuplicateTest.java          повторное создание того же owner
    │   ├── CreateOwnerFieldLengthTest.java        границы длины каждого поля: 1 символ,
    │   │                                          максимум, максимум + 1
    │   ├── CreateOwnerFieldFormatTest.java        значения, ломающие паттерн поля; точка
    │   │                                          в конце lastName разрешена
    │   ├── CreateOwnerFieldTypeTest.java          в строковом поле число, boolean, массив,
    │   │                                          объект
    │   ├── CreateOwnerMalformedBodyTest.java      пустое тело, обрезанный JSON, массив,
    │   │                                          строка, чужой Content-Type
    │   └── CreateOwnerTelephoneLengthTest.java    границы длины телефона (draft)
    ├── read/
    │   ├── GetOwnerHappyPathTest.java             happy path: 200 и тело ответа
    │   ├── GetOwnerNotFoundTest.java              неизвестный и удалённый owner → 404
    │   └── GetOwnerInvalidIdTest.java             матрица невалидных id → 400; путь без id
    │                                              не должен отвечать 5xx
    ├── update/
    │   ├── UpdateOwnerHappyPathTest.java          happy path: 200, тело ответа и повторный GET
    │   ├── UpdateOwnerValidationTest.java         по кейсу на каждое пропущенное и на каждое
    │   │                                          пустое поле, owner не меняется после
    │   │                                          отклонённого запроса
    │   ├── UpdateOwnerNotFoundTest.java           неизвестный owner → 404
    │   └── UpdateOwnerInvalidIdTest.java          матрица невалидных id → 400;
    │                                              без id → 404 или 405
    └── delete/
        ├── DeleteOwnerHappyPathTest.java          happy path: 200 и удалённый owner в теле
        ├── DeleteOwnerTwiceTest.java              повторное удаление → 404
        ├── DeleteOwnerNotFoundTest.java           неизвестный owner → 404
        └── DeleteOwnerInvalidIdTest.java          матрица невалидных id → 400;
                                                   без id → 404 или 405
```

Ожидания взяты из OpenAPI / Swagger, не из фактического ответа контроллера.

`GET`, `PUT` и `DELETE` принимают один и тот же `ownerId`, поэтому матрица невалидных значений
лежит в одном месте — `OwnerTestBase#invalidOwnerIds` — и подставляется во все три теста.

Тестовые данные генерируются случайно (Datafaker): имя, фамилия, адрес и город — словарными
генераторами, чтобы в запросах были реалистичные и легкочитаемые значения, `telephone` — по паттерну `^[0-9]*$`
из спецификации. Значения остаются в границах схемы: имена состоят из букв, соединённых
пробелом, апострофом или дефисом, адрес и город укладываются в `maxLength`.

## Найденные дефекты

Список с воспроизведением и разделением на дефекты реализации и дефекты контракта —
в [BUGS.md](BUGS.md). Часть тестов падает намеренно: ожидания соответствуют OpenAPI.
На текущем образе это 38 падений из 89 тестов, и каждое сводится к одному из одиннадцати
дефектов: 204 вместо 200 на `PUT` и `DELETE`, 500 вместо 400 на невалидные телефон, `ownerId`
и нечитаемое тело, 500 вместо 415 на чужой `Content-Type`, пустое тело у 404, принятые вместо
отклонённых значения не строкового типа. Двенадцатый дефект — минимальная длина `address`
в один символ — падением не проявляется: приложение следует схеме, вопросы к самой схеме.

Общие тестовые данные используют телефон из 10 цифр, хотя схема разрешает 1..20
(см. BUG-3): иначе подготовка данных падала бы в тестах, которые проверяют совсем другое.

## Draft-тесты

`CreateOwnerTelephoneLengthTest` помечен тегом `draft`: ожидания написаны под требование
«`telephone` — ровно 10 цифр» и ждут подтверждения смены требований, потому что текущие
границы схемы (1..20 цифр) допускают значения, не являющиеся телефоном. Метка видна в
`@DisplayName` (`[DRAFT]`) и в описании теста в Allure.

Запуск без них:

```bash
mvn clean test -DexcludedGroups=draft
```

