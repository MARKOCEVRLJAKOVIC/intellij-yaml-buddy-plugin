package dev.marko.yamlbuddy.reference;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;

/**
 * Registruje provider koji daje PsiReference-ove za string literal unutar @Value.
 * Provider vraća YamlPropertyReference-e.
 */
public class YamlPropertyReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(PsiLiteralExpression.class),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference[] getReferencesByElement(PsiElement element, ProcessingContext context) {
                        if (!(element instanceof PsiLiteralExpression)) return PsiReference.EMPTY_ARRAY;
                        PsiLiteralExpression literal = (PsiLiteralExpression) element;
                        Object valueObj = literal.getValue();
                        if (!(valueObj instanceof String)) return PsiReference.EMPTY_ARRAY;
                        String value = (String) valueObj;

                        // proveri da li je literal unutar @Value
                        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(literal, PsiAnnotation.class);
                        String qName = annotation == null ? null : annotation.getQualifiedName();
                        if (!"org.springframework.beans.factory.annotation.Value".equals(qName)) {
                            return PsiReference.EMPTY_ARRAY;
                        }

                        return YamlPropertyReference.createReferencesForLiteral(literal, value);
                    }
                }
        );
    }
}
