package com.github.mauricioaniche.ck.metric;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;

public class NumberOfInnerClassesLambdasAndAnonymousClasses
    implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {

  private int anonymousClassesQty = 0;
  private int innerClassesQty = 0;
  private int lambdasQty = 0;

  private String firstFound = null;

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof TypeDeclaration nodeT) internalVisit(nodeT);
    else if (node instanceof EnumDeclaration nodeT) internalVisit(nodeT);
    else if (node instanceof LambdaExpression nodeT) internalVisit(nodeT);
    else if (node instanceof AnonymousClassDeclaration nodeT) internalVisit(nodeT);
  }

  void internalVisit(TypeDeclaration node) {
    if (firstFound == null) firstFound = "type";

    innerClassesQty++;
  }

  void internalVisit(EnumDeclaration node) {
    // we count enum as class declaration!
    innerClassesQty++;

    if (firstFound == null) firstFound = "enum";
  }

  void internalVisit(LambdaExpression node) {
    lambdasQty++;

    if (firstFound == null) firstFound = "lambda";
  }

  void internalVisit(AnonymousClassDeclaration node) {
    anonymousClassesQty++;

    if (firstFound == null) firstFound = "anonymous";
  }

  @Override
  public void setResult(CKClassResult result) {
    // the -1 there is because the main type under analysis here is counted as +1.
    result.setAnonymousClassesQty(anonymousClassesQty - (firstFound.equals("anonymous") ? 1 : 0));
    result.setInnerClassesQty(
        innerClassesQty - (firstFound.equals("type") || firstFound.equals("enum") ? 1 : 0));
    result.setLambdasQty(lambdasQty - (firstFound.equals("lambda") ? 1 : 0));
  }

  @Override
  public void setResult(CKMethodResult result) {
    result.setAnonymousClassesQty(anonymousClassesQty);
    result.setInnerClassesQty(innerClassesQty);
    result.setLambdasQty(lambdasQty);
  }
}
