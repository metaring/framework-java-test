/**
 *    Copyright 2019 MetaRing s.r.l.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.metaring.framework.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.metaring.framework.Core;
import com.metaring.framework.Tools;
import com.metaring.framework.persistence.PersistenceFunctionalitiesManager;
import com.metaring.framework.rpc.RpcFunctionalitiesManager;
import com.metaring.framework.rpc.RpcRequest;
import com.metaring.framework.rpc.RpcResponse;
import com.metaring.framework.type.DataRepresentation;
import com.metaring.framework.type.series.TextSeries;
import com.metaring.framework.util.StringUtil;

public abstract class AbstractFunctionalityTestCase {

    private static final CoreTestsBatterySupervisor SUPERVISOR;

    private static final String ERROR_MESSAGE_FORMAT = "Property: %s - Expecting %s - Found %s";

    private Long id;
    private String name;
    private String inputParam;
    private String expectedOutput;
    private String title;
    private String description;
    private TextSeries persistencePreambleActions;
    private TextSeries persistenceEpilogueVerifications;

    static {
        try {
            Class.forName(Core.class.getName());
        }
        catch (Exception e) {
        }
        SUPERVISOR = TestBatterySupervisorManager.initSupervisor();
    }

    @SuppressWarnings("unchecked")
    public AbstractFunctionalityTestCase(String title, String description, String inputParam, String expectedOutput, TextSeries persistencePreambleActions, TextSeries persistenceEpilogueVerifications) {
        this.title = title;
        this.description = description;
        this.inputParam = inputParam;
        this.expectedOutput = expectedOutput;
        this.persistencePreambleActions = persistencePreambleActions;
        this.persistenceEpilogueVerifications = persistenceEpilogueVerifications;
        long idN = 0;
        Class<? extends AbstractFunctionalityTestCase> clazz = this.getClass();
        while (!clazz.getSuperclass().equals(AbstractFunctionalityTestCase.class)) {
            clazz = (Class<? extends AbstractFunctionalityTestCase>) clazz.getSuperclass();
        }
        this.name = clazz.getPackage().getName();
        String simpleName = clazz.getSimpleName();
        if (Character.isDigit(simpleName.charAt(simpleName.length() - 1))) {
            int start = simpleName.length() - 1;
            while (Character.isDigit(--start))
                ;
            idN = Long.parseLong(simpleName.substring(++start));
        }
        this.id = idN;
    }

    @Before
    public final void before() {
        if (SUPERVISOR != null) {
            try {
                SUPERVISOR.preBeforeTest();
            }
            catch (Exception e) {
                throw new RuntimeException("Error while running Supervisor Pre Before Test", e);
            }
        }
        try {
            performBeforeTest();
        }
        catch (Exception e) {
            throw new RuntimeException("Error while running Test Case Before Test", e);
        }
        if (SUPERVISOR != null) {
            try {
                SUPERVISOR.postBeforeTest();
            }
            catch (Exception e) {
                throw new RuntimeException("Error while running Supervisor Post Before Test", e);
            }
        }
    }

    protected void performBeforeTest() throws Exception {

    }

    @Test
    public final void test() {
        CountDownLatch counter = new CountDownLatch(1);
        List<String> errorMessages = new ArrayList<>();

        performPreambleThenTest(counter, errorMessages);

        await(counter);
        if (errorMessages != null && !errorMessages.isEmpty()) {
            System.err.println("\n------ ASSERTION FAILED ------");
            System.err.println("\nFunctionality: " + name);
            System.err.println("\nTitle: " + title);
            System.err.println("\nDescription: " + description);
            System.err.println("\nClass: " + this.getClass().getName());
            System.err.println("\nErrors:");
            for (String errorMessage : errorMessages) {
                System.err.println("\n\t- " + errorMessage + ";");
            }
            System.err.println("\n-------------------------------------");
            System.err.println("\n");

            StringBuilder message = new StringBuilder();
            for (String errorMessage : errorMessages) {
                message.append("\n\n- ").append(errorMessage).append(";");
            }
            assertTrue("\n" + message.toString().trim(), false);
        }
        else {
            assertTrue(true);
        }
    }

    private final void performPreambleThenTest(CountDownLatch counter, List<String> errorMessages) {
        performPreambleThenTest(counter, errorMessages, null);
    }

    private final void performPreambleThenTest(CountDownLatch counter, List<String> errorMessages, Integer i) {
        if (persistencePreambleActions == null || !(persistencePreambleActions.size() > 0)) {
            performTest(counter, errorMessages);
            return;
        }
        if (i != null && i == persistencePreambleActions.size()) {
            try {
                performTest(counter, errorMessages);
//                PersistenceFunctionalitiesManager.execute(PersistenceFunctionalitiesManager.COMMIT_TRANSACTION, result -> {
//                    try {
//                        FunctionalitiesManager.verifyAndReturnFunctionalityExecutionResult(result);
//                        performTest(counter, errorMessages);
//                    }
//                    catch (Exception e) {
//                        String errorMessageFormat = "Persistence Preamble actions commit returned the following error:\n\n%s";
//                        errorMessages.add(String.format(errorMessageFormat, StringUtil.fromThrowableToString(e)));
//                        counter.countDown();
//                    }
//                });
            }
            catch (Exception e) {
                String errorMessageFormat = "Persistence Preamble actions commit returned the following error:\n\n%s";
                errorMessages.add(String.format(errorMessageFormat, StringUtil.fromThrowableToString(e)));
                counter.countDown();
            }
            return;
        }
        if (i == null) {
            performPreambleThenTest(counter, errorMessages, 0);
            return;
        }
        String preambleAction = persistencePreambleActions.get(i);
        try {
            PersistenceFunctionalitiesManager.update(preambleAction).handle((result, error) -> {
                if(error == null) {
                    performPreambleThenTest(counter, errorMessages, (i + 1));
                } else {
                    String errorMessageFormat = "Persistence Preamble action\n\n%s\n\nreturned the following error:\n\n%s";
                    errorMessages.add(String.format(errorMessageFormat, preambleAction, StringUtil.fromThrowableToString(error)));
                    counter.countDown();
                }
                return null;
            });
        }
        catch (Exception e) {
            String errorMessageFormat = "Persistence Preamble action\n\n%s\n\nreturned the following error:\n\n%s";
            errorMessages.add(String.format(errorMessageFormat, preambleAction, StringUtil.fromThrowableToString(e)));
            counter.countDown();
        }
    }

    private final void performTest(CountDownLatch counter, List<String> errorMessages) {
        RpcFunctionalitiesManager.call(RpcRequest.create(id, null, name, Tools.FACTORY_DATA_REPRESENTATION.fromJson(inputParam))).handle((response, error) -> {
            assertResult(response, result -> {
                if (result != null) {
                    errorMessages.addAll(result);
                }
                counter.countDown();
            });
            return null;
        });
    }

    private final void assertResult(RpcResponse rpcResponse, Consumer<List<String>> consumer) {
        String expected = expectedOutput;
        DataRepresentation expectedJson = null;
        try {
            expectedJson = Tools.FACTORY_DATA_REPRESENTATION.fromJson(expected);
        }
        catch (Exception e) {
        }

        String result = rpcResponse.getResult().toJson();
        DataRepresentation resultJson = null;
        try {
            resultJson = Tools.FACTORY_DATA_REPRESENTATION.fromJson(result);
        }
        catch (Exception e) {
        }

        List<String> errorMessages = new ArrayList<>();

        verifyResult(errorMessages, "testOutput", expectedJson, resultJson);

        verifyEpilogueQueriesThenAccept(consumer, errorMessages);
    }

    private final void verifyEpilogueQueriesThenAccept(Consumer<List<String>> consumer, List<String> errorMessages) {
        verifyEpilogueQueriesThenAccept(consumer, errorMessages, null);
    }

    private final void verifyEpilogueQueriesThenAccept(Consumer<List<String>> consumer, List<String> errorMessages, Integer i) {
        if (persistenceEpilogueVerifications == null || !(persistenceEpilogueVerifications.size() > 0) || (i != null && i == persistenceEpilogueVerifications.size())) {
            consumer.accept(errorMessages);
            return;
        }
        if (i == null) {
            verifyEpilogueQueriesThenAccept(consumer, errorMessages, 0);
            return;
        }
        String epilogueVerification = persistenceEpilogueVerifications.get(i);
        String manimulatedEpilogueVerification = String.format("SELECT (CASE WHEN EXISTS (%s) THEN 'YES' ELSE 'NO' END) as result", epilogueVerification);
        try {
            PersistenceFunctionalitiesManager.query(manimulatedEpilogueVerification).handle((databaseResponse, error) -> {
                try {
                    if(error != null) {
                        throw error;
                    }
                    if (databaseResponse == null) {
                        throw new NullPointerException("databaseResponse");
                    }
                    if (databaseResponse.length() != 1) {
                        throw new IllegalArgumentException("Query result must return just one row");
                    }

                    databaseResponse = databaseResponse.first();

                    if (databaseResponse.getProperties().size() != 1) {
                        throw new IllegalArgumentException("Query result must return just one column");
                    }
                    String value = databaseResponse.getText(databaseResponse.getProperties().get(0));
                    if (!value.equals("YES")) {
                        throw new IllegalArgumentException("Expected YES, found " + value);
                    }
                }
                catch (Throwable e) {
                    String errorMessageFormat = "Persistence epilogue verification\n\n%s\n\nreturned the following error:\n\n%s";
                    errorMessages.add(String.format(errorMessageFormat, epilogueVerification, StringUtil.fromThrowableToString(e)));
                }
                verifyEpilogueQueriesThenAccept(consumer, errorMessages, (i + 1));
                return null;
            });
        }
        catch (Exception e) {
            String errorMessageFormat = "Persistence epilogue verification\n\n%s\n\nreturned the following error:\n\n%s";
            errorMessages.add(String.format(errorMessageFormat, epilogueVerification, StringUtil.fromThrowableToString(e)));
            verifyEpilogueQueriesThenAccept(consumer, errorMessages, (i + 1));
        }
    }

    private final void verifyResult(List<String> errorMessages, String propertyName, DataRepresentation expectedJson, DataRepresentation resultJson) {
        if(expectedJson == null) {
            expectedJson = Tools.FACTORY_DATA_REPRESENTATION.fromJson("null");
        }
        if(resultJson == null) {
            resultJson = Tools.FACTORY_DATA_REPRESENTATION.fromJson("null");
        }
        if (expectedJson.isNull() && resultJson.isNull()) {
            return;
        }
        if (expectedJson.isNull()) {
            errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, "null value", resultJson.hasLength() ? ("an array of size " + resultJson.length() + ":\n\n" + resultJson.toString() + "\n\n") : resultJson.hasProperties() ? ("a JSON object:\n\n" + resultJson.toString() + "\n\n") : resultJson.toString()));
            return;
        }
        SpecialTypeEnum specialType = null;
        try {
            specialType = SpecialTypeEnum.fromText(expectedJson.toJson());
        }
        catch (Exception e) {
        }
        if (specialType != null) {
            if (specialType == SpecialTypeEnum.ANY) {
                if (resultJson.hasLength()) {
                    errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, SpecialTypeEnum.ANY.getText(), "an array of size " + resultJson.length() + ":\n\n" + resultJson.toString() + "\n\n"));
                }
                return;
            }

            if (specialType == SpecialTypeEnum.SOME) {
                if (resultJson.isNull()) {
                    errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, SpecialTypeEnum.SOME.getText(), "null value"));
                }
                else
                    if (resultJson.hasLength()) {
                        errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, SpecialTypeEnum.SOME.getText(), "an array of size " + resultJson.length() + ":\n\n" + resultJson.toString() + "\n\n"));
                    }
                return;
            }

            if (!resultJson.hasLength()) {
                errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, specialType.getText(), resultJson.hasProperties() ? ("a JSON Object:\n\n" + resultJson.toString() + "\n\n") : resultJson.isNull() ? "null value" : resultJson.toString()));
                return;
            }

            if (specialType == SpecialTypeEnum.ARRAY_UNDEFINED_LENGTH) {
                return;
            }
            if (specialType == SpecialTypeEnum.ARRAY_JUST_ONE_ELEMENT && resultJson.length() != 1) {
                errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, SpecialTypeEnum.ARRAY_JUST_ONE_ELEMENT.getText(), "an array of size " + resultJson.length() + ":\n\n" + resultJson.toJson() + "\n\n"));
                return;
            }

            if (specialType == SpecialTypeEnum.ARRAY_MORE_THAN_AN_ELEMENT && !(resultJson.length() > 1)) {
                errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, SpecialTypeEnum.ARRAY_MORE_THAN_AN_ELEMENT.getText(), "an array of size " + resultJson.length() + ":\n\n" + resultJson.toJson() + "\n\n"));
                return;
            }
        }
        else
            if (expectedJson.hasLength()) {
                if (!resultJson.hasLength()) {
                    errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, "an array of size " + expectedJson.length() + ":\n\n" + SpecialTypeEnum.clean(expectedJson.toJson()) + "\n\n", resultJson.hasProperties() ? ("a JSON Object:\n\n" + resultJson.toString() + "\n\n") : resultJson.isNull() ? "null value" : resultJson.toJson()));
                    return;
                }
                else {
                    if (expectedJson.length() != resultJson.length()) {
                        errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, "an array of size " + expectedJson.length() + ":\n\n" + SpecialTypeEnum.clean(expectedJson.toJson()) + "\n\n", "an array of size " + resultJson.length() + ":\n\n" + resultJson.toJson() + "\n\n"));
                        return;
                    }
                    for (int i = 0; i < expectedJson.length(); i++) {
                        verifyResult(errorMessages, propertyName + "[" + i + "]", expectedJson.get(i), resultJson.get(i));
                    }
                }
            }
            else
                if (expectedJson.hasProperties()) {
                    if (!resultJson.hasProperties()) {
                        errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, "a JSON object:\n\n" + SpecialTypeEnum.clean(expectedJson.toJson()) + "\n\n", resultJson.hasLength() ? ("an array of size " + resultJson.length() + ":\n\n" + resultJson.toString() + "\n\n") : resultJson.isNull() ? "null value" : resultJson.toString()));
                        return;
                    }
                    else {
                        TextSeries expectedPropertyNames = expectedJson.getProperties();
                        for (String expectedPropertyName : expectedPropertyNames) {
                            verifyResult(errorMessages, propertyName + "." + expectedPropertyName, expectedJson.get(expectedPropertyName), resultJson.get(expectedPropertyName));
                        }
                    }
                }
                else
                    if (!expectedJson.toJson().equals(resultJson.toJson())) {
                        errorMessages.add(String.format(ERROR_MESSAGE_FORMAT, propertyName, expectedJson.toString(), resultJson.toString()));
                    }
    }

    @After
    public final void after() {
        if (SUPERVISOR != null) {
            try {
                SUPERVISOR.preAfterTest();
            }
            catch (Exception e) {
                throw new RuntimeException("Error while running Supervisor Pre Before Test", e);
            }
        }
        try {
            performAfterTest();
        }
        catch (Exception e) {
            throw new RuntimeException("Error while running Test Case Before Test", e);
        }
        if (SUPERVISOR != null) {
            try {
                SUPERVISOR.postAfterTest();
            }
            catch (Exception e) {
                throw new RuntimeException("Error while running Supervisor Post Before Test", e);
            }
        }
    }

    protected void performAfterTest() throws Exception {

    }

    private static final void await(CountDownLatch counter) {
        try {
            counter.await();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
