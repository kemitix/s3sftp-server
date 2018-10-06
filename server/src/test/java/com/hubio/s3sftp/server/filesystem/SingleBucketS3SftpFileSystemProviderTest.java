package com.hubio.s3sftp.server.filesystem;

import com.hubio.s3sftp.server.S3SftpServer;
import lombok.val;
import me.andrz.builder.map.MapBuilder;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link SingleBucketS3SftpFileSystemProvider}.
 *
 * @author Paul Campbell (paul.campbell@hubio.com)
 */
public class SingleBucketS3SftpFileSystemProviderTest {

    private SingleBucketS3SftpFileSystemProvider subject;

    @Mock
    private S3SftpFileSystemProvider delegate;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        subject = new SingleBucketS3SftpFileSystemProvider(delegate);
    }

    @Test
    public void overloadProperties() {
        //given
        val props = new Properties();
        val env = new HashMap<String, String>();
        val bucket = "bucket";
        env.put(S3SftpServer.BUCKET, bucket);
        //when
        subject.overloadProperties(props, env);
        //then
        then(delegate).should()
                      .overloadProperties(props, env);
        then(delegate).should()
                      .overloadPropertiesWithEnv(props, env, S3SftpServer.BUCKET);
    }

    @Test
    public void overloadPropertiesWhenBucketIsMissing() {
        //given
        val props = new Properties();
        val env = new HashMap<String, String>();
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Bucket not available");
        //when
        subject.overloadProperties(props, env);
    }

    @Test
    public void newFileSystem() throws Throwable {
        //given
        val hostname = "uri";
        val uri = URI.create("s3://" + hostname);
        val props = new Properties();
        val bucket = "bucket";
        props.setProperty(S3SftpServer.BUCKET, bucket);
        val key = "uribucket";
        given(delegate.getFileSystemKey(any(), eq(props))).willReturn(key);
        //when
        val result = subject.newFileSystem(uri, props).orElseThrow();
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
    public void newFileSystemWhenBucketIsMissing() {
        //given
        val hostname = "uri";
        val uri = URI.create("s3://" + hostname);
        val props = new Properties();
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Bucket not specified");
        //when
        subject.newFileSystem(uri, props);
    }

    @Test
    public void getFileSystemWhenExists() {
        //given
        final URI uri = URI.create("s3://uri");
        final Map<String, String> env = new MapBuilder<String, String>().build();
        final FileSystem expected = mock(FileSystem.class);
        given(delegate.getFileSystem(uri, env)).willReturn(expected);
        given(delegate.fileSystemExists(uri, env)).willReturn(true);
        //when
        val result = subject.getFileSystem(uri, env);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    public void getFileSystemWhenDoesNotExist() {
        //given
        final URI uri = URI.create("s3://uri");
        final Map<String, String> env = new MapBuilder<String, String>().build();
        final Properties props = new Properties();
        props.setProperty(S3SftpServer.BUCKET, "bucket");
        given(delegate.mapAsProperties(env)).willReturn(props);
        given(delegate.fileSystemExists(uri, env)).willReturn(false);
        //when
        val result = subject.getFileSystem(uri, env);
        //then
        assertThat(result).isInstanceOf(FilteredS3FileSystem.class);
        then(delegate).should(never())
                      .getFileSystem(uri, env);
    }
}
