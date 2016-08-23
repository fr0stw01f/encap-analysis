package edu.nju.cs.util.ClassAndMethodAnalysis.encap;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.nju.cs.ui.Demo;

public class PackageInfoVisitor extends ASTVisitor {
	
	PackageDeclaration pd = null;
	
	public PackageInfoVisitor(PackageDeclaration pd) {
		this.pd = pd;
	}
	
	public boolean visit(TypeDeclaration tpd) {
		Demo.project.connectTypeAndPackage(tpd, pd);
		
		// 处理内部类
		return true;
	}
}
