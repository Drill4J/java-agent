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

public class Build1 {

    void theSameMethodBody(List<String> strings) {
        BigDecimal i = BigDecimal.valueOf(10);
        int result = 0;
        for (int y = 0; y < 100; y++) {
            result = i.intValue();
            result++;
        }
        System.out.println(result);
        strings.stream().map(str -> str + "12").collect(Collectors.joining());
    }

    void differentContextChangeAtLambda(List<String> strings) {
        strings.stream().map(str -> str + "2").collect(Collectors.joining());
    }

    void differentContextInnerLambda(List<String> strings) {
        BigDecimal i = BigDecimal.valueOf(10);
        for (int y = 0; y < 100; y++) {
            System.out.println(i.add(BigDecimal.ONE));
        }
        strings.stream().map(str ->
                Arrays.stream(str.split(".")).map(it -> it + "2").collect(Collectors.toList())
        ).collect(Collectors.toList());
    }

    void referenceMethodCall(List<String> strings) {
        strings.stream().filter(el -> el.contains("qwe")).skip(1).forEach(System.out::printf);
    }

    void differentInstanceType() {
        Integer valuer = new Random().nextInt();
    }

    void multiANewArrayInsnNode() {
        int arraySize = 13;
        Integer[][] array = new Integer[arraySize][arraySize];
        for (int i = 0; i < arraySize; i++) {
            array[i][i] = new Random().nextInt();
        }
    }

    void tableSwitchMethodTest(int value) {
        switch (value) {
            case 1: {
                System.out.println(1);
                break;
            }
            case 2: {
                System.out.println(2);
                break;
            }
            case 3: {
                System.out.println(3);
                break;
            }
            case 4: {
                System.out.println(4);
                break;
            }
            case 5: {
                System.out.println(5);
                break;
            }
            default: {
                System.out.println("Default");
            }
        }
    }

    void lookupSwitchMethodTest(Integer value) {
        switch (value) {
            case 1: {
                System.out.println(1);
                break;
            }
            case 100: {
                System.out.println(100);
                break;
            }
            case 200: {
                System.out.println(200);
                break;
            }
            case 300: {
                System.out.println(300);
                break;
            }
            default: {
                System.out.println("Default");
            }
        }
    }

    void callOtherMethod() {
        Integer i = new Random().nextInt();
        callMe(i);
    }

    private void callMe(Integer i) {
        for (int j = 0; j < i; j++) {
            System.out.println("Test");
        }
    }

    void methodWithDifferentParams(Integer test) {
        System.out.println(test);
    }

    void methodWithIteratedValue(Integer test) {
        for (int i = 0; i < test; i++) {
            System.out.println(test);
        }
    }

    void changeLocalVarName(Integer test) {
        String strValue = String.valueOf(test);
        for (int i = 0; i < test; i++) {
            System.out.println(strValue);
        }
    }

    void constantLambdaHashCalculation(List<String> strings) {
        strings.stream().map(str -> str + "12").collect(Collectors.joining());
    }
}
