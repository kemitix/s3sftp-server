package com.hubio.s3sftp.server.filechannel;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.hubio.s3sftp.server.filesystem.FileSystemProviderMother;
import com.hubio.s3sftp.server.filesystem.S3SftpFileSystemProvider;
import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.S3FileSystemProvider;
import com.upplication.s3fs.S3Path;
import lombok.val;
import org.apache.sshd.common.session.Session;
import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.WithAssertions;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@EnableRuleMigrationSupport
class FileChannelS3SftpFileSystemProviderTest implements WithAssertions {

    private final Session session = mock(Session.class);
    private final S3SftpFileSystemProvider subject = FileSystemProviderMother.fileChannelProvider(session);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    void newFileChannel() throws Exception {
        //given
        val fileSystem = mock(S3FileSystem.class);
        //given(fileSystem.parts2Key(any())).willReturn("key");

        val provider = mock(S3FileSystemProvider.class);
        given(fileSystem.provider()).willReturn(provider);

        val amazonS3 = mock(AmazonS3.class);
        given(fileSystem.getClient()).willReturn(amazonS3);

        val objectMetadata = mock(ObjectMetadata.class);
        given(amazonS3.getObjectMetadata(any(), any())).willReturn(objectMetadata);

        val accessControlList = mock(AccessControlList.class);
        given(amazonS3.getObjectAcl(any(), any())).willReturn(accessControlList);

        val path = new S3Path(
                fileSystem, folder.newFile()
                                  .getAbsolutePath());
        val options = EnumSet.of(StandardOpenOption.CREATE_NEW);
        //when
        final FileChannel result = subject.newFileChannel(path, options);
        //then
        assertThat(result).as("S3FileChannel instance")
                          .isInstanceOf(S3FileChannel.class);
    }

    @Test
    void newFileChannelWhenNotS3Path() {
        //given
        val path = mock(Path.class);
        val options = EnumSet.of(StandardOpenOption.CREATE_NEW);
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                subject.newFileChannel(path, options);
        //then
        assertThatCode(callable)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("path must be an instance of S3Path");
    }
}
