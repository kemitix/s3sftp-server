package com.hubio.s3sftp.server.filesystem;

import com.amazonaws.services.s3.AmazonS3;
import com.upplication.s3fs.AmazonS3Factory;
import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.util.Cache;
import lombok.val;
import me.andrz.builder.map.MapBuilder;
import net.kemitix.mon.result.Result;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.Session;
import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class InvertedS3FileSystemProviderTest implements WithAssertions {

    private final S3SftpFileSystemProvider delegate = mock(S3SftpFileSystemProvider.class);
    private final Session session = mock(Session.class);
    private final IoSession ioSession = mock(IoSession.class);
    private final SocketAddress remoteAddress = InetSocketAddress.createUnresolved("localhost", 22);

    private final InvertedS3FileSystemProvider subject = new InvertedS3FileSystemProvider(delegate);

    @Mock
    private DirectoryStream<Path> directoryStreamPath;

    @BeforeEach
    void setUp() {
        given(delegate.getSession()).willReturn(session);
        given(session.getUsername()).willReturn("username");
        given(session.getIoSession()).willReturn(ioSession);
        given(ioSession.getRemoteAddress()).willReturn(remoteAddress);
    }

    @Test
    void getScheme() {
        assertThatCode(subject::getScheme)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Inverted - getScheme");
    }

    @Test
    void newFileSystem() {
        //given
        val uri = URI.create("s3://uri");
        final Map<String, String> env = new HashMap<>();
        val expected = mock(S3FileSystem.class);
        given(delegate.newFileSystem(eq(uri), any())).willReturn(Result.ok(expected));
        //when
        final FileSystem result = subject.newFileSystem(uri, env);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void newFileSystem1() throws Exception {
        //given
        val path = mock(Path.class);
        final Map<String, String> env = new HashMap<>();
        val expected = mock(S3FileSystem.class);
        given(delegate.newFileSystem(eq(path), any())).willReturn(expected);
        //when
        final FileSystem result = subject.newFileSystem(path, env);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void getFileSystemKey() {
        //given
        val uri = URI.create("s3://uri");
        val props = new Properties();
        val expected = "expected";
        given(delegate.getFileSystemKey(uri, props)).willReturn(expected);
        //when
        final String result = subject.getFileSystemKey(uri, props);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void overloadProperties() {
        //given
        val props = new Properties();
        final Map<String, ?> env = new HashMap<>();
        //when
        subject.overloadPropertiesWithEnv(props, env, "");
        //then
        then(delegate).should().overloadPropertiesWithEnv(props, env, "");
    }

    @Test
    void overloadPropertiesWithEnv() {
        //given
        val props = new Properties();
        final Map<String, ?> env = new HashMap<>();
        val key = "key";
        given(delegate.overloadPropertiesWithEnv(props, env, key)).willReturn(true);
        //when
        final boolean result = subject.overloadPropertiesWithEnv(props, env, key);
        //then
        then(delegate).should().overloadPropertiesWithEnv(props, env, key);
        assertThat(result).isTrue();
    }

    @Test
    void getFileSystemUriEnv() {
        //given
        final URI uri = URI.create("s3://uri");
        final Map<String, ?> env = new HashMap<>();
        final FileSystem expected = mock(FileSystem.class);
        given(delegate.getFileSystem(uri, env)).willReturn(expected);
        //when
        val result = subject.getFileSystem(uri, env);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void getPath() {
        //given
        final URI uri = URI.create("s3://uri");
        final Path expected = mock(Path.class);
        given(delegate.getPath(uri)).willReturn(expected);
        //when
        val result = subject.getPath(uri);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void newDirectoryStream() throws Exception {
        MockitoAnnotations.initMocks(this);
        //given
        final Path dir = mock(Path.class);
        final DirectoryStream.Filter<? super Path> filter = (DirectoryStream.Filter<Path>) entry -> false;
        given(delegate.newDirectoryStream(dir, filter)).willReturn(directoryStreamPath);
        //when
        val result = subject.newDirectoryStream(dir, filter);
        //then
        assertThat(result).isSameAs(directoryStreamPath);
    }

    @Test
    void newInputStream() throws Exception {
        //given
        final Path path = mock(Path.class);
        final OpenOption options = StandardOpenOption.APPEND;
        final InputStream expected = mock(InputStream.class);
        given(delegate.newInputStream(path, options)).willReturn(expected);
        //when
        val result = subject.newInputStream(path, options);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void newByteChannel() throws Exception {
        //given
        final Path path = mock(Path.class);
        final Set<? extends OpenOption> options = EnumSet.noneOf(StandardOpenOption.class);
        final FileAttribute<?>[] attrs = new FileAttribute[0];
        final SeekableByteChannel expected = mock(SeekableByteChannel.class);
        given(delegate.newByteChannel(path, options, attrs)).willReturn(expected);
        //when
        val result = subject.newByteChannel(path, options, attrs);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void createDirectory() throws Exception {
        //given
        final Path path = mock(Path.class);
        final FileAttribute<?>[] attrs = new FileAttribute[0];
        //when
        subject.createDirectory(path, attrs);
        //then
        then(delegate).should()
                      .createDirectory(path, attrs);
    }

    @Test
    void delete() throws Exception {
        //given
        final Path path = mock(Path.class);
        //when
        subject.delete(path);
        //then
        then(delegate).should()
                      .delete(path);
    }

    @Test
    void copy() throws Exception {
        //given
        final Path source = mock(Path.class);
        final Path target = mock(Path.class);
        final CopyOption options = StandardCopyOption.ATOMIC_MOVE;
        //when
        subject.copy(source, target, options);
        //then
        then(delegate).should()
                      .copy(source, target, options);
    }

    @Test
    void move() throws Exception {
        //given
        final Path source = mock(Path.class);
        final Path target = mock(Path.class);
        final CopyOption options = StandardCopyOption.ATOMIC_MOVE;
        //when
        subject.move(source, target, options);
        //then
        then(delegate).should()
                      .move(source, target, options);
    }

    @Test
    void isSameFile() throws Exception {
        //given
        final Path path1 = mock(Path.class);
        final Path path2 = mock(Path.class);
        given(delegate.isSameFile(path1, path2)).willReturn(true)
                                                .willReturn(false);
        //when
        val resultTrue = subject.isSameFile(path1, path2);
        val resultFalse = subject.isSameFile(path1, path2);
        //then
        assertThat(resultTrue).isTrue();
        assertThat(resultFalse).isFalse();
    }

    @Test
    void isHidden() throws Exception {
        //given
        final Path path = mock(Path.class);
        given(delegate.isHidden(path)).willReturn(true)
                                      .willReturn(false);
        //when
        val resultTrue = subject.isHidden(path);
        val resultFalse = subject.isHidden(path);
        //then
        assertThat(resultTrue).isTrue();
        assertThat(resultFalse).isFalse();
    }

    @Test
    void getFileStore() throws Exception {
        //given
        final Path path = mock(Path.class);
        final FileStore expected = mock(FileStore.class);
        given(delegate.getFileStore(path)).willReturn(expected);
        //when
        val result = subject.getFileStore(path);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void checkAccess() throws Exception {
        //given
        final Path path = mock(Path.class);
        final AccessMode modes = AccessMode.READ;
        //when
        subject.checkAccess(path, modes);
        //then
        then(delegate).should()
                      .checkAccess(path, modes);
    }

    @Test
    void getFileAttributeView() {
        //given
        final Path path = mock(Path.class);
        final Class<FileAttributeView> type = FileAttributeView.class;
        final LinkOption options = LinkOption.NOFOLLOW_LINKS;
        final FileAttributeView expected = mock(FileAttributeView.class);
        given(delegate.getFileAttributeView(path, type, options)).willReturn(expected);
        //when
        val result = subject.getFileAttributeView(path, type, options);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void readAttributes() throws Exception {
        //given
        final Path path = mock(Path.class);
        final Class<BasicFileAttributes> type = BasicFileAttributes.class;
        final LinkOption options = LinkOption.NOFOLLOW_LINKS;
        final BasicFileAttributes expected = mock(BasicFileAttributes.class);
        given(delegate.readAttributes(path, type, options)).willReturn(expected);
        //when
        val result = subject.readAttributes(path, type, options);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void readAttributes1() throws Exception {
        //given
        final Path path = mock(Path.class);
        final String attributes = "attributes";
        final LinkOption options = LinkOption.NOFOLLOW_LINKS;
        final Map<String, Object> expected = new MapBuilder<String, Object>().build();
        given(delegate.readAttributes(path, attributes, options)).willReturn(expected);
        //when
        val result = subject.readAttributes(path, attributes, options);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void setAttribute() throws Exception {
        //given
        final Path path = mock(Path.class);
        final String attribute = "attribute";
        final Object value = new Object();
        final LinkOption options = LinkOption.NOFOLLOW_LINKS;
        //when
        subject.setAttribute(path, attribute, value, options);
        //then
        then(delegate).should()
                      .setAttribute(path, attribute, value, options);
    }

    @Test
    void getAmazonS3() {
        //given
        final URI uri = URI.create("s3://uri");
        final Properties props = new Properties();
        final AmazonS3 expected = mock(AmazonS3.class);
        given(delegate.getAmazonS3(uri, props)).willReturn(expected);
        //when
        val result = subject.getAmazonS3(uri, props);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void getAmazonS3Factory() {
        //given
        final Properties props = new Properties();
        final AmazonS3Factory expected = mock(AmazonS3Factory.class);
        given(delegate.getAmazonS3Factory(props)).willReturn(expected);
        //when
        val result = subject.getAmazonS3Factory(props);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void close() {
        //given
        final S3FileSystem s3FileSystem = mock(S3FileSystem.class);
        //when
        subject.close(s3FileSystem);
        //then
        /// no exception thrown
    }

    @Test
    void newOutputStream() throws Exception {
        //given
        final Path path = mock(Path.class);
        final OpenOption options = StandardOpenOption.WRITE;
        final OutputStream expected = mock(OutputStream.class);
        given(delegate.newOutputStream(path, options)).willReturn(expected);
        //when
        val result = subject.newOutputStream(path, options);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void newFileChannel() throws Exception {
        //given
        final Path path = mock(Path.class);
        final Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.READ);
        final FileAttribute<?>[] attrs = new FileAttribute[0];
        final FileChannel expected = mock(FileChannel.class);
        given(delegate.newFileChannel(path, options, attrs)).willReturn(expected);
        //when
        val result = subject.newFileChannel(path, options, attrs);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void newAsynchronousFileChannel() throws Exception {
        //given
        final Path path = mock(Path.class);
        final Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.READ);
        final FileAttribute<?>[] attrs = new FileAttribute[0];
        final ExecutorService executor = mock(ExecutorService.class);
        final AsynchronousFileChannel expected = mock(AsynchronousFileChannel.class);
        given(delegate.newAsynchronousFileChannel(path, options, executor, attrs)).willReturn(expected);
        //when
        val result = subject.newAsynchronousFileChannel(path, options, executor, attrs);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void createSymbolicLink() throws Exception {
        //given
        final Path link = mock(Path.class);
        final Path target = mock(Path.class);
        final FileAttribute<?>[] attrs = new FileAttribute[0];
        //when
        subject.createSymbolicLink(link, target, attrs);
        //then
        then(delegate).should()
                      .createSymbolicLink(link, target, attrs);
    }

    @Test
    void createLink() throws Exception {
        //given
        final Path link = mock(Path.class);
        final Path existing = mock(Path.class);
        //when
        subject.createLink(link, existing);
        //then
        then(delegate).should()
                      .createLink(link, existing);
    }

    @Test
    void deleteIfExists() throws Exception {
        //given
        final Path path = mock(Path.class);
        given(delegate.deleteIfExists(path)).willReturn(true)
                                            .willReturn(false);
        //when
        val resultTrue = subject.deleteIfExists(path);
        val resultFalse = subject.deleteIfExists(path);
        //then
        assertThat(resultTrue).isTrue();
        assertThat(resultFalse).isFalse();
    }

    @Test
    void readSymbolicLink() throws Exception {
        //given
        final Path link = mock(Path.class);
        final Path expected = mock(Path.class);
        given(delegate.readSymbolicLink(link)).willReturn(expected);
        //when
        val result = subject.readSymbolicLink(link);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Nested
    class UnsupportedOperations {

        private ThrowableAssert.ThrowingCallable callable;

        @AfterEach
        void thenThrowsException() {
            assertThatCode(callable)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Inverted");
        }

        @Test
        void validateUri() {
            callable = () -> subject.validateUri(URI.create("s3://uri"));
        }

        @Test
        void overloadPropertiesWithSystemProps() {
            callable = () -> subject.overloadPropertiesWithSystemProps(new Properties(), "key");
        }

        @Test
        void overloadPropertiesWithSystemEnv() {
            callable = () -> subject.overloadPropertiesWithSystemEnv(new Properties(), "key");
        }

        @Test
        void systemGetEnv() {
            callable = () -> subject.systemGetEnv("key");
        }

        @Test
        void getFileSystemUri() {
            callable = () -> subject.getFileSystem(URI.create("s3://uri"));
        }

        @Test
        void createFileSystem() {
            callable = () -> subject.createFileSystem(URI.create("s3://uri"), new Properties());
        }

        @Test
        void loadAmazonProperties() {
            callable = subject::loadAmazonProperties;
        }

        @Test
        void isOpen() {
            callable = () -> subject.isOpen(mock(S3FileSystem.class));
        }

        @Test
        void getCache() {
            callable = subject::getCache;
        }

        @Test
        void setCache() {
            callable = () -> subject.setCache(mock(Cache.class));
        }

    }

}




