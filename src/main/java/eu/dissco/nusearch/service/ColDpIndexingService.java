package eu.dissco.nusearch.service;

import static eu.dissco.nusearch.Profiles.S3_INDEXER;
import static eu.dissco.nusearch.Profiles.S3_RESOLVER;
import static eu.dissco.nusearch.Profiles.STANDALONE;
import static eu.dissco.nusearch.repository.NubIndex.FIELD_CANONICAL_NAME;
import static eu.dissco.nusearch.repository.NubIndex.FIELD_ID;
import static eu.dissco.nusearch.repository.NubIndex.FIELD_RANK;
import static eu.dissco.nusearch.repository.NubIndex.FIELD_SCIENTIFIC_NAME;
import static eu.dissco.nusearch.repository.NubIndex.FIELD_STATUS;
import static eu.dissco.nusearch.repository.NubIndex.addIfNotNull;

import com.univocity.parsers.tsv.TsvRoutines;
import eu.dissco.nusearch.domain.ColDpClassification;
import eu.dissco.nusearch.domain.ColDpNameUsage;
import eu.dissco.nusearch.domain.NameUsageCsvRow;
import eu.dissco.nusearch.property.IndexingProperties;
import eu.dissco.nusearch.repository.StorageRepositoryInterface;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.zip.ZipFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.util.BytesRef;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TaxonomicStatus;
import org.gbif.nameparser.NameParserGbifV1;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ColDpIndexingService {

  private final TsvRoutines routine;
  private final IndexWriter indexWriter;
  private final IndexingProperties properties;
  private final NameParserGbifV1 nameParser;
  private final ColDpDownloadingService colDpDownloadingService;
  private final Environment environment;
  private final StorageRepositoryInterface storageRepository;

  private static ColDpClassification toColDpClassification(NameUsageCsvRow classification) {
    ColDpClassification colDpClassification = new ColDpClassification();
    colDpClassification.setColId(classification.getId());
    colDpClassification.setScientificName(classification.getScientificName());
    colDpClassification.setAuthorship(classification.getAuthorship());
    colDpClassification.setRank(classification.getRank());
    colDpClassification.setStatus(parseTaxonomicStatus(classification.getStatus()));
    colDpClassification.setExtinct(getExtinct(classification));
    return colDpClassification;
  }

  private static Rank getRank(NameUsageCsvRow row) {
    try {
      var verbatimRank = row.getRank();
      if (verbatimRank == null) {
        log.warn("Rank is null for: {}", row.getScientificName());
        return Rank.UNRANKED;
      }
      var rank = verbatimRank.toUpperCase().replace(' ', '_');
      return Rank.valueOf(rank);
    } catch (IllegalArgumentException e) {
      log.warn("Unknown rank: {} defaulting to Unranked", row.getRank());
      return Rank.UNRANKED;
    }
  }

  private static TaxonomicStatus parseTaxonomicStatus(String status) {
    TaxonomicStatus result;
    switch (status) {
      case "accepted" -> result = TaxonomicStatus.ACCEPTED;
      case "synonym" -> result = TaxonomicStatus.SYNONYM;
      default -> result = TaxonomicStatus.DOUBTFUL;
    }
    return result;
  }

  private static String setClassification(int i) {
    return "c[" + i + "]";
  }

  private static boolean getExtinct(NameUsageCsvRow row) {
    var stringValue = row.getExtinct();
    if (stringValue == null) {
      return false;
    } else {
      return stringValue.equals("1") || Boolean.parseBoolean(stringValue);
    }
  }

  @PostConstruct
  void setup() throws Exception {
    if (environment.matchesProfiles(S3_RESOLVER)) {
      storageRepository.downloadIndex(properties.getIndexLocation());
    } else {
      var tempFile = colDpDownloadingService.downloadColDpDataset();
      log.info("Start filling cache...");
      var nameUsageMap = populateCache(tempFile);
      log.info("Starting indexer...");
      processNameUsages(nameUsageMap, tempFile);
      log.info("Finished indexing");
      if (environment.matchesProfiles(S3_INDEXER)) {
        storageRepository.uploadIndex(properties.getIndexLocation());
      }
    }
  }

  @PreDestroy
  void destroy() throws IOException {
    if (environment.matchesProfiles(S3_INDEXER, STANDALONE)) {
      cleanupFiles(properties.getTempColdpLocation());
    }
    cleanUpIndex(properties.getIndexLocation());
  }

  private void cleanUpIndex(String indexLocation) throws IOException {
    log.info("Deleting index folder: {}", indexLocation);
    var path = Path.of(indexLocation);
    Files.walk(path)
        .map(Path::toFile)
        .forEach(File::delete);
    Files.delete(path);
  }

  private void cleanupFiles(String tempColdpLocation) throws IOException {
    log.info("Deleting temporary coldp download: {}", tempColdpLocation);
    var tempFile = Path.of(tempColdpLocation);
    if (Files.exists(tempFile)) {
      Files.delete(tempFile);
    }
  }

  private Document parseToDocument(NameUsageCsvRow row,
      HashSet<NameUsageCsvRow> classification) {
    ColDpNameUsage nameUsage = new ColDpNameUsage();
    nameUsage.setColId(row.getId());
    nameUsage.setColParentId(row.getParentId());
    nameUsage.setScientificName(row.getScientificName());
    nameUsage.setRank(getRank(row));
    nameUsage.setTaxonomicStatus(parseTaxonomicStatus(row.getStatus()));
    nameUsage.setAuthorship(row.getAuthorship());
    nameUsage.setSpecificEpithet(row.getSpecificEpithet());
    nameUsage.setGenericName(row.getGenericName());
    nameUsage.setCode(row.getCode());
    nameUsage.setNameStatus(row.getNameStatus());
    nameUsage.setExtinct(getExtinct(row));
    nameUsage.setClassifications(
        classification.stream().map(ColDpIndexingService::toColDpClassification).toList());
    for (NameUsageCsvRow nameUsageCsvRow : classification) {
      if (nameUsageCsvRow.getRank() != null) {
        switch (nameUsageCsvRow.getRank()) {
          case "kingdom" -> nameUsage.setKingdom(nameUsageCsvRow.getScientificName());
          case "phylum" -> nameUsage.setPhylum(nameUsageCsvRow.getScientificName());
          case "class" -> nameUsage.setClazz(nameUsageCsvRow.getScientificName());
          case "order" -> nameUsage.setOrder(nameUsageCsvRow.getScientificName());
          case "family" -> nameUsage.setFamily(nameUsageCsvRow.getScientificName());
          case "genus" -> nameUsage.setGenus(nameUsageCsvRow.getScientificName());
          case "subgenus" -> nameUsage.setSubgenus(nameUsageCsvRow.getScientificName());
          case "species" -> nameUsage.setSpecies(nameUsageCsvRow.getScientificName());
          default -> log.debug("Unknown rank: {}", nameUsageCsvRow.getRank());
        }
      } else {
        log.warn("Rank is null for: {}", nameUsageCsvRow.getScientificName());
      }
    }
    return toDoc(nameUsage);
  }

  private Document toDoc(ColDpNameUsage nameUsage) {
    Document doc = new Document();
    Optional<String> optCanonical = Optional.ofNullable(
        nameParser.parseToCanonical(nameUsage.getScientificName(), nameUsage.getRank()));
    final String canonical = optCanonical.orElse(nameUsage.getScientificName());

    doc.add(new StringField(FIELD_ID, nameUsage.getColId(), Field.Store.YES));

    // analyzed name field - this is what we search upon
    doc.add(new TextField(FIELD_CANONICAL_NAME, canonical, Field.Store.YES));
    doc.add(new SortedDocValuesField("canString", new BytesRef(canonical)));

    // store full name and classification only to return a full match object for hits
    doc.add(new StoredField(FIELD_SCIENTIFIC_NAME,
        nameUsage.getScientificName()));

    addIfNotNull(doc, "pId", nameUsage.getColParentId());
    addIfNotNull(doc, "auth", nameUsage.getAuthorship());
    addIfNotNull(doc, "se", nameUsage.getSpecificEpithet());
    addIfNotNull(doc, "gn", nameUsage.getGenericName());
    addIfNotNull(doc, "code", nameUsage.getCode());
    addIfNotNull(doc, "nstatus", nameUsage.getNameStatus());
    addIfNotNull(doc, "e", String.valueOf(nameUsage.isExtinct()));
    addIfNotNull(doc, "k", nameUsage.getKingdom());
    addIfNotNull(doc, "p", nameUsage.getPhylum());
    addIfNotNull(doc, "c", nameUsage.getClazz());
    addIfNotNull(doc, "o", nameUsage.getOrder());
    addIfNotNull(doc, "f", nameUsage.getFamily());
    addIfNotNull(doc, "g", nameUsage.getGenus());
    addIfNotNull(doc, "sub", nameUsage.getSubgenus());
    addIfNotNull(doc, "s", nameUsage.getSpecies());

    // higher ranks
    for (int i = 0; i < nameUsage.getClassifications().size(); i++) {
      doc.add(new StoredField(setClassification(i) + ".id",
          nameUsage.getClassifications().get(i).getColId()));
      doc.add(new StoredField(setClassification(i) + ".sn",
          nameUsage.getClassifications().get(i).getScientificName()));
      addIfNotNull(doc, setClassification(i) + ".auth",
          nameUsage.getClassifications().get(i).getAuthorship());
      addIfNotNull(doc, setClassification(i) + ".rank",
          nameUsage.getClassifications().get(i).getRank());
      addIfNotNull(doc, setClassification(i) + ".status",
          nameUsage.getClassifications().get(i).getStatus().toString());
      addIfNotNull(doc, setClassification(i) + ".e",
          String.valueOf(nameUsage.getClassifications().get(i).isExtinct()));
    }

    // store rank if existing as ordinal int
    // this lucene index is not persistent, so not risk in changing ordinal numbers
    var rank = nameUsage.getRank();
    doc.add(new StoredField(FIELD_RANK, rank == null ? Rank.UNRANKED.ordinal() : rank.ordinal()));

    // allow only 3 values for status: accepted, doubtful and synonym
    var status = nameUsage.getTaxonomicStatus();
    if (status == null) {
      status = TaxonomicStatus.DOUBTFUL;
    } else if (status.isSynonym()) {
      status = TaxonomicStatus.SYNONYM;
    }
    doc.add(new StoredField(FIELD_STATUS, status.ordinal()));

    return doc;
  }

  private void processNameUsages(HashMap<String, NameUsageCsvRow> nameUsageMap, Path path)
      throws IOException {
    log.info("Opening coldp zip file at location: {}", path);
    try (var zis = new ZipFile(path.toFile())) {
      var entry = zis.getEntry("NameUsage.tsv");
      try (var in = new BufferedInputStream(zis.getInputStream(entry))) {
        processNameUsageFile(nameUsageMap, in);
      }
      log.info("Finished indexing, closing index writer");
      indexWriter.close();
    }
  }

  private void processNameUsageFile(HashMap<String, NameUsageCsvRow> nameUsageMap,
      InputStream bais)
      throws IOException {
    log.info("Processing name usages...");
    for (var row : routine.iterate(NameUsageCsvRow.class, bais)) {
      var parentId = row.getParentId();
      var classification = new HashSet<NameUsageCsvRow>();
      while (true) {
        var parent = nameUsageMap.get(parentId);
        if (parent != null) {
          classification.add(parent);
          parentId = parent.getParentId();
        } else {
          var document = parseToDocument(row, classification);
          log.debug("Resulting document: {}", document);
          indexWriter.addDocument(document);
          break;
        }
      }
    }
  }

  private HashMap<String, NameUsageCsvRow> populateCache(Path path)
      throws IOException {
    try (var zis = new ZipFile(path.toFile())) {
      var nameUsageMap = new HashMap<String, NameUsageCsvRow>();
      var entry = zis.getEntry("NameUsage.tsv");
      try (var in = new BufferedInputStream(zis.getInputStream(entry))) {
        return populateCacheWithNameUsages(in, nameUsageMap);
      }
    }
  }

  private HashMap<String, NameUsageCsvRow> populateCacheWithNameUsages(
      InputStream bais, HashMap<String, NameUsageCsvRow> nameUsageMap) {
    var count = 0;
    for (var row : routine.iterate(NameUsageCsvRow.class, bais)) {
      count += 1;
      if (nameUsageMap.containsKey(row.getId())) {
        log.warn("Duplicate key: {}", row.getId());
      } else {
        nameUsageMap.put(row.getId(), row);
      }
      if (count % 10000 == 0) {
        log.info("Processed {} rows", count);
      }
    }
    log.info("Total rows read is: {}", nameUsageMap.size());
    return nameUsageMap;
  }
}
