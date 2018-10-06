package com.hubio.s3sftp.server.filesystem;

import com.hubio.s3sftp.server.S3SftpServer;
import com.upplication.s3fs.S3FileSystem;
import lombok.val;
import me.andrz.builder.map.MapBuilder;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class SingleBucketS3SftpFileSystemProviderTest implements WithAssertions {

    private final S3SftpFileSystemProvider delegate = mock(S3SftpFileSystemProvider.class);

    private final SingleBucketS3SftpFileSystemProvider subject = new SingleBucketS3SftpFileSystemProvider(delegate);

    @Test
    void overloadProperties() {
        //given
        val props = new Properties();
        final Map<String, String> env = new HashMap<>();
        val bucket = "bucket";
        env.put(S3SftpServer.BUCKET, bucket);
        //when
        subject.overloadProperties(props, env);
        //then
        then(delegate).should().overloadProperties(props, env);
        then(delegate).should().overloadPropertiesWithEnv(props, env, S3SftpServer.BUCKET);
    }

    @Test
    void overloadPropertiesWhenBucketIsMissing() {
        //given
        val props = new Properties();
        final Map<String, String> env = new HashMap<>();
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                subject.overloadProperties(props, env);
        //then
        assertThatCode(callable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Bucket not available");
    }

    @Test
    void newFileSystem() throws Throwable {
        //given
        val hostname = "uri";
        val uri = URI.create("s3://" + hostname);
        val props = new Properties();
        val bucket = "bucket";
        props.setProperty(S3SftpServer.BUCKET, bucket);
        val key = "uribucket";
        given(delegate.getFileSystemKey(any(), eq(props))).willReturn(key);
        //when
        final S3FileSystem result = subject.newFileSystem(uri, props).orElseThrow();
        //then
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(result)
             .as("Filtered S3 Filesystem")
             .isInstanceOf(FilteredS3FileSystem.class);
            s.assertThat(result)
             .as("field: bucket")
             .hasFieldOrPropertyWithValue("bucketName", bucket);
            s.assertThat(result)
             .as("field: endpoint")
             .hasFieldOrPropertyWithValue("endpoint", hostname);
            s.assertThat(result)
             .as("field: key")
             .hasFieldOrPropertyWithValue("key", key);
        });
    }

    @Test
    void newFileSystemWhenBucketIsMissing() {
        //given
        val hostname = "uri";
        val uri = URI.create("s3://" + hostname);
        val props = new Properties();
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                subject.newFileSystem(uri, props);
        //then
        assertThatCode(callable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bucket not specified");
    }

    @Test
    void getFileSystemWhenExists() {
        //given
        val uri = URI.create("s3://uri");
        final Map<String, String> env = emptyMap();
        val expected = mock(FileSystem.class);
        given(delegate.getFileSystem(uri, env)).willReturn(expected);
        given(delegate.fileSystemExists(uri, env)).willReturn(true);
        //when
        final FileSystem result = subject.getFileSystem(uri, env);
        //then
        assertThat(result).isSameAs(expected);
    }

    private Map<String, String> emptyMap() {
        return new MapBuilder<String, String>().build();
    }

    @Test
    void getFileSystemWhenDoesNotExist() {
        //given
        val uri = URI.create("s3://uri");
        final Map<String, String> env = emptyMap();
        val props = new Properties();
        props.setProperty(S3SftpServer.BUCKET, "bucket");
        given(delegate.mapAsProperties(env)).willReturn(props);
        given(delegate.fileSystemExists(uri, env)).willReturn(false);
        //when
        final FileSystem result = subject.getFileSystem(uri, env);
        //then
        assertThat(result).isInstanceOf(FilteredS3FileSystem.class);
        then(delegate).should(never())
                      .getFileSystem(uri, env);
    }
}
