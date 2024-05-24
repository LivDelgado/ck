package com.github.mauricioaniche.ck.metric;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class NumberOfVariables implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {
  private int qty = 0;

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof VariableDeclarationFragment) qty++;
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setVariablesQty(qty);
  }

  @Override
  public void setResult(CKClassResult result) {
    result.setVariablesQty(qty);
  }
}
