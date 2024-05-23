package com.github.mauricioaniche.ck.metric;

import static com.github.mauricioaniche.ck.util.WordCounter.removeSpacesAndIdentation;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.util.WordCounter;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ClassLevelWordCounter implements CKASTVisitor, ClassLevelMetric {

  private String classSourceCode;

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof TypeDeclaration
        || node instanceof AnonymousClassDeclaration
        || node instanceof EnumDeclaration) {
      setOrRemoveSourceCode(node.toString());
    }
  }

  private void setOrRemoveSourceCode(String typeSourceCode) {
    if (classSourceCode == null) {
      classSourceCode = removeSpacesAndIdentation(typeSourceCode);
    } else {
      String otherType = removeSpacesAndIdentation(typeSourceCode);
      classSourceCode = removeSpacesAndIdentation(classSourceCode.replace(otherType, ""));
    }
  }

  @Override
  public void setResult(CKClassResult result) {
    int qtyOfUniqueWords = WordCounter.wordsIn(classSourceCode).size();
    result.setUniqueWordsQty(qtyOfUniqueWords);
  }
}
