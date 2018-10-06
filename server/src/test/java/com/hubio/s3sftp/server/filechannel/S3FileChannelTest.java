package com.hubio.s3sftp.server.filechannel;

import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3FileChannelTest implements WithAssertions {

    private final SeekableByteChannel content = mock(SeekableByteChannel.class);
    private final ByteBuffer dst = mock(ByteBuffer.class);
    private final ByteBuffer dst1 = mock(ByteBuffer.class);
    private final ByteBuffer dst2 = mock(ByteBuffer.class);
    private final ByteBuffer[] destinationArray = new ByteBuffer[3];
    private final ByteBuffer src = mock(ByteBuffer.class);
    private final ByteBuffer src1 = mock(ByteBuffer.class);
    private final ByteBuffer src2 = mock(ByteBuffer.class);
    private final ByteBuffer[] sources = new ByteBuffer[3];

    private final S3FileChannel wrapper = new S3FileChannel(content);

    @BeforeEach
    void setUp() {
        destinationArray[0] = dst;
        destinationArray[1] = dst1;
        destinationArray[2] = dst2;
        sources[0] = src;
        sources[1] = src1;
        sources[2] = src2;
    }

    @Nested
    class UnsupportedOperations {

        private ThrowableAssert.ThrowingCallable callable;

        @AfterEach
        void thenThrowsException() {
            assertThatCode(callable).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void force() {
            callable = () -> wrapper.force(true);
        }

        @Test
        void lockFile() {
            callable = wrapper::lock;
        }

        @Test
        void lockRegion() {
            callable = () -> wrapper.lock(0, 0, false);
        }

        @Test
        void map() {
            callable = () -> wrapper.map(null, 0, 0);
        }

        @Test
        void readByBufferFromPosition() {
            callable = () -> wrapper.read(dst, 123L);
        }

        @Test
        void transferFrom() {
            callable = () -> wrapper.transferFrom(null, 0, 0);
        }

        @Test
        void transferTo() {
            callable = () -> wrapper.transferTo(0, 0, null);
        }

        @Test
        void tryLockFile() {
            callable = wrapper::tryLock;
        }

        @Test
        void tryLockRegion() {
            callable = () -> wrapper.tryLock(0, 0, false);
        }

        @Test
        void writeByteBufferToPosition() {
            callable = () -> wrapper.write(src, 456L);
        }

    }

    @Test
    void position() throws Exception {
        //given
        given(content.position()).willReturn(20L);
        //when
        final long result = wrapper.position();
        //then
        assertThat(result).isEqualTo(20L);
    }

    @Test
    void setPosition() throws Exception {
        //when
        wrapper.position(200L);
        //then
        verify(content).position(200L);
    }

    @Test
    void readByteBuffer() throws Exception {
        //given
        given(content.read(dst)).willReturn(23);
        //when
        final int result = wrapper.read(dst);
        //then
        assertThat(result).isEqualTo(23);
    }

    @Test
    void readByteBufferArray() throws Exception {
        //given
        given(content.read(dst)).willReturn(20);
        given(content.read(dst1)).willReturn(123);
        given(content.read(dst2)).willReturn(321);
        //when
        final long result = wrapper.read(destinationArray);
        //then
        assertThat(result).isEqualTo(20 + 123 + 321);
    }

    @Test
    void readByteBufferArraySegment() throws Exception {
        //given
        given(content.read(dst)).willReturn(20);
        given(content.read(dst1)).willReturn(123);
        given(content.read(dst2)).willReturn(321);
        //when
        final long result = wrapper.read(destinationArray, 1, 2);
        //then
        assertThat(result).isEqualTo(123 + 321);
    }

    @Test
    void size() throws Exception {
        //given
        given(content.size()).willReturn(123L);
        //when
        final long result = wrapper.size();
        //then
        assertThat(result).isEqualTo(123L);
    }

    @Test
    void truncate() throws Exception {
        //when
        wrapper.truncate(123L);
        //then
        verify(content).truncate(123L);
    }

    @Test
    void writeByteBuffer() throws Exception {
        //given
        given(content.write(src)).willReturn(123);
        //when
        final int result = wrapper.write(src);
        //then
        assertThat(result).isEqualTo(123);
    }

    @Test
    void writeByteBufferArray() throws Exception {
        //given
        given(content.write(src)).willReturn(123);
        given(content.write(src1)).willReturn(321);
        given(content.write(src2)).willReturn(456);
        //when
        final long result = wrapper.write(sources);
        //then
        assertThat(result).isEqualTo(123 + 321 + 456);
    }

    @Test
    void writeByteBufferArraySegment() throws Exception {
        //given
        given(content.write(src1)).willReturn(321);
        given(content.write(src2)).willReturn(456);
        //when
        final long result = wrapper.write(sources, 1, 2);
        //then
        assertThat(result).isEqualTo(321 + 456);
    }

    @Test
    void whenCloseChannelThenByteChannelIsClosed() throws IOException {
        //when
        wrapper.implCloseChannel();
        //then
        then(content).should().close();
    }
}
