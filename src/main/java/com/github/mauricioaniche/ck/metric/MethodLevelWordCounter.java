package com.github.mauricioaniche.ck.metric;

import static com.github.mauricioaniche.ck.util.WordCounter.removeSpacesAndIdentation;

import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.mauricioaniche.ck.util.WordCounter;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class MethodLevelWordCounter implements CKASTVisitor, MethodLevelMetric {
  private String methodSourceCode;

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof MethodDeclaration || node instanceof Initializer)
      this.methodSourceCode = removeSpacesAndIdentation(node.toString());
    else if (node instanceof TypeDeclaration nodeT) internalVisit(nodeT);
  }

  void internalVisit(TypeDeclaration node) {
    String otherType = removeSpacesAndIdentation(node.toString());
    methodSourceCode = removeSpacesAndIdentation(methodSourceCode.replace(otherType, ""));
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setUniqueWordsQty(WordCounter.wordsIn(methodSourceCode).size());
  }
}
