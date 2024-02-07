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
package eu.dissco.nusearch.repository;

// Copied and adapted from GBIF:
// https://github.com/gbif/checklistbank/blob/master/checklistbank-nub/src/main/java/org/gbif/nub/lookup/fuzzy/NubIndex.java

import eu.dissco.nusearch.component.ScientificNameAnalyzer;
import eu.dissco.nusearch.domain.ColDpClassification;
import eu.dissco.nusearch.domain.ColDpNameUsageMatch;
import eu.dissco.nusearch.utils.LuceneUtils;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A read only lucene index keeping the core attributes of a nub name usage. The index exposes
 * matching methods that allow to select usages based on their nub key or do fuzzy matches on the
 * canonical name alone.
 * <p>
 * The index lies at the core of the nub matching service to preselect a list of potential good
 * matches.
 * <p>
 * The index can either be purely memory based or on the filesystem using a memory mapped OS cache.
 * For the entire nub with roughly 4.5 million usages this index requires 4GB of heap memory if the
 * RAMDirectory is used. The memory mapped file index uses very little heap memory and instead all
 * available memory should be given to the OS to enabling caching on the file system level.
 */
@Component
@AllArgsConstructor
public class NubIndex {

  /**
   * Type for a stored IntField with max precision to minimize memory usage as we dont need range
   * queries.
   */
  public static final String FIELD_ID = "id";
  public static final String FIELD_CANONICAL_NAME = "canonical";
  public static final String FIELD_SCIENTIFIC_NAME = "sciname";
  public static final String FIELD_RANK = "rank";
  public static final String FIELD_STATUS = "status";
  private static final Logger LOG = LoggerFactory.getLogger(NubIndex.class);

  private final IndexSearcher searcher;
  private final ScientificNameAnalyzer analyzer;

  public static void addIfNotNull(Document doc, String key, String value) {
    if (value != null) {
      doc.add(new StoredField(key, value));
    }
  }

  private static int toInt(Document doc, String field) {
    return (int) doc.getField(field).numericValue();
  }

  /**
   * Builds a NameUsageMatch instance from a lucene Document and populates all fields but the
   * matching specifics i.e. confidence and matchType.
   */
  private ColDpNameUsageMatch fromDoc(Document doc) {
    ColDpNameUsageMatch match = new ColDpNameUsageMatch();
    match.setColId(doc.get(FIELD_ID));
    match.setColParentId(doc.get("pId"));
    match.setAuthorship(doc.get("auth"));
    match.setSpecificEpithet(doc.get("se"));
    match.setGenericName(doc.get("gn"));
    match.setCode(doc.get("code"));
    match.setNameStatus(doc.get("nstatus"));
    match.setExtinct(Boolean.parseBoolean(doc.get("e")));
    match.setKingdom(doc.get("k"));
    match.setPhylum(doc.get("p"));
    match.setClazz(doc.get("c"));
    match.setOrder(doc.get("o"));
    match.setFamily(doc.get("f"));
    match.setGenus(doc.get("g"));
    match.setSubgenus(doc.get("sub"));
    match.setSpecies(doc.get("s"));

    var classification = new ArrayList<ColDpClassification>();
    int i = 0;
    boolean continueFlag = true;
    while (continueFlag) {
      var colDpClassification = new ColDpClassification();
      var colId = doc.get("c[" + i + "].id");
      if (colId != null) {
        colDpClassification.setColId(colId);
        colDpClassification.setScientificName(doc.get("c[" + i + "].sn"));
        colDpClassification.setAuthorship(doc.get("c[" + i + "].auth"));
        colDpClassification.setRank(doc.get("c[" + i + "].rank"));
        colDpClassification.setStatus(TaxonomicStatus.valueOf(doc.get("c[" + i + "].status")));
        colDpClassification.setExtinct(Boolean.parseBoolean(doc.get("c[" + i + "].e")));
        classification.add(colDpClassification);
        i++;
      } else {
        continueFlag = false;
      }
    }
    match.setClassifications(classification);

    match.setScientificName(doc.get(FIELD_SCIENTIFIC_NAME));
    match.setCanonicalName(doc.get(FIELD_CANONICAL_NAME));

    match.setRank(Rank.values()[toInt(doc, FIELD_RANK)]);
    match.setTaxonomicStatus(TaxonomicStatus.values()[toInt(doc, FIELD_STATUS)]);

    return match;
  }


  public ColDpNameUsageMatch matchByUsageId(String colId) {
    Query q = new TermQuery(new Term(NubIndex.FIELD_ID, colId));
    try {
      var docs = searcher.search(q, 3);
      var storedFields = searcher.storedFields();
      if (docs.scoreDocs.length > 0) {
        Document doc = storedFields.document(docs.scoreDocs[0].doc);
        var match = fromDoc(doc);
        match.setConfidence(100);
        return match;
      } else {
        LOG.warn("No usage {} found in lucene index", colId);
      }
    } catch (IOException e) {
      LOG.error("Cannot load usage {} from lucene index", colId, e);
    }
    return null;
  }

  public List<ColDpNameUsageMatch> matchByName(String name, boolean fuzzySearch, int maxMatches) {
    // use the same lucene analyzer to normalize input
    final String analyzedName = LuceneUtils.analyzeString(analyzer, name).get(0);
    LOG.debug("Analyzed {} query \"{}\" becomes >>{}<<", fuzzySearch ? "fuzzy" : "straight", name,
        analyzedName);

    // query needs to have at least 2 letters to match a real name
    if (analyzedName.length() < 2) {
      return Lists.newArrayList();
    }

    Term t = new Term(NubIndex.FIELD_CANONICAL_NAME, analyzedName);
    Query q;
    if (fuzzySearch) {
      // allow 2 edits for names longer than 10 chars
      q = new FuzzyQuery(t, analyzedName.length() > 10 ? 2 : 1, 1);
    } else {
      q = new TermQuery(t);
    }

    try {
      return search(q, name, fuzzySearch, maxMatches);
    } catch (RuntimeException e) {
      // for example TooComplexToDeterminizeException, see http://dev.gbif.org/issues/browse/POR-2725
      LOG.warn("Lucene failed to fuzzy search for name [{}]. Try a straight match instead", name);
      return search(new TermQuery(t), name, false, maxMatches);
    }
  }

  private List<ColDpNameUsageMatch> search(Query q, String name, boolean fuzzySearch,
      int maxMatches) {
    List<ColDpNameUsageMatch> results = Lists.newArrayList();
    try {
      TopDocs docs = searcher.search(q, maxMatches);
      var storedFields = searcher.storedFields();
      if (docs.scoreDocs.length > 0) {
        for (ScoreDoc sdoc : docs.scoreDocs) {
          var match = fromDoc(storedFields.document(sdoc.doc));
          if (name.equalsIgnoreCase(match.getCanonicalName())) {
            match.setMatchType(NameUsageMatch.MatchType.EXACT);
            results.add(match);
          } else {
            // even though we used a term query for straight matching the lucene analyzer has already normalized
            // the name drastically. So we include these matches here only in case of fuzzy queries
            match.setMatchType(NameUsageMatch.MatchType.FUZZY);
            results.add(match);
          }
        }

      } else {
        LOG.debug("No {} match for name {}", fuzzySearch ? "fuzzy" : "straight", name);
      }

    } catch (IOException e) {
      LOG.error("lucene search error", e);
    }
    return results;
  }
  public List<ColDpNameUsageMatch> autocomplete(String prefix, int limit) {
    final String analyzedName = LuceneUtils.analyzeString(analyzer, prefix).get(0);
    Sort sort = new Sort(new SortField("canString", Type.STRING));
    var prefixQuery = new PrefixQuery(new Term(FIELD_CANONICAL_NAME, analyzedName));
    var resultList = new ArrayList<ColDpNameUsageMatch>(limit);
    try {
      var docs = searcher.search(prefixQuery, limit, sort);
      var storedFields = searcher.storedFields();
      for (ScoreDoc sdoc : docs.scoreDocs) {
        Document doc = storedFields.document(sdoc.doc);
        var match = fromDoc(doc);
        match.setConfidence(100);
        resultList.add(match);
      }
    } catch (IOException e) {
      LOG.error("lucene search error", e);
    }
    return resultList;
  }

}
