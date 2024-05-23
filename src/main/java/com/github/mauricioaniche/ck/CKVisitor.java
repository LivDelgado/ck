package com.github.mauricioaniche.ck;

import static com.github.mauricioaniche.ck.util.LOCCalculator.calculate;

import com.github.mauricioaniche.ck.metric.CKASTVisitor;
import com.github.mauricioaniche.ck.metric.ClassLevelMetric;
import com.github.mauricioaniche.ck.metric.MethodLevelMetric;
import com.github.mauricioaniche.ck.util.JDTUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import org.eclipse.jdt.core.dom.*;

public class CKVisitor extends ASTVisitor {

  private final String sourceFilePath;
  private int anonymousNumber;
  private int initializerNumber;

  class MethodInTheStack {
    CKMethodResult result;
    List<MethodLevelMetric> methodLevelMetrics;

    MethodInTheStack(CKMethodResult result, List<MethodLevelMetric> methodLevelMetrics) {
      this.result = result;
      this.methodLevelMetrics = methodLevelMetrics;
    }
  }

  class ClassInTheStack {
    CKClassResult result;
    List<ClassLevelMetric> classLevelMetrics;
    Stack<MethodInTheStack> methods;

    ClassInTheStack(CKClassResult result, List<ClassLevelMetric> classLevelMetrics) {
      methods = new Stack<>();
      this.result = result;
      this.classLevelMetrics = classLevelMetrics;
    }
  }

  private final Stack<ClassInTheStack> classes;

  private final Set<CKClassResult> collectedClasses;

  private final CompilationUnit cu;
  private final Callable<List<ClassLevelMetric>> classLevelMetrics;
  private final Callable<List<MethodLevelMetric>> methodLevelMetrics;

  public CKVisitor(
      String sourceFilePath,
      CompilationUnit cu,
      Callable<List<ClassLevelMetric>> classLevelMetrics,
      Callable<List<MethodLevelMetric>> methodLevelMetrics) {
    this.sourceFilePath = sourceFilePath;
    this.cu = cu;
    this.classLevelMetrics = classLevelMetrics;
    this.methodLevelMetrics = methodLevelMetrics;
    this.classes = new Stack<>();
    this.collectedClasses = new HashSet<>();
  }

  @Override
  public boolean visit(TypeDeclaration node) {
    ITypeBinding binding = node.resolveBinding();

    // build a CKClassResult based on the current type
    // declaration we are visiting
    String className =
        binding != null ? binding.getBinaryName() : node.getName().getFullyQualifiedName();
    String type = getTypeOfTheUnit(node);
    int modifiers = node.getModifiers();
    CKClassResult currentClass = new CKClassResult(sourceFilePath, className, type, modifiers);
    currentClass.setLoc(calculate(node.toString()));

    // there might be metrics that use it (even before a class is declared)
    visitInvokeMetrics(node);

    // store everything in a 'class in the stack' data structure
    ClassInTheStack classInTheStack =
        new ClassInTheStack(currentClass, instantiateClassLevelMetricVisitors(className));

    // push it to the stack, so we know the current class we are visiting
    classes.push(classInTheStack);

    // there might be class level metrics that use the TypeDeclaration
    // so, let's run them
    visitClass(node);

    return true;
  }

  @Override
  public void endVisit(TypeDeclaration node) {

    // let's first visit any metrics that might make use of this endVisit
    endVisitClass(node);

    ClassInTheStack completedClass = classes.pop();

    // persist the results of the class level metrics in the result
    completedClass.classLevelMetrics.forEach(m -> m.setResult(completedClass.result));

    // we are done processing this class, so now let's
    // store it in the collected classes set
    collectedClasses.add(completedClass.result);
  }

  @Override
  public boolean visit(MethodDeclaration node) {

    node.resolveBinding();
    String currentMethodName = JDTUtils.getMethodFullName(node);
    String currentQualifiedMethodName = JDTUtils.getQualifiedMethodFullName(node);
    boolean isConstructor = node.isConstructor();

    CKMethodResult currentMethod =
        new CKMethodResult(
            currentMethodName, currentQualifiedMethodName, isConstructor, node.getModifiers());
    currentMethod.setLoc(calculate(node.toString()));
    currentMethod.setStartLine(JDTUtils.getStartLine(cu, node));

    // we add it to the current class we are visiting
    MethodInTheStack methodInTheStack =
        new MethodInTheStack(
            currentMethod, instantiateMethodLevelMetricVisitors(currentQualifiedMethodName));
    classes.peek().methods.push(methodInTheStack);

    visitInvokeMetrics(node);

    return true;
  }

  @Override
  public void endVisit(MethodDeclaration node) {

    // let's first invoke the metrics, because they might use this node
    endVisitInvokeMetrics(node);

    // remove the method from the stack
    MethodInTheStack completedMethod = classes.peek().methods.pop();

    // persist the data of the visitors in the CKMethodResult
    completedMethod.methodLevelMetrics.forEach(m -> m.setResult(completedMethod.result));

    // store its final version in the current class
    classes.peek().result.addMethod(completedMethod.result);
  }

  @Override
  public boolean visit(AnonymousClassDeclaration node) {
    // there might be metrics that use it
    // (even before an anonymous class is created)
    visitInvokeMetrics(node);

    // we give the anonymous class a 'class$AnonymousN' name
    String anonClassName = classes.peek().result.getClassName() + "$Anonymous" + ++anonymousNumber;
    CKClassResult currentClass = new CKClassResult(sourceFilePath, anonClassName, "anonymous", -1);
    currentClass.setLoc(calculate(node.toString()));

    // store everything in a 'class in the stack' data structure
    ClassInTheStack classInTheStack =
        new ClassInTheStack(currentClass, instantiateClassLevelMetricVisitors(anonClassName));

    // push it to the stack, so we know the current class we are visiting
    classes.push(classInTheStack);

    // and there might be metrics that also use the methoddeclaration node.
    // so, let's call them
    return visitInvokeMetrics(node);
  }

  @Override
  public void endVisit(AnonymousClassDeclaration node) {

    endVisitClass(node);

    ClassInTheStack completedClass = classes.pop();

    // persist the results of the class level metrics in the result
    completedClass.classLevelMetrics.forEach(m -> m.setResult(completedClass.result));

    // we are done processing this class, so now let's
    // store it in the collected classes set
    collectedClasses.add(completedClass.result);
  }

  // static blocks
  @Override
  public boolean visit(Initializer node) {

    String currentMethodName = "(initializer " + (++initializerNumber) + ")";

    CKMethodResult currentMethod =
        new CKMethodResult(currentMethodName, currentMethodName, false, node.getModifiers());
    currentMethod.setLoc(calculate(node.toString()));
    currentMethod.setStartLine(JDTUtils.getStartLine(cu, node));

    // we add it to the current class we are visiting
    MethodInTheStack methodInTheStack =
        new MethodInTheStack(
            currentMethod, instantiateMethodLevelMetricVisitors(currentMethodName));
    classes.peek().methods.push(methodInTheStack);

    // and there might be metrics that also use the methoddeclaration node.
    // so, let's call them
    visitInvokeMetrics(node);

    return true;
  }

  @Override
  public void endVisit(Initializer node) {

    // let's first invoke the metrics, because they might use this node
    endVisitInvokeMetrics(node);

    // remove the method from the stack
    MethodInTheStack completedMethod = classes.peek().methods.pop();

    // persist the data of the visitors in the CKMethodResult
    completedMethod.methodLevelMetrics.forEach(m -> m.setResult(completedMethod.result));

    // store its final version in the current class
    classes.peek().result.addMethod(completedMethod.result);
  }

  public boolean visit(EnumDeclaration node) {
    ITypeBinding binding = node.resolveBinding();

    // there might be metrics that use it
    // (even before a enum is declared)
    visitInvokeMetrics(node);

    // build a CKClassResult based on the current type
    // declaration we are visiting
    String className =
        binding != null ? binding.getBinaryName() : node.getName().getFullyQualifiedName();
    String type = "enum";
    int modifiers = node.getModifiers();
    CKClassResult currentClass = new CKClassResult(sourceFilePath, className, type, modifiers);
    currentClass.setLoc(calculate(node.toString()));

    // store everything in a 'class in the stack' data structure
    ClassInTheStack classInTheStack =
        new ClassInTheStack(currentClass, instantiateClassLevelMetricVisitors(className));

    // push it to the stack, so we know the current class we are visiting
    classes.push(classInTheStack);

    // there might be class level metrics that use the TypeDeclaration
    // so, let's run them
    visitClass(node);

    return true;
  }

  @Override
  public void endVisit(EnumDeclaration node) {
    // let's first visit any metrics that might make use of this endVisit
    endVisitClass(node);

    ClassInTheStack completedClass = classes.pop();

    // persist the results of the class level metrics in the result
    completedClass.classLevelMetrics.forEach(m -> m.setResult(completedClass.result));

    // we are done processing this class, so now let's
    // store it in the collected classes set
    collectedClasses.add(completedClass.result);
  }

  private List<ClassLevelMetric> instantiateClassLevelMetricVisitors(String className) {
    try {
      List<ClassLevelMetric> classLevelMetricsList = classLevelMetrics.call();
      classLevelMetricsList.forEach(
          c -> {
            c.setClassName(className);
          });
      return classLevelMetricsList;
    } catch (Exception e) {
      throw new RuntimeException("Could not instantiate class level visitors", e);
    }
  }

  private List<MethodLevelMetric> instantiateMethodLevelMetricVisitors(String methodName) {
    try {
      List<MethodLevelMetric> methods = methodLevelMetrics.call();
      methods.forEach(
          m -> {
            m.setMethodName(methodName);
          });
      return methods;
    } catch (Exception e) {
      throw new RuntimeException("Could not instantiate method level visitors", e);
    }
  }

  public Set<CKClassResult> getCollectedClasses() {
    return collectedClasses;
  }

  private String getTypeOfTheUnit(TypeDeclaration node) {
    return node.isInterface() ? "interface" : (classes.isEmpty() ? "class" : "innerclass");
  }

  // -------------------------------------------------------
  // From here, just delegating the calls to the metrics
  @Override
  public boolean visit(AnnotationTypeDeclaration node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(AnnotationTypeMemberDeclaration node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ArrayAccess node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ArrayCreation node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ArrayInitializer node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ArrayType node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(AssertStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(Assignment node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(Block node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(BlockComment node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(BooleanLiteral node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(BreakStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(CastExpression node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(CatchClause node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(CharacterLiteral node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ClassInstanceCreation node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(CompilationUnit node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ConditionalExpression node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ConstructorInvocation node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ContinueStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(CreationReference node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(Dimension node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(DoStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(EmptyStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(EnhancedForStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(EnumConstantDeclaration node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ExpressionMethodReference node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ExpressionStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(FieldAccess node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(FieldDeclaration node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ForStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(IfStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ImportDeclaration node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(InfixExpression node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(InstanceofExpression node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(IntersectionType node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(LabeledStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(LambdaExpression node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(LineComment node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(MarkerAnnotation node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(MemberRef node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(MemberValuePair node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(MethodRef node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(MethodRefParameter node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(MethodInvocation node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(Modifier node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(NameQualifiedType node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(NormalAnnotation node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(NullLiteral node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(NumberLiteral node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(PackageDeclaration node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ParameterizedType node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ParenthesizedExpression node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(PostfixExpression node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(PrefixExpression node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(PrimitiveType node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(QualifiedName node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(QualifiedType node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ReturnStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SimpleName node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SimpleType node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SingleMemberAnnotation node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SingleVariableDeclaration node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(StringLiteral node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SuperConstructorInvocation node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SuperFieldAccess node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SuperMethodInvocation node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SuperMethodReference node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SwitchCase node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SwitchStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(SynchronizedStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(TagElement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(TextElement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ThisExpression node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(ThrowStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(TryStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(TypeDeclarationStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(TypeLiteral node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(TypeMethodReference node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(TypeParameter node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(UnionType node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(VariableDeclarationExpression node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(VariableDeclarationStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(VariableDeclarationFragment node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(WhileStatement node) {
    return visitInvokeMetrics(node);
  }

  @Override
  public boolean visit(WildcardType node) {
    return visitInvokeMetrics(node);
  }

  // we only visit if we found a type already.
  // TODO: understand what happens with a javadoc in a class. Will the TypeDeclaration come first?
  @Override
  public boolean visit(Javadoc node) {
    return visitInvokeMetrics(node);
  }

  // ---------------------------------------------
  // End visits

  @Override
  public void endVisit(Block node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(FieldAccess node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(ConditionalExpression node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(ForStatement node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(EnhancedForStatement node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(DoStatement node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(WhileStatement node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(SwitchCase node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(IfStatement node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(SwitchStatement node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(CatchClause node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(Javadoc node) {
    endVisitInvokeMetrics(node);
  }

  @Override
  public void endVisit(QualifiedName node) {
    endVisitInvokeMetrics(node);
  }

  // TODO: add all other endVisit blocks

  private void endVisitInvokeMetrics(ASTNode node) {
    if (!classes.empty()) {
      endVisitClass(node);
      if (!classes.peek().methods.isEmpty()) endVisitMethods(node);
    }
  }

  private void endVisitClass(ASTNode node) {
    classes.peek().classLevelMetrics.stream()
        .map(metric -> (CKASTVisitor) metric)
        .forEach(ast -> ast.endVisit(node));
  }

  private void endVisitMethods(ASTNode node) {
    classes.peek().methods.peek().methodLevelMetrics.stream()
        .map(metric -> (CKASTVisitor) metric)
        .forEach(ast -> ast.endVisit(node));
  }

  private boolean visitInvokeMetrics(ASTNode node) {
    if (!classes.empty()) {
      visitClass(node);
      if (!classes.peek().methods.isEmpty()) visitMethod(node);
    }

    return true;
  }

  private void visitClass(ASTNode node) {
    classes.peek().classLevelMetrics.stream()
        .map(metric -> (CKASTVisitor) metric)
        .forEach(ast -> ast.visit(node));
  }

  private void visitMethod(ASTNode node) {
    classes.peek().methods.peek().methodLevelMetrics.stream()
        .map(metric -> (CKASTVisitor) metric)
        .forEach(ast -> ast.visit(node));
  }
}
