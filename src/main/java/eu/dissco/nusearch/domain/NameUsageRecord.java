package eu.dissco.nusearch.domain;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NameUsageRecord {

  private NameUsageCsvRow row;

  private Set<NameUsageCsvRow> classification;

  private NameUsageCsvRow accepted;

}
