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
package eu.dissco.nusearch.service;

// Copied and adapted from GBIF:
// https://github.com/gbif/checklistbank/blob/master/checklistbank-nub-ws/src/main/java/org/gbif/checklistbank/ws/nub/ClassificationProvider.java

import static eu.dissco.nusearch.Profiles.S3_RESOLVER;
import static eu.dissco.nusearch.Profiles.STANDALONE;
import static org.gbif.ws.util.CommonWsUtils.getFirst;

import eu.dissco.nusearch.domain.Classification;
import java.util.Map;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.ws.server.provider.ContextProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Provider class that extracts LinneanClassification instances from the query parameters.
 */
@Profile({STANDALONE, S3_RESOLVER})
public class ClassificationProvider
    implements HandlerMethodArgumentResolver, ContextProvider<LinneanClassification> {

  @Override
  public LinneanClassification getValue(WebRequest webRequest) {
    Map<String, String[]> params = webRequest.getParameterMap();

    Classification cl = new Classification();
    cl.setKingdom(getFirst(params,"kingdom"));
    cl.setPhylum(getFirst(params,"phylum"));
    cl.setClazz(getFirst(params,"class"));
    cl.setOrder(getFirst(params,"order"));
    cl.setFamily(getFirst(params,"family"));
    cl.setGenus(getFirst(params,"genus"));
    cl.setSubgenus(getFirst(params,"subgenus"));
    cl.setSpecies(getFirst(params,"species"));

    return cl;
  }

  @Override
  public boolean supportsParameter(MethodParameter methodParameter) {
    return LinneanClassification.class.equals(methodParameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
    MethodParameter methodParameter,
    ModelAndViewContainer modelAndViewContainer,
    NativeWebRequest nativeWebRequest,
    WebDataBinderFactory webDataBinderFactory
  ) {
    return getValue(nativeWebRequest);
  }
}
