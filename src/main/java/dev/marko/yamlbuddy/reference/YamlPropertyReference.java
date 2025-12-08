package dev.marko.yamlbuddy.reference;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import dev.marko.yamlbuddy.util.YamlPsiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PsiReference that uses dot-path (npr. server.port.number) and tries resolve
 * in YAMLKeyValue in project.
 */
public class YamlPropertyReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference, EmptyResolveMessageProvider {

    private final String keyPath;

    private static final Pattern EL_PATTERN = Pattern.compile("\\$\\{\\s*([^}]+)\\s*}");

    public YamlPropertyReference(PsiElement element, TextRange range, String keyPath) {
        super(element, range);
        this.keyPath = keyPath;
    }

    @Override
    public PsiElement resolve() {
        return YamlPsiUtils.findYamlKey(getElement().getProject(), keyPath);
    }

    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        PsiElement target = resolve();
        if (target == null) return ResolveResult.EMPTY_ARRAY;
        return new ResolveResult[]{new PsiElementResolveResult(target)};
    }

    @Override
    public Object[] getVariants() {
        // Autocomplete variants can be added later.
        return new Object[0];
    }

    @Override
    public String getUnresolvedMessagePattern() {
        return "YAML key '" + keyPath + "' not found";
    }

    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        throw new IncorrectOperationException("Rename not supported for literal references");
    }

    /**
     * Parse literals and create appropriate PsiReferences
     */
    public static PsiReference[] createReferencesForLiteral(PsiLiteralExpression literal, String literalText) {
        List<PsiReference> refs = new ArrayList<>();
        Matcher matcher = EL_PATTERN.matcher(literalText);

        while (matcher.find()) {
            String key = matcher.group(1);
            if (key == null || key.trim().isEmpty()) continue;
            int start = matcher.start();
            int end = matcher.end();
            // Move in literal.text
            TextRange range = new TextRange(1 + start, 1 + end);
            refs.add(new YamlPropertyReference(literal, range, key));
        }
        return refs.toArray(new PsiReference[0]);
    }
}
