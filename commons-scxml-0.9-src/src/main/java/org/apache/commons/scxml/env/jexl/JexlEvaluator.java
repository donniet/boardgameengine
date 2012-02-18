/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.scxml.env.jexl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.scxml.Context;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.SCXMLExpressionException;
import org.w3c.dom.Node;

/**
 * Evaluator implementation enabling use of JEXL expressions in
 * SCXML documents.
 *
 */
public class JexlEvaluator implements Evaluator, Serializable {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** Error message if evaluation context is not a JexlContext. */
    private static final String ERR_CTX_TYPE = "Error evaluating JEXL "
        + "expression, Context must be a org.apache.commons.jexl.JexlContext";

    /** Pattern for recognizing the SCXML In() special predicate. */
    private static Pattern inFct = Pattern.compile("In\\(");
    /** Pattern for recognizing the Commons SCXML Data() builtin function. */
    private static Pattern dataFct = Pattern.compile("Data\\(");

    /** Constructor. */
    public JexlEvaluator() {
        super();
    }

    /**
     * Evaluate an expression.
     *
     * @param ctx variable context
     * @param expr expression
     * @return a result of the evaluation
     * @throws SCXMLExpressionException For a malformed expression
     * @see Evaluator#eval(Context, String)
     */
    public Object eval(final Context ctx, final String expr)
    throws SCXMLExpressionException {
        if (expr == null) {
            return null;
        }
        JexlContext jexlCtx = null;
        if (ctx instanceof JexlContext) {
            jexlCtx = (JexlContext) ctx;
        } else {
            throw new SCXMLExpressionException(ERR_CTX_TYPE);
        }
        Expression exp = null;
        try {
            String evalExpr = inFct.matcher(expr).
                replaceAll("_builtin.isMember(_ALL_STATES, ");
            evalExpr = dataFct.matcher(evalExpr).
                replaceAll("_builtin.data(_ALL_NAMESPACES, ");
            exp = ExpressionFactory.createExpression(evalExpr);
            return exp.evaluate(getEffectiveContext(jexlCtx));
        } catch (Exception e) {
            throw new SCXMLExpressionException("eval('" + expr + "'):"
                + e.getMessage(), e);
        }
    }

    /**
     * @see Evaluator#evalCond(Context, String)
     */
    public Boolean evalCond(final Context ctx, final String expr)
    throws SCXMLExpressionException {
        if (expr == null) {
            return null;
        }
        JexlContext jexlCtx = null;
        if (ctx instanceof JexlContext) {
            jexlCtx = (JexlContext) ctx;
        } else {
            throw new SCXMLExpressionException(ERR_CTX_TYPE);
        }
        Expression exp = null;
        try {
            String evalExpr = inFct.matcher(expr).
                replaceAll("_builtin.isMember(_ALL_STATES, ");
            evalExpr = dataFct.matcher(evalExpr).
                replaceAll("_builtin.data(_ALL_NAMESPACES, ");
            exp = ExpressionFactory.createExpression(evalExpr);
            return (Boolean) exp.evaluate(getEffectiveContext(jexlCtx));
        } catch (Exception e) {
            throw new SCXMLExpressionException("eval('" + expr + "'):"
                + e.getMessage(), e);
        }
    }

    /**
     * @see Evaluator#evalLocation(Context, String)
     */
    public Node evalLocation(final Context ctx, final String expr)
    throws SCXMLExpressionException {
        if (expr == null) {
            return null;
        }
        JexlContext jexlCtx = null;
        if (ctx instanceof JexlContext) {
            jexlCtx = (JexlContext) ctx;
        } else {
            throw new SCXMLExpressionException(ERR_CTX_TYPE);
        }
        Expression exp = null;
        try {
            String evalExpr = inFct.matcher(expr).
                replaceAll("_builtin.isMember(_ALL_STATES, ");
            evalExpr = dataFct.matcher(evalExpr).
                replaceFirst("_builtin.dataNode(_ALL_NAMESPACES, ");
            evalExpr = dataFct.matcher(evalExpr).
                replaceAll("_builtin.data(_ALL_NAMESPACES, ");
            exp = ExpressionFactory.createExpression(evalExpr);
            return (Node) exp.evaluate(getEffectiveContext(jexlCtx));
        } catch (Exception e) {
            throw new SCXMLExpressionException("eval('" + expr + "'):"
                + e.getMessage(), e);
        }
    }

    /**
     * Create a new child context.
     *
     * @param parent parent context
     * @return new child context
     * @see Evaluator#newContext(Context)
     */
    public Context newContext(final Context parent) {
        return new JexlContext(parent);
    }

    /**
     * Create a new context which is the summation of contexts from the
     * current state to document root, child has priority over parent
     * in scoping rules.
     *
     * @param nodeCtx The JexlContext for this state.
     * @return The effective JexlContext for the path leading up to
     *         document root.
     */
    private JexlContext getEffectiveContext(final JexlContext nodeCtx) {
        List contexts = new ArrayList();
        // trace path to root
        JexlContext currentCtx = nodeCtx;
        while (currentCtx != null) {
            contexts.add(currentCtx);
            currentCtx = (JexlContext) currentCtx.getParent();
        }
        Map vars = new HashMap();
        // summation of the contexts, parent first, child wins
        for (int i = contexts.size() - 1; i > -1; i--) {
            vars.putAll(((JexlContext) contexts.get(i)).getVars());
        }
        return new JexlContext(vars);
    }

	@Override
	public Object evalData(Context ctx, String data)
			throws SCXMLExpressionException {
		// TODO Auto-generated method stub
		return eval(ctx, data);
	}

}

