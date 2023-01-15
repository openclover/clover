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
class UnrollWithSeqNumber extends Specification {

  def "maximum of two numbers"() {
    expect:
    Math.max(a, b) == c
    println "call: $a, $b"

    where:
    a << [3, 5, 9]
    b << [7, 4, 9]
    c << [7, 5, 9]
  }

}
