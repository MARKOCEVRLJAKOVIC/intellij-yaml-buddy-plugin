package dev.marko.yamlbuddy.reference;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import dev.marko.yamlbuddy.util.YamlPsiUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlPropertyReference
        extends PsiReferenceBase<PsiElement>
        implements PsiPolyVariantReference, EmptyResolveMessageProvider {

    private final String keyPath;

    private static final Pattern EL_PATTERN = Pattern.compile("\\$\\{\\s*([^}]+)\\s*}");

    public YamlPropertyReference(@NotNull PsiElement element,
                                 @NotNull TextRange range,
                                 @NotNull String keyPath) {
        super(element, range);
        this.keyPath = keyPath;
    }

    @Override
    public PsiElement resolve() {
        return YamlPsiUtils.findYamlKey(getElement().getProject(), keyPath);
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        PsiElement resolved = resolve();
        if (resolved == null) {
            return ResolveResult.EMPTY_ARRAY;
        }
        return new ResolveResult[]{new PsiElementResolveResult(resolved)};
    }

    @Override
    public Object @NotNull [] getVariants() {
        return new Object[0]; // Later: YAML autocomplete
    }

    @Override
    public String getUnresolvedMessagePattern() {
        return "YAML key '" + keyPath + "' not found";
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName)
            throws IncorrectOperationException {
        throw new IncorrectOperationException("Rename not supported for YAML EL references");
    }

    /**
     * Factory method: extracts all ${...} references from a PsiLiteral.
     */
    public static PsiReference @NotNull [] createReferences(@NotNull PsiLiteral literal) {

        Object rawValue = literal.getValue();
        if (!(rawValue instanceof String literalText)) {
            return PsiReference.EMPTY_ARRAY;
        }

        List<PsiReference> refs = new ArrayList<>();
        Matcher matcher = EL_PATTERN.matcher(literalText);

        while (matcher.find()) {
            String key = matcher.group(1);
            if (key == null || key.isBlank()) continue;

            int start = matcher.start(1);
            int end = matcher.end(1);

            // offset by 1 to skip opening quote
            TextRange range = new TextRange(1 + start, 1 + end);

            refs.add(new YamlPropertyReference(literal, range, key));
        }

        return refs.toArray(PsiReference[]::new);
    }
}
