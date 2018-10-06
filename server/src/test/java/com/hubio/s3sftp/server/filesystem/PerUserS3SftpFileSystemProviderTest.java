package com.hubio.s3sftp.server.filesystem;

import com.hubio.s3sftp.server.S3SftpServer;
import lombok.val;
import org.apache.sshd.common.session.Session;
import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.mock;

class PerUserS3SftpFileSystemProviderTest implements WithAssertions {

    private final Session session = mock(Session.class);
    private final DelegatableS3FileSystemProvider provider = new DelegatableS3FileSystemProvider(session);
    private final PerUserS3SftpFileSystemProvider subject = new PerUserS3SftpFileSystemProvider(provider);

    @Test
    void overloadProperties() {
        //given
        val properties = new Properties();
        final Map<String, String> env = new HashMap<>();
        val username = "username";
        env.put(S3SftpServer.USERNAME, username);
        //when
        subject.overloadProperties(properties, env);
        //then
        assertThat(properties).as("Username key")
                              .containsOnlyKeys(S3SftpServer.USERNAME)
                              .as("Username correct")
                              .containsValue(username);
    }

    @Test
    void overloadPropertiesWhenUsernameIsMissing() {
        //given
        val properties = new Properties();
        final Map<String, String> env = new HashMap<>();
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                subject.overloadProperties(properties, env);
        //then
        assertThatCode(callable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Username not available");
    }

    @Test
    void getFileSystemKey() {
        //given
        val hostname = "uri";
        val uri = URI.create("s3://" + hostname);
        val props = new Properties();
        val username = "username";
        props.setProperty(S3SftpServer.USERNAME, username);
        //when
        final String result = subject.getFileSystemKey(uri, props);
        //then
        assertThat(result).isEqualTo(String.format("%s@%s", username, hostname));
    }

    @Test
    void getFileSystemKeyWhenUsernameIsMissing() {
        //given
        val hostname = "uri";
        val uri = URI.create("s3://" + hostname);
        val props = new Properties();
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                subject.getFileSystemKey(uri, props);
        //then
        assertThatCode(callable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username not specified");
    }

    @Test
    void getFileSystemKeyWhenUriIsInvalid() {
        //given
        val hostname = "uri+22";
        val uri = URI.create("s3://" + hostname);
        val props = new Properties();
        val username = "username";
        props.setProperty(S3SftpServer.USERNAME, username);
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                subject.getFileSystemKey(uri, props);
        //then
        assertThatCode(callable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid base URI: s3://uri+22");
    }
}
