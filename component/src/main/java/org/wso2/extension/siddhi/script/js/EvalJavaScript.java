/*
 * Copyright (c)  2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.extension.siddhi.script.js;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.ReturnAttribute;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.exception.SiddhiAppRuntimeException;
import org.wso2.siddhi.core.function.Script;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.Attribute;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 * This class is for evaluate javascript
 **/
@Extension(
        name = "javascript",
        namespace = "script",
        description = "This extension allows you to include JavaScript functions within the Siddhi Query Language.",
        examples = @Example(
                description = "This JS function will consume 3 var variables, concatenate them and will" +
                        " return as a string",
                syntax = "define function concatJ[JavaScript] return string {\"  " +
                        "var str1 = data[0];\n var str2 = data[1];\n var str3 = data[2];\n " +
                        "var res = str1.concat(str2,str3);\n return res;\n};"),
        returnAttributes = @ReturnAttribute(name = "returnType", description = "return type of the function",
                type = DataType.STRING)

)
/**
 * This class is for evaluate javascript
 **/
public class EvalJavaScript extends Script {

    private static final NashornScriptEngineFactory SCRIPT_ENGINE_FACTORY = new NashornScriptEngineFactory();

    private NashornScriptEngine engine;
    private Attribute.Type returnType;
    private String functionName;

    public EvalJavaScript() {
        this.engine = (NashornScriptEngine) SCRIPT_ENGINE_FACTORY.getScriptEngine();
    }

    @Override
    public void init(String name, String body, ConfigReader configReader) {
        this.functionName = name;
        if (returnType == null) {
            throw new SiddhiAppCreationException("Cannot find the return type of the function " + functionName);
        }
        Bindings engineBindings = new SimpleBindings();
        engineBindings.put(ScriptEngine.FILENAME, name);
        this.engine.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);
        try {
            engine.eval("function " + name + "(data)" + "{" + body + "}");
        } catch (ScriptException e) {
            throw new SiddhiAppCreationException("Compilation Failure of the JavaScript Function " + name, e);
        }
    }

    @Override
    public Object eval(String name, Object[] args) {
        try {
            return engine.invokeFunction(name, args, null);
        } catch (ScriptException e) {
            throw new SiddhiAppRuntimeException("Error evaluating JavaScript Function " + name, e);
        } catch (NoSuchMethodException e) {
            throw new SiddhiAppRuntimeException("Cannot evaluate non-existing JavaScript Function " + name, e);
        }
    }

    @Override
    public void setReturnType(Attribute.Type returnType) {
        this.returnType = returnType;
    }

    @Override
    public Attribute.Type getReturnType() {
        return returnType;
    }
}
