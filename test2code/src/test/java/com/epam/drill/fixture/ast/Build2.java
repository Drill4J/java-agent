/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.fixture.ast;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Build2 {

    void theSameMethodBody(List<String> strings) {
        BigDecimal i = BigDecimal.valueOf(10);
        int result = 0;
        for (int y = 0; y < 100; y++) {
            result = i.intValue();
            result++;
        }
        System.out.println(result);
        strings.stream().map(str -> str + "10").collect(Collectors.joining());
    }

    void differentContextChangeAtLambda(List<String> strings) {
        strings.stream().map(str ->
                str + "5"
        ).collect(Collectors.joining());
    }

    void differentContextInnerLambda(List<String> strings) {
        BigDecimal i = BigDecimal.valueOf(10);
        for (int y = 0; y < 100; y++) {
            System.out.println(i.add(BigDecimal.ONE));
        }
        strings.stream().map(str ->
                Arrays.stream(str.split(",")).map(it -> it + "100").collect(Collectors.toList())
        ).collect(Collectors.toList());
    }

    void referenceMethodCall(List<String> strings) {
        strings.stream().filter(el -> el.contains("qwe")).skip(1).forEach(System.err::printf);
    }

    void differentInstanceType() {
        Double valuer = new Random().nextDouble();
    }

    void multiANewArrayInsnNode() {
        int arraySize = 15;
        int[][][] array = new int[arraySize][][];
        for (int i = 0; i < arraySize; i++) {
            array[i][i][i] = new Random().nextInt();
        }
    }

    void tableSwitchMethodTest(String value) {
        switch (value) {
            case "1": {
                System.out.println(1);
                break;
            }
            case "2": {
                System.out.println(2);
                break;
            }
            case "3": {
                System.out.println(3);
                break;
            }
            default: {
                System.out.println("Default-123");
            }
        }
    }

    void lookupSwitchMethodTest(Integer value) {
        switch (value) {
            case 2: {
                System.out.println(2);
                break;
            }
            case 102: {
                System.out.println(102);
                break;
            }
            case 202: {
                System.out.println(202);
                break;
            }
            default: {
                System.out.println("Default-2");
            }
        }
    }

    void callOtherMethod() {
        Integer i = new Random().nextInt();
        callMe(i);
    }

    private void callMe(Integer i) {
        for (int j = 0; j < i; j++) {
            System.out.println("Print");
        }
    }

    void methodWithDifferentParams(Integer test, Integer test2) {
        System.out.println(test);
    }

    void methodWithIteratedValue(Integer test) {
        for (int i = 0; i < test; i += 2) {
            System.out.println(test);
        }
    }

    void changeLocalVarName(Integer test) {
        String changedName = String.valueOf(test);
        for (int i = 0; i < test; i++) {
            System.out.println(changedName);
        }
    }

    void constantLambdaHashCalculation(List<String> strings) {
        strings.stream().map(str -> str + "12").collect(Collectors.joining());
    }
}
