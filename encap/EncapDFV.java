package edu.nju.cs.util.ClassAndMethodAnalysis.encap;

import java.util.List;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import edu.nju.cs.DataFlowAnalysis.FrameWork.DFV;
import edu.nju.cs.model.Formula;

public class EncapDFV extends DFV {	
	ConnectionGraph cg = new ConnectionGraph();
	
	public EncapDFV() {
		// do nothing
	}
	
	public EncapDFV(EncapDFV dfv) {
		if (dfv == null || dfv.cg == null)
			CommonUtils.printlnError("Error");
		this.cg = dfv.cg.clone();
	}

	@Override
	public boolean meet(DFV dfv) {		
		if (dfv instanceof EncapDFV) {
			this.cg.merge(((EncapDFV)dfv).cg);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Java�淶��ֻ��������ʽ����ֱ����Ϊ���:
	 * 1. Assignment expressions
	 * 2. Any use of ++ or --
	 * 3. Method invocations
	 * 4. Object creation expressions
	 * ����ֻ��1 3 4���ܻ�ı�ָ���ָ��
	 * ������ʽ����ֱ����Ϊ��䣬��ô������ʽ�ܿ�������ΪReturnStatement��һ���֡�
	 */
	@Override
	public DFV transferAndGenerate(Expression exp) {
		EncapDFV newDFV = (EncapDFV) clone();
		EncapDFVUtils.handle(newDFV, exp);
		
		return newDFV;
	}

	@Override
	public DFV transferAndGenerate(VariableDeclarationFragment vdf) {		
		EncapDFV newDFV = (EncapDFV) clone();		
		Expression left = vdf.getName();
		Expression right = vdf.getInitializer();
		
		EncapDFVUtils.handle(newDFV, left, right);
		
		return newDFV;
	}

	@Override
	public void lift(Integer ppt) {
		// do nothing for now
	}

	@Override
	public DFV clone() {
		return new EncapDFV(this);
	}

	@Override
	public String toString() {
		return cg.toString();
	}

	@Override
	public List<Formula> toFormulas(int ppt) {
		// do nothing for now
		return null;
	}

	@Override
	public List<Formula> toFormulasDiff(DFV dfv, int ppt) {
		// do nothing for now
		return null;
	}
	
	public boolean equals(Object dfv) {
		if(!(dfv instanceof EncapDFV))
			return false;
		EncapDFV encapdfv = (EncapDFV)dfv;
		
		return this.cg.equals(encapdfv.cg);
	}
	
	@Override
	public DFV getBoundary() {
		return this.clone();
	}
	
	public ConnectionGraph getCG() {
		return cg;
	}
	
	public void setCG(ConnectionGraph cg) {
		this.cg = cg;
	}
}
