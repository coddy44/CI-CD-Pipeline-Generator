<#-- Шаблон: GitLab CI / Java / Maven.
     Переменные: languageVersion, branch, hasTests, hasDockerfile, frameworks (см. README.md рядом).
     Подстановка — [=имя]; переменные GitLab вида $CI_... остаются как есть. -->
stages:
  - build
  - test
<#if hasDockerfile>
  - docker
</#if>

default:
  image: maven:3.9-eclipse-temurin-[=languageVersion]

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=$CI_PROJECT_DIR/.m2/repository"

cache:
  key: "$CI_COMMIT_REF_SLUG"
  paths:
    - .m2/repository

workflow:
  rules:
    - if: $CI_COMMIT_BRANCH == "[=branch]"
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"

build:
  stage: build
  script:
    - mvn -B package -DskipTests
  artifacts:
    paths:
      - target/*.jar
<#if hasTests>

test:
  stage: test
  script:
    - mvn -B test
  artifacts:
    when: always
    reports:
      junit: target/surefire-reports/TEST-*.xml
</#if>
<#if hasDockerfile>

docker:
  stage: docker
  image: docker:27
  services:
    - docker:27-dind
  script:
    - docker build -t "$CI_PROJECT_NAME:$CI_COMMIT_SHORT_SHA" .
</#if>
