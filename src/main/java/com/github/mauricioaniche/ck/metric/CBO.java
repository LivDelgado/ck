package com.github.mauricioaniche.ck.metric;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.*;

public class CBO implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {

  private final Set<String> coupling = new HashSet<>();

  Map<Class<? extends ASTNode>, Boolean> nodeTypeFunctionMapper;

  public CBO() {
    this.nodeTypeFunctionMapper = new HashMap<>();
    this.nodeTypeFunctionMapper.put(TypeLiteral.class, true);
    this.nodeTypeFunctionMapper.put(CastExpression.class, true);
    this.nodeTypeFunctionMapper.put(NormalAnnotation.class, true);
    this.nodeTypeFunctionMapper.put(MarkerAnnotation.class, true);
    this.nodeTypeFunctionMapper.put(SingleMemberAnnotation.class, true);
  }

  @Override
  public <T extends ASTNode> void visit(T node) {
    if (node instanceof ClassInstanceCreation nodeT) nodeCouple(nodeT);
    else if (node instanceof ArrayCreation nodeT) nodeCouple(nodeT);
    else if (node instanceof FieldDeclaration nodeT) nodeCouple(nodeT);
    else if (node instanceof TypeLiteral nodeT) nodeCouple(nodeT);
    else if (node instanceof CastExpression nodeT) nodeCouple(nodeT);
    else if (node instanceof NormalAnnotation nodeT) nodeCouple(nodeT);
    else if (node instanceof MarkerAnnotation nodeT) nodeCouple(nodeT);
    else if (node instanceof SingleMemberAnnotation nodeT) nodeCouple(nodeT);
    else if (node instanceof ReturnStatement nodeT) internalVisit(nodeT);
    else if (node instanceof ThrowStatement nodeT) internalVisit(nodeT);
    else if (node instanceof MethodDeclaration nodeT) internalVisit(nodeT);
    else if (node instanceof InstanceofExpression nodeT) internalVisit(nodeT);
    else if (node instanceof MethodInvocation nodeT) internalVisit(nodeT);
    else if (node instanceof ParameterizedType nodeT) internalVisit(nodeT);
  }

  // region nodeCouple
  private void nodeCouple(ClassInstanceCreation node) {
    coupleTo(node.getType());
  }

  private void nodeCouple(ArrayCreation node) {
    coupleTo(node.getType());
  }

  private void nodeCouple(FieldDeclaration node) {
    coupleTo(node.getType());
  }

  private void nodeCouple(TypeLiteral node) {
    coupleTo(node.getType());
  }

  private void nodeCouple(CastExpression node) {
    coupleTo(node.getType());
  }

  private void nodeCouple(NormalAnnotation node) {
    coupleTo(node);
  }

  private void nodeCouple(MarkerAnnotation node) {
    coupleTo(node);
  }

  private void nodeCouple(SingleMemberAnnotation node) {
    coupleTo(node);
  }

  // endregion

  // region internalVisit
  private void internalVisit(ReturnStatement node) {
    if (node.getExpression() != null) {
      coupleTo(node.getExpression().resolveTypeBinding());
    }
  }

  private void internalVisit(ThrowStatement node) {
    if (node.getExpression() != null) coupleTo(node.getExpression().resolveTypeBinding());
  }

  private void internalVisit(MethodDeclaration node) {

    IMethodBinding resolvedMethod = node.resolveBinding();
    if (resolvedMethod != null) {

      coupleTo(resolvedMethod.getReturnType());

      for (ITypeBinding param : resolvedMethod.getParameterTypes()) {
        coupleTo(param);
      }
    } else {
      coupleTo(node.getReturnType2());
      List<TypeParameter> list = node.typeParameters();
      list.forEach(x -> coupleTo(x.getName()));
    }
  }

  private void internalVisit(InstanceofExpression node) {

    coupleTo(node.getRightOperand());
    coupleTo(node.getLeftOperand().resolveTypeBinding());
  }

  private void internalVisit(MethodInvocation node) {

    IMethodBinding binding = node.resolveMethodBinding();
    if (binding != null) coupleTo(binding.getDeclaringClass());
  }

  private void internalVisit(ParameterizedType node) {
    try {
      ITypeBinding binding = node.resolveBinding();
      if (binding != null) {

        coupleTo(binding);

        for (ITypeBinding types : binding.getTypeArguments()) {
          coupleTo(types);
        }
      } else {
        coupleTo(node.getType());
      }
    } catch (NullPointerException e) {
      // TODO: handle exception
    }
  }

  // endregion

  private void coupleTo(Annotation type) {
    ITypeBinding resolvedType = type.resolveTypeBinding();
    if (resolvedType != null) coupleTo(resolvedType);
    else {
      addToSet(type.getTypeName().getFullyQualifiedName());
    }
  }

  private void coupleTo(Type type) {
    if (type == null) return;

    ITypeBinding resolvedBinding = type.resolveBinding();
    if (resolvedBinding != null) coupleTo(resolvedBinding);
    else {
      if (type instanceof SimpleType castedType) {
        addToSet(castedType.getName().getFullyQualifiedName());
      } else if (type instanceof QualifiedType castedType) {
        addToSet(castedType.getName().getFullyQualifiedName());
      } else if (type instanceof NameQualifiedType castedType) {
        addToSet(castedType.getName().getFullyQualifiedName());
      } else if (type instanceof ParameterizedType) {
        ParameterizedType castedType = (ParameterizedType) type;
        coupleTo(castedType.getType());
      } else if (type instanceof WildcardType castedType) {
        coupleTo(castedType.getBound());
      } else if (type instanceof ArrayType castedType) {
        coupleTo(castedType.getElementType());
      } else if (type instanceof IntersectionType castedType) {
        List<Type> types = castedType.types();
        types.stream().forEach(x -> coupleTo(x));
      } else if (type instanceof UnionType castedType) {
        List<Type> types = castedType.types();
        types.stream().forEach(x -> coupleTo(x));
      }
    }
  }

  private void coupleTo(SimpleName name) {
    addToSet(name.getFullyQualifiedName());
  }

  private void coupleTo(ITypeBinding binding) {

    if (binding == null) return;
    if (binding.isWildcardType()) return;
    if (binding.isNullType()) return;

    String type = binding.getQualifiedName();
    if (type.equals("null")) return;

    if (isFromJava(type) || binding.isPrimitive()) return;

    String cleanedType = cleanClassName(type);
    addToSet(cleanedType);
  }

  private String cleanClassName(String type) {
    // remove possible array(s) in the class name
    String cleanedType = type.replace("[]", "").replace("\\$", ".");

    // remove generics declaration, let's stype with the type
    if (cleanedType.contains("<")) cleanedType = cleanedType.substring(0, cleanedType.indexOf("<"));

    return cleanedType;
  }

  private boolean isFromJava(String type) {
    return type.startsWith("java.") || type.startsWith("javax.");
  }

  private void addToSet(String name) {
    this.coupling.add(name);
  }

  @Override
  public void setResult(CKClassResult result) {
    clean();
    result.setCbo(getValue());
  }

  // given that some resolvings might fail, we remove types that might
  // had appeared here twice.
  // e.g. if the set contains 'A.B.Class' and 'Class', it is likely that
  // 'Class' == 'A.B.Class'
  private void clean() {
    Set<String> singleQualifiedTypes =
        coupling.stream().filter(x -> !x.contains(".")).collect(Collectors.toSet());

    for (String singleQualifiedType : singleQualifiedTypes) {
      long count = coupling.stream().filter(x -> x.endsWith("." + singleQualifiedType)).count();

      boolean theSameFullyQualifiedTypeExists = count > 0;
      if (theSameFullyQualifiedTypeExists) coupling.remove(singleQualifiedType);
    }
  }

  @Override
  public void setResult(CKMethodResult result) {
    clean();
    result.setCbo(getValue());
  }

  private int getValue() {
    return coupling.size();
  }
}
