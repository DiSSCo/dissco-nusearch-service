package eu.dissco.nusearch.repository;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import eu.dissco.nusearch.exception.IndexingFailedException;
import eu.dissco.nusearch.property.IndexingProperties;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FailedFileDownload;
import software.amazon.awssdk.transfer.s3.model.FailedFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

@ExtendWith(MockitoExtension.class)
class S3StorageRepositoryTest {

  @Mock
  private S3AsyncClient s3Client;

  @Mock
  private S3TransferManager transferManager;

  @Mock
  private IndexingProperties indexingProperties;
  private S3StorageRepository s3StorageRepository;

  @BeforeEach
  void setup() {
    s3StorageRepository = new S3StorageRepository(s3Client, transferManager, indexingProperties);
  }

  @Test
  void testUploadIndex() throws IndexingFailedException {
    // Given
    String indexLocation = "src/test/resources/index";
    given(s3Client.createBucket(any(Consumer.class))).willThrow(
        BucketAlreadyOwnedByYouException.class);
    var dirUpload = mock(DirectoryUpload.class);
    given(transferManager.uploadDirectory(any(Consumer.class))).willReturn(dirUpload);
    given(dirUpload.completionFuture()).willReturn(CompletableFuture.completedFuture(
        CompletedDirectoryUpload.builder().failedTransfers(List.of()).build()));

    // When / Then
    s3StorageRepository.uploadIndex(indexLocation);
  }

  @Test
  void testUploadIndexFailed() {
    // Given
    String indexLocation = "src/test/resources/index";
    given(s3Client.createBucket(any(Consumer.class))).willThrow(
        BucketAlreadyOwnedByYouException.class);
    var dirUpload = mock(DirectoryUpload.class);
    given(transferManager.uploadDirectory(any(Consumer.class))).willReturn(dirUpload);
    given(dirUpload.completionFuture()).willReturn(CompletableFuture.completedFuture(
        CompletedDirectoryUpload.builder().failedTransfers(
            List.of(FailedFileUpload.builder()
                .exception(new IOException())
                .request(
                    UploadFileRequest.builder().putObjectRequest(PutObjectRequest.builder().build())
                        .source(Paths.get(indexLocation).toFile()).build())
                .build())).build()));

    // When/Then
    assertThrows(IndexingFailedException.class,
        () -> s3StorageRepository.uploadIndex(indexLocation));
  }

  @Test
  void testDownloadIndex() throws IndexingFailedException {
    // Given
    String indexLocation = "src/test/resources/index";
    given(s3Client.listObjects(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(
        ListObjectsResponse.builder().contents(
                List.of(S3Object.builder().key("a").build(),
                    S3Object.builder().key("couple").build(),
                    S3Object.builder().key("of").build(),
                    S3Object.builder().key("objects").build()))
            .build()));
    var dirDownload = mock(DirectoryDownload.class);
    given(transferManager.downloadDirectory(any(Consumer.class))).willReturn(dirDownload);
    given(dirDownload.completionFuture()).willReturn(CompletableFuture.completedFuture(
        CompletedDirectoryDownload.builder().failedTransfers(List.of()).build()));

    // When / Then
    s3StorageRepository.downloadIndex(indexLocation);
  }

  @Test
  void testDownloadIndexNoFiles() {
    // Given
    String indexLocation = "src/test/resources/index";
    given(s3Client.listObjects(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(
        ListObjectsResponse.builder().build()));

    // When/Then
    assertThrows(IndexingFailedException.class,
        () -> s3StorageRepository.downloadIndex(indexLocation));
  }

  @Test
  void testDownloadIndexFailed() {
    // Given
    String indexLocation = "src/test/resources/index";
    given(s3Client.listObjects(any(Consumer.class))).willReturn(CompletableFuture.completedFuture(
        ListObjectsResponse.builder().contents(
                List.of(S3Object.builder().key("a").build(),
                    S3Object.builder().key("couple").build(),
                    S3Object.builder().key("of").build(),
                    S3Object.builder().key("objects").build()))
            .build()));
    var dirDownload = mock(DirectoryDownload.class);
    given(transferManager.downloadDirectory(any(Consumer.class))).willReturn(dirDownload);
    given(dirDownload.completionFuture()).willReturn(CompletableFuture.completedFuture(
        CompletedDirectoryDownload.builder().failedTransfers(
            List.of(FailedFileDownload.builder().exception(new IOException()).request(
                DownloadFileRequest.builder().getObjectRequest(GetObjectRequest.builder().build())
                    .destination(Paths.get(indexLocation).toFile())
                    .build()).build())).build()));

    // When/Then
    assertThrows(IndexingFailedException.class,
        () -> s3StorageRepository.downloadIndex(indexLocation));
  }


}
