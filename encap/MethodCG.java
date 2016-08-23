package edu.nju.cs.util.ClassAndMethodAnalysis.encap;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import edu.nju.cs.DataFlowAnalysis.FrameWork.ForwardAnalysisAlgorithm;
import edu.nju.cs.ui.Demo;
import edu.nju.cs.util.MethodUtils;

/**
 * 方法的CG
 * @author Tom
 *
 */
public class MethodCG {	
	private MethodDeclaration mtd = null;	// 对应的方法
	
	private Integer exitPPT = null;
	
	// The CG at the exit point of the method
	private EncapDFV exitDFV = null;
	private Set<CGNode> retNodes = CommonUtils.newHashSet();
	
	private ForwardAnalysisAlgorithm faa = null;
	
	private ConnectionGraph classCG = null;
	
	public MethodCG(MethodDeclaration mtd, ConnectionGraph classCG) {
		super();
		this.mtd = mtd;
		this.classCG = classCG;
		CommonUtils.println("=== Analyzing method " + CommonUtils.getMethodSignature(mtd) + " of class " + CommonUtils.getTpdName(CommonUtils.getDeclaringTpd(mtd)) + " ===");
		
		if (CommonUtils.getMethodSignature(mtd).equals("random(int min, int max)")) {
			CommonUtils.debug();
		}
		
		buildCG();
		//toNeo4jDb();
	}
	
	/**
	 * 构建方法的CG
	 * 首先进行数据流分析
	 * 然后对方法结束的程序点上的CG进行可达性分析，更新EscapeState
	 */
	void buildCG() {
		// 忽略没有方法体的方法和匿名类中的方法
		if (mtd.getBody() == null || CommonUtils.isDeclaredInAcd(mtd))
			return;
		
		EncapDFV initDFV = new EncapDFV();
		ConnectionGraph initCG = classCG.clone();
		initDFV.setCG(initCG);
		
		List<SingleVariableDeclaration> params = mtd.parameters();
		for (SingleVariableDeclaration ai : params) {			
			// 仅考虑非基础数据类型的参数
			if (!ai.getType().isPrimitiveType()) {
				ParamVarNode node = initCG.addParamVarNode(ai.getName());
				ObjectNode phantom = initCG.addJavaObjectNode(ai);
				initCG.addPointsToEdge(node, phantom);
			}
		}
		
//		TypeDeclaration declaringTPD = Utils.getDeclaringTpd(mtd);
//		if (declaringTPD.resolveBinding().isAnonymous())
//			return;
		
		faa = new ForwardAnalysisAlgorithm(Demo.project, mtd, initDFV);
		//faa.TwoPhaseExecution();
		
		exitPPT = Demo.project.getMethodExit(MethodUtils.getMethodIdStr(mtd));
		if (exitPPT == null)
			return;
		exitDFV = (EncapDFV) faa.getDFVs().get(exitPPT);
		
		// 处理返回值
		List<Integer> ppts = Demo.project.getPreds(exitPPT);
		if (ppts != null)
			for (int ppt : ppts) {
				Statement st = Demo.project.getStBeforePPT(ppt);
				if (st instanceof ReturnStatement) {
					Expression returnExp = ((ReturnStatement)st).getExpression();
					Set<CGNode> rets = EncapDFVUtils.handle(exitDFV, returnExp);
					if (rets == null) {
						continue;
					}
					for (CGNode node : rets) {
						if (node == null)
							continue;
						// 如果是return this,不降其逃逸值设为RetEscape
						if (node instanceof ObjectNode) {
							ObjectNode obj = (ObjectNode)node;
							if (obj.getASTNode() instanceof TypeDeclaration)
								continue;
						}
						// TODO 是不是只需要考虑FieldNode？
						node.adjustEscapeState(EscapeState.RetEscape);
					}

					retNodes.addAll(rets);
				}
			}
		
		exitDFV.getCG().updateEscapeState();
//		Utils.println("Printing CG of method <" + Utils.getMethodSignature(mtd) + "> ...");
//		Utils.println("**************** Begin ****************");
//		Utils.print(exitDFV.getCG().toString());
//		Utils.println("****************  End  ****************");
	}
	
	ConnectionGraph getExitCG() {
		if (exitDFV == null) {
			//Utils.printlnError("Error!");
			return null;
		}
		return exitDFV.getCG();
	}
	
	Set<CGNode> getRetNodes() {
		return retNodes;
	}
	
	MethodDeclaration getMtd() {
		return mtd;
	}
	
	@ Override
	public String toString() {
		if (exitDFV == null)
			return "Invalid CG";
		ConnectionGraph cg = exitDFV.getCG();
		return cg == null ? "Invalid CG." : cg.toString();
	}
	
	public void toNeo4jDb() {
		Neo4jService.getInstance().store(this);
		CommonUtils.println("toNeo4jDb succeed!");
	}
	
	@ Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof MethodCG))
			return false;
		
		MethodCG othermcg = (MethodCG) other;
		
		// 以下三个都可以用指针比较，而不用equals比较
		if (mtd != othermcg.mtd)
			return false;
		
		if (exitPPT != othermcg.exitPPT)
			return false;
		
		if (classCG != othermcg.classCG)
			return false;
		
		return exitDFV.equals(othermcg.exitDFV);
	}

}
