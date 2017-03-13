/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import spock.lang.*

@Unroll
class UnrollWithVarsWithSelectors extends Specification {

    static class Person {
        String name
        String getSex() {
            name == "Fred" ? "Male" : "Female"
        }
    }

    def "#person.name is a #sex.toLowerCase() person"() {
        expect:
        person.getSex() == sex
        println "call: $person.name $person.sex"

        where:
        person                    || sex
        new Person(name: "Fred")  || "Male"
        new Person(name: "Wilma") || "Female"
    }
}
