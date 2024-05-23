package com.github.mauricioaniche.ck.metric;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import java.util.Stack;
import org.eclipse.jdt.core.dom.*;

public class NumberOfMaxNestedBlock implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {

  private int current = 0;
  private int max = 0;
  private final Stack<ASTNode> currentNode = new Stack<>();
  private final Stack<Boolean> blocks = new Stack<>();
  private final Stack<Boolean> nodes = new Stack<>();

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof Block nodeT) internalVisit(nodeT);
    else if (node instanceof ForStatement nodeT) internalVisit(nodeT);
    else if (node instanceof EnhancedForStatement nodeT) internalVisit(nodeT);
    else if (node instanceof DoStatement nodeT) internalVisit(nodeT);
    else if (node instanceof WhileStatement nodeT) internalVisit(nodeT);
    else if (node instanceof SwitchStatement nodeT) internalVisit(nodeT);
    else if (node instanceof SwitchCase nodeT) internalVisit(nodeT);
    else if (node instanceof CatchClause nodeT) internalVisit(nodeT);
    else if (node instanceof IfStatement nodeT) internalVisit(nodeT);
  }

  @Override
  public <T extends ASTNode> void endVisit(T node) {
    if (node instanceof Block nodeT) internalEndVisit(nodeT);
    else if (node instanceof IfStatement
        || node instanceof CatchClause
        || node instanceof WhileStatement
        || node instanceof DoStatement
        || node instanceof ForStatement
        || node instanceof EnhancedForStatement
        || node instanceof SwitchStatement) popBlock();
  }

  // region internalVisit

  void internalVisit(Block node) {

    // we always do a +1 if we see a block, with the exception of
    // a switch case, as we do the +1 in the block.
    // note that blocks might not always exist, and that's why we check
    // for their existence at every node later on...
    // if they do not exist, we +1 in the node.
    if (currentNode.empty() || !(currentNode.peek() instanceof SwitchCase)) {
      plusOne();
      blocks.push(true);
    } else {
      blocks.push(false);
    }

    currentNode.push(node);
  }

  void internalVisit(ForStatement node) {
    currentNode.push(node);

    boolean containsBlock = containsBlock(node.getBody());
    if (!containsBlock) {
      plusOne();
      nodes.push(true);
    } else {
      nodes.push(false);
    }
  }

  void internalVisit(EnhancedForStatement node) {

    currentNode.push(node);

    boolean containsBlock = containsBlock(node.getBody());
    if (!containsBlock) {
      plusOne();
      nodes.push(true);
    } else {
      nodes.push(false);
    }
  }

  void internalVisit(DoStatement node) {
    currentNode.push(node);

    boolean containsBlock = containsBlock(node.getBody());
    if (!containsBlock) {
      plusOne();
      nodes.push(true);
    } else {
      nodes.push(false);
    }
  }

  void internalVisit(WhileStatement node) {
    currentNode.push(node);

    boolean containsBlock = containsBlock(node.getBody());
    if (!containsBlock) {
      plusOne();
      nodes.push(true);
    } else {
      nodes.push(false);
    }
  }

  void internalVisit(SwitchStatement node) {

    currentNode.push(node);
    nodes.push(true);
    plusOne();
  }

  void internalVisit(SwitchCase node) {
    currentNode.push(node);
  }

  void internalVisit(CatchClause node) {

    currentNode.push(node);

    boolean containsBlock = containsBlock(node.getBody());
    if (!containsBlock) {
      plusOne();
      nodes.push(true);
    } else {
      nodes.push(false);
    }
  }

  void internalVisit(IfStatement node) {

    currentNode.push(node);

    boolean containsBlock = containsBlock(node.getThenStatement());
    if (!containsBlock) {
      plusOne();
      nodes.push(true);
    } else {
      nodes.push(false);
    }
  }

  void internalEndVisit(Block node) {
    Boolean pop = blocks.pop();
    if (pop) current--;

    currentNode.pop();
  }

  // endregion

  private boolean containsBlock(Statement body) {
    return (body instanceof Block);
  }

  private void plusOne() {
    current++;
    max = Math.max(current, max);
  }

  private void popBlock() {
    Boolean pop = nodes.pop();
    if (pop) current--;
  }

  @Override
  public void setResult(CKMethodResult result) {
    // -1 because the method block is considered a block.
    // and we avoid 0, that can happen in case of enums
    result.setMaxNestedBlocks(Math.max(0, max - 1));
  }

  @Override
  public void setResult(CKClassResult result) {
    result.setMaxNestedBlocks(Math.max(0, max - 1));
  }
}
