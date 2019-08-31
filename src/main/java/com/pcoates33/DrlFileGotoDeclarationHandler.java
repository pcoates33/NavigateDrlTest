package com.pcoates33;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import org.jetbrains.annotations.Nullable;

public class DrlFileGotoDeclarationHandler extends GotoDeclarationHandlerBase {

    /**
     * If the element text ends with .drl then try and find the drl file and return it.
     */
    @Override
    @Nullable
    public PsiElement getGotoDeclarationTarget(@Nullable PsiElement element, Editor editor) {

        if (element != null && element.getText() != null) {
            String drlFilename = extractDrlFilename(element.getText());
            if (drlFilename != null) {
                GlobalSearchScope allScope = ProjectScope.getAllScope(element.getProject());
                PsiFile[] files = FilenameIndex.getFilesByName(element.getProject(), drlFilename, allScope);
                if (files.length > 0) {
                    return files[0];
                }
            }
        }
        return null;
    }

    String extractDrlFilename(String text) {
        int pos = text.indexOf(".drl");
        if (pos <= 0) {
            return null;
        }

        int start = Math.max(text.lastIndexOf(' ', pos) + 1, Math.max(text.lastIndexOf('"', pos) + 1, Math.max(text.lastIndexOf('\\', pos) + 1, text.lastIndexOf('/', pos) + 1)));
        return text.substring(start, pos + 4);
    }
}
