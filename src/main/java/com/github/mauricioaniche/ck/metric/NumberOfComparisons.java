package com.github.mauricioaniche.ck.metric;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;

public class NumberOfComparisons implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {

  private int qty = 0;

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof InfixExpression nodeT) {
      if (nodeT.getOperator() == InfixExpression.Operator.EQUALS
          || nodeT.getOperator() == InfixExpression.Operator.NOT_EQUALS) qty++;
    }
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setComparisonsQty(qty);
  }

  @Override
  public void setResult(CKClassResult result) {
    result.setComparisonsQty(qty);
  }
}
