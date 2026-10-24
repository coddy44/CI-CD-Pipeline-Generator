# Контракт API (черновик, итерация 0)

Базовый адрес: `http://localhost:8080/api/v1`
Формат: JSON, кодировка UTF-8.
Интерактивное описание (Swagger UI): `http://localhost:8080/swagger-ui.html`
OpenAPI-спецификация: `http://localhost:8080/v3/api-docs`

---

## 1. Сгенерировать пайплайн

`POST /api/v1/pipelines/generate`

### Запрос

```json
{
  "repoUrl": "https://github.com/user/my-project",
  "branch": "main",
  "ciPlatform": "GITLAB_CI"
}
```

| Поле         | Тип    | Обязательно | Описание |
|--------------|--------|-------------|----------|
| `repoUrl`    | строка | да          | Ссылка на публичный репозиторий вида `https://host/owner/repo` |
| `branch`     | строка | нет         | Ветка; если не указана, берётся ветка по умолчанию |
| `ciPlatform` | строка | да          | `GITLAB_CI` или `JENKINS` |

### Ответ `200 OK`

```json
{
  "repoUrl": "https://github.com/user/my-project",
  "detectedStack": {
    "language": "JAVA",
    "languageVersion": "21",
    "buildTool": "MAVEN",
    "frameworks": ["spring-boot"],
    "hasTests": true,
    "hasDockerfile": false,
    "defaultBranch": "main"
  },
  "pipeline": {
    "platform": "GITLAB_CI",
    "fileName": ".gitlab-ci.yml",
    "content": "stages:\n  - build\n  - test\n ..."
  },
  "warnings": []
}
```

| Поле            | Описание |
|-----------------|----------|
| `detectedStack` | Что определил анализатор — фронтенд показывает это пользователю |
| `pipeline.fileName` | Куда положить файл в репозитории |
| `pipeline.content`  | Готовый текст пайплайна (фронтенд показывает и даёт скопировать/скачать) |
| `warnings`      | Предупреждения для пользователя, например «тесты не найдены» |

---

## 2. Поддерживаемые платформы и стеки

`GET /api/v1/supported`

### Ответ `200 OK`

```json
{
  "platforms": ["GITLAB_CI", "JENKINS"],
  "stacks": [
    { "language": "JAVA",       "buildTool": "MAVEN" },
    { "language": "TYPESCRIPT", "buildTool": "NPM" },
    { "language": "PYTHON",     "buildTool": "PIP" }
  ]
}
```

---

## 3. Проверка работоспособности

`GET /actuator/health` → `{"status":"UP"}`

---

## Ошибки

Все ошибки возвращаются в одном формате:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Некорректные данные запроса",
  "details": ["repoUrl: Ожидается ссылка вида https://github.com/owner/repo"],
  "timestamp": "2026-10-24T13:00:00Z"
}
```

| HTTP | `code`                      | Когда |
|------|-----------------------------|-------|
| 400  | `VALIDATION_ERROR`          | Пустая или некорректная ссылка, не выбрана CI-система |
| 400  | `MALFORMED_REQUEST`         | Тело не JSON или неизвестное значение `ciPlatform` |
| 422  | `REPOSITORY_NOT_ACCESSIBLE` | Репозиторий не найден, приватный или не клонируется |
| 422  | `UNSUPPORTED_STACK`         | Стек определён, но шаблона для него пока нет |
| 500  | `INTERNAL_ERROR`            | Непредвиденная ошибка сервера |

---

## Формат результата анализа (backend1 → backend2)

Внутренний формат, не виден фронтенду напрямую (но целиком возвращается в `detectedStack`).
Анализатор реализует интерфейс `RepositoryAnalyzer` и возвращает объект `ProjectStack`:

| Поле              | Тип          | Пример |
|-------------------|--------------|--------|
| `language`        | enum         | `JAVA`, `TYPESCRIPT`, `JAVASCRIPT`, `PYTHON` |
| `languageVersion` | строка       | `"21"`, `"20"`, `"3.12"` |
| `buildTool`       | enum         | `MAVEN`, `GRADLE`, `NPM`, `PIP` |
| `frameworks`      | список строк | `["spring-boot"]`, `["react"]`, `[]` |
| `hasTests`        | boolean      | `true` |
| `hasDockerfile`   | boolean      | `false` |
| `defaultBranch`   | строка       | `"main"` |

Если репозиторий недоступен, анализатор бросает `RepositoryAccessException`.

## Открытые вопросы на следующую итерацию

- Анализ может занимать несколько секунд (клонирование): достаточно ли синхронного запроса или нужен асинхронный режим с опросом статуса.
- Ограничение на размер репозитория и таймаут клонирования.
- Нужен ли отдельный эндпоинт «только анализ» без генерации.
