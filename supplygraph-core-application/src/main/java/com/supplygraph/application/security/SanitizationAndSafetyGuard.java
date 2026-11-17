package com.supplygraph.application.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Hardening utility providing input sanitization, file upload boundaries, and zero-trust prompt validation.
 */
@Service
public class SanitizationAndSafetyGuard {

    private static final Logger log = LoggerFactory.getLogger(SanitizationAndSafetyGuard.class);

    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 Megabytes

    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(
            "csv", "json", "txt", "xml"
    );

    private static final List<Pattern> PROMPT_INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore (all )?previous instructions"),
            Pattern.compile("(?i)system prompt override"),
            Pattern.compile("(?i)you are now in developer mode"),
            Pattern.compile("(?i)disregard security boundaries"),
            Pattern.compile("(?i)bypass concurguard"),
            Pattern.compile("(?i)grant admin permissions"),
            Pattern.compile("(?i)sudo approve plan")
    );

    /**
     * Sanitizes plain text input to neutralize potential script/HTML or control character injection.
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.trim()
                .replace("<script>", "")
                .replace("</script>", "")
                .replace("javascript:", "")
                .replace("\u0000", ""); // Strip null bytes
    }

    /**
     * Validates uploaded file size and extension to prevent file-bomb and arbitrary execution attacks.
     */
    public void validateFileUpload(String filename, byte[] content) {
        Objects.requireNonNull(filename, "Filename cannot be null");
        Objects.requireNonNull(content, "File content cannot be null");

        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format(
                    "Uploaded file size (%d bytes) exceeds maximum allowable limit of %d bytes (10MB)",
                    content.length, MAX_FILE_SIZE_BYTES));
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            throw new IllegalArgumentException("Uploaded file lacks a valid extension: " + filename);
        }

        String extension = filename.substring(dotIndex + 1).toLowerCase();
        if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(String.format(
                    "File extension .%s is not permitted. Allowed extensions: %s",
                    extension, ALLOWED_FILE_EXTENSIONS));
        }
    }

    /**
     * Evaluates prompt text against zero-trust prompt boundaries to prevent malicious LLM jailbreaks.
     * Throws SecurityException if an attack pattern is detected.
     */
    public void validatePromptSafety(String promptText) {
        if (promptText == null || promptText.isBlank()) {
            return;
        }
        for (Pattern pattern : PROMPT_INJECTION_PATTERNS) {
            if (pattern.matcher(promptText).find()) {
                log.warn("Zero-trust prompt boundary triggered: suspicious pattern detected in prompt: [{}]", pattern.pattern());
                throw new SecurityException("Prompt rejected: Detected attempt to manipulate agent security boundaries");
            }
        }
    }
}

