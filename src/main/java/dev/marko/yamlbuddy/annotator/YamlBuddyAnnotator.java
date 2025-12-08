package dev.marko.yamlbuddy.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Annotator that detects @Vale("${...}") String literal and adds warning highlight
 * if the key looks like ${...}. Annotator gives a quick visaul feedback; real QuickFix action
 * and register Inspection.
 */
public class YamlBuddyAnnotator implements Annotator {

    private static final Pattern EL_PATTERN = Pattern.compile("\\$\\{\\s*([^}]+)\\s*}");

    @Override
    public void annotate(PsiElement element, AnnotationHolder holder) {
        // Works on java literals
        if (!(element instanceof PsiLiteralExpression)) return;
        PsiLiteralExpression literal = (PsiLiteralExpression) element;
        Object valueObj = literal.getValue();
        if (!(valueObj instanceof String)) return;
        String value = (String) valueObj;

        // Check if the literal is inside @Value(...)
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(literal, PsiAnnotation.class);
        String qName = annotation == null ? null : annotation.getQualifiedName();
        if (!"org.springframework.beans.factory.annotation.Value".equals(qName)) {
            return;
        }

        Matcher matcher = EL_PATTERN.matcher(value);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key == null || key.trim().isEmpty()) continue;

            // Warning: This is not complex resolution (that's supposed to be done in Reference/Inspection)
            // Annotation shows warning if it contains ${...}.
            int start = matcher.start();
            int end = matcher.end();
            // PsiLiteralExpression.text contains "", so we need offset for 1 inside text
            TextRange range = new TextRange(literal.getTextRange().getStartOffset() + 1 + start,
                    literal.getTextRange().getStartOffset() + 1 + end);
            holder.createWarningAnnotation(range, "YAML key '" + key + "' might be missing in application.yml/.yaml")
                    .setTooltip("Key '" + key + "' not found in application configuration. Use quick fix to create it.");
        }
    }
}
