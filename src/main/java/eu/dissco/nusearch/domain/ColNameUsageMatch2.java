package eu.dissco.nusearch.domain;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ColNameUsageMatch2 {

  private boolean synonym;
  private ColDpRankedName usage;
  private ColDpRankedName acceptedUsage;
  private List<ColDpRankedName> classification = new ArrayList<>();
  private Diagnostics diagnostics = new Diagnostics();

}
