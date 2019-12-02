/*
 * Copyright (c)  2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.event.Event;
import io.siddhi.core.exception.SiddhiAppCreationException;
import io.siddhi.core.query.output.callback.QueryCallback;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.util.EventPrinter;
import io.siddhi.extension.script.js.test.util.SiddhiTestHelper;
import org.apache.log4j.Logger;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class EvalFunctionExtensionTestCase {

    private static final Logger log = Logger.getLogger(EvalFunctionExtensionTestCase.class);
    private AtomicInteger count = new AtomicInteger(0);

    @BeforeMethod
    public void init() {
        count.set(0);
    }

    @Test
    public void testEvalArithmeticExpression() throws InterruptedException {
        log.info("testEvalArithmeticExpression testing an arithmetic expression evaluation");

        SiddhiManager siddhiManager = new SiddhiManager();
        String cseEventStream = "define stream inputStream(executionTemplate string);";
        String query = ("@info(name = 'query1') from inputStream" +
                " select js:eval(executionTemplate, 'int') as result " +
                "insert into outputStream;");
        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);
        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                Object value = inEvents[inEvents.length - 1].getData(0);
                AssertJUnit.assertEquals(24, value);
                count.incrementAndGet();
            }
        });

        InputHandler inputHandler = executionPlanRuntime.getInputHandler("inputStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[]{"7 + 8 + 9"});
        SiddhiTestHelper.waitForEvents(100, 1, count, 60000);
        AssertJUnit.assertEquals(1, count.get());
        executionPlanRuntime.shutdown();
    }

    @Test
    public void testEvalLogicalExpression() throws InterruptedException {
        log.info("testEvalArithmeticExpression testing a logical expression evaluation");

        SiddhiManager siddhiManager = new SiddhiManager();
        String cseEventStream = "define stream inputStream(executionTemplate string);";
        String query = ("@info(name = 'query1') from inputStream" +
                " select js:eval(executionTemplate, 'bool') as result " +
                "insert into outputStream;");

        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);
        executionPlanRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                Object value = inEvents[inEvents.length - 1].getData(0);
                AssertJUnit.assertEquals(false, value);
                count.incrementAndGet();
            }
        });
        InputHandler inputHandler = executionPlanRuntime.getInputHandler("inputStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[]{"7 > 100"});
        SiddhiTestHelper.waitForEvents(100, 1, count, 60000);
        AssertJUnit.assertEquals(1, count.get());
        executionPlanRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testEvalInvalidArgument() throws InterruptedException {
        log.info("testEvalInvalidExpression testing an invalid argument evaluation");

        SiddhiManager siddhiManager = new SiddhiManager();
        String cseEventStream = "define stream inputStream(executionTemplate string);";
        String query = ("@info(name = 'query1') from inputStream" +
                " select js:eval(executionTemplate, 'str') as result " +
                "insert into outputStream;");

        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);
        InputHandler inputHandler = executionPlanRuntime.getInputHandler("inputStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[]{"7 > 100 && 8 < 0"});
        executionPlanRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void testEvalInvalidNumberOfArguments() throws InterruptedException {
        log.info("testEvalInvalidExpression testing an invalid number of evaluation");

        SiddhiManager siddhiManager = new SiddhiManager();
        String cseEventStream = "define stream inputStream(executionTemplate string);";
        String query = ("@info(name = 'query1') from inputStream" +
                " select js:eval(executionTemplate) as result " +
                "insert into outputStream;");

        SiddhiAppRuntime executionPlanRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);
        InputHandler inputHandler = executionPlanRuntime.getInputHandler("inputStream");
        executionPlanRuntime.start();
        inputHandler.send(new Object[]{"7 > 100 && 8 < 0"});
        executionPlanRuntime.shutdown();
    }
}
