package com.github.mauricioaniche.ck.util;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JDTUtils {
	private static JDTUtils instance;

	public static JDTUtils getInstance(){
		if(instance == null){
			instance = new JDTUtils();
		}
		return instance;
	}

	public int getStartLine(CompilationUnit cu, MethodDeclaration node) {
		return node.getBody() != null ?
				cu.getLineNumber(node.getBody().getStartPosition()) :
				cu.getLineNumber(node.getStartPosition());
	}

	public int getStartLine(CompilationUnit cu, Initializer node) {
		return node.getBody() != null ?
				cu.getLineNumber(node.getBody().getStartPosition()) :
				cu.getLineNumber(node.getStartPosition());
	}

	//Get the method name with parameter count and types, e.g. m1/1[int]
	public String getMethodFullName(IMethodBinding binding) {
		String methodName = binding.getName();
		return methodName + "/" + getMethodSignature(binding);
	}

	public String getMethodFullName(MethodDeclaration node) {
		if(node.resolveBinding() != null){
			return getMethodFullName(node.resolveBinding());
		}
		String methodName = node.getName().getFullyQualifiedName();
		return methodName + "/" + getMethodSignature(node);
	}

	//Get the fully qualified method name with parameter count and types, e.g. rfc.GO.m1/1[int]
	public String getQualifiedMethodFullName(IMethodBinding binding){
		String methodName = binding.getName();
		if(binding.getDeclaringClass() != null){
			methodName = binding.getDeclaringClass().getQualifiedName() + "." + binding.getName();
		}
		return methodName + "/" + getMethodSignature(binding);
	}

	//Get the fully qualified method name with parameter count and types, e.g. rfc.GO.m1/1[int]
	public String getQualifiedMethodFullName(MethodDeclaration node) {
		if(node.resolveBinding() != null){
			return getQualifiedMethodFullName(node.resolveBinding());
		}
		String methodName = node.getName().getFullyQualifiedName();
		return methodName + "/" + getMethodSignature(node);
	}

	//Get the fully qualified method name with parameter count and types, e.g. rfc.GO.m1/1[int]
	public String getQualifiedMethodFullName(MethodInvocation node) {
		IMethodBinding binding = node.resolveMethodBinding();
		if(binding != null){
			return getQualifiedMethodFullName(binding);
		} else {
			return node.getName().getFullyQualifiedName() + "/" + getMethodSignature(node.arguments(), node.typeArguments());
		}
	}

	//Get the fully qualified method name with parameter count and types, e.g. rfc.GO.m1/1[int]
	public String getQualifiedMethodFullName(SuperMethodInvocation node) {
		IMethodBinding binding = node.resolveMethodBinding();
		if(binding != null){
			return getQualifiedMethodFullName(binding);
		} else if(node.getQualifier() != null){
			return node.getQualifier().getFullyQualifiedName() + getMethodSignature(node.arguments(), node.typeArguments());
		}
		return node.getName().getFullyQualifiedName() + "/" + getMethodSignature(node.arguments(), node.typeArguments());
	}

	//Get the signature of a method with parameter count and types, e.g. 1[int]
	public String getMethodSignature(IMethodBinding node){
		int parameterCount = node.getParameterTypes()==null ? 0 : node.getParameterTypes().length;
		List<String> parameterTypes = new ArrayList<>();

		if(parameterCount > 0) {
			for(ITypeBinding binding : node.getParameterTypes()) {

				String v = binding.getQualifiedName();

				parameterTypes.add(v);
			}
		}
		return formatSignature(parameterTypes);
	}

	//Get the signature of a method with parameter count and types, e.g. 1[int]
	public String getMethodSignature(MethodDeclaration node){
		int parameterCount = node.parameters()==null ? 0 : node.parameters().size();
		List<String> parameterTypes = new ArrayList<>();

		if(parameterCount > 0) {
			for(Object p0 : node.parameters()) {
				SingleVariableDeclaration parameter = (SingleVariableDeclaration) p0;

				ITypeBinding binding = parameter.getType().resolveBinding();

				String v;
				if(binding == null || binding.isRecovered())
					v = parameter.getType().toString();
				else
					v = binding.getQualifiedName();

				if(parameter.isVarargs()) v+="[]";

				parameterTypes.add(v);
			}
		}

		return formatSignature(parameterTypes);
	}

	//Helper method to extract the number of arguments from an argument list used to generate the method signature for MethodInvocation nodes
	private String getMethodSignature(List<?> arguments, List<?> typeArguments) {
		int argumentCount = arguments != null ? arguments.size() : 0;
		List<String> parameterTypes = typeArguments.stream().map(object -> object.toString()).collect(Collectors.toList());
		return formatSignature(parameterTypes);
	}

	private String formatSignature(List<String> parameters){
		int parameterCount = parameters.size();
		return String.format("%d%s%s%s",
				parameterCount,
				(parameterCount > 0 ? "[" : ""),
				(parameterCount > 0 ? String.join(",", parameters) : ""),
				(parameterCount > 0 ? "]" : "")
		);
	}

	//get the simple name from the fragments of a variable or field declaration, e.g. [a=10] -> a
	//Be aware: the function might return the empty string
	public List<String> getVariableName(List<VariableDeclarationFragment> fragments){
		if (fragments != null)
			return fragments.stream().map(fragment -> fragment.getName().getIdentifier()).collect(Collectors.toList());
		return Collections.emptyList();
	}
}