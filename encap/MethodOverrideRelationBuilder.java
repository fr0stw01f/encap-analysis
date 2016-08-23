package edu.nju.cs.util.ClassAndMethodAnalysis.encap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.nju.cs.ui.Demo;

public class MethodOverrideRelationBuilder {
	
	TypeDeclaration superTypeDec = null;
	TypeDeclaration subTypeDec = null;
	
	public MethodOverrideRelationBuilder(TypeDeclaration superTpd, TypeDeclaration subTpd) {
		superTypeDec = superTpd;
		subTypeDec = subTpd;
	}
	
	// 建立superTypdDec和subTypeDec继承（包括实现）关系中方法的直接重写（override）关系
	// 如果superTypdDec中的某个方法没有重写（override）subTypeDec中的方法，则递归继续向上找superTpd的super类型中的方法，直到方法没有super类型
	public void build() {
		MethodDeclaration[] subTypeMethods = subTypeDec.getMethods();
		MethodDeclaration[] superTypeMethods = superTypeDec.getMethods();
		for (MethodDeclaration subTypeMtd : subTypeMethods) {
			IMethodBinding imbSub = subTypeMtd.resolveBinding();
			boolean found = false;
			for (MethodDeclaration superTypeMtd : superTypeMethods) {
				IMethodBinding imbSuper = superTypeMtd.resolveBinding();
				if (imbSub.overrides(imbSuper)) {
					Demo.project.connectOverridingAndOverriden(subTypeMtd, superTypeMtd);
					found = true;
				}
			}
			if (!found) {
				String superSuperClassName = Demo.project.getSuperClassName(superTypeDec.resolveBinding().getQualifiedName());
				// 优先考虑class，其次考虑interface
				ASTNode superSuperTypeDec = Demo.project.getClassByName(superSuperClassName);
				if (superSuperTypeDec == null)
					superSuperTypeDec = Demo.project.getInterfaceByName(superSuperClassName);
				
				if (superSuperTypeDec != null)
					new MethodOverrideRelationBuilder((TypeDeclaration) superSuperTypeDec, subTypeDec).build();
			}
		}
	}
	
}
