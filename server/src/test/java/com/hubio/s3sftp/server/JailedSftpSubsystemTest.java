package com.hubio.s3sftp.server;

import com.hubio.s3sftp.server.filesystem.UserFileSystemResolver;
import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.S3Path;
import lombok.val;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.random.Random;
import org.apache.sshd.common.util.threads.CloseableExecutorService;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.*;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class JailedSftpSubsystemTest implements WithAssertions {

    private final CloseableExecutorService executorService = mock(CloseableExecutorService.class);
    private final SessionBucket sessionBucket = mock(SessionBucket.class);
    private final SessionHome sessionHome = mock(SessionHome.class);
    private final SessionJail sessionJail = mock(SessionJail.class);
    private final UserFileSystemResolver userFileSystemResolver = mock(UserFileSystemResolver.class);
    private final ServerSession serverSession = mock(ServerSession.class);
    private final ServerFactoryManager serverFactoryManager = mock(ServerFactoryManager.class);
    private final Random randomizer = mock(Random.class);
    private final S3FileSystem s3FileSystem = mock(S3FileSystem.class);
    private final SftpFileSystemAccessor accessor = mock(SftpFileSystemAccessor.class);
    private final SftpErrorStatusDataHandler errorStatusDataHandler = mock(SftpErrorStatusDataHandler.class);

    private JailedSftpSubsystem sftpSubsystem =
            new JailedSftpSubsystem(
                    executorService, UnsupportedAttributePolicy.Warn, sessionBucket,
                    sessionHome, sessionJail, userFileSystemResolver, accessor, errorStatusDataHandler);

    private SftpSession sftpSession;

    @Test
    void shouldRemoveOnlyPermissionsFromAttributes() throws Exception {
        //given
        val path = Paths.get(".");
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("basic", "keep me");
        attributes.put("permissions", "drop me");
        final Map<String, Object> modifiedAttributes = new HashMap<>();
        sftpSubsystem.addSftpEventListener(new AbstractSftpEventListenerAdapter() {
            @Override
            public void modifyingAttributes(final ServerSession session, final Path path, final Map<String, ?> attrs) {
                modifiedAttributes.putAll(attrs);
            }
        });
        //when
        sftpSubsystem.doSetAttributes(path, attributes);
        //then
        assertThat(modifiedAttributes)
                .containsOnlyKeys("basic")
                .containsValues("keep me")
                .doesNotContainValue("drop me");
    }

    @Nested
    class ResolveFile {

        private final String username = "bob";
        private final String bucket = "bucket";

        @Mock
        private Factory<Random> randomFactory;

        @BeforeEach
        void setUp() {
            MockitoAnnotations.initMocks(this);
            given(serverSession.getFactoryManager()).willReturn(serverFactoryManager);
            given(serverFactoryManager.getRandomFactory()).willReturn(randomFactory);
            given(randomFactory.create()).willReturn(randomizer);
            given(serverSession.getUsername()).willReturn(username);
            given(serverSession.getIntProperty(SftpSubsystem.FILE_HANDLE_SIZE, SftpSubsystem.DEFAULT_FILE_HANDLE_SIZE))
                    .willReturn(SftpSubsystem.DEFAULT_FILE_HANDLE_SIZE);
            given(serverSession.getIntProperty(SftpSubsystem.MAX_FILE_HANDLE_RAND_ROUNDS, SftpSubsystem.DEFAULT_FILE_HANDLE_ROUNDS))
                    .willReturn(SftpSubsystem.DEFAULT_FILE_HANDLE_ROUNDS);
            sftpSubsystem.setSession(serverSession);
            sftpSession = SftpSession.of(serverSession);
            given(sessionBucket.getBucket(any())).willReturn(bucket);
            given(sessionJail.getJail(any())).willReturn("");
            given(sessionHome.getHomePath(any())).willReturn("");
            given(userFileSystemResolver.resolve(username)).willReturn(Optional.of(s3FileSystem));
            given(s3FileSystem.getSeparator()).willReturn("/");
        }

        @Nested
        class Root {

            @BeforeEach
            void setUp() {
                given(sessionHome.getHomePath(sftpSession)).willReturn("");
            }

            @Test
            void resolveFileRoot() {
                //when
                final Path path = sftpSubsystem.resolveFile(".");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/");
            }

            @Test
            void resolveFileDirectory() {
                //when
                final Path path = sftpSubsystem.resolveFile("/subdir");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/subdir");
            }

            @Test
            void resolveFileFile() {
                //when
                final Path path = sftpSubsystem.resolveFile("/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/file.txt");
            }

            @Test
            void resolverFilePreresolved() {
                //when
                final Path path = sftpSubsystem.resolveFile("/bucket/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/file.txt");
            }

            @Test
            void resolveFileShouldRemoveTrailingPeriod() {
                //when
                final Path path = sftpSubsystem.resolveFile("dir/.");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/dir");
            }

            @Test
            void resolveFileShouldResolveParentDir() {
                //when
                final Path path = sftpSubsystem.resolveFile("dir/subdir/..");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/dir/subdir/..");
            }
        }

        @Nested
        class Home {

            @BeforeEach
            void setUp() {
                given(sessionHome.getHomePath(any())).willReturn(username);
            }

            @Test
            void resolveUserDir() {
                //when
                final Path path = sftpSubsystem.resolveFile("");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob");
            }

            @Test
            void resolveWithinUserDir() {
                //when
                final Path path = sftpSubsystem.resolveFile("file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob/file.txt");
            }

            @Test
            void resolveFileRoot() {
                //when
                final Path path = sftpSubsystem.resolveFile(".");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob");
            }

            @Test
            void resolveFileDirectory() {
                //when
                final Path path = sftpSubsystem.resolveFile("/subdir");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob/subdir");
            }

            @Test
            void resolveFileFile() {
                //when
                final Path path = sftpSubsystem.resolveFile("/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob/file.txt");
            }

            @Test
            void resolverFilePreresolved() {
                //when
                final Path path = sftpSubsystem.resolveFile("/bucket/bob/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob/file.txt");
            }

            @Test
            void resolveFileShouldRemoveTrailingPeriod() {
                //given
                val path = "dir/.";
                //when
                final Path result = sftpSubsystem.resolveFile(path);
                //then
                assertThat(S3PathUtil.dirPath(((S3Path) result))).isEqualTo("/bucket/bob/dir");
            }

            @Test
            void resolveFileShouldResolveParentDir() {
                //given
                val path = "dir/subdir/..";
                //when
                final Path result = sftpSubsystem.resolveFile(path);
                //then
                assertThat(S3PathUtil.dirPath(((S3Path) result))).isEqualTo("/bucket/bob/dir/subdir/..");
            }
        }

        @Nested
        class Jailed {

            @BeforeEach
            void setUp() {
                given(sessionHome.getHomePath(any())).willReturn(String.format("users/%s", username));
                given(sessionJail.getJail(any())).willReturn("users");
                // i.e. user should only see their own username as the visible path
            }

            @Test
            void dot() {
                //when
                final Path path = sftpSubsystem.resolveFile(".");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/");
            }

            @Test
            void home() {
                //when
                final Path path = sftpSubsystem.resolveFile("/bob");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/");
            }

            @Test
            void file() {
                //when
                final Path path = sftpSubsystem.resolveFile("/bob/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/file.txt");
            }

            @Test
            void fullJailedPath() {
                //when
                final Path path = sftpSubsystem.resolveFile("/bucket/users/bob/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/file.txt");
            }

            @Test
            void removeTrailingPeriod() {
                //when
                final Path path = sftpSubsystem.resolveFile("/bob/dir/.");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/dir");
            }

            @Test
            void acceptNavToParentDir() {
                //when
                final Path path = sftpSubsystem.resolveFile("/bob/dir/subdir/..");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/dir/subdir/..");
            }

            @Test
            void homeIsOutsideJail() {
                //given
                given(sessionJail.getJail(any())).willReturn("jail");
                given(sessionHome.getHomePath(any())).willReturn("home");
                //then
                assertThatExceptionOfType(SftpServerJailMappingException.class)
                        .isThrownBy(() -> sftpSubsystem.resolveFile("."))
                        .withMessage("User directory is outside jailed path: jail: home");
            }

            @Test
            void tryToExitJail() {
                //given
                final Path path = sftpSubsystem.resolveFile("/");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/");
            }
        }

        @Test
        void filesystemForUserIsMissing() {
            //given
            given(userFileSystemResolver.resolve(username)).willReturn(Optional.empty());
            //then
            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> sftpSubsystem.resolveFile("path"))
                    .withMessage("Error finding filesystem.");
        }
    }
}
