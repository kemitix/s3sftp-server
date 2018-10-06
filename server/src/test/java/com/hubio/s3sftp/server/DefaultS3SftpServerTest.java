package com.hubio.s3sftp.server;

import lombok.val;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.AcceptAllPublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.WithAssertions;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@EnableRuleMigrationSupport
class DefaultS3SftpServerTest implements WithAssertions {

    private static final String HOSTKEY =
              "-----BEGIN RSA PRIVATE KEY-----\n"
            + "MIIEpAIBAAKCAQEAp0oxAoPJXBnkg4S0i0TvrFrnyK4MHP7JrFFk5t0cCixq43wQ\n"
            + "R09VvLh0dZhdUe+uIyLEBYzGdZMqmgFiPRG0oFoQJ/YoRkJ7Y00ajKoCgiVrkPV2\n"
            + "Q7aYacfkayF2jnugwq5bAAspw4jQqVitRimT8yQcahPltgqFlubgWdc1ehOE9eCL\n"
            + "QaHihL/qCteN/Keaj3aFfx9QqbA9RjARFDRhiWYQ+QF8UTI2KJ8ZoMJEEGEQfMlV\n"
            + "HCACCZ2YZpFRm50/3O8iSM2XzhZQOBfQit7HY4lGd5kdDRPdeVKcDkSpgJSp4DuA\n"
            + "hlnVgy9KHELzgcfcKEkt2vuWOkUT0SSFk6IJvQIDAQABAoIBAQCeXF4uqkB4Pk6S\n"
            + "rZIXcGeN+fQGhbQT0qFozRg+bzs26js5I11pk9FuuBIOq/BTOxfWTPfQ5SWNcYXX\n"
            + "ic3MT7F0Ri2bFqujbxXMt9WVKO788p1z+Nk+WmDHaiFxxJitYpyZDmI4lViwsBgO\n"
            + "51IH5B4ZAasgQ7ulaypw4hepFE+cQcuJfZTe/EQgK3raP7T4jYXdAJpsnRkeWdkN\n"
            + "iFqbVnf+8YDPcj6+9OY+Xc2VM5NEfWhKvBTnFVDbBWKYyhSzUO51EFS4s39TPRig\n"
            + "uwta4SfUDYVyXySJrd5cIknUQBJHDvL4ZKlmfJ7dnpjE51+lkZt41T/vAxHDnMVU\n"
            + "IIPp0DvhAoGBANvp6Rqud6cqs75frdJuH/DLOJVUGCfBcPadSvgPHKQUdvfaqjyq\n"
            + "rU/8BWp9GuokQb0qDFtTE7vpD6Wbkns1eFI8uAC/128o9089pqGx4DH6o/t+RoAB\n"
            + "Zy6syU56kIuCeLbTMsrd61/8QlPsI6gTFXVbkscPVxLxzLd9iSXFldnpAoGBAMK9\n"
            + "qrxtxlau95Req0CYm9kT//pa+IuJEz7J/+gYENbbf6XxDpw0opRqgUObwuGDPfgk\n"
            + "B43nMd2zwYBmLw6YZcOD/L4obDQhRX7q+CZvJBkoYrnp4h5xcXOnnUu3xOBmoHL9\n"
            + "Wuab9yZktRJVE/k76WpAbVxGr0ijtSSvpuDqZDi1AoGBAMEcBSr922o60D2y7QNk\n"
            + "2r1q5tQSVWfLsPOOKf/r3T2kDtgU9vpw8eHTr7nUA+dpUSTYIKOtLx4KSUgmdZml\n"
            + "2XN1iCp4S6h8M7csrv88IGAi9Q5p02SiVsYgymEUtYscVf5NNUP5XbAa5u+k46a6\n"
            + "o1Q7xobwTIkBNcBHB0DY4X7JAoGAR7jlDfr8JmbQZkOrnOHX3E5iY4lnqrR0cxag\n"
            + "epGKeidjTvGGKP+1tSW4r/bJApd8lkxmv9ubYQTYSnrX7+8u46BT0JFAsL5kQwc1\n"
            + "F6qtR9q46bH7Bq1PVIIyC3YGO4NwqoknFnHwx6IlkjflYFCxeeF6pZae7gjlKTrM\n"
            + "ImARQ1UCgYBcRfZci4aDS4wuZF8euLfmc6k1ZW6tSk6RN0U/fIH8tOmr3N/yISLx\n"
            + "6y8FbjkT++WtLRDEAJ+/uSTW0gnJNr5xvvXFSkI9AwOv6jA5Oufo4ZNDuUZ92f/8\n"
            + "jfuqbPp3XxkGc0K2KWb8YhL+qD3CpId39FeIM2b8CSqB7p4R7YukrA==\n"
            + "-----END RSA PRIVATE KEY-----\n";

    private ServerSession serverSession = mock(ServerSession.class);

    private S3SftpServer subject;
    private int port;
    private String hostKeyPrivate;
    private File hostKeyPrivateFile;
    private AuthenticationProvider authenticationProvider;
    private Map<String, String> users = new HashMap<>();
    private String bucket = "bucket";
    private String home = "home";
    private SftpSession sftpSession = SftpSession.of(serverSession);
    private SessionBucket sessionBucket = S3SftpServer.simpleSessionBucket(bucket);
    private SessionHome sessionHome = S3SftpServer.perUserHome(home);

    // creating a server can sometime be slow due to bouncycastle blocking while loading a random seed.
    private S3SftpServer createServer() {
        final String hostKeyAlgorithm = "RSA";
        final String uri = "uri";
        return S3SftpServer.using(
                S3SftpServerConfiguration.builder()
                        .port(port)
                        .hostKeyAlgorithm(hostKeyAlgorithm)
                        .hostKeyPrivate(hostKeyPrivate)
                        .hostKeyPrivateFile(hostKeyPrivateFile)
                        .authenticationProvider(authenticationProvider)
                        .sessionBucket(sessionBucket)
                        .sessionHome(sessionHome)
                        .uri(uri)
                        .build());
    }

    @Nested
    class ThenDoesNotThrowAnyException {

        private ThrowableAssert.ThrowingCallable callable;

        @Rule
        public TemporaryFolder folder = new TemporaryFolder();

        private void useFileHostKey() throws IOException {
            hostKeyPrivateFile = folder.newFile();
            Files.write(hostKeyPrivateFile.toPath(), HOSTKEY.getBytes(StandardCharsets.UTF_8));
        }

        @AfterEach
        void thenDoesNotThrowAnyException() {
            subject = createServer();
            callable = () -> {
                subject.start();
                subject.stop();
            };
            assertThatCode(callable).doesNotThrowAnyException();
        }

        @Test
        void startAndStopWithHostKeyStringAndPasswordAuth() {
            //given
            hostKeyPrivate = HOSTKEY;
            authenticationProvider = S3SftpServer.simpleAuthenticator(users);
        }

        @Test
        void startAndStopWithHostKeyFileAndPasswordAuth() throws IOException {
            //given
            useFileHostKey();
            authenticationProvider = S3SftpServer.simpleAuthenticator(users);
        }

        @Test
        void startAndStopWithHostKeyStringAndPublicKeyAuth() {
            //given
            hostKeyPrivate = HOSTKEY;
            authenticationProvider = S3SftpServer.publicKeyAuthenticator(AcceptAllPublickeyAuthenticator.INSTANCE);
        }

    }

    @Test
    void shouldErrorWhenNoHostKey() {
        //given
        authenticationProvider = S3SftpServer.simpleAuthenticator(users);
        subject = createServer();
        //when
        final ThrowableAssert.ThrowingCallable callable = () -> subject.start();
        //then
        assertThatCode(callable)
                .isInstanceOf(S3SftpServerStartException.class)
                .hasMessage("Could not load host key");
    }

    @Nested
    class CouldNotLoadHostKey {

        @AfterEach
        void thenCouldNotLoadHostKey() {
            assertThatCode(() -> createServer().start())
                    .isInstanceOf(S3SftpServerStartException.class)
                    .hasMessage("Could not load host key");
        }

        @Test
        void shouldErrorWhenHostKeyIsEmpty() {
            //given
            authenticationProvider = S3SftpServer.simpleAuthenticator(users);
            hostKeyPrivate = "";
        }

        @Test
        void shouldErrorWhenPrivateHostKeyIsNull() {
            //given
            authenticationProvider = S3SftpServer.simpleAuthenticator(users);
            hostKeyPrivate = null;
        }

    }

    @Test
    void shouldErrorWhenNoAuthenticationProvider() {
        //given
        authenticationProvider = null;
        //when
        final ThrowableAssert.ThrowingCallable callable = this::createServer;
        //then
        assertThatCode(callable)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("authenticationProvider");
    }

    @Test
    void shouldErrorWhenPortIsInvalid() {
        //given
        authenticationProvider = S3SftpServer.simpleAuthenticator(users);
        hostKeyPrivate = HOSTKEY;
        // not running as root so permission should be denied
        port = 22;
        subject = createServer();
        //when
        final ThrowableAssert.ThrowingCallable callable = () -> subject.start();
        //then
        assertThatCode(callable)
                .isInstanceOf(S3SftpServerStartException.class)
                .hasMessage("Could not start server")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void simpleAuthenticatorShouldErrorWhenUsersIsNull() {
        //given
        final Map<String, String> users = null;
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                S3SftpServer.simpleAuthenticator(users);
        //then
        assertThatNullPointerException()
                .isThrownBy(callable)
                .withMessageContaining("users");
    }

    @Test
    void simpleSessionBucketShouldReturnBucket() {
        //given
        sessionBucket = S3SftpServer.simpleSessionBucket(bucket);
        //when
        final String result = sessionBucket.getBucket(sftpSession);
        //then
        assertThat(result).isSameAs(bucket);
    }

    @Test
    void simpleSessionBucketShouldErrorWhenBucketIsNull() {
        //given
        final String bucket = null;
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                S3SftpServer.simpleSessionBucket(bucket);
        //then
        assertThatNullPointerException()
                .isThrownBy(callable)
                .withMessageContaining("bucket");
    }

    @Test
    void simpleSessionHomeShouldReturnHome() {
        //given
        sessionHome = S3SftpServer.perUserHome(home);
        given(sftpSession.getServerSession().getUsername()).willReturn("username");
        //when
        final String result = sessionHome.getHomePath(sftpSession);
        //then
        assertThat(result).isEqualTo("home/username");
    }

    @Test
    void simpleSessionHomeShouldErrorWhenSubdirIsNull() {
        //given
        final String subdir = null;
        //when
        final ThrowableAssert.ThrowingCallable callable = () ->
                S3SftpServer.perUserHome(subdir);
        //then
        assertThatNullPointerException()
                .isThrownBy(callable)
                .withMessageContaining("subdir");
    }

    @Test
    void whenConfigureAuthenticationWithPasswordThenAddPasswordFactoryInstance() {
        //given
        val authenticationProvider = S3SftpServer.simpleAuthenticator(users);
        val configuration = new S3SftpServerConfiguration(
                2000, "hka", "hkp", new File("hkfp"),
                authenticationProvider, sessionBucket, sessionHome, SftpSession::getUsername, "uri");
        val sshServer = mock(SshServer.class);
        val server = new DefaultS3SftpServer(sshServer, configuration);
        //when
        server.start();
        //then
        then(sshServer).should().setPasswordAuthenticator(any());
    }

    @Test
    void whenStopWhereIOExceptionThenServerStopException() throws IOException {
        //given
        val sshServer = mock(SshServer.class);
        val s3SftpServer = new DefaultS3SftpServer(sshServer, mock(S3SftpServerConfiguration.class));
        doThrow(IOException.class).when(sshServer).stop();
        //then
        assertThatThrownBy(s3SftpServer::stop)
                .isInstanceOf(S3SftpServerStopException.class);
    }

    private interface MyPasswordAuthenticator extends AuthenticationProvider, PasswordAuthenticator {
    }
}
