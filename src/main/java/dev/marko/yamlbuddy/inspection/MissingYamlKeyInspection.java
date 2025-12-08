package dev.marko.yamlbuddy.inspection;

import com.intellij.codeInspection.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import dev.marko.yamlbuddy.reference.YamlPropertyReference;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MissingYamlKeyInspection extends LocalInspectionTool {

    private static final Pattern EL_PATTERN = Pattern.compile("\\$\\{\\s*([^}]+)\\s*}");

    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder,
            boolean isOnTheFly
    ) {
        return new JavaElementVisitor() {

            @Override
            public void visitLiteralExpression(PsiLiteralExpression expression) {
                super.visitLiteralExpression(expression);

                Object rawValue = expression.getValue();
                if (!(rawValue instanceof String literal)) return;

                PsiAnnotation annotation =
                        PsiTreeUtil.getParentOfType(expression, PsiAnnotation.class);

                if (annotation == null ||
                        !"org.springframework.beans.factory.annotation.Value"
                                .equals(annotation.getQualifiedName())) {
                    return;
                }

                Matcher matcher = EL_PATTERN.matcher(literal);

                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (key == null || key.isBlank()) continue;

                    boolean resolved = false;

                    for (PsiReference ref : YamlPropertyReference.createReferences(expression)) {
                        if (ref.resolve() != null) {
                            resolved = true;
                            break;
                        }
                    }

                    if (!resolved) {

                        TextRange range = new TextRange(
                                matcher.start() + 1,
                                matcher.end() + 1
                        );

                        ProblemDescriptor descriptor = holder.getManager().createProblemDescriptor(
                                expression,
                                range,
                                "YAML key '" + key + "' not found",
                                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                isOnTheFly,
                                new CreateYamlKeyQuickFix(key)
                        );

                        holder.registerProblem(descriptor);
                    }
                }
            }
        };
    }
}
