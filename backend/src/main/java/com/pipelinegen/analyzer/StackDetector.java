package com.pipelinegen.analyzer;

import com.pipelinegen.exception.UnsupportedStackException;
import com.pipelinegen.model.BuildTool;
import com.pipelinegen.model.Language;
import org.springframework.stereotype.Component;
import org.tomlj.Toml;
import org.tomlj.TomlTable;
import org.tomlj.TomlArray;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.Set;

/** Статический анализ одного проекта в корне репозитория, без запуска его кода. */
@Component
public class StackDetector {

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git", "node_modules", ".venv", "venv", "target", "build", "dist", "vendor", "__pycache__");
    private static final int MAX_ENTRIES = 20_000;
    private static final int MAX_MANIFEST_BYTES = 1_048_576;
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    /**
     * @throws UnsupportedStackException стек не поддержан, не подтверждён исходниками или неоднозначен
     * @throws IOException ошибка чтения файлов
     */
    public DetectedStack detect(Path directory) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Ожидается папка репозитория, не символическая ссылка");
        }
        Set<String> files = collectFiles(directory);
        var candidates = new ArrayList<DetectedStack>();
        var problems = new ArrayList<UnsupportedStackException>();
        if (files.contains("pom.xml")) {
            addCandidate(candidates, problems, () -> detectMaven(directory, files));
        }
        if (files.contains("requirements.txt") || files.contains("pyproject.toml") || files.contains("setup.py")) {
            addCandidate(candidates, problems, () -> detectPython(directory, files));
        }
        if (files.contains("package.json")) {
            boolean requireNpmMarker = !candidates.isEmpty();
            addCandidate(candidates, problems, () -> detectNpm(directory, files, requireNpmMarker));
        }
        if (candidates.size() > 1) {
            throw new UnsupportedStackException("В корне найдены несколько поддерживаемых проектов");
        }
        if (!candidates.isEmpty()) return candidates.getFirst();
        if (!problems.isEmpty()) throw problems.getFirst();

        boolean nested = files.stream().anyMatch(path ->
                path.matches("[^/]+/(pom\\.xml|package\\.json|requirements\\.txt|pyproject\\.toml|setup\\.py)"));
        throw new UnsupportedStackException(nested
                ? "Конфигурация найдена во вложенной папке. В MVP сборка запускается из корня репозитория."
                : "Поддерживаемый стек не найден: нужны корневая конфигурация и исходники");
    }

    @FunctionalInterface
    private interface Detection {
        DetectedStack detect() throws IOException;
    }

    private static void addCandidate(List<DetectedStack> candidates, List<UnsupportedStackException> problems,
                                     Detection detection) throws IOException {
        try {
            candidates.add(detection.detect());
        } catch (UnsupportedStackException e) {
            problems.add(e);
        }
    }

    private static DetectedStack detectMaven(Path directory, Set<String> files) throws IOException {
        Element pom = parsePom(readManifest(directory, "pom.xml"));
        boolean hasTests = files.stream().anyMatch(path -> path.startsWith("src/test/java/") && path.endsWith(".java"));
        if (!hasTests && files.stream().noneMatch(path -> path.startsWith("src/main/java/") && path.endsWith(".java"))) {
            throw new UnsupportedStackException("Исходники Java не найдены в стандартных каталогах Maven");
        }
        Element properties = child(pom, "properties");
        Element parent = child(pom, "parent");
        boolean bootParent = "org.springframework.boot".equals(value(parent, "groupId"))
                && "spring-boot-starter-parent".equals(value(parent, "artifactId"));
        Element plugins = child(child(pom, "build"), "plugins");
        String version = null;
        if (plugins != null) {
            for (var node = plugins.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element plugin
                        && "maven-compiler-plugin".equals(value(plugin, "artifactId"))) {
                    version = javaVersion(child(plugin, "configuration"), properties, "release", "source");
                    break;
                }
            }
        }
        if (version == null) {
            version = javaVersion(properties, properties, "maven.compiler.release", "maven.compiler.source");
        }
        if (version == null && bootParent) version = javaVersion(properties, properties, "java.version");

        boolean boot = bootParent || hasMavenGroup(child(pom, "dependencies"), "org.springframework.boot");
        List<String> frameworks = boot ? List.of("spring-boot")
                : hasMavenGroup(child(pom, "dependencies"), "org.springframework") ? List.of("spring") : List.of();
        return new DetectedStack(Language.JAVA, BuildTool.MAVEN, version, frameworks, hasTests, files.contains("Dockerfile"));
    }

    private static boolean hasMavenGroup(Element dependencies, String group) {
        if (dependencies == null) return false;
        for (var node = dependencies.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element dependency && group.equals(value(dependency, "groupId"))) return true;
        }
        return false;
    }

    private static String javaVersion(Element element, Element properties, String... names) {
        for (String name : names) {
            String version = value(element, name);
            if (version != null && version.startsWith("${") && version.endsWith("}")) {
                version = value(properties, version.substring(2, version.length() - 1));
            }
            if (version != null) {
                version = version.replaceFirst("^1\\.", "");
                if (version.matches("[1-9]\\d*")) return version;
            }
        }
        return null;
    }

    private static Element child(Element element, String name) {
        if (element != null) {
            for (var node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element candidate && name.equals(candidate.getLocalName())
                        && Objects.equals(element.getNamespaceURI(), candidate.getNamespaceURI())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static String value(Element element, String name) {
        Element child = child(element, name);
        return child == null ? null : child.getTextContent().strip();
    }

    private static DetectedStack detectPython(Path directory, Set<String> files) throws IOException {
        boolean requirements = files.contains("requirements.txt");
        TomlTable project = null;
        if (files.contains("pyproject.toml")) {
            var configuration = Toml.parse(readManifest(directory, "pyproject.toml"));
            if (configuration.hasErrors()) throw new UnsupportedStackException("Некорректный pyproject.toml");
            if (configuration.get("project") instanceof TomlTable table) project = table;
            if (!requirements && configuration.contains("tool.poetry")) {
                throw new UnsupportedStackException("Poetry пока не поддерживается. Для pip добавьте requirements.txt.");
            }
        }
        if (!requirements) rejectFiles(files, "poetry.lock", "uv.lock", "Pipfile", "Pipfile.lock");
        if (!(requirements || files.contains("setup.py") || project != null)
                || files.stream().noneMatch(path -> path.endsWith(".py") && !path.equals("setup.py"))) {
            throw new UnsupportedStackException("Нужны исходники Python и requirements.txt, setup.py или таблица [project]");
        }
        var dependencies = new HashSet<String>();
        if (requirements) {
            readManifest(directory, "requirements.txt").lines().forEach(line -> addPythonDependency(dependencies, line));
        }
        if (project != null && project.get("dependencies") instanceof TomlArray array) {
            for (Object item : array.toList()) {
                if (item instanceof String dependency) addPythonDependency(dependencies, dependency);
            }
        }
        String version = readVersion(directory, files, ".python-version", "runtime.txt");
        if (project != null && project.get("requires-python") instanceof String requirement) {
            version = PythonVersion.select(version, requirement);
        }
        return new DetectedStack(Language.PYTHON, BuildTool.PIP, version,
                List.of("django", "flask", "fastapi").stream().filter(dependencies::contains).toList(),
                files.stream().anyMatch(path -> path.matches("(?:.*/)?(?:test_.*|.*_test)\\.py")),
                files.contains("Dockerfile"));
    }

    private static final Pattern PYTHON_PACKAGE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_.-]*");

    private static void addPythonDependency(Set<String> dependencies, String line) {
        var name = PYTHON_PACKAGE.matcher(line.strip());
        if (name.find()) dependencies.add(name.group().toLowerCase(Locale.ROOT));
    }

    private static String readVersion(Path directory, Set<String> files, String... names) throws IOException {
        for (String name : names) {
            if (files.contains(name)) {
                String version = runtimeVersion(readManifest(directory, name));
                if (version != null) return version;
            }
        }
        return null;
    }

    private static String runtimeVersion(String value) {
        String version = value.strip().replaceFirst("^(?:v|python-)", "");
        return version.matches("[1-9]\\d*(?:\\.\\d+){0,2}") ? version : null;
    }

    private DetectedStack detectNpm(Path directory, Set<String> files, boolean requireNpmMarker) throws IOException {
        rejectFiles(files, "yarn.lock", "pnpm-lock.yaml", "bun.lock", "bun.lockb");
        try {
            var manifest = jsonMapper.readTree(readManifest(directory, "package.json"));
            if (manifest == null || !manifest.isObject()) {
                throw new UnsupportedStackException("package.json должен содержать JSON-объект");
            }
            var manager = manifest.get("packageManager");
            boolean npmDeclared = manager != null && manager.isString()
                    && manager.asString().matches("npm@\\d+\\..+");
            if (manager != null && !npmDeclared) {
                throw new UnsupportedStackException("Поддерживается только packageManager npm с указанной версией");
            }
            boolean hasLockfile = files.contains("package-lock.json") || files.contains("npm-shrinkwrap.json");
            if (requireNpmMarker && !hasLockfile && !npmDeclared) {
                throw new UnsupportedStackException("Для отдельного npm-проекта нужен lock-файл или packageManager npm");
            }
            boolean typescript = files.stream().filter(StackDetector::isApplicationSource)
                    .anyMatch(path -> path.matches(".*\\.(ts|tsx|mts|cts)$")
                    && !path.matches(".*\\.d\\.(ts|mts|cts)$"));
            boolean javascript = files.stream().filter(StackDetector::isApplicationSource)
                    .anyMatch(path -> path.matches(".*\\.(js|jsx|mjs|cjs)$"));
            if (!typescript && !javascript) {
                throw new UnsupportedStackException("Исходники JavaScript/TypeScript не найдены");
            }
            String version = readVersion(directory, files, ".nvmrc", ".node-version");
            var nodeVersion = manifest.path("engines").path("node");
            if (version == null && nodeVersion.isString()) version = runtimeVersion(nodeVersion.asString());
            List<String> frameworks = List.of("react", "vue", "next", "express", "@angular/core").stream()
                    .filter(name -> manifest.path("dependencies").has(name) || manifest.path("devDependencies").has(name))
                    .toList();
            var script = manifest.path("scripts").path("test");
            boolean testScript = script.isString() && !script.asString().isBlank()
                    && !script.asString().contains("no test specified");
            boolean hasTests = testScript
                    || files.stream().anyMatch(path -> path.matches(".*\\.(test|spec)\\.(?:[cm]?[jt]s|[jt]sx)$"));
            return new DetectedStack(typescript ? Language.TYPESCRIPT : Language.JAVASCRIPT, BuildTool.NPM,
                    version, frameworks, hasTests, files.contains("Dockerfile"), hasLockfile, testScript);
        } catch (JacksonException e) {
            throw new UnsupportedStackException("Некорректный package.json");
        }
    }

    private static boolean isApplicationSource(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        return !name.matches("(?:\\..+|.+\\.config)\\.(?:[cm]?[jt]s|[jt]sx)");
    }

    private static Element parsePom(String content) throws IOException {
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
            Element root = builder.parse(new InputSource(new StringReader(content))).getDocumentElement();
            String namespace = root.getNamespaceURI();
            if (!"project".equals(root.getLocalName())
                    || (namespace != null && !"http://maven.apache.org/POM/4.0.0".equals(namespace))
                    || value(root, "artifactId") == null) {
                throw new UnsupportedStackException("pom.xml не содержит описание Maven-проекта");
            }
            if (child(root, "modules") != null || "pom".equals(value(root, "packaging"))) {
                throw new UnsupportedStackException("Многомодульный Maven пока не поддерживается");
            }
            return root;
        } catch (SAXException e) {
            throw new UnsupportedStackException("Некорректный или небезопасный pom.xml");
        } catch (ParserConfigurationException e) {
            throw new IOException("Некорректный или небезопасный pom.xml", e);
        }
    }

    private static String readManifest(Path directory, String name) throws IOException {
        try (var input = Files.newInputStream(directory.resolve(name), LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(MAX_MANIFEST_BYTES + 1);
            if (bytes.length > MAX_MANIFEST_BYTES) {
                throw new UnsupportedStackException("Файл " + name + " превышает 1 МиБ");
            }
            String content = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
            return content.startsWith("\uFEFF") ? content.substring(1) : content;
        } catch (CharacterCodingException e) {
            throw new UnsupportedStackException("Файл " + name + " содержит некорректный UTF-8");
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
                    throw new UnsupportedStackException("В репозитории больше " + MAX_ENTRIES + " файлов и папок");
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
