package com.hubio.s3sftp.server.filesystem;

import com.hubio.s3sftp.server.S3SftpServer;
import com.upplication.s3fs.AmazonS3Factory;
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

class JailedS3SftpFileSystemProviderTest implements WithAssertions {

    private final Session session = mock(Session.class);
    private final DelegatableS3FileSystemProvider provider = new DelegatableS3FileSystemProvider(session);
    private final JailedS3SftpFileSystemProvider subject = new JailedS3SftpFileSystemProvider(provider);

    @Test
    void overloadProperties() {
        //given
        val props = new Properties();
        final Map<String, String> env = new HashMap<>();
        val jail = "jail";
        env.put(S3SftpServer.JAIL, jail);
        //when
        subject.overloadProperties(props, env);
        //then
        assertThat(props).as("has jail key")
                         .containsKey(S3SftpServer.JAIL);
        assertThat(props.getProperty(S3SftpServer.JAIL)).as("has jail value")
                                                        .isEqualTo(jail);
    }

    @Test
    void overloadPropertiesWithMissingJail() {
        //given
        val props = new Properties();
        final Map<String, String> env = new HashMap<>();
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                subject.overloadProperties(props, env);
        //then
        assertThatThrownBy(callable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Jail not available");
    }

    @Test
    void getFileSystemKey() {
        //given
        val uri = URI.create("s3://uri");
        val props = new Properties();
        props.setProperty(S3SftpServer.JAIL, "jail");
        props.setProperty(AmazonS3Factory.ACCESS_KEY, "accessKey");
        //when
        final String result = subject.getFileSystemKey(uri, props);
        //then
        /// only the host and any accesskey is used to make the key (unless an username is included)
        assertThat(result).isEqualTo("accessKey@uri");
    }
}
