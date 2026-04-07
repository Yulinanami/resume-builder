package com.resumebuilder.springaibackend.cleaner;

import org.springframework.stereotype.Component;

// author: jf
@Component
public class ResumeMarkdownCleaner {

    public String sanitizeResumeMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        String sanitized = markdown
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\u3000', ' ')
                .replace('\uFF03', '#')
                .replaceAll("(?m)(^|\\n)(\\s*)\\*\\*(?=\\s+)", "$1$2")
                .replaceFirst("(?i)^\\s*(?:\\u5982\\u4e0b|\\u4f18\\u5316\\u540e\\u5982\\u4e0b|\\u4f18\\u5316\\u5982\\u4e0b|\\u4f18\\u5316\\u540e\\u5185\\u5bb9\\u5982\\u4e0b)\\s*[\\uFF1A:]?\\s*", "");

        String[] lines = sanitized.split("\n", -1);
        StringBuilder rebuilt = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine;
            // Normalize headings wrapped by bold markers, e.g. **# Company Name**.
            line = line.replaceAll("^(\\s*)(\\*\\*|__)(\\s*#{1,6}\\s*.+?)\\s*\\2\\s*$", "$1$3");
            // Normalize list item wrapping with bold heading markers.
            line = line.replaceAll("^(\\s*[-*+]\\s*)(\\*\\*|__)(\\s*#{1,6}\\s*.+?)\\s*\\2\\s*$", "$1$3");
            // Unescape heading markers at line start, e.g. \\# Title.
            line = line.replaceAll("^(\\s*)\\\\+(#{1,6})(?=\\S)", "$1$2 ");
            line = line.replaceAll("^(\\s*)\\\\+(#{1,6})\\s+", "$1$2 ");
            // Normalize accidentally inline heading markers into a new line.
            line = line.replaceAll("([^\\n])\\s+(#{1,6})\\s*(?=\\S)", "$1\n$2 ");
            line = line.replaceAll("^(\\s*)([#]{1,6})(\\S)", "$1$2 $3");
            line = normalizeSubHeadingLine(line);
            line = normalizeSectionHeadingLine(line);
            line = line.replaceAll("^(\\s*)([\\u2022\\u00B7\\u25CF\\u25E6\\u2023\\u25AA\\u25AB])\\s*", "$1- ");
            line = line.replaceAll("^(\\s*)([\\-\\*\\+\\uFF0D\\u2014\\u2013\\u2212])(?!\\2)(\\S)", "$1- $3");
            line = line.replaceAll("^(\\s*)([\\-\\*\\+\\uFF0D\\u2014\\u2013\\u2212])(?!\\2)\\s+", "$1- ");
            line = line.replaceAll("^(\\s*)(\\d+)\\s*[\\.\\u3002\\uFF0E\\u3001]\\s*(\\S)", "$1$2. $3");
            line = line.replaceAll("^(\\s*)(\\d+)\\.(?=\\S)", "$1$2. ");
            line = line.replaceAll("^(\\s*)\\*\\*(?=\\s+)", "$1");

            if (!line.isBlank()) {
                if (!rebuilt.isEmpty()) {
                    rebuilt.append('\n');
                }
                rebuilt.append(line);
            }
        }

        sanitized = rebuilt.toString();

        String previous;
        do {
            previous = sanitized;
            sanitized = sanitized.replaceAll("\\*\\*\\s*\\*\\*", "");
        } while (!previous.equals(sanitized));

        int boldMarkerCount = (sanitized.length() - sanitized.replace("**", "").length()) / 2;
        if (boldMarkerCount % 2 != 0) {
            sanitized = sanitized.replace("**", "");
        }

        return sanitized.trim();
    }

    public String safeContent(String content) {
        return content == null ? "" : content.trim();
    }

    private String normalizeSubHeadingLine(String line) {
        if (line == null || line.isBlank()) {
            return line;
        }

        String normalized = line
                .replace('\uFF03', '#')
                .trim()
                .replaceAll("^(?:[-*+]\\s*)+", "")
                .replaceAll("^(?:\\*\\*|__|`)+\\s*", "")
                .replaceAll("\\s*(?:\\*\\*|__|`)+\\s*$", "")
                .replaceAll("^(#{1,6})(?=\\S)", "$1 ")
                .trim();

        if (!normalized.matches("^#{1,6}\\s+.+$")) {
            return line;
        }

        String text = normalized.replaceFirst("^#{1,6}\\s+", "").trim();
        text = text.replaceAll("^(?:#\\s*)+", "").trim();
        if (text.isBlank()) {
            return line;
        }

        String compact = text.replaceAll("\\s+", "").toLowerCase();
        if (compact.equals("\u4f18\u5316\u5efa\u8bae")
                || compact.equals("\u5efa\u8bae")
                || compact.equals("\u4f18\u5316\u540e\u5185\u5bb9")
                || compact.equals("\u4f18\u5316\u540e\u7684\u5185\u5bb9")
                || compact.equals("\u4f18\u5316\u5185\u5bb9")
                || compact.equals("\u4f18\u5316\u7ed3\u679c")
                || compact.equals("suggestion")
                || compact.equals("suggestions")
                || compact.equals("optimizedcontent")
                || compact.equals("optimizedoutput")) {
            return line;
        }

        return "### " + text;
    }

    private String normalizeSectionHeadingLine(String line) {
        if (line == null || line.isBlank()) {
            return line;
        }

        String normalized = line
                .replace('\uFF03', '#')
                .trim()
                .replaceAll("^(?:[-*+]\\s*)+", "")
                .replaceAll("^(?:\\*\\*|__|`)+\\s*", "")
                .replaceAll("^#{1,6}\\s*", "")
                .replaceAll("^(?:\\*\\*|__|`)+\\s*", "")
                .replaceAll("\\s*(?:\\*\\*|__|`)+\\s*$", "")
                .replaceAll("^(?:#\\s*)+", "")
                .trim();

        if (normalized.isBlank()) {
            return line;
        }

        String[] parts = normalized.split("[\\uFF1A:]", 2);
        String label = parts[0]
                .replaceAll("\\s+", "")
                .toLowerCase();
        String inline = parts.length > 1 ? parts[1].trim() : "";

        String canonicalHeading = null;
        if (label.equals("\u4f18\u5316\u5efa\u8bae") || label.equals("\u5efa\u8bae") || label.equals("suggestion") || label.equals("suggestions")) {
            canonicalHeading = "## \u4f18\u5316\u5efa\u8bae";
        } else if (label.equals("\u4f18\u5316\u540e\u5185\u5bb9")
                || label.equals("\u4f18\u5316\u540e\u7684\u5185\u5bb9")
                || label.equals("\u4f18\u5316\u5185\u5bb9")
                || label.equals("\u4f18\u5316\u7ed3\u679c")
                || label.equals("optimizedcontent")
                || label.equals("optimizedoutput")) {
            canonicalHeading = "## \u4f18\u5316\u540e\u5185\u5bb9";
        }

        if (canonicalHeading == null) {
            return line;
        }
        if (inline.isBlank()) {
            return canonicalHeading;
        }
        return canonicalHeading + "\n" + inline;
    }
}
