package eu.dissco.nusearch.repository;

import static eu.dissco.nusearch.Profiles.STANDALONE;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Profile(STANDALONE)
@Repository
public class StandaloneStorageRepository implements StorageRepositoryInterface {

  @Override
  public void uploadIndex(String indexLocation) {
    throw new UnsupportedOperationException(
        "Method not implemented for standalone mode. Standalone mode uses local storage.");
  }

  @Override
  public void downloadIndex(String indexLocation) {
    throw new UnsupportedOperationException(
        "Method not implemented for standalone mode. Standalone mode uses local storage.");
  }
}
