package com.pcoates33;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.testIntegration.TestFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class DrlTestFinder implements TestFinder {

    public static final String DRL_TEST_ANNOTATION = "DrlTest";

    @Nullable
    @Override
    public PsiElement findSourceElement(@NotNull PsiElement from) {

        if (isDrlFile(from)) {
            return from.getContainingFile();
        }

        return null;
    }

    private boolean isDrlFile(PsiElement element) {
        try {
            String fileExtension = element
                    .getContainingFile()
                    .getVirtualFile()
                    .getExtension();
            if (fileExtension.equalsIgnoreCase("drl")) {
                return true;
            }
        } catch (Exception ex) {
            // not much we can do with this
            return false;
        }
        return false;
    }

    /**
     * If the element is a .drl file then find tests which have the DrlTest annotation with the drl file in the files list.
     *
     * @param element in the file we're trying to navigate from.
     * @return List of test files found.
     */
    @NotNull
    @Override
    public Collection<PsiElement> findTestsForClass(@NotNull PsiElement element) {

        if (!isDrlFile(element)) {
            return Collections.emptyList();
        }

        GlobalSearchScope allScope = ProjectScope.getAllScope(element.getProject());

        // Find the DrlTest annotation
        PsiClass[] matchingAnnotations = PsiShortNamesCache
                .getInstance(element.getProject())
                .getClassesByName(DRL_TEST_ANNOTATION, allScope);

        if (matchingAnnotations.length == 0) {
            return Collections.emptyList();
        }

        // Now find classes annotated with DrlTest
        final PsiClass drlTestAnnotation = matchingAnnotations[0];
        final String drlTestAnnotationFqn = drlTestAnnotation.getQualifiedName();
        Collection<PsiClass> annotatedClasses = AnnotatedElementsSearch
                .searchPsiClasses(drlTestAnnotation, allScope)
                .findAll();

        // Now find the annotated tests which have our drl file in the list of tiles
        final String drlFile = element
                .getContainingFile()
                .getVirtualFile()
                .getName();

        Collection<PsiElement> classesWIthDrlFile = annotatedClasses
                .stream()
                .filter(psiClass -> isDrlFilePresent(drlTestAnnotationFqn, drlFile, psiClass))
                .map(psiClass -> (PsiElement) psiClass)
                .collect(Collectors.toList());

        return classesWIthDrlFile;
    }

    private boolean isDrlFilePresent(String drlTestAnnotationFqn, String drlFile, PsiClass psiClass) {
        PsiAnnotation annotation = psiClass.getAnnotation(drlTestAnnotationFqn);
        if (annotation == null) {
            return false;
        }
        PsiAnnotationMemberValue files = annotation.findAttributeValue("files");

        if (files == null) {
            return false;
        }
        if (files instanceof PsiLiteralExpressionImpl) {
            // handle single file in the files= parameter
            return checkForFile(drlFile, (PsiLiteralExpressionImpl) files);
        } else {
            // handle list of files in the files= parameter
            return Arrays
                    .stream(files.getChildren())
                    .filter(PsiLiteralExpressionImpl.class::isInstance)
                    .anyMatch(value -> checkForFile(drlFile, (PsiLiteralExpressionImpl) value));
        }
    }

    private boolean checkForFile(String fileName, PsiLiteralExpressionImpl files) {
        // TODO : maybe trim value to just get the bit after the /
        // or even just do ends with. But, probably want to add in a starting /

        return fileName.equalsIgnoreCase(files.getInnerText());
    }

    @NotNull
    @Override
    public Collection<PsiElement> findClassesForTest(@NotNull PsiElement element) {
        if (element.getContainingFile() instanceof PsiJavaFile) {
            // Find the DrlTest annotation
            final PsiClass drlTestAnnotation = findDrlTestAnnotation(element);
            if (drlTestAnnotation == null) {
                return Collections.emptyList();
            }

            // Now find classes annotated with DrlTest
            final String drlTestAnnotationFqn = drlTestAnnotation.getQualifiedName();

            PsiJavaFile containingFile = (PsiJavaFile) element.getContainingFile();
            PsiClass[] classes = containingFile.getClasses();
            if (classes.length == 0) {
                return Collections.emptyList();
            }

            // get the class.
            PsiClass mainClass = containingFile.getClasses()[0];
            PsiAnnotation annotation = mainClass.getAnnotation(drlTestAnnotationFqn);
            if (annotation == null) {
                return Collections.emptyList();
            }

            PsiAnnotationMemberValue files = annotation.findAttributeValue("files");
            final List<PsiElement> sourceFileList = new ArrayList<>();
            if (files != null) {
                final Project project = element.getProject();
                if (files instanceof PsiLiteralExpressionImpl) {
                    addFiles(sourceFileList, project, (PsiLiteralExpressionImpl) files);
                } else {
                    for (PsiElement filesElement : files.getChildren()) {
                        if (filesElement instanceof PsiLiteralExpressionImpl) {
                            addFiles(sourceFileList, project, (PsiLiteralExpressionImpl) filesElement);
                        }
                    }
                }
            }
            return sourceFileList;

        }
        return Collections.emptyList();
    }

    private PsiClass findDrlTestAnnotation(PsiElement element) {
        GlobalSearchScope allScope = ProjectScope.getAllScope(element.getProject());

        PsiClass[] matchingAnnotations = PsiShortNamesCache
                .getInstance(element.getProject())
                .getClassesByName(DRL_TEST_ANNOTATION, allScope);

        return (matchingAnnotations.length == 0) ? null : matchingAnnotations[0];
    }

    private void addFiles(List<PsiElement> sourceFiles, Project project, PsiLiteralExpressionImpl name) {
        GlobalSearchScope searchScope = ProjectScope.getAllScope(project);
        sourceFiles.addAll(Arrays.asList(FilenameIndex.getFilesByName(project, name.getInnerText(), searchScope)));
    }

    @Override
    public boolean isTest(@NotNull PsiElement element) {
        return false;
    }
}
