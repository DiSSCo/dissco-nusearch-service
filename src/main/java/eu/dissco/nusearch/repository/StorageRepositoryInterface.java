package eu.dissco.nusearch.repository;

import eu.dissco.nusearch.exception.IndexingFailedException;

public interface StorageRepositoryInterface {

  void uploadIndex(String indexLocation) throws IndexingFailedException;

  void downloadIndex(String indexLocation) throws IndexingFailedException;

}
