# Backend — генератор CI/CD-пайплайнов

Spring Boot 4 · Java 21 · Maven · FreeMarker

## Запуск

```bash
./mvnw spring-boot:run          # запуск на http://localhost:8080
./mvnw verify                   # сборка + тесты
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Структура

```
src/main/java/com/pipelinegen/
├── api/          # REST-контроллер, DTO запросов/ответов, обработка ошибок
├── service/      # сценарий: анализ → генерация
├── analyzer/     # интерфейс анализатора репозитория (+ временная заглушка)
├── generator/    # генерация пайплайна по шаблонам FreeMarker
├── model/        # стек проекта, CI-платформы, сгенерированный файл
├── exception/    # исключения предметной области
└── config/       # CORS для фронтенда
src/main/resources/pipeline-templates/   # шаблоны пайплайнов (см. README.md там)
docs/api-contract.md                     # контракт API
```

## Статус (итерация 0)

- Анализатор — заглушка: для любой ссылки возвращает Java/Maven.
- Шаблоны: GitLab CI и Jenkins для Java/Maven.
