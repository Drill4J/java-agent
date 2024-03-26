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
package com.epam.drill.fixture;

public class ClassWithLoop implements Runnable {
    @Override
    public void run() {
        printAnotherPlace(1);
        printAnotherPlace(2);
    }

    private void printAnotherPlace(int count) {
        int i = 0;
        while (i < count) {
            System.out.println("printAnotherPlace");
            i++;
        }
        int[] marks = new int[]{80, 85};
        for (int item : marks) {
            System.out.println(item);
        }
    }
}
