package com.pipelinegen.analyzer;

import com.pipelinegen.exception.UnsupportedStackException;
import com.pipelinegen.model.BuildTool;
import com.pipelinegen.model.Language;
import org.springframework.stereotype.Component;
import org.tomlj.Toml;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Статический анализ одного проекта в корне репозитория, без запуска его кода. */
@Component
public class StackDetector {

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git", "node_modules", ".venv", "venv", "target", "build", "dist", "vendor", "__pycache__");
    private static final int MAX_ENTRIES = 20_000;
    private static final int MAX_MANIFEST_BYTES = 1_048_576;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * @throws UnsupportedStackException стек не поддержан, не подтверждён исходниками или неоднозначен
     * @throws IOException конфигурация повреждена, файлы недоступны или превышен лимит чтения
     */
    public DetectedStack detect(Path directory) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Ожидается папка репозитория, не символическая ссылка");
        }
        Set<String> files = collectFiles(directory);
        var candidates = new ArrayList<DetectedStack>();

        if (files.contains("pom.xml")) {
            validatePom(readManifest(directory, "pom.xml"));
            if (files.stream().anyMatch(path -> path.endsWith(".java")
                    && (path.startsWith("src/main/java/") || path.startsWith("src/test/java/")))) {
                candidates.add(new DetectedStack(Language.JAVA, BuildTool.MAVEN));
            }
        }
        if (files.contains("package.json")) {
            candidates.add(detectNpm(directory, files));
        }
        if (files.contains("requirements.txt") || files.contains("pyproject.toml")) {
            boolean pythonProject = files.contains("requirements.txt");
            if (files.contains("pyproject.toml")) {
                var configuration = Toml.parse(readManifest(directory, "pyproject.toml"));
                if (configuration.hasErrors()) {
                    throw new IOException("Некорректный pyproject.toml: " + configuration.errors().getFirst());
                }
                pythonProject |= configuration.get("project") instanceof org.tomlj.TomlTable;
                if (configuration.contains("tool.poetry")) {
                    throw new UnsupportedStackException("Poetry пока не поддерживается");
                }
            }
            rejectFiles(files, "poetry.lock", "uv.lock", "Pipfile", "Pipfile.lock");
            if (pythonProject && files.stream().anyMatch(path -> path.endsWith(".py"))) {
                candidates.add(new DetectedStack(Language.PYTHON, BuildTool.PIP));
            }
        }
        if (candidates.size() > 1) {
            throw new UnsupportedStackException("В корне найдены несколько стеков; выберите репозиторий с одним проектом");
        }
        if (candidates.isEmpty()) {
            throw new UnsupportedStackException("Поддерживаемый стек не найден: нужны корневая конфигурация и исходники");
        }
        return candidates.getFirst();
    }

    private DetectedStack detectNpm(Path directory, Set<String> files) throws IOException {
        rejectFiles(files, "yarn.lock", "pnpm-lock.yaml", "bun.lock", "bun.lockb");
        try {
            var manifest = jsonMapper.readTree(readManifest(directory, "package.json"));
            if (!manifest.isObject()) {
                throw new IOException("package.json должен содержать JSON-объект");
            }
            var manager = manifest.get("packageManager");
            boolean npmDeclared = manager != null && manager.isString()
                    && manager.asString().matches("npm@\\d+\\..+");
            if (manager != null && !npmDeclared) {
                throw new UnsupportedStackException("Поддерживается только packageManager npm с указанной версией");
            }
            if (!npmDeclared && !files.contains("package-lock.json") && !files.contains("npm-shrinkwrap.json")) {
                throw new UnsupportedStackException("npm не подтверждён: нужен lock-файл npm или packageManager=npm@версия");
            }
            boolean typescript = files.stream().anyMatch(path -> path.matches(".*\\.(ts|tsx|mts|cts)$")
                    && !path.matches(".*\\.d\\.(ts|mts|cts)$"));
            boolean javascript = files.stream().anyMatch(path -> path.matches(".*\\.(js|jsx|mjs|cjs)$"));
            if (!typescript && !javascript) {
                throw new UnsupportedStackException("Исходники JavaScript/TypeScript не найдены");
            }
            return new DetectedStack(typescript ? Language.TYPESCRIPT : Language.JAVASCRIPT, BuildTool.NPM);
        } catch (JacksonException e) {
            throw new IOException("Некорректный package.json", e);
        }
    }

    private static void validatePom(String content) throws IOException {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
            Element root = builder.parse(new org.xml.sax.InputSource(new StringReader(content))).getDocumentElement();
            String namespace = root.getNamespaceURI();
            if (!"project".equals(root.getLocalName())
                    || (namespace != null && !"http://maven.apache.org/POM/4.0.0".equals(namespace))
                    || root.getElementsByTagNameNS("*", "artifactId").getLength() == 0) {
                throw new IOException("pom.xml не содержит описание Maven-проекта");
            }
            if (root.getElementsByTagNameNS("*", "modules").getLength() > 0) {
                throw new UnsupportedStackException("Многомодульный Maven пока не поддерживается");
            }
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Некорректный или небезопасный pom.xml", e);
        }
    }

    private static String readManifest(Path directory, String name) throws IOException {
        try (var input = Files.newInputStream(directory.resolve(name), LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(MAX_MANIFEST_BYTES + 1);
            if (bytes.length > MAX_MANIFEST_BYTES) {
                throw new IOException("Файл " + name + " превышает 1 МиБ");
            }
            return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        }
    }

    private static void rejectFiles(Set<String> files, String... unsupported) {
        for (String name : unsupported) {
            if (files.contains(name)) {
                throw new UnsupportedStackException("Не поддерживается менеджер зависимостей: " + name);
            }
        }
    }

    private static Set<String> collectFiles(Path directory) throws IOException {
        Set<String> files = new HashSet<>();
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            private int entries;

            private void countEntry() throws IOException {
                if (++entries > MAX_ENTRIES) {
                    throw new IOException("В репозитории больше " + MAX_ENTRIES + " файлов и папок");
                }
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
                countEntry();
                return !dir.equals(directory) && EXCLUDED_DIRECTORIES.contains(dir.getFileName().toString())
                        ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                countEntry();
                if (attributes.isRegularFile()) {
                    files.add(directory.relativize(file).toString().replace('\\', '/'));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }
}
