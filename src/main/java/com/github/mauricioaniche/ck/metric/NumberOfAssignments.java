package com.github.mauricioaniche.ck.metric;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;

public class NumberOfAssignments implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {

  private int qty = 0;

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof Assignment) qty++;
    else if (node instanceof VariableDeclarationFragment nodeT) {
      if (nodeT.getInitializer() != null) qty++;
    }
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setAssignmentsQty(qty);
  }

  @Override
  public void setResult(CKClassResult result) {
    result.setAssignmentsQty(qty);
  }
}
