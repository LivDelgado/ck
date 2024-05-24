package com.github.mauricioaniche.ck.metric;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.dom.*;

public class WMC implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {

  protected int cc = 0;
  // this stack helps us in knowing whether we could evaluate InfixExpressions or not
  // if we count the conditions directly in the branch node (e.g., if, for, ...), then
  // do not need to (re-)count the Infix agains
  // this is needed for some inline conditions, e.g., boolean x = a > 10;
  private final LinkedList<ASTNode> stack = new LinkedList<>();

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof MethodDeclaration
        || node instanceof Initializer
        || node instanceof CatchClause) increaseCc();
    else if (node instanceof IfStatement nodeT)
      increaseFromExpression(nodeT.getExpression(), nodeT);
    else if (node instanceof WhileStatement nodeT)
      increaseFromExpression(nodeT.getExpression(), nodeT);
    else if (node instanceof DoStatement nodeT)
      increaseFromExpression(nodeT.getExpression(), nodeT);
    else if (node instanceof ConditionalExpression nodeT)
      increaseFromExpression(nodeT.getExpression(), nodeT);
    else if (node instanceof ForStatement nodeT)
      increaseFromExpression(nodeT.getExpression(), nodeT);
    else if (node instanceof EnhancedForStatement nodeT)
      increaseFromExpression(nodeT.getExpression(), nodeT);
    else if (node instanceof InfixExpression nodeT) internalVisit(nodeT);
    else if (node instanceof SwitchCase nodeT) internalVisit(nodeT);
  }

  @Override
  public <T extends ASTNode> void endVisit(T node) {
    if (node instanceof ForStatement
        || node instanceof EnhancedForStatement
        || node instanceof ConditionalExpression
        || node instanceof DoStatement
        || node instanceof WhileStatement
        || node instanceof SwitchCase
        || node instanceof IfStatement) stack.pop();
    else if (node instanceof MethodDeclaration) stack.clear();
  }

  private void internalVisit(SwitchCase node) {
    if (!node.isDefault()) {
      increaseCCFromExpression(node.getExpression());
    }

    stack.push(node);
  }

  private void internalVisit(InfixExpression node) {

    if (stack.isEmpty()) {
      Set<InfixExpression.Operator> operatorsToConsider =
          new HashSet<InfixExpression.Operator>() {
            {
              add(InfixExpression.Operator.LESS);
              add(InfixExpression.Operator.GREATER);
              add(InfixExpression.Operator.LESS_EQUALS);
              add(InfixExpression.Operator.GREATER_EQUALS);
              add(InfixExpression.Operator.EQUALS);
              add(InfixExpression.Operator.NOT_EQUALS);
            }
          };

      if (operatorsToConsider.contains(node.getOperator())) increaseCc();
    }
  }

  private void increaseFromExpression(Expression expression, ASTNode node) {
    increaseCCFromExpression(expression);
    stack.push(node);
  }

  private int increaseCCFromExpression(Expression expression) {
    if (expression == null) {
      increaseCc();
      return 0;
    }

    if (!containsIfTenary(expression)) {
      increaseCc();
    }

    String expr = expression.toString().replace("&&", "&").replace("||", "|");
    int ands = StringUtils.countMatches(expr, "&");
    int ors = StringUtils.countMatches(expr, "|");

    increaseCc(ands + ors);
    return ands + ors;
  }

  private boolean containsIfTenary(Expression expression) {
    if (expression instanceof ParenthesizedExpression x) {
      return containsIfTenary(x.getExpression());
    } else if (expression instanceof InfixExpression x) {
      return containsIfTenary(x.getLeftOperand()) || containsIfTenary(x.getRightOperand());
    } else if (expression instanceof ConditionalExpression) {
      return true;
    }

    return false;
  }

  private void increaseCc() {
    increaseCc(1);
  }

  protected void increaseCc(int qtd) {
    cc += qtd;
  }

  @Override
  public void setResult(CKClassResult result) {
    result.setWmc(cc);
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setWmc(cc);
  }
}
