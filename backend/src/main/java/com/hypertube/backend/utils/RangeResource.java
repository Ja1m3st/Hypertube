package com.hypertube.backend.utils;

import org.springframework.core.io.AbstractResource;
import java.io.*;

public class RangeResource extends AbstractResource {
    private final File file;
    private final long start;
    private final long length;

    public RangeResource(File file, long start, long length) {
        this.file = file;
        this.start = start;
        this.length = length;
    }

    @Override public String getDescription() { return "RangeResource"; }

    @Override
    public InputStream getInputStream() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        fis.skip(start);
        return new LimitedInputStream(fis, length);
    }

    @Override public long contentLength() { return length; }

    static class LimitedInputStream extends FilterInputStream {
        private long remaining;
        LimitedInputStream(InputStream in, long limit) {
            super(in);
            this.remaining = limit;
        }
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead = (int) Math.min(len, remaining);
            int read = super.read(b, off, toRead);
            if (read > 0) remaining -= read;
            return read;
        }
    }
}