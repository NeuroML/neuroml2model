package org.neuroml2.model.utils;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lemsml.exprparser.utils.ExpressionParser;

import org.lemsml.exprparser.utils.SymbolExpander;
import org.lemsml.exprparser.utils.UndefinedSymbolException;
import org.lemsml.exprparser.visitors.ARenderAs;
import org.lemsml.exprparser.visitors.AntlrExpressionParser;
import org.lemsml.exprparser.visitors.RenderLatex;
import org.lemsml.exprparser.visitors.RenderMathJS;
import org.lemsml.model.Case;
import org.lemsml.model.ConditionalDerivedVariable;
import org.lemsml.model.exceptions.LEMSCompilerException;
import org.lemsml.model.extended.Lems;
import org.lemsml.model.extended.LemsNode;
import org.lemsml.model.extended.Scope;
import org.lemsml.model.extended.Symbol;

public class FunctionNodeHelper {
    
	private String name;
	private String independentVariable;
	private Double[] xRange;
	private Double deltaX;
	//context must be toposorted!!
	LinkedHashMap<String, String> context = new LinkedHashMap<String, String>(){
		private static final long serialVersionUID = 1242351L;
	{
		//null is allowed in the antlr lems grammar
		put("null", "null");
	}};
	private LinkedHashMap<String, String> expandedContext;


	public void setIndependentVariable(String x) {
		this.independentVariable = x;
		this.context.put(independentVariable, independentVariable);
	}

	public String getExpression() {
		return context.get(getName());
	}

	public void register(String variable, String value) {
		this.context.put(variable, value);
	}

	public void register(Map<String, String> ctxt) {
		this.context.putAll(ctxt);
	}

	public void deRegister(String k) {
		this.context.remove(k);
	}

	String getIndependentVariable() {
		return independentVariable;
	}

	public Double getDeltaX() {
		return deltaX;
	}

	public void setDeltaX(Double deltaX) {
		this.deltaX = deltaX;
	}

	public Double[] getxRange() {
		return xRange;
	}

	public void setxRange(Double[] xRange) {
		this.xRange = xRange;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    @Override
	public String toString() {
		return this.getName()  + ": " + this.context;
	}

	public String toTeX() {
		RenderLatex adaptor = new RenderLatex();
		AntlrExpressionParser p = new AntlrExpressionParser(getExpression());
		return p.parseAndVisitWith(adaptor);
	}

	public String getBigFatExpression(String var){
		if(expandedContext == null){
			expandedContext = new LinkedHashMap<String, String>(context);
			SymbolExpander.expandSymbols(expandedContext);
		}
		return "f(" + independentVariable + ")="  + expandedContext.get(var);
	}

	public String getExpression(String var){
		if(expandedContext == null){
			expandedContext = new LinkedHashMap<String, String>(context);
			SymbolExpander.expandSymbols(expandedContext);
		}
		return expandedContext.get(var);
	}
    
    
	public static Set<String> findIndependentVariables(String expression,
			Map<String, String> context) {
		Set<String> vars = ExpressionParser.listSymbolsInExpression(expression);
		vars.removeAll(context.keySet());
		return vars;
	}

	private static String conditionalDVToMathJS(ConditionalDerivedVariable cdv, Lems lems) {
		List<String> condsVals = new ArrayList<String>();
		String defaultCase = null;

        int caseNum = cdv.getCase().size();
        
		for (int i=0; i<caseNum; i++) {
            Case c = cdv.getCase().get(i);
            
			if (null == c.getCondition()) // undocumented LEMS feature: no
											// condition, "catch-all" case
            {
				defaultCase = adaptToMathJS(c.getValueDefinition(), lems);
            }
			else
            {
                if (i==caseNum-1 && defaultCase==null)
                {
                    // set this as default...
                    defaultCase = adaptToMathJS(c.getValueDefinition(), lems);
                    
                }
                else
                {
                    condsVals.add(adaptToMathJS(c.getCondition(), lems) + " ? "
                            + adaptToMathJS(c.getValueDefinition(), lems));
                }
            }
		}
        
		if (null != defaultCase)
			condsVals.add(defaultCase);
		//else
            /* Note: assume cond expression is defined correctly (e.g. with cases: ==0 & !=0) 
            as opposed to adding null...
            */
            
			//condsVals.add("null"); // no case satisfied, no default

        String a = Joiner.on(" : ").join(condsVals);
		return a;
	}
    
	public static String processExpression(Symbol resolved, Lems lems)
			throws LEMSCompilerException, UndefinedSymbolException {

		LemsNode type = resolved.getType();
		FunctionNodeHelper f = new FunctionNodeHelper();
		f.setName(resolved.getName());
		f.register(depsToMathJS(resolved, lems));
		f.setIndependentVariable("v");

		if (type instanceof ConditionalDerivedVariable) {
			ConditionalDerivedVariable cdv = (ConditionalDerivedVariable) resolved.getType();
			f.register(f.getName(), conditionalDVToMathJS(cdv, lems));
		}
        
        

		return f.getExpression(f.getName());
	}
    
    
	private static Map<String, String> depsToMathJS(Symbol resolved, Lems lems)
			throws LEMSCompilerException, UndefinedSymbolException {
		Map<String, String> ret = new LinkedHashMap<String, String>();
		Scope scope = resolved.getScope();
		Map<String, String> sortedContext = scope.buildTopoSortedContext(resolved);
		for(Map.Entry<String, String> kv : sortedContext.entrySet()){
			String var = kv.getKey();
			String def = kv.getValue();
			ret.put(var, adaptToMathJS(def, lems));
		}
		return ret;
	}

	private static String adaptToMathJS(String expression, Lems lems) {
		ARenderAs adaptor = new RenderMathJS(lems.getSymbolToUnit());
		AntlrExpressionParser p = new AntlrExpressionParser(expression);
		return p.parseAndVisitWith(adaptor);
	}
    



}