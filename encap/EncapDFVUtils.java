package edu.nju.cs.util.ClassAndMethodAnalysis.encap;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.nju.cs.ui.Demo;

public class EncapDFVUtils {

	static Set<CGNode> handle(EncapDFV dfv, Expression exp) {
		return handle(dfv.cg, exp);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, Expression exp) {
		if (exp instanceof Assignment) {
			return handle(cg, (Assignment) exp);
		}
		if (exp instanceof MethodInvocation) {
			return handle(cg, (MethodInvocation) exp);
		}
		if (exp instanceof ClassInstanceCreation) {
			return handle(cg, (ClassInstanceCreation) exp);
		}
		if (exp instanceof Name) {
			return handle(cg, (Name) exp);
		}
		if (exp instanceof ThisExpression) {
			return handle(cg, (ThisExpression) exp);
		}
		if (exp instanceof FieldAccess) {
			return handle(cg, (FieldAccess) exp);
		}
		if (exp instanceof SuperFieldAccess) {
			return handle(cg, (SuperFieldAccess) exp);
		}
		if (exp instanceof CastExpression) {
			return handle(cg, ((CastExpression) exp).getExpression());
		}
		if (exp instanceof ParenthesizedExpression) {
			return handle(cg, ((ParenthesizedExpression) exp).getExpression());
		}
		if (exp instanceof ArrayCreation) {
			
		}
		if (exp instanceof ArrayAccess) {
			
		}
		return null;
	}
	
	static Set<CGNode> handle(EncapDFV dfv, Assignment assignment) {		
		return handle(dfv.cg, assignment);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, Assignment assignment) {
		//Utils.println(">>>" + assignment);
		Expression left = assignment.getLeftHandSide();
		Expression right = assignment.getRightHandSide();
		
		return handle(cg, left, right);
	}
	
	static Set<CGNode> handle(EncapDFV dfv, Expression left, Expression right) {
		return handle(dfv.cg, left, right);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, Expression left, Expression right) {
		Set<CGNode> leftNodes = handle(cg, left);
		Set<CGNode> rightNodes = handle(cg, right);
		if (leftNodes == null || rightNodes == null)
			return null;
		
		for (CGNode leftNode : leftNodes) {
			for (CGNode rightNode : rightNodes) {
				if (leftNode instanceof RefNode) {
					if (rightNode instanceof RefNode)
						cg.addDeferredEdge(leftNode, rightNode);
					else
						cg.addPointsToEdge(leftNode, (ObjectNode) rightNode);
				} else {
					CommonUtils.printlnError("Error: left side of the assignment is not RefNode.");
				}
			}
		}
	
		return leftNodes;
	}

	static Set<CGNode> handle(EncapDFV dfv, MethodInvocation mti) {
		return handle(dfv.cg, mti);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, MethodInvocation mti) {
		return cg.map(mti);
	}

	static Set<CGNode> handle(EncapDFV dfv, ClassInstanceCreation cic) {
		return handle(dfv.cg, cic);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, ClassInstanceCreation cic) {
		//Utils.println(">>>" + cic);
		Set<CGNode> ret = CommonUtils.newHashSet();
		ret.add(cg.getOrAddJavaObjectNode(cic));
		return ret;
	}

	static Set<CGNode> handle(EncapDFV dfv, Name name) {
		return handle(dfv.cg, name);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, Name name) {
		if (name instanceof SimpleName)
			return handle(cg, (SimpleName)name);
		else
			return handle(cg, (QualifiedName)name);
	}
	
	// 返回SimpleName对应的节点
	static Set<CGNode> handle(EncapDFV dfv, SimpleName simpleName) {
		return handle(dfv.cg, simpleName);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, SimpleName simpleName) {
		// 如果simpleName是基础数据类型变量
		if (CommonUtils.isPrimitive(simpleName) == null || CommonUtils.isPrimitive(simpleName))
			return null;
		
		Set<CGNode> nodes = CommonUtils.newHashSet();
		
		// 如果simpleName是类名
		// 将这个类（TPD）的CG的GlobalEscape部分拷贝到当前CG
		ITypeBinding itb = CommonUtils.getTypeBinding(simpleName);
		if (itb != null) {
			String qualifiedName = itb.getQualifiedName();
			ASTNode astnode = Demo.project.getClassByName(qualifiedName);
			
			if (astnode instanceof TypeDeclaration) {
				TypeDeclaration tpd = (TypeDeclaration) astnode;
				// 获取simpleName对应的tpd对应的ClassCG
				ClassCG ccg = ClassCG.tpd2ccg.get(tpd);
				
				// 将ClassCG表示类部分的cg的GlobalEscape节点加入到当前cg中
				if (ccg != null)
					cg.merge(ccg.getClassCG(), EscapeState.GlobalEscape);
			}
			// 直接返回一个空集
			return nodes;
		}
		
		// 如果simpleName是非基础数据类型变量
		CGNode node = null;
		if (CommonUtils.isField(simpleName)) {
			// TODO 找出simpleName实际所在的外部类（隐式访问外部类的成员，显式访问必然通过ThisExpression，已经处理过）
			TypeDeclaration actualTpd = getActualDeclaringTpd(simpleName, ClassCG.currTpd);
			if (actualTpd == null)
				return null;
			node = cg.getOrAddJavaObjectNode(actualTpd).getFieldNode(simpleName);
			if (node == null)
				CommonUtils.debug("filed node not found!");
			else
				nodes.add(node);
		} else if (CommonUtils.isParameter(simpleName)) {
			node = cg.getParamVarNode(simpleName);
			if (node == null)
				CommonUtils.debug("parameter node not found!");
			else
				nodes.add(node);
		} else {
			node = cg.getOrAddLocalVarNode(simpleName);
			if (node == null)
				CommonUtils.debug("local var node not found!");
			else
				nodes.add(node);
		}
		return nodes;
	}
	
	// 从当前类型中向外部类中递归获取包含fieldName的TypeDeclaration
	static TypeDeclaration getActualDeclaringTpd(SimpleName fieldName, TypeDeclaration currTpd) {
		for (FieldDeclaration fieldDec : currTpd.getFields()) {
			@SuppressWarnings("unchecked")
			List<VariableDeclarationFragment> vdfs = fieldDec.fragments();
			for (VariableDeclarationFragment vdf : vdfs) {
				SimpleName vdfName = vdf.getName();
				if (vdfName.subtreeMatch(new ASTMatcher(), fieldName))
					return currTpd;
			}
		}
		
		String currentTpdName = currTpd.resolveBinding().getQualifiedName();
		String outerTpdName = Demo.project.getOuterClassName(currentTpdName);
		if (outerTpdName != null)
			return getActualDeclaringTpd(fieldName, (TypeDeclaration) Demo.project.getClassByName(outerTpdName));
		
		return null;
	}
	
	// 返回QualifiedName指向的节点集合
	static Set<CGNode> handle(EncapDFV dfv, QualifiedName qn) {
		return handle(dfv.cg, qn);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, QualifiedName qn) {
		// TODO 处理QualifiedName为包名的情况
		if (qn == null)
			return null;
		
		SimpleName field = qn.getName();
		if (CommonUtils.isPrimitive(field) == null || CommonUtils.isPrimitive(field))
			return null;

		Set<ObjectNode> ptNodes = null;	
		Set<CGNode> refs = CommonUtils.newHashSet();
		
		Name qualifierName = qn.getQualifier();
		if (qualifierName instanceof SimpleName) {
			SimpleName simpleName = (SimpleName)qualifierName;
			// SimpleName只会指向一个节点
			Set<CGNode> nodes = handle(cg, simpleName);
			CGNode qualifierNode = null; 
			// QualifierName是类名
			// 直接查找
			if (nodes == null || nodes.size() == 0) {
				
				ITypeBinding itb = CommonUtils.getTypeBinding(simpleName);
				if (itb != null) {
					String qualifiedName = itb.getQualifiedName();
					ASTNode astnode = Demo.project.getClassByName(qualifiedName);
					
					if (astnode instanceof TypeDeclaration) {
						TypeDeclaration tpd = (TypeDeclaration) astnode;
						
						ObjectNode node = (ObjectNode) cg.getCGNodeByAST(tpd);
						FieldNode fieldNode = null;
						
						if (node != null) {
							fieldNode = node.getFieldNode(field);
						
							// 此时查找到的节点应该是GlobalEscape
							if (fieldNode != null && fieldNode.getEscapeState() == EscapeState.GlobalEscape)
								refs.add(fieldNode);
							else 
								CommonUtils.debug("field node not found!");
						}
					}
					
				}
			} else if (nodes.size() == 1) {
				// QualifierName是变量名
				qualifierNode = handle(cg, simpleName).iterator().next();
				ptNodes = qualifierNode.getPointsToNode();
				
				if (ptNodes == null || ptNodes.isEmpty()) {
					if (CommonUtils.isParameter(simpleName) || CommonUtils.isField(simpleName)) {
						TypeDeclaration tpd = (TypeDeclaration)Demo.project.getClassByName(simpleName.resolveTypeBinding().getName());
						if (tpd != null) {
							ObjectNode phantom = cg.getOrAddJavaObjectNode(tpd);
							cg.addPointsToEdge(qualifierNode, phantom);
							ptNodes = CommonUtils.newHashSet();
							ptNodes.add(phantom);
						} else {
							CommonUtils.debug("tpd for field or parameter not found.");
						}
					} else {
						// 局部变量指向的对象集合为空，很有可能是空指针异常
						CommonUtils.debug("potential NullPointerExceptions.");
					}
				}
				
				for (ObjectNode obj : ptNodes)
					if (obj != null) {
						FieldNode fldNode = obj.getFieldNode(field);
						if (fldNode != null)
							refs.add(fldNode);
						else
							CommonUtils.debug("field node not found.");
					}
			} else
				CommonUtils.debug("SimpleName should not has more than one corresponding node.");

			
		} else if (qualifierName instanceof QualifiedName) {
			Set<CGNode> refNodes = handle(cg, (QualifiedName) qualifierName);
			if (refNodes == null) {
				// TODO java.util.ArrayList
				CommonUtils.debug("ref nodes of qulified name (" + qualifierName + ") not found.");
			} else {
				for (CGNode ref : refNodes) {
					if (ref == null) {
						CommonUtils.debug("invalid (null) ref node.");
						continue;
					}
					ptNodes = ref.getPointsToNode();
					for (ObjectNode obj : ptNodes) {
						FieldNode fldNode = obj.getFieldNode(field);
						if (fldNode != null)
							refs.add(fldNode);
						else
							CommonUtils.debug("field node not found.");
					}
				}
			}
		}

		return refs;
	}
	
	/**
	 * 返回ThisExpression实际指向的对象
	 * 考虑几种情况：
	 * 1. 一般类的this
	 * 2. 匿名类的this(由于程序点还没包含匿名类，所以暂时不考虑)
	 * 3. ClassName.this, 其中ClassName可能是嵌套的
	 * @param dfv
	 * @param texp
	 * @return
	 */
	static Set<CGNode> handle(EncapDFV dfv, ThisExpression texp) {
		return handle(dfv.cg, texp);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, ThisExpression texp) {
		Name className = texp.getQualifier();
		Set<CGNode> ret = CommonUtils.newHashSet();
		// this
		if (className == null) {
			CGNode node = cg.getCurrThisObject();
			if (node != null)
				ret.add(node);
		}
		// ClassName.this
		else {
			TypeDeclaration enclosingTpd = getEnclosingTpd(ClassCG.currTpd, className);
			ret.add(ClassCG.tpd2obj.get(enclosingTpd));
		}
		return ret;
	}
	
	// 获取tpd的名字为targetClassName的外部类
	static TypeDeclaration getEnclosingTpd(TypeDeclaration tpd, Name targetClassName) {		
		String targetFullName = targetClassName.resolveTypeBinding().getQualifiedName();		
		String currentTpdName = tpd.resolveBinding().getQualifiedName();
		
		String tmp = null;
		for (tmp = currentTpdName; tmp != null && !tmp.equals(targetFullName); tmp = Demo.project.getOuterClassName(tmp))
			;
		if (tmp != null && tmp.equals(targetFullName))
			return (TypeDeclaration) Demo.project.getClassByName(tmp);

		return null;
	}

	static Set<CGNode> handle(EncapDFV dfv, FieldAccess fa) {
		return handle(dfv.cg, fa);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, FieldAccess fa) {
		SimpleName field = fa.getName();
		if (CommonUtils.isPrimitive(field) == null || CommonUtils.isPrimitive(field))
			return null;
		
		Expression efa = fa.getExpression();
		Set<CGNode> refs = CommonUtils.newHashSet();
		
		// 简化this.exp为exp
		if (efa.toString().equals("this")) {
			Set<CGNode> nodes = handle(cg, field);
			if (nodes != null)
				refs.addAll(nodes);
		} else {
			Set<CGNode> refNodes = handle(cg, efa);
			Set<ObjectNode> ptNodes = null;
			if (refNodes == null)
				return null;
			for (CGNode ref : refNodes) {
				ptNodes = ref.getPointsToNode();
				for (ObjectNode obj : ptNodes)
					refs.add(obj.getFieldNode(field));
			}
		}
		
		return refs;
	}

	static Set<CGNode> handle(EncapDFV dfv, SuperFieldAccess sfa) {
		return handle(dfv.cg, sfa);
	}
	
	static Set<CGNode> handle(ConnectionGraph cg, SuperFieldAccess sfa) {
		SimpleName fieldName = sfa.getName();
		if (CommonUtils.isPrimitive(fieldName))
			return null;
		
		Name qualifierName = sfa.getQualifier();
		Set<ObjectNode> ptNodes = null;
		Set<CGNode> refs = CommonUtils.newHashSet();
		
		Set<CGNode> refNodes = handle(cg, qualifierName);
		for (CGNode ref : refNodes) {
			ptNodes = ref.getPointsToNode();
			for (ObjectNode obj : ptNodes)
				refs.add(obj.getFieldNode(fieldName));
		}
		return refs;
	}

}
