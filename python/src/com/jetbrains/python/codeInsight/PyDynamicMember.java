package com.jetbrains.python.codeInsight;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyStatement;
import com.jetbrains.python.psi.impl.PyElementImpl;
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl;
import com.jetbrains.python.psi.resolve.ResolveImportUtil;
import com.jetbrains.python.psi.stubs.PyClassNameIndex;
import com.jetbrains.python.psi.types.PyClassType;
import com.jetbrains.python.psi.types.PyType;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Dennis.Ushakov
 */
public class PyDynamicMember {
  private String myName;
  private final boolean myResolveToInstance;
  private final String myTypeName;

  private final String myResolveShortName;
  private final String myResolveModuleName;

  public PyDynamicMember(final String name, final String type, final boolean resolveToInstance) {
    this(name, type, type, resolveToInstance);
  }

  public PyDynamicMember(final String name, final String type, final String resolveTo, final boolean resolveToInstance) {
    myName = name;
    myResolveToInstance = resolveToInstance;
    myTypeName = type;

    int split = resolveTo.lastIndexOf('.');
    myResolveShortName = resolveTo.substring(split + 1);
    myResolveModuleName = resolveTo.substring(0, split);
  }

  public String getName() {
    return myName;
  }

  public Icon getIcon() {
    return IconLoader.getIcon("/nodes/method.png");
  }

  @Nullable
  public PsiElement resolve(Project project, PyClass modelClass) {
    PyClass targetClass = PyClassNameIndex.findClass(myTypeName, project);
    if (targetClass != null) {
      return new MyInstanceElement(targetClass, findResolveTarget(modelClass));
    }
    return null;
  }

  @Nullable
  private PsiElement findResolveTarget(PyClass clazz) {
    PsiElement module = ResolveImportUtil.resolveInRoots(clazz, myResolveModuleName);
    if (module instanceof PsiDirectory) {
      module = PyReferenceExpressionImpl.turnDirIntoInit(module);
    }
    if (module == null) return null;
    final PyFile file = (PyFile)module;
    for (PyStatement statement : file.getStatements()) {
      final String name = statement.getName();
      if (myResolveShortName.equals(name)) return statement;
    }
    return module;
  }

  public String getShortType() {
    int pos = myTypeName.lastIndexOf('.');
    return myTypeName.substring(pos+1);
  }

  private class MyInstanceElement extends PyElementImpl implements PyExpression {
    private final PyClass myClass;
    public MyInstanceElement(PyClass clazz, PsiElement resolveTarget) {
      super(resolveTarget != null ? resolveTarget.getNode() : clazz.getNode());
      myClass = clazz;
    }

    public PyType getType() {
      return new PyClassType(myClass, !myResolveToInstance);
    }
  }
}
