package com.pipelinegen.analyzer;

import com.pipelinegen.exception.UnsupportedStackException;

import java.math.BigInteger;
import java.util.regex.Pattern;

final class PythonVersion {
    private static final String VERSION = "(3\\.\\d+(?:\\.\\d+)?)";
    private static final Pattern EXACT = Pattern.compile("==\\s*" + VERSION);
    private static final Pattern RANGE = Pattern.compile(">=\\s*" + VERSION + "(?:\\s*,\\s*<\\s*" + VERSION + ")?");

    private PythonVersion() {
    }

    static String select(String pinned, String requirement) {
        String constraint = requirement.strip();
        var exact = EXACT.matcher(constraint);
        var range = RANGE.matcher(constraint);
        if (exact.matches()) {
            String required = exact.group(1);
            if (pinned == null) return required;
            if (pinned.matches(VERSION) && compare(pinned, required) == 0) {
                return pinned.split("\\.").length == 3 ? pinned : required;
            }
        } else if (range.matches()) {
            String lower = range.group(1);
            String upper = range.group(2);
            String candidate = pinned == null ? lower : pinned;
            if (upper != null && upper.split("\\.").length == 3 && candidate.split("\\.").length == 2) {
                if (pinned != null) throw new UnsupportedStackException("Уточните patch-версию Python для " + requirement);
                candidate += ".0";
            }
            if (candidate.matches("3\\.\\d+(?:\\.\\d+)?")
                    && compare(candidate, lower) >= 0 && (upper == null || compare(candidate, upper) < 0)) {
                return candidate;
            }
        }
        throw new UnsupportedStackException("Не удалось выбрать Python для requires-python = "
                + requirement + (pinned == null ? "" : ", закреплённая версия: " + pinned));
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
