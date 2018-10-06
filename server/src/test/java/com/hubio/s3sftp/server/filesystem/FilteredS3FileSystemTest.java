package com.hubio.s3sftp.server.filesystem;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.upplication.s3fs.S3FileSystemProvider;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.nio.file.FileStore;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class FilteredS3FileSystemTest implements WithAssertions {

    private final S3FileSystemProvider s3FileSystemProvider = mock(S3FileSystemProvider.class);
    private final AmazonS3 amazonS3 = mock(AmazonS3.class);

    private final FilteredS3FileSystem subject =
            new FilteredS3FileSystem(s3FileSystemProvider, "testKey", amazonS3, "testEndpoint", "testUserHome");

    @Test
    void testGetFileStoresWithValidHome() {
        //given
        given(amazonS3.listBuckets()).willReturn(
                Arrays.asList(
                        new Bucket("invalidHome3"),
                        new Bucket("invalidHome2"),
                        // the only valid value:
                        new Bucket("testUserHome"),
                        new Bucket("invalidHome1")
                ));
        //when
        final List<FileStore> fileStores = StreamSupport.stream(subject.getFileStores()
                .spliterator(), false)
                .collect(Collectors.toList());
        //then
        assertThat(fileStores).hasSize(1);
        assertThat(fileStores).extractingResultOf("name").containsOnly("testUserHome");
        assertThat(fileStores).extractingResultOf("type").containsOnly("S3Bucket");
    }

    @Test
    void testGetFileStoresWithInvalidHome() {
        //given
        given(amazonS3.listBuckets()).willReturn(
                Arrays.asList(
                        new Bucket("invalidHome3"),
                        new Bucket("invalidHome2"),
                        new Bucket("invalidHome1")));
        //when
        final List<FileStore> fileStores = StreamSupport.stream(subject.getFileStores()
                .spliterator(), false)
                .collect(Collectors.toList());
        //then
        assertThat(fileStores).isEmpty();
    }
}
