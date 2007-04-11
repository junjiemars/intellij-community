/*
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Dec 17, 2001
 * Time: 2:40:54 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.intellij.codeInspection.reference;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

public class SmartRefElementPointerImpl implements SmartRefElementPointer {
  @NonNls public static final String FQNAME_ATTR = "FQNAME";
  @NonNls public static final String TYPE_ATTR = "TYPE";
  @NonNls public static final String ENTRY_POINT = "entry_point";

  private final boolean myIsPersistent;
  private RefEntity myRefElement;
  private String myFQName;
  private String myType;
  private static final Logger LOG = Logger.getInstance("#" + SmartRefElementPointerImpl.class.getName());

  public SmartRefElementPointerImpl(RefEntity ref, boolean isPersistent) {
      myIsPersistent = isPersistent;
      myRefElement = ref;
      if (ref instanceof RefImplicitConstructor) {
        ref = ((RefImplicitConstructor)ref).getOwnerClass();
      }

      myFQName = ref.getExternalName();

      if (ref instanceof RefMethod) {
        myType = METHOD;
      } else if (ref instanceof RefClass) {
        myType = CLASS;
      } else if (ref instanceof RefField) {
        myType = FIELD;
      } else if (ref instanceof RefFile) {
        myType = FILE;
      } else if (ref instanceof RefParameter) {
        myType = PARAMETER;
      } else if (ref instanceof RefPackage) {
        myType = PACKAGE;
      } else if (ref instanceof RefModule) {
        myType = MODULE;
      } else if (ref instanceof RefProject) {
        myType = PROJECT;
      } else {
        myType = null;
      }
    }

  public SmartRefElementPointerImpl(Element jDomElement) {
    myIsPersistent = true;
    myRefElement = null;
    myFQName = jDomElement.getAttributeValue(FQNAME_ATTR);
    String type = jDomElement.getAttributeValue(TYPE_ATTR);

    initType(type);
  }

  public SmartRefElementPointerImpl(final String type, final String fqName) {
     myIsPersistent = true;
     myFQName = fqName;
     myType = type;
     initType(type);
   }

  private void initType(final String type) {
    if (METHOD.equals(type)) {
      myType = METHOD;
    } else if (CLASS.equals(type)) {
      myType = CLASS;
    } else if (FIELD.equals(type)) {
      myType = FIELD;
    } else if (FILE.equals(type)) {
      myType = FILE;
    } else if (PARAMETER.equals(type)) {
      myType = PARAMETER;
    } else if (PACKAGE.equals(type)) {
      myType = PACKAGE;
    } else if (MODULE.equals(type)) {
      myType = MODULE;
    } else if (PROJECT.equals(type)) {
      myType = PROJECT;
    } else {
      myType = null;
    }
  }

  public SmartRefElementPointerImpl(final String type, final String fqName, final RefManager manager) {
    myIsPersistent = false;
    myFQName = fqName;
    initType(type);
    resolve(manager);
  }

  public boolean isPersistent() {
    return myIsPersistent;
  }

  public String getFQName() {
    return myFQName;
  }

  public RefEntity getRefElement() {
    return myRefElement;
  }

  public void writeExternal(Element parentNode) {
    Element element = new Element(ENTRY_POINT);
    element.setAttribute(TYPE_ATTR, myType);
    element.setAttribute(FQNAME_ATTR, getFQName());
    if (myRefElement != null) {
      final RefEntity entity = myRefElement.getOwner();
      if (entity instanceof RefElement) {
        final RefElement elementOwner = findSyntheticJSP((RefElement)entity);
        if (elementOwner.isSyntheticJSP()) {
          final PsiFile psiFile = PsiUtil.getJspFile(elementOwner.getElement());
          if (psiFile != null) {
            final VirtualFile virtualFile = psiFile.getVirtualFile();
            LOG.assertTrue(virtualFile != null);
            new SmartRefElementPointerImpl(FILE, virtualFile.getUrl(), elementOwner.getRefManager()).writeExternal(parentNode);
            return;
          }
        }
        else {
          new SmartRefElementPointerImpl(entity, myIsPersistent).writeExternal(element);
        }
      }
    }
    parentNode.addContent(element);
  }

  private static RefElement findSyntheticJSP(RefElement refElement) {
    if (refElement.isSyntheticJSP()) return refElement;
    final RefEntity entity = refElement.getOwner();
    if (entity instanceof RefElement) {
      return findSyntheticJSP((RefElement)entity);
    }
    return refElement;
  }

  public boolean resolve(RefManager manager) {
    if (myRefElement != null) {
      if (myRefElement instanceof RefElement && myRefElement.isValid()) return true;
      return false;
    }

    if (METHOD.equals(myType)) {
      myRefElement = RefMethodImpl.methodFromExternalName(manager, getFQName());
    } else if (CLASS.equals(myType)) {
      myRefElement = RefClassImpl.classFromExternalName(manager, getFQName());
    } else if (FIELD.equals(myType)) {
      myRefElement = RefFieldImpl.fieldFromExternalName(manager, getFQName());
    } else if (FILE.equals(myType)) {
      myRefElement = RefFileImpl.fileFromExternalName(manager, getFQName());
    } else if (PARAMETER.equals(myType)) {
      myRefElement = RefParameterImpl.parameterFromExternalName(manager, getFQName());
    } else if (MODULE.equals(myType)) {
      myRefElement = RefModuleImpl.moduleFromName(manager, getFQName());
    } else if (PACKAGE.equals(myType)) {
      myRefElement = RefPackageImpl.packageFromFQName(manager, getFQName());
    } else if (PROJECT.equals(myType)) {
      myRefElement = manager.getRefProject();
    }

    return myRefElement != null;
  }

  public void freeReference() {
    myRefElement = null;
  }
}
