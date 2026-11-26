stages:
    - build

default:
    image: node:[=languageVersion]

build:
    stage: build
    script:
        - |
            if [ -f package-lock.json ] || [ -f npm-shrinkwrap.json ]; then
                npm ci
            else
                npm install
            fi
        - npm run build --if-present
<#if hasTests>
        - npm run test --if-present
</#if>