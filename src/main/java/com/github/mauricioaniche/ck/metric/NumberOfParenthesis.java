package com.github.mauricioaniche.ck.metric;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;

public class NumberOfParenthesis implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {

  private int qty = 0;

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof ParenthesizedExpression) qty++;
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setParenthesizedExpsQty(qty);
  }

  @Override
  public void setResult(CKClassResult result) {
    result.setParenthesizedExpsQty(qty);
  }
}
