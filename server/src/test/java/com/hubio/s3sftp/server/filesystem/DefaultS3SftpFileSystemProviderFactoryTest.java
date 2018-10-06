package com.hubio.s3sftp.server.filesystem;

import com.hubio.s3sftp.server.S3PathEnhancer;
import lombok.val;
import org.apache.sshd.common.session.Session;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class DefaultS3SftpFileSystemProviderFactoryTest implements WithAssertions {

    private final DefaultS3SftpFileSystemProviderFactory subject = new DefaultS3SftpFileSystemProviderFactory();

    @Test
    void createWith() {
        //given
        val s3PathEnhancer = mock(S3PathEnhancer.class);
        val session = mock(Session.class);
        //when
        final S3SftpFileSystemProvider result = subject.createWith(s3PathEnhancer, session);
        //then
        assertThat(result).isNotNull();
    }
}
