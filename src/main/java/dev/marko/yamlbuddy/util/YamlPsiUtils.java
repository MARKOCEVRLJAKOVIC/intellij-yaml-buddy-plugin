package dev.marko.yamlbuddy.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jetbrains.yaml.psi.YAMLValue;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class: locating application.yml, searching dot-paths and creating hierarchy.
 */
public class YamlPsiUtils {

    private static final List<String> CANDIDATES = Arrays.asList("application.yml", "application.yaml");

    /**
     * Starts searching through all candidate files; returns the first YAMLKeyValue that matches the dotPath.
     */
    public static YAMLKeyValue findYamlKey(Project project, String dotPath) {
        List<YAMLFile> files = findApplicationYamlFiles(project);
        for (YAMLFile file : files) {
            YAMLKeyValue kv = findKeyInYamlPsi(file, dotPath);
            if (kv != null) return kv;
        }
        return null;
    }

    /**
     * Finds application.yml/yaml files in the project, prioritizing src/main/resources.
     */
    public static List<YAMLFile> findApplicationYamlFiles(Project project) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        List<YAMLFile> result = new ArrayList<>();
        for (String name : CANDIDATES) {
            PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, name, scope);
            Arrays.sort(psiFiles, (a, b) -> {
                String pa = a.getVirtualFile().getPath();
                String pb = b.getVirtualFile().getPath();
                int wa = pa.contains("/src/main/resources") ? 0 : (pa.contains("/resources") ? 1 : 2);
                int wb = pb.contains("/src/main/resources") ? 0 : (pb.contains("/resources") ? 1 : 2);
                return Integer.compare(wa, wb);
            });
            for (PsiFile psi : psiFiles) {
                if (psi instanceof YAMLFile) result.add((YAMLFile) psi);
            }
        }
        return result;
    }

    /**
     * Searches dotPath inside YAML PSI. Returns YAMLKeyValue if it exists.
     */
    private static YAMLKeyValue findKeyInYamlPsi(YAMLFile yamlFile, String dotPath) {
        List<String> parts = Arrays.asList(dotPath.split("\\."));
        YAMLMapping root = PsiTreeUtil.findChildOfType(yamlFile, YAMLMapping.class);
        if (root == null) return null;

        YAMLMapping currentMapping = root;
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            YAMLKeyValue kv = currentMapping.getKeyValueByKey(part);
            if (kv == null) {
                return null;
            }
            if (i == parts.size() - 1) return kv;
            // go deeper
            YAMLValue value = kv.getValue();
            if (value instanceof YAMLMapping) {
                currentMapping = (YAMLMapping) value;
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Creates (or updates) the application.yaml file so that it contains the dotPath hierarchy.
     */
    public static void createMissingPathInYaml(Project project, String dotPath) {
        YAMLFile yamlFile = chooseOrCreateApplicationYaml(project);
        if (yamlFile == null) return;
        YAMLElementGenerator generator = YAMLElementGenerator.getInstance(project);

        WriteCommandAction.runWriteCommandAction(project, () -> {
            YAMLMapping root = PsiTreeUtil.findChildOfType(yamlFile, YAMLMapping.class);

            if (root == null) {
                // Empty file -> insert full block
                String text = buildYamlText(Arrays.asList(dotPath.split("\\.")));
                YAMLFile dummy = generator.createDummyYamlWithText(text);
                YAMLMapping newMap = PsiTreeUtil.findChildOfType(dummy, YAMLMapping.class);
                if (newMap != null) {
                    yamlFile.add(newMap);
                } else {
                    // fallback: write text directly to document
                    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
                    documentManager.getDocument(yamlFile).setText(text);
                    documentManager.commitAllDocuments();
                }
                return;
            }

            // Root mapping exists – iteratively insert missing levels
            YAMLMapping current = root;
            String[] parts = dotPath.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                String key = parts[i];
                YAMLKeyValue existing = current.getKeyValueByKey(key);
                if (existing == null) {
                    if (i == parts.length - 1) {
                        // last level: add a key without value
                        YAMLKeyValue newKv = generator.createYamlKeyValue(key, "");
                        current.addBefore(newKv, current.getLastChild());
                    } else {
                        // create submap for the remainder
                        List<String> remaining = new ArrayList<>();
                        for (int j = i; j < parts.length; j++) remaining.add(parts[j]);
                        String text = buildYamlText(remaining);
                        YAMLFile dummy = generator.createDummyYamlWithText(text);
                        YAMLKeyValue firstKv = PsiTreeUtil.findChildOfType(dummy, YAMLKeyValue.class);
                        if (firstKv != null) {
                            current.addBefore(firstKv, current.getLastChild());
                            // move current to new mapping
                            YAMLKeyValue justAdded = current.getKeyValueByKey(key);
                            if (justAdded != null && justAdded.getValue() instanceof YAMLMapping) {
                                current = (YAMLMapping) justAdded.getValue();
                                // continue loop from next index
                                continue;
                            }
                        } else {
                            // fallback: add empty key
                            YAMLKeyValue newKv = generator.createYamlKeyValue(key, "");
                            current.addBefore(newKv, current.getLastChild());
                        }
                    }
                } else {
                    // existing key – go deeper if mapping
                    YAMLValue val = existing.getValue();
                    if (val instanceof YAMLMapping) {
                        current = (YAMLMapping) val;
                    } else {
                        // replace value with a map (carefully) – using dummy
                        String text = buildYamlText(Arrays.asList(parts).subList(i, parts.length));
                        YAMLFile dummy = generator.createDummyYamlWithText(text);
                        YAMLKeyValue newKv = PsiTreeUtil.findChildOfType(dummy, YAMLKeyValue.class);
                        if (newKv != null) {
                            existing.replace(newKv);
                            YAMLValue newVal = existing.getValue();
                            if (newVal instanceof YAMLMapping) current = (YAMLMapping) newVal;
                        } else {
                            // nothing
                            return;
                        }
                    }
                }
            }
            PsiDocumentManager.getInstance(project).commitAllDocuments();
        });
    }

    private static String buildYamlText(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        for (int i = 0; i < parts.size(); i++) {
            for (int j = 0; j < indent; j++) sb.append("  ");
            sb.append(parts.get(i)).append(":\n");
            indent++;
        }
        return sb.toString();
    }

    private static YAMLFile chooseOrCreateApplicationYaml(Project project) {
        List<YAMLFile> existing = findApplicationYamlFiles(project);
        if (!existing.isEmpty()) return existing.get(0);

        String basePath = project.getBasePath();
        if (basePath == null) return null;
        String resourcesPath = basePath + "/src/main/resources";
        try {
            com.intellij.openapi.vfs.VirtualFile resourcesDir = VfsUtil.createDirectories(resourcesPath);
            String filePath = resourcesPath + "/application.yaml";
            LocalFileSystem lfs = LocalFileSystem.getInstance();
            com.intellij.openapi.vfs.VirtualFile vf = lfs.findFileByPath(filePath);
            if (vf == null) {
                vf = resourcesDir.createChildData(YamlPsiUtils.class, "application.yaml");
                VfsUtil.saveText(vf, "# created by YAML Buddy\n");
            }
            PsiFile psi = PsiManager.getInstance(project).findFile(vf);
            if (psi instanceof YAMLFile) return (YAMLFile) psi;
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
