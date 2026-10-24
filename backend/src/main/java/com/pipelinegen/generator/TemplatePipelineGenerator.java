package com.pipelinegen.generator;

import com.pipelinegen.exception.UnsupportedStackException;
import com.pipelinegen.model.CiPlatform;
import com.pipelinegen.model.PipelineFile;
import com.pipelinegen.model.ProjectStack;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
            template.process(buildModel(stack, branch), out);
            return new PipelineFile(platform, platform.outputFileName(), out.toString());
        } catch (TemplateNotFoundException e) {
            throw new UnsupportedStackException(
                    "Нет шаблона для %s / %s / %s".formatted(platform, stack.language(), stack.buildTool()));
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Ошибка обработки шаблона " + templatePath, e);
        }
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
        model.put("languageVersion", stack.languageVersion());
        model.put("frameworks", stack.frameworks());
        model.put("hasTests", stack.hasTests());
        model.put("hasDockerfile", stack.hasDockerfile());
        model.put("branch", branch);
        return model;
    }
}
