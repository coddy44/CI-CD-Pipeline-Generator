stages:
    - verify

default:
    image: python:[=languageVersion]

verify:
    stage: verify
    script:
        - |
            if [ -f requirements.txt ]; then
                python -m pip install -r requirements.txt
            else
                python -m pip install .
            fi
<#if hasTests>
        - python -m pip install pytest
        - python -m pytest
</#if>