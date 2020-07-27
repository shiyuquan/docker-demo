package persion.bleg.dockerdemo.minio.my;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;


/**
 * @author shiyuquan
 * @since 2020/4/22 10:18 上午
 */
public class MyHttpRequestBody extends RequestBody {
    private RandomAccessFile file = null;
    private BufferedInputStream stream = null;
    private byte[] bytes = null;
    private int length = -1;
    private String contentType = null;

    MyHttpRequestBody(final RandomAccessFile file, final int length, final String contentType) {
        this.file = file;
        this.length = length;
        this.contentType = contentType;
    }

    MyHttpRequestBody(final BufferedInputStream stream, final int length, final String contentType) {
        this.stream = stream;
        this.length = length;
        this.contentType = contentType;
    }

    MyHttpRequestBody(final byte[] bytes, final int length, final String contentType) {
        this.bytes = bytes;
        this.length = length;
        this.contentType = contentType;
    }

    @Override
    public MediaType contentType() {
        MediaType mediaType = null;

        if (contentType != null) {
            mediaType = MediaType.parse(contentType);
        }
        if (mediaType == null) {
            mediaType = MediaType.parse("application/octet-stream");
        }

        return mediaType;
    }

    @Override
    public long contentLength() {
        return length;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (file != null) {
            sink.write(Okio.source(Channels.newInputStream(file.getChannel())), length);
        } else if (stream != null) {
            sink.write(Okio.source(stream), length);
        } else {
            sink.write(bytes, 0, length);
        }
    }
}
