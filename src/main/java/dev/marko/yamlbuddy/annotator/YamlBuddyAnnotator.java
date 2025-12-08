package dev.marko.yamlbuddy.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlBuddyAnnotator implements Annotator {

    // Matches:  ${server.port}
    private static final Pattern EL_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private static final String SPRING_VALUE_ANNOTATION =
            "org.springframework.beans.factory.annotation.Value";

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {

        // Apply only to string literals
        if (!(element instanceof PsiLiteralExpression literal)) return;

        Object raw = literal.getValue();
        if (!(raw instanceof String text)) return;

        // Check if literal is inside @Value(...)
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(literal, PsiAnnotation.class);
        if (annotation == null) return;

        // Works even when annotation is imported (short name only)
        String qName = annotation.getQualifiedName();
        if (!SPRING_VALUE_ANNOTATION.equals(qName)) return;

        Matcher matcher = EL_PATTERN.matcher(text);

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            if (key.isEmpty()) continue;

            int startOffsetInLiteral = matcher.start();
            int endOffsetInLiteral = matcher.end();

            // literal.getText() includes quotes → offset by +1
            int literalStart = literal.getTextRange().getStartOffset();
            TextRange highlightRange = new TextRange(
                    literalStart + 1 + startOffsetInLiteral,
                    literalStart + 1 + endOffsetInLiteral
            );

            holder.newAnnotation(
                            HighlightSeverity.WARNING,
                            "YAML key '" + key + "' might be missing"
                    )
                    .range(highlightRange)
                    .tooltip("Key '" + key + "' not found in application.yaml.\nClick to create it.")
                    .create();
        }
    }
}
