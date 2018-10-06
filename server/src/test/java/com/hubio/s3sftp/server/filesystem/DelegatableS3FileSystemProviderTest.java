package com.hubio.s3sftp.server.filesystem;

import com.amazonaws.services.s3.AmazonS3;
import com.upplication.s3fs.AmazonS3Factory;
import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.S3FileSystemProvider;
import lombok.val;
import net.kemitix.mon.result.Result;
import org.apache.sshd.common.session.Session;
import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.*;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.mock;

class DelegatableS3FileSystemProviderTest implements WithAssertions {

    private final AmazonS3 amazonS3 = mock(AmazonS3.class);
    private final Session session = mock(Session.class);
    private final DelegatableS3FileSystemProvider subject = new DelegatableS3FileSystemProvider(session);

    @Test
    void getFileSystemKey() {
        //given
        val uri = URI.create("s3://uri");
        val props = new Properties();
        //when
        final String result = subject.getFileSystemKey(uri, props);
        //then
        assertThat(result).isEqualTo("uri");
    }

    @Test
    void validateUri() {
        //given
        val uri = URI.create("s3://uri");
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                subject.validateUri(uri);
        //then
        assertThatCode(callable).doesNotThrowAnyException();
    }

    @Test
    void overloadProperties() {
        //given
        final Map<String, String> env = new HashMap<>();
        val accessKey = "access key";
        env.put(AmazonS3Factory.ACCESS_KEY, accessKey);
        val secretKey = "secret key";
        env.put(AmazonS3Factory.SECRET_KEY, secretKey);
        //when
        val props = new Properties();
        subject.overloadProperties(props, env);
        //then
        assertThat(props).containsEntry(AmazonS3Factory.ACCESS_KEY, accessKey)
                         .containsEntry(AmazonS3Factory.SECRET_KEY, secretKey);
    }

    @Test
    void fileSystemExists() {
        //given
        val uri = URI.create("s3://uri");
        final Map<String, String> env = new HashMap<>();
        /// force closure of any file systems held in static map in core FileSystemProvider
        subject.getAllFileSystems()
               .forEach(subject::close);
        assumeThat(subject.fileSystemExists(uri, env))
                .as("filesystem does not exist")
                .isFalse();
        //when
        subject.newFileSystem(uri, subject.mapAsProperties(env));
        final boolean resultTrue = subject.fileSystemExists(uri, env);
        //then
        assertThat(resultTrue).as("filesystem does exist")
                              .isTrue();
    }

    @Test
    void getS3FileSystemProvider() {
        //when
        final S3FileSystemProvider result = subject.getS3FileSystemProvider();
        //then
        assertThat(result).isSameAs(subject);
    }

    @Test
    void overloadPropertiesWithEnv() {
        //given
        val props = new Properties();
        final Map<String, String> env = new HashMap<>();
        val key = "key";
        val value = "value";
        env.put(key, value);
        env.put("other key", "other value");
        //when
        final boolean result = subject.overloadPropertiesWithEnv(props, env, key);
        //then
        assertThat(result).isTrue();
        assertThat(props).containsExactly(new AbstractMap.SimpleEntry<>(key, value));
    }

    @Test
    void getAmazonS3WhenSet() {
        //given
        val uri = URI.create("S3://uri");
        val props = new Properties();
        subject.setAmazonS3(amazonS3);
        //when
        final AmazonS3 result = subject.getAmazonS3(uri, props);
        //then
        assertThat(result).isSameAs(amazonS3);
    }

    @Test
    void getAmazonS3WhenNotSet() {
        //given
        val uri = URI.create("S3://uri");
        val props = new Properties();
        //when
        final AmazonS3 result = subject.getAmazonS3(uri, props);
        //then
        assertThat(result).isNotNull();
    }

    @Test
    void getAmazonS3Factory() {
        //given
        val props = new Properties();
        //when
        final AmazonS3Factory result = subject.getAmazonS3Factory(props);
        //then
        assertThat(result).isNotNull();
    }

    @Test
    void newFileSystem() {
        //given
        val uri = URI.create("s3://uri1");
        val props = new Properties();
        //when
        final Result<S3FileSystem> result = subject.newFileSystem(uri, props);
        //then
        assertThat(result.isOkay()).isTrue();
    }

    @Test
    void newFileSystemWithProperties() {
        //given
        val hostname = UUID.randomUUID().toString();
        val accessKey = UUID.randomUUID().toString();
        val secretKey = UUID.randomUUID().toString();
        val uri = URI.create("s3://" + hostname);
        val props = new Properties();
        props.setProperty(AmazonS3Factory.ACCESS_KEY, accessKey);
        props.setProperty(AmazonS3Factory.SECRET_KEY, secretKey);
        //when
        final Result<S3FileSystem> result = subject.newFileSystem(uri, props);
        //then
        final S3FileSystem s3FileSystem = result.orElseThrowUnchecked();
        assertThat(s3FileSystem.getKey()).contains(accessKey);
        assertThat(s3FileSystem.getKey()).contains(hostname);
    }

    @Test
    void mapAsProperties() {
        //given
        final Map<String, String> map = new HashMap<>();
        val key = "key";
        val value = "value";
        map.put(key, value);
        //when
        final Properties result = subject.mapAsProperties(map);
        //then
        assertThat(result).containsExactly(new AbstractMap.SimpleEntry<>(key, value));
    }
}
