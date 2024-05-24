package com.github.mauricioaniche.ck.metric;

import com.github.mauricioaniche.ck.CKMethodResult;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class NumberOfParameters implements CKASTVisitor, MethodLevelMetric {

  private int qty = 0;

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof MethodDeclaration nodeT)
      qty = nodeT.parameters() == null ? 0 : nodeT.parameters().size();
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setParametersQty(qty);
  }
}
