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

import org.apache.log4j.Logger;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.extension.siddhi.script.js.test.util.SiddhiTestHelper;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.query.api.exception.SiddhiAppValidationException;

import java.util.concurrent.atomic.AtomicInteger;

public class EvalScriptTestCase {

    private static final Logger log = Logger.getLogger(EvalScriptTestCase.class);
    private AtomicInteger count = new AtomicInteger(0);

    @BeforeMethod
    public void init() {
        count.set(0);
    }

    @Test
    public void testEvalJavaScriptConcat() throws InterruptedException {

        log.info("testEvalJavaScriptConcat");

        SiddhiManager siddhiManager = new SiddhiManager();
//        siddhiManager.setExtension("scriptcript:javascript", EvalJavaScript.class);

        String concatFunc = "define function concatJ[JavaScript] return string {\n" +
                "  var str1 = data[0];\n" +
                "  var str2 = data[1];\n" +
                "  var str3 = data[2];\n" +
                "  var res = str1.concat(str2,str3);\n" +
                "  return res;\n" +
                "};";

        String cseEventStream = "define stream cseEventStream (symbol string, price float, volume long);";
        String query = ("@info(name = 'query1') from cseEventStream select price ," +
                " concatJ(symbol,' ',price) as concatStr " +
                "group by volume insert into mailOutput;");
        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(
                concatFunc + cseEventStream + query);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                Object value = inEvents[inEvents.length - 1].getData(1);
                AssertJUnit.assertEquals("WSO2 50", value);
                count.incrementAndGet();
            }
        });

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("cseEventStream");
        executionPlanRuntime.start();

        inputHandler.send(new Object[]{"WSO2", 50f, 60f, 60L, 6});
        SiddhiTestHelper.waitForEvents(100, 1, count, 60000);
        AssertJUnit.assertEquals(1, count.get());

        executionPlanRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testJavaScriptCompilationFailure() throws InterruptedException {

        log.info("testJavaScriptCompilationFailure");

        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("script:javascript", EvalJavaScript.class);

        String concatFunc = "define function concatJ[JavaScript] return string {\n" +
                "  var str1 = data[0;\n" +
                "  var str2 = data[1];\n" +
                "  var str3 = data[2];\n" +
                "  var res = str1.concat(str2,str3);\n" +
                "  return res;\n" +
                "};";

        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(concatFunc);

        executionPlanRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void testUseUndefinedFunction() throws InterruptedException {
        log.info("testUseUndefinedFunction");

        SiddhiManager siddhiManager = new SiddhiManager();
        siddhiManager.setExtension("script:javascript", EvalJavaScript.class);
        String cseEventStream = "define stream cseEventStream (symbol string, price float, volume long);";
        String query = ("@info(name = 'query1') from cseEventStream select price , " +
                "undefinedFunc(symbol,' ',price) as concatStr " +
                "group by volume insert into mailOutput;");
        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                Object value = inEvents[inEvents.length - 1].getData(1);
                AssertJUnit.assertEquals("IBM 700.0", value);
                count.incrementAndGet();
            }
        });

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("cseEventStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[]{"IBM", 700f, 100L});
        SiddhiTestHelper.waitForEvents(1000, 1, count, 60000);
        AssertJUnit.assertEquals(1, count.get());

        executionPlanRuntime.shutdown();
    }
}
