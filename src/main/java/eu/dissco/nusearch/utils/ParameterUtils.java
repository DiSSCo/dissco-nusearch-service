/*
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
package eu.dissco.nusearch.utils;

// Copied and adapted from GBIF:
// https://github.com/gbif/checklistbank/blob/master/checklistbank-common/src/main/java/org/gbif/checklistbank/utils/ParameterUtils.java

import org.apache.commons.lang3.StringUtils;

public class ParameterUtils {

  private ParameterUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * @return the first non blank value
   */
  public static String first(String... values){
    if (values != null) {
      for (String val : values) {
        if (!StringUtils.isBlank(val)) {
          return val;
        }
      }
    }
    return null;
  }
}
