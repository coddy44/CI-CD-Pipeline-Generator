stages:
    - [=stageName]

default:
    image: node:[=languageVersion]

build:
    stage: [=stageName]
    script:
<#list scriptCommands as command>
        - |-
<#list command as line>
            [=line]
</#list>
</#list>