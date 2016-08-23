package edu.nju.cs.util.ClassAndMethodAnalysis.encap;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;

enum NodeType {
	JavaObject, Field, LocalVar, ParamVar
}

enum EscapeState {
	NoEscape, ArgEscape, RetEscape, GlobalEscape
}

enum EdgeType {
	PointsToEdge, DeferredEdge, FieldEdge
}

/**
 * CGNode为ConnectionGraph中的节点类型的基类。
 * 每个CGNode对应一个ASTNode，具体类型可以是声明、表达式(如变量和类创建表达式)等，分别对应不同的子类。
 * 当节点为phantom或者表示this所指向的对象时，astnode可以为空；
 * 当astnode对应于类创建表达式(new T)时，即type为JavaObject时，CGNode的等价性取决于ASTNode的等价性(是否代表同一个代码片段)；
 * 其它情况等价性取决于astnode在ASTMatcher上的等价性。
 */
public abstract class CGNode implements Cloneable {
	// 用于辅助生成ASTNode的哈希值
	private static ArrayList<ASTNode> hashIndices = CommonUtils.newArrayList();
	private static Integer getHashIndex(ASTNode astnode) {
		if (astnode == null)
			return 0;
		int index = hashIndices.indexOf(astnode);
		if (index != -1) 
			return index;
		else {
			hashIndices.add(astnode);
			return hashIndices.size()-1;
		}
	}
	
	protected ASTNode astnode = null;
	protected Integer hashIndex = null;
	protected EscapeState escape = EscapeState.NoEscape;
	
	protected Map<CGNode, EdgeType> inEdges = CommonUtils.newHashMap();
	protected Map<CGNode, EdgeType> outEdges = CommonUtils.newHashMap();
	
	protected CGNode(ASTNode ast) {
		this.astnode = ast;
		this.hashIndex = getHashIndex(this.astnode);
	}
	
	// 浅拷贝，只拷贝节点信息，不拷贝边的信息
	CGNode(CGNode cgnode) {
		this.astnode = cgnode.astnode;
		this.hashIndex = cgnode.hashIndex;
		this.escape = cgnode.escape;
	}
	
	// 将目标节点的信息合并到当前节点
	abstract void merge(CGNode cgnode);
	
	// 递归获取一个节点所指向的JavaObjectNode
	abstract Set<ObjectNode> getPointsToNode();
	
	abstract public boolean correspondTo(ASTNode astnode);

	// 判断逻辑等价，用于判断CG是否到达不动点
	abstract public boolean isEqualTo(Object obj);

	abstract public String toSimpleString();
	
	abstract public String toNeo4jString();
	
	abstract public NodeType getNodeType();
	
	abstract public Object clone();

	/**
	 * GlobalEscape < ArgEscape < NoEscape
	 * GlobalEscape < RetEscape < NoEscape
	 * ArgEscape与RetEscape不可比
	 * @param es
	 * @return
	 */
	public boolean adjustEscapeState(EscapeState es) {
		if (escape == EscapeState.NoEscape) {
			if (es == EscapeState.NoEscape)
				return false;
			
			escape = es;
			return true;
		}
		
		if (escape == EscapeState.ArgEscape || escape == EscapeState.RetEscape) {
			if (es == EscapeState.GlobalEscape) {
				escape = EscapeState.GlobalEscape;
				return true;	
			}			
			return false;
		}
		
		return false;
	}
	
	public EscapeState getEscapeState() {
		return escape;
	}

	public CGNode setEscapeState(EscapeState es) {
		this.escape = es;
		return this;
	}

	Map<CGNode, EdgeType> getInEdges() {
		return this.inEdges;
	}

	Map<CGNode, EdgeType> getOutEdges() {
		return this.outEdges;
	}
	
	void addInEdge(CGNode node, EdgeType et) {
		inEdges.put(node, et);
	}
	
	void removeInEdge(CGNode node, EdgeType et) {
		inEdges.remove(node, et);
	}
	
	void addOutEdge(CGNode node, EdgeType et) {
		outEdges.put(node, et);
	}
	
	void removeOutEdge(CGNode node, EdgeType et) {
		outEdges.remove(node, et);
	}
	
	ASTNode getASTNode() {
		return astnode;
	}
	
	public boolean isOrdinary() {
		return this.astnode != null;
	}
}

abstract class RefNode extends CGNode {
	
	protected RefNode(ASTNode ast) {
		super(ast);
	}
	
	@ Override
	public boolean correspondTo(ASTNode astnode) {
		if (astnode == null) {
			if (this.astnode == null)
				return true;
			return false;
		} else if (this.astnode == null) {
			return false;
		}
		
		if (this.astnode == astnode)
			return true;
		
		return this.astnode.subtreeMatch(new ASTMatcher(), astnode);
	}

	@ Override
	public Set<ObjectNode> getPointsToNode() {
		Set<CGNode> travelledHistory = CommonUtils.newHashSet();
		return getPointsToNode(travelledHistory);
	}
	
	private Set<ObjectNode> getPointsToNode(Set<CGNode> travelledHistory) {
		Set<ObjectNode> pointsToNodes = CommonUtils.newHashSet();
		for (Entry<CGNode, EdgeType> entry : getOutEdges().entrySet()) {
			CGNode node = entry.getKey();
			if (!travelledHistory.contains(node)) {
				travelledHistory.add(node);
				if (entry.getValue() == EdgeType.PointsToEdge) {
					pointsToNodes.add((ObjectNode) node);
				} else {
					pointsToNodes.addAll(((RefNode) node).getPointsToNode(travelledHistory));
				}
			}
		}
		return pointsToNodes;
	}
	
	public Set<RefNode> getDirectRefersToNode() {
		Set<RefNode> refersToNodes = CommonUtils.newHashSet();
		for (Entry<CGNode, EdgeType> entry : getOutEdges().entrySet()) {
			CGNode node = entry.getKey();
			if (entry.getValue() == EdgeType.DeferredEdge) {
				refersToNodes.add((RefNode) node);
			}
		}
		return refersToNodes;
	}
	
	@Override
	void merge(CGNode cgnode) {
		if (getClass() == cgnode.getClass())
			adjustEscapeState(cgnode.getEscapeState());
	}
}