package jopj;

import java.util.Objects;

/**
 * Java skeleton of OpenJPEG's cio.c / cio.h stream system.
 *
 * This corresponds roughly to:
 *   - opj_stream_private_t / opj_stream_t
 *   - opj_stream_* exported functions
 *   - opj_* byte read/write helpers
 *
 * NOTES:
 * - This is intentionally simplified:
 *   * The original C code has an internal buffered I/O engine.
 *   * Here, opj_stream_read_data / write_data mostly delegate to the
 *     user-supplied function pointers (ReadFn, WriteFn, etc.).
 *   * The internal buffer fields are present but not yet used to fully
 *     emulate the original chunked buffering logic.
 * - External dependencies like opj_event_mgr_t and opj_event_msg are
 *   stubbed as OpjEventMgr + opj_event_msg.
 */
public final class OpjStream extends Cio.OpjStreamPrivate {

    /* --------------------------------------------------------------------- */
    /* Public constants (from cio.h)                                         */
    /* --------------------------------------------------------------------- */

    /** Stream is opened for output. */
    public static final int OPJ_STREAM_STATUS_OUTPUT = 0x1;
    /** Stream is opened for input.  */
    public static final int OPJ_STREAM_STATUS_INPUT  = 0x2;
    /** Stream has reached end.      */
    public static final int OPJ_STREAM_STATUS_END    = 0x4;
    /** Stream has encountered an error. */
    public static final int OPJ_STREAM_STATUS_ERROR  = 0x8;

    /* Event levels (matching EVT_INFO, EVT_WARNING, EVT_ERROR in C) */
    public static final int EVT_INFO    = 1;
    public static final int EVT_WARNING = 2;
    public static final int EVT_ERROR   = 3;

    /* --------------------------------------------------------------------- */
    /* Functional interfaces – C function pointer typedefs                   */
    /* --------------------------------------------------------------------- */

    /**
     * Simple in-memory source for jopj.OpjStream: wraps a byte[] with read/skip/seek.
     */
     static class ByteArraySource {
        private final byte[] data;
        private int pos = 0;

        public  ByteArraySource(byte[] data) {
            this.data = Objects.requireNonNull(data);
        }

        long length() {
            return data.length;
        }

        long read(byte[] buffer, long nbBytes) {
            if (nbBytes <= 0) return 0;
            if (pos >= data.length) return -1; // EOF

            int toRead = (int) Math.min(nbBytes, (long) (data.length - pos));
            System.arraycopy(data, pos, buffer, 0, toRead);
            pos += toRead;
            return toRead;
        }

        long skip(long nbBytes) {
            if (nbBytes <= 0) return 0;
            int toSkip = (int) Math.min(nbBytes, (long) (data.length - pos));
            pos += toSkip;
            return toSkip;
        }

        boolean seek(long offset) {
            if (offset < 0 || offset > data.length) {
                return false; // failure
            }
            pos = (int) offset;
            return true; // success
        }
    }

    /**
     * Utility to create an jopj.OpjStream over a byte[] codestream.
     */
    public static OpjStream createFromByteArray(byte[] data) {
        ByteArraySource src = new ByteArraySource(data);

        // Get a default-configured stream (buffer size, etc.)
        Cio.OpjStreamPrivate base = Cio.opj_stream_default_create(true);

        // Our public stream type extends OpjStreamPrivate
        OpjStream stream = new OpjStream();

        // Copy basic fields from base
        stream.storedData        = base.storedData;
        stream.bufferSize        = base.bufferSize;
        stream.bytesInBuffer     = base.bytesInBuffer;
        stream.currentDataOffset = base.currentDataOffset;
        stream.byteOffset        = base.byteOffset;
        stream.status            = base.status;
        stream.opjSkip           = base.opjSkip;
        stream.opjSeek           = base.opjSeek;

        // Attach the user data
        Cio.opj_stream_set_user_data(stream, src, userData -> {
            // nothing to free
        });
        Cio.opj_stream_set_user_data_length(stream, src.length());

        // Wire callbacks to ByteArraySource
        Cio.opj_stream_set_read_function(stream, (buffer, nbBytes, userData) -> {
            return ((ByteArraySource) userData).read(buffer, nbBytes);
        });
        Cio.opj_stream_set_skip_function(stream, (nbBytes, userData) -> {
            return ((ByteArraySource) userData).skip(nbBytes);
        });
        Cio.opj_stream_set_seek_function(stream, (offset, userData) -> {
            return ((ByteArraySource) userData).seek(offset);
        });

        // No write support for decoding
        Cio.opj_stream_set_write_function(stream, (buffer, nbBytes, userData) -> -1L);

        return stream;
    }
    /**
     * C: typedef OPJ_SIZE_T (* opj_stream_read_fn)(void * p_buffer,
     *                                              OPJ_SIZE_T p_nb_bytes,
     *                                              void * p_user_data);
     *
     * Java: read up to p_nb_bytes into p_buffer[0..), return #bytes read
     * or -1 on error/end.
     */
    @FunctionalInterface
    public interface ReadFn {
        long read(byte[] buffer, long nbBytes, Object userData);
    }

    /**
     * C: typedef OPJ_SIZE_T (* opj_stream_write_fn)(void * p_buffer,
     *                                               OPJ_SIZE_T p_nb_bytes,
     *                                               void * p_user_data);
     *
     * Java: write p_nb_bytes from buffer[0..), return #bytes written
     * or -1 on error.
     */
    @FunctionalInterface
    public interface WriteFn {
        long write(byte[] buffer, long nbBytes, Object userData);
    }

    /**
     * C: typedef OPJ_OFF_T (* opj_stream_skip_fn)(OPJ_OFF_T p_nb_bytes,
     *                                             void * p_user_data);
     */
    @FunctionalInterface
    public interface SkipFn {
        long skip(long nbBytes, Object userData);
    }

    /**
     * C: typedef OPJ_BOOL (* opj_stream_seek_fn)(OPJ_OFF_T p_nb_bytes,
     *                                            void * p_user_data);
     */
    @FunctionalInterface
    public interface SeekFn {
        boolean seek(long position, Object userData);
    }

    /**
     * C: typedef void (* opj_stream_free_user_data_fn)(void * p_user_data);
     */
    @FunctionalInterface
    public interface FreeUserDataFn {
        void free(Object userData);
    }

    /* --------------------------------------------------------------------- */
    /* Event manager stub (opj_event_mgr_t / opj_event_msg)                  */
    /* --------------------------------------------------------------------- */

    /**
     * Minimal stub for the event manager struct.
     * In real code you'd plug this into your logging system.
     */
    public static final class OpjEventMgr {
        public void info (String msg) { System.out.print (msg); }
        public void warn (String msg) { System.err.print("WARNING: " + msg); }
        public void error(String msg) { System.err.print("ERROR: " + msg); }
    }

    /**
     * C: opj_event_msg(event_mgr, EVT_*, "msg");
     */
    public static void opj_event_msg(OpjEventMgr mgr, int evt, String msg) {
        if (mgr == null) return;
        switch (evt) {
            case EVT_INFO:    mgr.info(msg);  break;
            case EVT_WARNING: mgr.warn(msg);  break;
            case EVT_ERROR:   mgr.error(msg); break;
            default: mgr.info(msg);           break;
        }
    }

    /* --------------------------------------------------------------------- */
    /* Stream structure (opj_stream_private_t)                               */
    /* --------------------------------------------------------------------- */

    /**
     * Java equivalent of opj_stream_private_t / opj_stream_t.
     *
     * NOTE: We keep all fields for compatibility with the C layout, but the
     * buffering/skip/seek logic is simplified. You can expand it later to
     * mirror cio.c exactly (m_stored_data, m_bytes_in_buffer, etc.).
     */
    public static final class Stream {

        /* User data, e.g., a RandomAccessFile, InputStream, etc. */
        public Object userData;

        /* Optional deallocator for userData. */
        public FreeUserDataFn freeUserDataFn;

        /* Total length of underlying data, if known. */
        public long userDataLength;

        /* Actual user-level callbacks. */
        public ReadFn  readFn;
        public WriteFn writeFn;
        public SkipFn  skipFn;
        public SeekFn  seekFn;

        /* Internal buffer (kept for future compatibility, not fully used
           in the simplified implementation below). */
        public byte[] storedData;
        public int    currentOffset;   // index into storedData
        public long   bytesInBuffer;   // bytes available (read) or pending (write)

        /* Byte offset since beginning of stream. */
        public long byteOffset;

        /* Buffer size used when internal buffering is implemented. */
        public int bufferSize;

        /* Status flags (OPJ_STREAM_STATUS_*). */
        public int status;

        /**
         * Factory helper corresponding to opj_stream_create(bufferSize, isInput).
         */
        public static Stream opj_stream_create(long bufferSize, boolean isInput) {
            Stream s = new Stream();
            s.bufferSize = (bufferSize <= 0 || bufferSize > Integer.MAX_VALUE)
                    ? 8192
                    : (int) bufferSize;
            s.storedData    = new byte[s.bufferSize];
            s.currentOffset = 0;
            s.bytesInBuffer = 0L;
            s.byteOffset    = 0L;
            s.status        = 0;
            if (isInput) {
                s.status |= OPJ_STREAM_STATUS_INPUT;
            } else {
                s.status |= OPJ_STREAM_STATUS_OUTPUT;
            }
            return s;
        }

        /**
         * Destroy stream; frees userData via freeUserDataFn (if non-null).
         * C: opj_stream_destroy(opj_stream_private_t*).
         */
        public void opj_stream_destroy(OpjEventMgr eventMgr) {
            // Flush if we were writing (simplified: no internal buffering used):
            if ((status & OPJ_STREAM_STATUS_OUTPUT) != 0) {
                opj_stream_flush(this, eventMgr);
            }

            if (freeUserDataFn != null && userData != null) {
                freeUserDataFn.free(userData);
            }

            userData       = null;
            freeUserDataFn = null;
            readFn         = null;
            writeFn        = null;
            skipFn         = null;
            seekFn         = null;
            storedData     = null;
            bytesInBuffer  = 0;
            bufferSize     = 0;
            status         = 0;
        }
    }

    /* --------------------------------------------------------------------- */
    /* Byte helpers: BE/LE for int/double/float                              */
    /* --------------------------------------------------------------------- */

    /* NOTE: We add an 'offset' argument for pointer arithmetic. The
       3-arg overload just uses offset = 0 for simple calls. */

    /* ---------- 32-bit integer ---------- */

    public static void opj_write_bytes_BE(byte[] buffer, int offset,
                                          long value, int nbBytes) {
        if (nbBytes <= 0 || nbBytes > 4) {
            throw new IllegalArgumentException("nbBytes must be 1..4");
        }
        for (int i = 0; i < nbBytes; ++i) {
            int shift = (nbBytes - 1 - i) * 8;
            buffer[offset + i] = (byte) ((value >>> shift) & 0xFF);
        }
    }

    public static void opj_write_bytes_BE(byte[] buffer, long value, int nbBytes) {
        opj_write_bytes_BE(buffer, 0, value, nbBytes);
    }

    public static void opj_write_bytes_LE(byte[] buffer, int offset,
                                          long value, int nbBytes) {
        if (nbBytes <= 0 || nbBytes > 4) {
            throw new IllegalArgumentException("nbBytes must be 1..4");
        }
        for (int i = 0; i < nbBytes; ++i) {
            int shift = i * 8;
            buffer[offset + i] = (byte) ((value >>> shift) & 0xFF);
        }
    }

    public static void opj_write_bytes_LE(byte[] buffer, long value, int nbBytes) {
        opj_write_bytes_LE(buffer, 0, value, nbBytes);
    }

    public static int opj_read_bytes_BE(byte[] buffer, int offset, int nbBytes) {
        if (nbBytes <= 0 || nbBytes > 4) {
            throw new IllegalArgumentException("nbBytes must be 1..4");
        }
        int result = 0;
        for (int i = 0; i < nbBytes; ++i) {
            result = (result << 8) | (buffer[offset + i] & 0xFF);
        }
        return result;
    }

    public static int opj_read_bytes_BE(byte[] buffer, int nbBytes) {
        return opj_read_bytes_BE(buffer, 0, nbBytes);
    }

    public static int opj_read_bytes_LE(byte[] buffer, int offset, int nbBytes) {
        if (nbBytes <= 0 || nbBytes > 4) {
            throw new IllegalArgumentException("nbBytes must be 1..4");
        }
        int result = 0;
        for (int i = nbBytes - 1; i >= 0; --i) {
            result = (result << 8) | (buffer[offset + i] & 0xFF);
        }
        return result;
    }

    public static int opj_read_bytes_LE(byte[] buffer, int nbBytes) {
        return opj_read_bytes_LE(buffer, 0, nbBytes);
    }

    /* ---------- 64-bit double ---------- */

    public static void opj_write_double_BE(byte[] buffer, int offset, double value) {
        long bits = Double.doubleToRawLongBits(value);
        for (int i = 0; i < 8; ++i) {
            int shift = (7 - i) * 8;
            buffer[offset + i] = (byte) ((bits >>> shift) & 0xFF);
        }
    }

    public static void opj_write_double_BE(byte[] buffer, double value) {
        opj_write_double_BE(buffer, 0, value);
    }

    public static void opj_write_double_LE(byte[] buffer, int offset, double value) {
        long bits = Double.doubleToRawLongBits(value);
        for (int i = 0; i < 8; ++i) {
            int shift = i * 8;
            buffer[offset + i] = (byte) ((bits >>> shift) & 0xFF);
        }
    }

    public static void opj_write_double_LE(byte[] buffer, double value) {
        opj_write_double_LE(buffer, 0, value);
    }

    public static double opj_read_double_BE(byte[] buffer, int offset) {
        long bits = 0L;
        for (int i = 0; i < 8; ++i) {
            bits = (bits << 8) | (buffer[offset + i] & 0xFFL);
        }
        return Double.longBitsToDouble(bits);
    }

    public static double opj_read_double_BE(byte[] buffer) {
        return opj_read_double_BE(buffer, 0);
    }

    public static double opj_read_double_LE(byte[] buffer, int offset) {
        long bits = 0L;
        for (int i = 7; i >= 0; --i) {
            bits = (bits << 8) | (buffer[offset + i] & 0xFFL);
        }
        return Double.longBitsToDouble(bits);
    }

    public static double opj_read_double_LE(byte[] buffer) {
        return opj_read_double_LE(buffer, 0);
    }

    /* ---------- 32-bit float ---------- */

    public static void opj_write_float_BE(byte[] buffer, int offset, float value) {
        int bits = Float.floatToRawIntBits(value);
        for (int i = 0; i < 4; ++i) {
            int shift = (3 - i) * 8;
            buffer[offset + i] = (byte) ((bits >>> shift) & 0xFF);
        }
    }

    public static void opj_write_float_BE(byte[] buffer, float value) {
        opj_write_float_BE(buffer, 0, value);
    }

    public static void opj_write_float_LE(byte[] buffer, int offset, float value) {
        int bits = Float.floatToRawIntBits(value);
        for (int i = 0; i < 4; ++i) {
            int shift = i * 8;
            buffer[offset + i] = (byte) ((bits >>> shift) & 0xFF);
        }
    }

    public static void opj_write_float_LE(byte[] buffer, float value) {
        opj_write_float_LE(buffer, 0, value);
    }

    public static float opj_read_float_BE(byte[] buffer, int offset) {
        int bits = 0;
        for (int i = 0; i < 4; ++i) {
            bits = (bits << 8) | (buffer[offset + i] & 0xFF);
        }
        return Float.intBitsToFloat(bits);
    }

    public static float opj_read_float_BE(byte[] buffer) {
        return opj_read_float_BE(buffer, 0);
    }

    public static float opj_read_float_LE(byte[] buffer, int offset) {
        int bits = 0;
        for (int i = 3; i >= 0; --i) {
            bits = (bits << 8) | (buffer[offset + i] & 0xFF);
        }
        return Float.intBitsToFloat(bits);
    }

    public static float opj_read_float_LE(byte[] buffer) {
        return opj_read_float_LE(buffer, 0);
    }

    /* --------------------------------------------------------------------- */
    /* Stream data I/O (simplified)                                         */
    /* --------------------------------------------------------------------- */

    /**
     * C: OPJ_SIZE_T opj_stream_read_data(opj_stream_private_t *p_stream,
     *                                    OPJ_BYTE *p_buffer,
     *                                    OPJ_SIZE_T p_size,
     *                                    opj_event_mgr_t *p_event_mgr);
     *
     * Java: reads up to p_size bytes into p_buffer. Returns number of bytes
     * read, or -1 on error or if the stream is at end (matching C contract).
     *
     * This implementation simply delegates to stream.readFn.
     */
    public static long opj_stream_read_data(Stream stream,
                                            byte[] buffer,
                                            long size,
                                            OpjEventMgr eventMgr) {
        if (stream == null || buffer == null || size < 0) {
            return -1;
        }
        if ((stream.status & OPJ_STREAM_STATUS_INPUT) == 0) {
            return -1;
        }
        if (stream.readFn == null) {
            return -1;
        }

        long toRead = size;
        if (toRead > buffer.length) {
            toRead = buffer.length;
        }

        long n = stream.readFn.read(buffer, toRead, stream.userData);
        if (n < 0) {
            stream.status |= OPJ_STREAM_STATUS_ERROR;
            opj_event_msg(eventMgr, EVT_ERROR, "Read error in opj_stream_read_data\n");
            return -1;
        } else if (n == 0) {
            stream.status |= OPJ_STREAM_STATUS_END;
            opj_event_msg(eventMgr, EVT_INFO, "Stream reached its end !\n");
            return -1;
        }

        stream.byteOffset += n;
        return n;
    }

    /**
     * C: OPJ_SIZE_T opj_stream_write_data(opj_stream_private_t *p_stream,
     *                                     const OPJ_BYTE *p_buffer,
     *                                     OPJ_SIZE_T p_size,
     *                                     opj_event_mgr_t *p_event_mgr);
     *
     * Java: writes up to p_size bytes from p_buffer. Returns number of bytes
     * written or -1 on error.
     */
    public static long opj_stream_write_data(Stream stream,
                                             byte[] buffer,
                                             long size,
                                             OpjEventMgr eventMgr) {
        if (stream == null || buffer == null || size < 0) {
            return -1;
        }
        if ((stream.status & OPJ_STREAM_STATUS_OUTPUT) == 0) {
            return -1;
        }
        if (stream.writeFn == null) {
            return -1;
        }

        long toWrite = size;
        if (toWrite > buffer.length) {
            toWrite = buffer.length;
        }

        long n = stream.writeFn.write(buffer, toWrite, stream.userData);
        if (n < 0) {
            stream.status |= OPJ_STREAM_STATUS_ERROR;
            opj_event_msg(eventMgr, EVT_ERROR, "Write error in opj_stream_write_data\n");
            return -1;
        }

        stream.byteOffset += n;
        return n;
    }

    /**
     * C: OPJ_OFF_T opj_stream_skip(opj_stream_private_t *p_stream,
     *                              OPJ_OFF_T p_size,
     *                              opj_event_mgr_t *p_event_mgr);
     *
     * Java: delegates to skipFn if present; otherwise performs a dumb
     * skip by reading and discarding bytes.
     */
    public static long opj_stream_skip(Stream stream,
                                       long size,
                                       OpjEventMgr eventMgr) {
        if (stream == null || size < 0) return -1;

        // If the underlying source supports a native skip, use it.
        if (stream.skipFn != null) {
            long n = stream.skipFn.skip(size, stream.userData);
            if (n < 0) {
                stream.status |= OPJ_STREAM_STATUS_ERROR;
                opj_event_msg(eventMgr, EVT_ERROR, "Skip error in opj_stream_skip\n");
                return -1;
            }
            stream.byteOffset += n;
            return n;
        }

        // Fallback: read and discard.
        byte[] tmp = new byte[(int) Math.min(size, (stream.bufferSize > 0 ? stream.bufferSize : 4096))];
        long remaining = size;
        long totalSkipped = 0;

        while (remaining > 0) {
            long chunk = Math.min(tmp.length, remaining);
            long n = opj_stream_read_data(stream, tmp, chunk, eventMgr);
            if (n <= 0) break; // EOF or error
            remaining -= n;
            totalSkipped += n;
        }

        return totalSkipped > 0 ? totalSkipped : -1;
    }

    /**
     * C: OPJ_OFF_T opj_stream_tell(opj_stream_private_t *p_stream);
     */
    public static long opj_stream_tell(Stream stream) {
        if (stream == null) return -1;
        return stream.byteOffset;
    }

    /**
     * C: OPJ_SIZE_T opj_stream_get_number_byte_left(opj_stream_private_t* p_stream);
     */
    public static long opj_stream_get_number_byte_left(Stream stream) {
        if (stream == null || stream.userDataLength <= 0) return -1;
        long remaining = stream.userDataLength - stream.byteOffset;
        return Math.max(remaining, 0L);
    }

    /**
     * C: OPJ_BOOL opj_stream_flush(opj_stream_private_t *p_stream,
     *                              opj_event_mgr_t *p_event_mgr);
     *
     * Simplified version: nothing to flush since we don't buffer writes.
     */
    public static boolean opj_stream_flush(Stream stream, OpjEventMgr eventMgr) {
        if (stream == null) return false;
        // If later you implement buffered writes, flush them here.
        return true;
    }

    /* --------------------------------------------------------------------- */
    /* User-data and callback setup (opj_stream_set_*)                       */
    /* --------------------------------------------------------------------- */

    public static void opj_stream_set_user_data(Stream stream,
                                                Object data,
                                                FreeUserDataFn freeFn) {
        if (stream == null) return;
        stream.userData = data;
        stream.freeUserDataFn = freeFn;
    }

    public static void opj_stream_set_user_data_length(Stream stream,
                                                       long dataLength) {
        if (stream == null) return;
        stream.userDataLength = dataLength;
    }

    public static void opj_stream_set_read_function(Stream stream, ReadFn fn) {
        if (stream == null) return;
        if ((stream.status & OPJ_STREAM_STATUS_INPUT) == 0) return;
        stream.readFn = fn;
    }

    public static void opj_stream_set_write_function(Stream stream, WriteFn fn) {
        if (stream == null) return;
        if ((stream.status & OPJ_STREAM_STATUS_OUTPUT) == 0) return;
        stream.writeFn = fn;
    }

    public static void opj_stream_set_skip_function(Stream stream, SkipFn fn) {
        if (stream == null) return;
        stream.skipFn = fn;
    }

    public static void opj_stream_set_seek_function(Stream stream, SeekFn fn) {
        if (stream == null) return;
        stream.seekFn = fn;
    }

    /* --------------------------------------------------------------------- */
    /* Default skip/seek helpers (cio.h declares default_* prototypes)       */
    /* --------------------------------------------------------------------- */

    /**
     * C equivalent: OPJ_OFF_T opj_stream_read_skip(opj_stream_private_t *p_stream,...)
     *
     * Here we just call opj_stream_skip() for input streams.
     */
    public static long opj_stream_read_skip(Stream stream,
                                            long size,
                                            OpjEventMgr eventMgr) {
        if (stream == null ||
                (stream.status & OPJ_STREAM_STATUS_INPUT) == 0) {
            return -1;
        }
        return opj_stream_skip(stream, size, eventMgr);
    }

    /**
     * C: OPJ_BOOL opj_stream_read_seek(opj_stream_private_t *p_stream, OPJ_OFF_T p_size,...)
     */
    public static boolean opj_stream_read_seek(Stream stream,
                                               long offset,
                                               OpjEventMgr eventMgr) {
        if (stream == null ||
                (stream.status & OPJ_STREAM_STATUS_INPUT) == 0) {
            return false;
        }
        if (stream.seekFn == null) {
            opj_event_msg(eventMgr, EVT_ERROR, "No seekFn set for read stream\n");
            return false;
        }
        boolean ok = stream.seekFn.seek(offset, stream.userData);
        if (ok) {
            stream.byteOffset = offset;
            stream.status &= ~OPJ_STREAM_STATUS_END;
        } else {
            stream.status |= OPJ_STREAM_STATUS_ERROR;
        }
        return ok;
    }

    /**
     * C: OPJ_BOOL opj_stream_write_seek(opj_stream_private_t *p_stream, OPJ_OFF_T p_size,...)
     */
    public static boolean opj_stream_write_seek(Stream stream,
                                                long offset,
                                                OpjEventMgr eventMgr) {
        if (stream == null ||
                (stream.status & OPJ_STREAM_STATUS_OUTPUT) == 0) {
            return false;
        }
        if (stream.seekFn == null) {
            opj_event_msg(eventMgr, EVT_ERROR, "No seekFn set for write stream\n");
            return false;
        }
        boolean ok = stream.seekFn.seek(offset, stream.userData);
        if (ok) {
            stream.byteOffset = offset;
        } else {
            stream.status |= OPJ_STREAM_STATUS_ERROR;
        }
        return ok;
    }

    /**
     * C: OPJ_BOOL opj_stream_default_seek(OPJ_OFF_T p_nb_bytes, void * p_user_data);
     *
     * Fully generic "do nothing" implementation; useful as a placeholder if
     * a seek function is required but not supported by the underlying source.
     */
    public static boolean opj_stream_default_seek(long nbBytes, Object userData) {
        // Default: cannot seek, return false.
        return false;
    }
}
