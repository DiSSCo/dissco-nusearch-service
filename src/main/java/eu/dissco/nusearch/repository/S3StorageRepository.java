package eu.dissco.nusearch.repository;

import eu.dissco.nusearch.Profiles;
import eu.dissco.nusearch.exception.IndexingFailedException;
import eu.dissco.nusearch.property.IndexingProperties;
import java.nio.file.Paths;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

@Slf4j
@Repository
@Profile({Profiles.S3_RESOLVER, Profiles.S3_INDEXER})
@AllArgsConstructor
public class S3StorageRepository implements StorageRepositoryInterface {

  private static final String BUCKET_NAME = "col-indexes";

  private final S3AsyncClient s3Client;
  private final S3TransferManager transferManager;
  private final IndexingProperties indexingProperties;

  public void uploadIndex(String indexLocation) throws IndexingFailedException {
    log.info("Uploading index to S3");
    try {
      s3Client.headBucket(b -> b.bucket(BUCKET_NAME));
    } catch (NoSuchBucketException e) {
      log.info("Bucket does not exists, please create the bucket first: {}", BUCKET_NAME);
      throw new IndexingFailedException(
          "Bucket does not exists, please create the bucket first: " + BUCKET_NAME);
    }

    var directoryUpload = transferManager.uploadDirectory(
        b -> b.source(Paths.get(indexLocation))
            .s3Prefix(String.valueOf(indexingProperties.getColDataset()))
            .bucket(BUCKET_NAME)
    );
    var completedDirectoryUpload = directoryUpload.completionFuture().join();
    if (!completedDirectoryUpload.failedTransfers().isEmpty()) {
      var firstEx = completedDirectoryUpload.failedTransfers().getFirst();
      log.error("Failed to upload index to S3 with message: {}", firstEx, firstEx.exception());
      throw new IndexingFailedException("Failed to upload index to S3");
    }
  }

  public void downloadIndex(String indexLocation) throws IndexingFailedException {
    log.info("Checking if index: {} exists in S3", indexingProperties.getColDataset());
    var directory = s3Client.listObjects(b -> b.bucket(BUCKET_NAME)
        .prefix(String.valueOf(indexingProperties.getColDataset()))).join();
    if (!directory.contents().isEmpty()) {
      log.info("Downloading index from S3");
      var directoryDownload = transferManager.downloadDirectory(
          b -> b.destination(Paths.get(indexLocation))
              .listObjectsV2RequestTransformer(
                  t -> t.prefix(String.valueOf(indexingProperties.getColDataset())))
              .bucket(BUCKET_NAME)
      );
      var completedDirectoryDownload = directoryDownload.completionFuture().join();
      if (!completedDirectoryDownload.failedTransfers().isEmpty()) {
        var firstEx = completedDirectoryDownload.failedTransfers().getFirst();
        log.error("Failed to download index to S3 with message: {}", firstEx, firstEx.exception());
        throw new IndexingFailedException("Failed to download index from S3");
      }
    } else {
      log.warn("No index for dataset {} found in S3", indexingProperties.getColDataset());
      throw new IndexingFailedException(
          "Index: " + indexingProperties.getColDataset() + " not available on S3");
    }
  }
}
