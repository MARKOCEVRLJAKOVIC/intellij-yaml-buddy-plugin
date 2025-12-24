package dev.marko.yamlbuddy.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import dev.marko.yamlbuddy.util.YamlPsiUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * LocalQuickFix which deliberate creation of YAML hierarchy in util.
 */
public class CreateYamlKeyQuickFix implements LocalQuickFix {

    private final String keyPath;

    public CreateYamlKeyQuickFix(String keyPath) {
        this.keyPath = keyPath;
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getName() {
        return "Create key in application.yaml";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "YAML Buddy QuickFixes";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        YAMLKeyValue kv = YamlPsiUtils.findYamlKey(project, keyPath);


        if (kv != null) {
            kv.navigate(true);
        }
    }
}
