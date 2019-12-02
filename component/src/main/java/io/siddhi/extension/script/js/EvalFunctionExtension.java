/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.siddhi.extension.script.js;

import io.siddhi.annotation.Example;
import io.siddhi.annotation.Extension;
import io.siddhi.annotation.Parameter;
import io.siddhi.annotation.ParameterOverload;
import io.siddhi.annotation.ReturnAttribute;
import io.siddhi.annotation.util.DataType;
import io.siddhi.core.config.SiddhiQueryContext;
import io.siddhi.core.exception.SiddhiAppRuntimeException;
import io.siddhi.core.executor.ConstantExpressionExecutor;
import io.siddhi.core.executor.ExpressionExecutor;
import io.siddhi.core.executor.function.FunctionExecutor;
import io.siddhi.core.util.config.ConfigReader;
import io.siddhi.core.util.snapshot.state.State;
import io.siddhi.core.util.snapshot.state.StateFactory;
import io.siddhi.query.api.definition.Attribute;
import io.siddhi.query.api.exception.SiddhiAppValidationException;
import jdk.nashorn.internal.runtime.ParserException;

import java.util.Locale;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * eval(expression, dataType)
 * Evaluate the string and return output as dataType
 * Accept Type(s): (STRING, STRING)
 * Return Type(s): STRING|INT|LONG|FLOAT|DOUBLE|BOOL
 */
@Extension(
        name = "eval",
        namespace = "js",
        description = "This extension evaluates a given string and " +
                "return the output according to the user specified data type.",
        parameters = {
                @Parameter(name = "expression",
                        description = "Any single line js expression or function.",
                        type = {DataType.STRING},
                        dynamic = true),
                @Parameter(name = "return.type",
                        description = "The return type of the evaluated expression." +
                                " Supported types are int|long|float|double|bool|string.",
                        type = {DataType.STRING}),
        },
        parameterOverloads = {
                @ParameterOverload(parameterNames = {"expression", "return.type"})
        },
        returnAttributes =
        @ReturnAttribute(
                description = "The output of the evaluated expression.",
                type = {DataType.INT, DataType.LONG, DataType.DOUBLE, DataType.FLOAT,
                        DataType.STRING, DataType.BOOL}),
        examples =
        @Example(
                syntax = "js:eval(\"700 > 800\", 'bool')",
                description = "" +
                        "In this example, the expression 700 > 800 will be evaluated and " +
                        "return result as false because user specified return type as bool.")
)
public class EvalFunctionExtension extends FunctionExecutor {

    private static final String ENGINE_NAME = "nashorn";
    private ScriptEngine engine;
    private Attribute.Type returnType = Attribute.Type.OBJECT;

    @Override
    protected StateFactory<State> init(ExpressionExecutor[] expressionExecutors,
                                       ConfigReader configReader,
                                       SiddhiQueryContext siddhiQueryContext) {

        ConstantExpressionExecutor constantExpressionExecutor =
                (ConstantExpressionExecutor) attributeExpressionExecutors[1];
        String type = String.valueOf(constantExpressionExecutor.getValue());
        switch (type.toLowerCase(Locale.ENGLISH)) {
            case "int":
                returnType = Attribute.Type.INT;
                break;
            case "long":
                returnType = Attribute.Type.LONG;
                break;
            case "float":
                returnType = Attribute.Type.FLOAT;
                break;
            case "double":
                returnType = Attribute.Type.DOUBLE;
                break;
            case "bool":
                returnType = Attribute.Type.BOOL;
                break;
            case "string":
                returnType = Attribute.Type.STRING;
                break;
            default:
                throw new SiddhiAppValidationException("Invalid return type found: Return types" +
                        " supported by js:eval() function are int|long|float|double|bool " +
                        "and string");
        }
        engine = new ScriptEngineManager().getEngineByName(ENGINE_NAME);
        if (engine ==  null) {
            throw new SiddhiAppRuntimeException(
                    "Error evaluating the given expression in js:eval(), " +
                            "failed to initialize script engine " + ENGINE_NAME
            );
        }
        return null;
    }

    @Override
    protected Object execute(Object[] objects, State state) {
        String expressionString = (String) objects[0];
        Object result;
        try {
            result = engine.eval(expressionString);
        } catch (ScriptException | ParserException e) {
            throw new SiddhiAppRuntimeException(
                    "Error evaluating the given expression " + expressionString, e);
        }
        return result;
    }

    @Override
    protected Object execute(Object o, State state) {
        return null;
    }

    @Override
    public Attribute.Type getReturnType() {
        return returnType;
    }
}
