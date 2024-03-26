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

import java.util.List;

public class CheckProbeRanges {

    public void noOp() {
    }

    public void oneOp(List<String> list) {
        list.add("1");
    }

    public void twoOps(List<String> list) {
        list.add("1");
        list.add("2");
    }

    public String ifOp(boolean b) {
        if (b)
            return "true";
        else
            return "false";
    }

    public String ifExpr(boolean b) {
        return b ? "true" : "false";
    }

    public void whileOp(List<String> list) {
        while (!list.isEmpty()) {
            list.remove(0);
        }
    }

    public void methodWithLambda(List<String> list) {
        list.forEach(s -> {
            System.out.println(s);
        });
    }

    public void methodRef(List<String> list) {
        list.forEach(System.out::println);
    }

}
