package edu.nju.cs.util.ClassAndMethodAnalysis.encap;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.nju.cs.ui.Demo;

public class OuterInnerClassVisitor extends ASTVisitor {
	
	TypeDeclaration outerTypeDec = null;
	
	public OuterInnerClassVisitor(TypeDeclaration tpd) {
		outerTypeDec = tpd;
	}
	
	public boolean visit(TypeDeclaration innerTypeDec) {
		if (innerTypeDec == outerTypeDec)
			return true;

		ITypeBinding innerItb = innerTypeDec.resolveBinding();		
		String innerClassName = innerItb.getQualifiedName();
		
		
		ITypeBinding outerItb = outerTypeDec.resolveBinding();		
		String outerClassName = outerItb.getQualifiedName();
		
		Demo.project.connectInnerAndOuter(innerClassName, outerClassName);
		
		// ÷ª∑√Œ “ª≤„
		return false;
	}
	
}
