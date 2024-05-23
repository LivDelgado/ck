package com.github.mauricioaniche.ck.metric;

import java.util.Arrays;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;

public class NumberOfMathOperators implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {

  private int qty = 0;

  static InfixExpression.Operator[] operators =
      new InfixExpression.Operator[] {
        InfixExpression.Operator.TIMES,
        InfixExpression.Operator.DIVIDE,
        InfixExpression.Operator.REMAINDER,
        InfixExpression.Operator.PLUS,
        InfixExpression.Operator.MINUS,
        InfixExpression.Operator.LEFT_SHIFT,
        InfixExpression.Operator.RIGHT_SHIFT_SIGNED,
        InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED
      };

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof InfixExpression nodeT)
      if (Arrays.stream(operators).anyMatch(nodeT.getOperator()::equals)) qty++;
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setMathOperationsQty(qty);
  }

  @Override
  public void setResult(CKClassResult result) {
    result.setMathOperationsQty(qty);
  }
}
