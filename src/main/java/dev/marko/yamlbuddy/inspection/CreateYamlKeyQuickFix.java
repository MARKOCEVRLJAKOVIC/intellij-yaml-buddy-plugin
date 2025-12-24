package dev.marko.yamlbuddy.inspection;

import com.intellij.codeInsight.intention.IntentionAction; 
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import dev.marko.yamlbuddy.util.YamlPsiUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * This class implements both LocalQuickFix (for inspections) and IntentionAction (for annotators).
 */
public class CreateYamlKeyQuickFix implements LocalQuickFix, IntentionAction {

    private final String keyPath;

    public CreateYamlKeyQuickFix(String keyPath) {
        this.keyPath = keyPath;
    }

    // Local Quick Fix implementation (used by Inspection)

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getName() {
        return "Create key '" + keyPath + "' in application.yaml";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "YAML Buddy QuickFixes";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        executeFix(project);
    }

    // Intention Action implementation (used by Annotator)

    @Override
    public @NotNull String getText() {
        // Text displayed in the Alt+Enter menu
        return getName();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        executeFix(project);
    }

    @Override
    public boolean startInWriteAction() {
        // Returns true as it modifies the application.yaml file
        return true;
    }

    // Shared logic

    private void executeFix(Project project) {
        YamlPsiUtils.createMissingPathInYaml(project, keyPath);

        YAMLKeyValue kv = YamlPsiUtils.findYamlKey(project, keyPath);
        if (kv != null) {
            kv.navigate(true);
        }
    }
}