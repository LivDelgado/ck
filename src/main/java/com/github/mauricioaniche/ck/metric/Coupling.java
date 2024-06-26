package com.github.mauricioaniche.ck.metric;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.mauricioaniche.ck.metric.CouplingExtras.CouplingClassification;
import com.github.mauricioaniche.ck.util.JDTUtils;

import org.apache.log4j.Category;
import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Coupling implements CKASTVisitor, ClassLevelMetric, MethodLevelMetric {

	private CouplingExtras extras;
	private String className;
	private String methodName;
	
	public Coupling() {
		this.extras = CouplingExtras.getInstance();
	}

	@Override
	public void visit(VariableDeclarationStatement node) {
		if(this.className != null) {
			coupleTo(node.getType(), CouplingClassification.DATA_ABSTRACTION_COUPLING);
		}
	}

	@Override
	public void visit(ClassInstanceCreation node) { //Parameter coupling -> constructor call.

        CouplingClassification category = CouplingClassification.ATOMIC_PARAMETER_COUPLING;
		if(this.className != null) {

            List<Expression> args = node.arguments();

            for (Expression argument : args){
                ITypeBinding binding = argument.resolveTypeBinding();
                if(binding!= null && !binding.isPrimitive()){
                    category = CouplingClassification.OBJECT_PARAMETER_COUPLING;
                }
            }
			
            coupleTo(node.getType(), category);

		} else if(this.methodName != null) {	
			IMethodBinding binding = node.resolveConstructorBinding();
			coupleTo(binding, null);
		} 
	}

	@Override
	public void visit(ArrayCreation node) {
		if(this.className != null) {
			coupleTo(node.getType(), CouplingClassification.DATA_ABSTRACTION_COUPLING);
		}
	}

	@Override
	public void visit(FieldDeclaration node) {
		if(this.className != null) {
			coupleTo(node.getType(), CouplingClassification.DATA_ABSTRACTION_COUPLING);
		}
	}

	public void visit(ReturnStatement node) {
		if(this.className != null){
			if (node.getExpression() != null) {
				coupleTo(node.getExpression().resolveTypeBinding(), CouplingClassification.DATA_ABSTRACTION_COUPLING);
			}
		}
	}

	@Override
	public void visit(TypeLiteral node) {
		if(this.className != null) {
			coupleTo(node.getType(), null);
		}
	}
	
	public void visit(ThrowStatement node) {
		if(this.className != null) {
			if(node.getExpression()!=null)
				coupleTo(node.getExpression().resolveTypeBinding(), null);
		}
	}

	public void visit(TypeDeclaration node) {
		if(this.className != null) {
			ITypeBinding resolvedType = node.resolveBinding();
	
			if(resolvedType!=null) {
				ITypeBinding binding = resolvedType.getSuperclass();
				if (binding != null)
					coupleTo(binding, CouplingClassification.INHERITANCE_COUPLING);
	
				for (ITypeBinding interfaces : resolvedType.getInterfaces()) {
					coupleTo(interfaces, CouplingClassification.INTERFACE_COUPLING);
				}
			} else {
				coupleTo(node.getSuperclassType(), CouplingClassification.INHERITANCE_COUPLING);
				List<Type> list = node.superInterfaceTypes();
				list.forEach(x -> coupleTo(x, CouplingClassification.INTERFACE_COUPLING));
			}
		}

	}

	public void visit(MethodDeclaration node) { // Data abstraction coupling: B methodOfA(X x, Y y) {return new B(...)}
		if(this.className != null) {
			IMethodBinding resolvedMethod = node.resolveBinding();
			if (resolvedMethod != null) {
	
				coupleTo(resolvedMethod.getReturnType(), CouplingClassification.DATA_ABSTRACTION_COUPLING);
	
				for (ITypeBinding param : resolvedMethod.getParameterTypes()) {
					coupleTo(param, CouplingClassification.DATA_ABSTRACTION_COUPLING);
				}
			} else {
				coupleTo(node.getReturnType2(), CouplingClassification.DATA_ABSTRACTION_COUPLING);
				List<TypeParameter> list = node.typeParameters();
				list.forEach(x -> coupleTo(x.getName(), CouplingClassification.DATA_ABSTRACTION_COUPLING));
			}
		}

	}

	@Override
	public void visit(CastExpression node) {
		if(this.className != null) {
			coupleTo(node.getType(), null);
		}

	}

	@Override
	public void visit(InstanceofExpression node) {
		if(this.className != null) {
			coupleTo(node.getRightOperand(), null);
			coupleTo(node.getLeftOperand().resolveTypeBinding(), null);
		}

	}

	@Override
	public void visit(MethodInvocation node) { //parameter coupling 
		
		IMethodBinding binding = node.resolveMethodBinding();
        CouplingClassification category = CouplingClassification.ATOMIC_PARAMETER_COUPLING;
		if(binding!=null) {
			if(this.className != null) {

                List<Expression> args = node.arguments();
                
                for(Expression argument : args){
                    ITypeBinding argBinding = argument.resolveTypeBinding();
                    if(argBinding!= null && !argBinding.isPrimitive()){
                        category = CouplingClassification.OBJECT_PARAMETER_COUPLING;
                    } 
                }

				coupleTo(binding.getDeclaringClass(), category);

			} else if(this.methodName != null) {
				coupleTo(binding, null);
			}
		}

	}

	public void visit(NormalAnnotation node) {
		if(this.className != null) {
			coupleTo(node, null);
		}
	}

	public void visit(MarkerAnnotation node) {
		if(this.className != null) {
			coupleTo(node, null);
		}
	}

	public void visit(SingleMemberAnnotation node) {
		if(this.className != null) {
			coupleTo(node, null);
		}
	}

	public void visit(ParameterizedType node) { // Data abstraction coupling.
		if(this.className != null) {
			
			try {	
				ITypeBinding binding = node.resolveBinding();
				if (binding != null) {
		
					coupleTo(binding, CouplingClassification.DATA_ABSTRACTION_COUPLING);
		
					for (ITypeBinding types : binding.getTypeArguments()) {
						coupleTo(types, CouplingClassification.DATA_ABSTRACTION_COUPLING);
					}
				} else {
					coupleTo(node.getType(), CouplingClassification.DATA_ABSTRACTION_COUPLING);
				}
			} catch (NullPointerException e) {
				// TODO: handle exception
			}
		}

	}
	private void coupleTo(Annotation type, CouplingClassification category) {
		if(this.className != null) {
			ITypeBinding resolvedType = type.resolveTypeBinding();
			if(resolvedType!=null)
				coupleTo(resolvedType, category);
			else {
				addToSet(type.getTypeName().getFullyQualifiedName(), category);
			}
		}
	}

	private void coupleTo(Type type, CouplingClassification category) {
		if(type==null)
			return;

		if(this.className != null) {
			ITypeBinding resolvedBinding = type.resolveBinding();
			if(resolvedBinding!=null)
				coupleTo(resolvedBinding, category);
			else {
				if(type instanceof SimpleType) {
					SimpleType castedType = (SimpleType) type;
					addToSet(castedType.getName().getFullyQualifiedName(), category);
				}
				else if(type instanceof QualifiedType) {
					QualifiedType castedType = (QualifiedType) type;
					addToSet(castedType.getName().getFullyQualifiedName(), category);
				}
				else if(type instanceof NameQualifiedType) {
					NameQualifiedType castedType = (NameQualifiedType) type;
					addToSet(castedType.getName().getFullyQualifiedName(), category);
				}
				else if(type instanceof ParameterizedType) {
					ParameterizedType castedType = (ParameterizedType) type;
					coupleTo(castedType.getType(), null);
				}
				else if(type instanceof WildcardType) {
					WildcardType castedType = (WildcardType) type;
					coupleTo(castedType.getBound(), null);
				}
				else if(type instanceof ArrayType) {
					ArrayType castedType = (ArrayType) type;
					coupleTo(castedType.getElementType(), null);
				}
				else if(type instanceof IntersectionType) {
					IntersectionType castedType = (IntersectionType) type;
					List<Type> types = castedType.types();
					types.stream().forEach(x -> coupleTo(x, null));
				}
				else if(type instanceof UnionType) {
					UnionType castedType = (UnionType) type;
					List<Type> types = castedType.types();
					types.stream().forEach(x -> coupleTo(x, null));
				}
			}
		}
	}

	private void coupleTo(SimpleName name, CouplingClassification category) {
		if(this.className != null) {
			addToSet(name.getFullyQualifiedName(), category);
		}
	}

	private void coupleTo(ITypeBinding binding, CouplingClassification category) {

		if(this.className != null) {
			if (binding == null)
				return;
			if (binding.isWildcardType())
				return;
			if (binding.isNullType())
				return;
	
			String type = binding.getQualifiedName();
			if (type.equals("null"))
				return;
	
			if (isFromJava(type) || binding.isPrimitive())
				return;
	
	
			String cleanedType = cleanClassName(type);
			addToSet(cleanedType, category);

		}
	}
	
	private void coupleTo(IMethodBinding binding, CouplingClassification category) {
		
		if(binding == null)
			return;
		
		String methodNameInvoked = JDTUtils.getQualifiedMethodFullName(binding);
		
		if (methodNameInvoked.equals("null"))
			return;

		if (isFromJava(methodNameInvoked))
			return;
		
		addToSet(methodNameInvoked, category);
		
	}

	private String cleanClassName(String type) {
		// remove possible array(s) in the class name
		String cleanedType = type.replace("[]", "").replace("\\$", ".");

		// remove generics declaration, let's stype with the type
		if(cleanedType.contains("<"))
			cleanedType = cleanedType.substring(0, cleanedType.indexOf("<"));

		return cleanedType;
	}

	private boolean isFromJava(String type) {
		return type.startsWith("java.") || type.startsWith("javax.");
	}

	private void addToSet(String name, CouplingClassification category) {
		if(className != null){
			this.extras.addToSetClassIn(name, this.className);
			this.extras.addToSetClassOut(this.className, name);
            if(category != null) this.extras.addCouplingCategoryBetweenClasses(this.className, name, category);
		} else {
			this.extras.addToSetMethodIn(name, this.methodName);
			this.extras.addToSetMethodOut(this.methodName, name);
		}
	}

	@Override
	public void setResult(CKClassResult result) {
		
	}

	@Override
	public void setResult(CKMethodResult result) {
		
	}
	
	@Override
	public void setClassName(String className) {
		this.className = className;
	}
	
	@Override
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
}
