package com.pipelinegen.generator;

import com.pipelinegen.exception.UnsupportedStackException;
import com.pipelinegen.model.*;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Генерирует файл пайплайна по результату анализа и FreeMarker-шаблону.
 *
 * Шаблон ищется по пути: pipeline-templates/{папка платформы}/{язык}-{сборщик}.{расширение}.ftl
 * Например: pipeline-templates/gitlab-ci/java-maven.yml.ftl
 */
@Component
public class TemplatePipelineGenerator {

    private static final String TEMPLATES_ROOT = "/pipeline-templates";

    private final Configuration freemarker;

    public TemplatePipelineGenerator() {
        freemarker = new Configuration(Configuration.VERSION_2_3_33);
        freemarker.setClassForTemplateLoading(getClass(), TEMPLATES_ROOT);
        freemarker.setDefaultEncoding("UTF-8");
        // Подстановка через [=var] вместо ${var}: в GitLab CI и Jenkins
        // свой синтаксис $CI_... / ${env.X}, и он должен попадать в файл как есть.
        freemarker.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        freemarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarker.setLogTemplateExceptions(false);
    }

    public PipelineFile generate(ProjectStack stack, CiPlatform platform, String branch) {
        String templatePath = templatePath(stack, platform);
        try {
            Template template = freemarker.getTemplate(templatePath);
            StringWriter out = new StringWriter();
            PipelineStructure structure = buildStructure(stack, platform);
            Map<String, Object> model = buildModel(stack, branch);
            if (structure != null) {
                model.put("stageName", structure.stages().getFirst());
                model.put("scriptCommands", structure.nodes().stream()
                        .map(step -> step.details().command().lines().toList())
                        .toList());
            }
            template.process(model, out);
            return new PipelineFile(platform, platform.outputFileName(), out.toString(), structure);
        } catch (TemplateNotFoundException e) {
            throw new UnsupportedStackException(
                    "Нет шаблона для %s / %s / %s".formatted(platform, stack.language(), stack.buildTool()));
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Ошибка обработки шаблона " + templatePath, e);
        }
    }

    private static PipelineStructure buildStructure(ProjectStack stack, CiPlatform platform) {
        if (platform != CiPlatform.GITLAB_CI || stack.language() != Language.TYPESCRIPT
                || stack.buildTool() != BuildTool.NPM) {
            return null;
        }

        String stage = "build";
        List<PipelineStructure.Step> nodes = new ArrayList<>();
        nodes.add(new PipelineStructure.Step(
                "install", "Install dependencies", stage, "install", List.of(),
                new PipelineStructure.Details("""
                        if [ -f package-lock.json ] || [ -f npm-shrinkwrap.json ]; then
                            npm ci
                        else
                            npm install
                        fi
                        """.stripTrailing())));
        nodes.add(new PipelineStructure.Step(
                "build", "Build project", stage, "build", List.of("install"),
                new PipelineStructure.Details("npm run build --if-present")));

        if (stack.hasTests()) {
            nodes.add(new PipelineStructure.Step(
                    "test", "Run tests", stage, "test", List.of("build"),
                    new PipelineStructure.Details("npm run test --if-present")));
        }

        return new PipelineStructure(List.of(stage), nodes);
    }

    /** gitlab-ci/java-maven.yml.ftl */
    static String templatePath(ProjectStack stack, CiPlatform platform) {
        return "%s/%s-%s.%s.ftl".formatted(
                platform.templateDir(),
                stack.language().name().toLowerCase(Locale.ROOT),
                stack.buildTool().name().toLowerCase(Locale.ROOT),
                platform.fileExtension());
    }

    /**
     * Переменные, доступные в шаблоне. Это «контракт» шаблонов:
     * любой шаблон может рассчитывать только на эти имена.
     */
    private static Map<String, Object> buildModel(ProjectStack stack, String branch) {
        Map<String, Object> model = new HashMap<>();
        model.put("languageVersion", resolveLanguageVersion(stack));
        model.put("frameworks", stack.frameworks());
        model.put("hasTests", stack.hasTests());
        model.put("hasDockerfile", stack.hasDockerfile());
        model.put("branch", branch);
        return model;
    }

    public static String resolveLanguageVersion(ProjectStack stack) {
        if (stack.languageVersion() != null && !stack.languageVersion().isBlank()) {
            return stack.languageVersion();
        }
        return switch (stack.language()) {
            case JAVA -> "21";
            case JAVASCRIPT, TYPESCRIPT -> "22";
            case PYTHON -> "3.12";
        };
    }
}
