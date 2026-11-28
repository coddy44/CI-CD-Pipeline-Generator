package com.pipelinegen.analyzer;

import com.pipelinegen.exception.UnsupportedStackException;

import java.math.BigInteger;
import java.util.regex.Pattern;

final class PythonVersion {
    private static final String VERSION = "(3\\.\\d+(?:\\.\\d+)?)";
    private static final Pattern EXACT = Pattern.compile("==\\s*" + VERSION);
    private static final Pattern RANGE = Pattern.compile(">=\\s*" + VERSION
            + "(?:\\s*,\\s*<\\s*([1-9]\\d*(?:\\.\\d+){0,2}))?");
    private static final Pattern COMPATIBLE = Pattern.compile("~=\\s*" + VERSION);

    private PythonVersion() {
    }

    static String select(String pinned, String requirement) {
        String constraint = requirement.strip();
        var exact = EXACT.matcher(constraint);
        var range = RANGE.matcher(constraint);
        var compatible = COMPATIBLE.matcher(constraint);
        boolean compatibleRelease = compatible.matches();
        if (exact.matches()) {
            String required = exact.group(1);
            if (pinned == null || pinned.matches(VERSION) && compare(pinned, required) == 0) {
                return normalize(hasPatch(required) ? required : required + ".0");
            }
        } else if (compatibleRelease || range.matches()) {
            String lower = compatibleRelease ? compatible.group(1) : range.group(1);
            String upper = compatibleRelease ? compatibleUpperBound(lower) : range.group(2);
            String candidate = pinned == null ? lower : pinned;
            if (upper != null && hasPatch(upper) && candidate.split("\\.").length == 2) {
                if (pinned != null) throw new UnsupportedStackException("Уточните patch-версию Python для " + requirement);
                candidate += ".0";
            }
            if (candidate.matches("3\\.\\d+(?:\\.\\d+)?")
                    && compare(candidate, lower) >= 0 && (upper == null || compare(candidate, upper) < 0)) {
                return normalize(candidate);
            }
        }
        throw new UnsupportedStackException("Не удалось выбрать Python для requires-python = "
                + requirement + (pinned == null ? "" : ", закреплённая версия: " + pinned));
    }

    private static boolean hasPatch(String version) {
        return version.split("\\.").length == 3;
    }

    private static String normalize(String version) {
        String[] parts = version.split("\\.");
        String minor = "3." + new BigInteger(parts[1]);
        return parts.length == 3 ? minor + "." + new BigInteger(parts[2]) : minor;
    }

    private static String compatibleUpperBound(String version) {
        if (!hasPatch(version)) return "4";
        String minor = version.split("\\.")[1];
        return "3." + new BigInteger(minor).add(BigInteger.ONE);
    }

    private static int compare(String left, String right) {
        String[] first = left.split("\\.");
        String[] second = right.split("\\.");
        for (int index = 0; index < 3; index++) {
            int comparison = new BigInteger(index < first.length ? first[index] : "0")
                    .compareTo(new BigInteger(index < second.length ? second[index] : "0"));
            if (comparison != 0) return comparison;
        }
        return 0;
    }
}
