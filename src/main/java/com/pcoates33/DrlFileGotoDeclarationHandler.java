package com.pcoates33;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class DrlFileGotoDeclarationHandler extends GotoDeclarationHandlerBase {

  private static final String TEST_DATA_DIR_ANNOTATION = "TestDataDir";

  /**
   * If the element text ends with .drl then try and find the drl file and return it.
   */
  @Override
  @Nullable
  public PsiElement getGotoDeclarationTarget(@Nullable PsiElement element, Editor editor) {

    if (element != null && element.getText() != null) {
      String jsonFilename = extractFilename(element.getText(), ".json");
      if (jsonFilename != null) {
        return findJsonFile(jsonFilename, element, editor);
      }

      String drlFilename = extractFilename(element.getText(), ".drl");
      if (drlFilename != null) {
        return findDrlFile(drlFilename, element, editor);
      }
    }
    return null;
  }

  private PsiElement findJsonFile(String jsonFilename, PsiElement element, Editor editor) {
    if (element.getContainingFile() instanceof PsiJavaFile) {
      // Find the TestDataDir annotation
      final PsiClass testDataDirAnnotation = findTestDataDirAnnotation(element.getProject());
      if (testDataDirAnnotation == null) {
        return null;
      }

      // Now find the annotation on the class
      final String annotationFqn = testDataDirAnnotation.getQualifiedName();

      PsiJavaFile containingFile = (PsiJavaFile) element.getContainingFile();
      PsiClass[] classes = containingFile.getClasses();
      if (classes.length == 0) {
        return null;
      }

      PsiClass mainClass = containingFile.getClasses()[0];
      PsiAnnotation annotation = mainClass.getAnnotation(annotationFqn);
      if (annotation == null) {
        // could use a more long winded way to find the file.
        return null;
      }

      PsiAnnotationMemberValue folder = annotation.findAttributeValue("value");
      if (folder != null && folder instanceof PsiLiteralExpressionImpl) {
        final Project project = element.getProject();
        String folderName = ((PsiLiteralExpressionImpl) folder).getInnerText();
        String scenario = ""; // TODO : get the scenario name
        PsiMethod parentMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (parentMethod != null) {
          for (PsiStatement statement : ((PsiMethod) parentMethod).getBody().getStatements()) {
            statement.getText();
          }
          String methodCode = ((PsiMethod) parentMethod).getBody().getText();
          int pos = methodCode.indexOf(".forScenario(\"");
          if (pos >= 0) {
            int start = pos + ".forScenario(\"".length();
            int end = methodCode.indexOf("\"", start);
            if (end > start) {
              scenario = methodCode.substring(start, end) + "/";
            }
          }
        }
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        VirtualFile jsonFile = localFileSystem
            .findFileByPath(project.getBasePath() + "/src/test/resources/integration/__files/" + folderName + scenario + jsonFilename);
        try {
          PsiFile file = PsiManager.getInstance(project).findFile(jsonFile);
          if (file != null) {
            return file;
          }
        } catch (IllegalArgumentException ex) {
          // couldn't find the file.
        }
        // so let's try the directory
        if (jsonFile == null) {
          // look for the scenario
          jsonFile = localFileSystem
              .findFileByPath(project.getBasePath() + "/src/test/resources/integration/__files/" + folderName + scenario);
        }
        if (jsonFile == null) {
          // look for the base folder
          jsonFile = localFileSystem
              .findFileByPath(project.getBasePath() + "/src/test/resources/integration/__files/" + folderName);
        }
        try {
          PsiDirectory dir = PsiManager.getInstance(project).findDirectory(jsonFile);
          if (dir != null) {
            return dir;
          }
        } catch (IllegalArgumentException ex) {
          // couldn't find the directory either.
        }
      }

    }
    return null;
  }

// LocalFileSystem.getInstance().findFileByPath("/src/test/resources/integration/__files/calculation/dividends/calculation/devScenarios/scenario1/expected.json");
  private PsiClass findTestDataDirAnnotation(Project project) {
    GlobalSearchScope allScope = ProjectScope.getAllScope(project);

    PsiClass[] matchingAnnotations = PsiShortNamesCache
        .getInstance(project)
        .getClassesByName(TEST_DATA_DIR_ANNOTATION, allScope);

    return (matchingAnnotations.length == 0) ? null : matchingAnnotations[0];
  }

  private PsiElement findDrlFile(String drlFilename, PsiElement element, Editor editor) {
    GlobalSearchScope allScope = ProjectScope.getAllScope(element.getProject());
    PsiFile[] files = FilenameIndex.getFilesByName(element.getProject(), drlFilename, allScope);
    if (files.length > 0) {
      return files[0];
    }
    return null;
  }

  String extractFilename(String text, String fileExtension) {
    int pos = text.indexOf(fileExtension);
    if (pos <= 0) {
      return null;
    }
    // just get text after a space or double quote
    int start = Math.max(text.lastIndexOf(' ', pos) + 1, text.lastIndexOf('"', pos) + 1);
    return text.substring(start, pos + fileExtension.length());
  }

}
