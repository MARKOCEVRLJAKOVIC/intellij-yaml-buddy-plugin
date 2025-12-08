package dev.marko.yamlbuddy.reference;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Contributes references for string literals inside @Value("${...}").
 */
public class YamlPropertyReferenceContributor extends PsiReferenceContributor {

    private static final ElementPattern<PsiLiteralExpression> STRING_LITERAL =
            PlatformPatterns.psiElement(PsiLiteralExpression.class);

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

        registrar.registerReferenceProvider(
                STRING_LITERAL,
                new PsiReferenceProvider() {

                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(
                            @NotNull PsiElement element,
                            @NotNull ProcessingContext context
                    ) {
                        if (!(element instanceof PsiLiteralExpression literal)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        Object rawValue = literal.getValue();
                        if (!(rawValue instanceof String value)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        // Must be inside @Value annotation
                        PsiAnnotation annotation =
                                PsiTreeUtil.getParentOfType(literal, PsiAnnotation.class);

                        if (annotation == null ||
                                !"org.springframework.beans.factory.annotation.Value"
                                        .equals(annotation.getQualifiedName())) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        // Must contain ${...}
                        if (!value.contains("${") || !value.contains("}")) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        return YamlPropertyReference.createReferences(literal);
                    }
                }
        );
    }
}
