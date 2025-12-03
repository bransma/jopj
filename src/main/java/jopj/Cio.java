package jopj;

/**
 * Java translation/skeleton of OpenJPEG's cio.c / cio.h.
 *
 *
 *
 * Provides:
 *  - Endian helpers for ints/floats/doubles
 *  - Stream struct (OpjStreamPrivate)
 *  - Stream callbacks & default implementations
 *  - High-level stream read/write/skip/seek/flush helpers
 */
public final class Cio {

    private Cio() {
    }

    /* ------------------------------------------------------------------ */
    /* Event / logging support (minimal stub for opj_event_msg)           */
    /* ------------------------------------------------------------------ */

    public static final int EVT_INFO    = 1;
    public static final int EVT_WARNING = 2;
    public static final int EVT_ERROR   = 3;

    public static class OpjEventMgr {
        public void info(String msg)   { System.out.print(msg); }
        public void warning(String msg){ System.err.print("WARNING: " + msg); }
        public void error(String msg)  { System.err.print("ERROR: " + msg); }
    }

    public static void opj_event_msg(OpjEventMgr mgr, int event, String msg) {
        if (mgr == null) {
            // fallback logging
            if (event == EVT_ERROR) {
                System.err.print("ERROR: " + msg);
            } else if (event == EVT_WARNING) {
                System.err.print("WARNING: " + msg);
            } else {
                System.out.print(msg);
            }
            return;
        }
        switch (event) {
            case EVT_ERROR:   mgr.error(msg);   break;
            case EVT_WARNING: mgr.warning(msg); break;
            default:          mgr.info(msg);    break;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Stream status flags & defaults                                     */
    /* ------------------------------------------------------------------ */

    public static final int OPJ_STREAM_STATUS_OUTPUT = 0x1;
    public static final int OPJ_STREAM_STATUS_INPUT  = 0x2;
    public static final int OPJ_STREAM_STATUS_END    = 0x4;
    public static final int OPJ_STREAM_STATUS_ERROR  = 0x8;

    public static final int OPJ_J2K_STREAM_CHUNK_SIZE = 16384;

    /* ------------------------------------------------------------------ */
    /* Functional interfaces for callbacks                                */
    /* ------------------------------------------------------------------ */

    @FunctionalInterface
    public interface OpjStreamReadFn {
        long read(byte[] buffer, long nbBytes, Object userData);
    }

    @FunctionalInterface
    public interface OpjStreamWriteFn {
        long write(byte[] buffer, long nbBytes, Object userData);
    }

    @FunctionalInterface
    public interface OpjStreamSkipFn {
        long skip(long nbBytes, Object userData);
    }

    @FunctionalInterface
    public interface OpjStreamSeekFn {
        boolean seek(long nbBytes, Object userData);
    }

    @FunctionalInterface
    public interface OpjStreamFreeUserDataFn {
        void free(Object userData);
    }

    /* ------------------------------------------------------------------ */
    /* Java representation of opj_stream_private_t                        */
    /* ------------------------------------------------------------------ */

    public static class OpjStreamPrivate {

        public Object userData;
        public OpjStreamFreeUserDataFn freeUserDataFn;
        public long userDataLength;

        public OpjStreamReadFn  readFn;
        public OpjStreamWriteFn writeFn;
        public OpjStreamSkipFn  skipFn;
        public OpjStreamSeekFn  seekFn;

        public byte[] storedData;
        public int    currentDataOffset;
        public int    bytesInBuffer;
        public int    bufferSize;

        public long byteOffset;
        public int  status;

        @FunctionalInterface
        public interface OpjStreamSkipImpl {
            long apply(OpjStreamPrivate stream, long nbBytes, OpjEventMgr mgr);
        }

        @FunctionalInterface
        public interface OpjStreamSeekImpl {
            boolean apply(OpjStreamPrivate stream, long offset, OpjEventMgr mgr);
        }

        public OpjStreamSkipImpl opjSkip;
        public OpjStreamSeekImpl opjSeek;
    }

    /* ------------------------------------------------------------------ */
    /* Byte helpers                                                       */
    /* ------------------------------------------------------------------ */

    public static void opj_write_bytes_BE(byte[] buffer, int offset,
                                          long value, int nbBytes) {
        if (nbBytes <= 0 || nbBytes > 4) {
            throw new IllegalArgumentException("nbBytes must be in 1..4");
        }
        for (int i = nbBytes - 1; i >= 0; --i) {
            buffer[offset + (nbBytes - 1 - i)] = (byte) ((value >> (8 * i)) & 0xFF);
        }
    }

    public static void opj_write_bytes_LE(byte[] buffer, int offset,
                                          long value, int nbBytes) {
        if (nbBytes <= 0 || nbBytes > 4) {
            throw new IllegalArgumentException("nbBytes must be in 1..4");
        }
        for (int i = 0; i < nbBytes; ++i) {
            buffer[offset + i] = (byte) ((value >> (8 * i)) & 0xFF);
        }
    }

    public static long opj_read_bytes_BE(byte[] buffer, int offset, int nbBytes) {
        if (nbBytes <= 0 || nbBytes > 4) {
            throw new IllegalArgumentException("nbBytes must be in 1..4");
        }
        long value = 0;
        for (int i = 0; i < nbBytes; ++i) {
            value = (value << 8) | ((long) buffer[offset + i] & 0xFFL);
        }
        return value;
    }

    public static long opj_read_bytes_LE(byte[] buffer, int offset, int nbBytes) {
        if (nbBytes <= 0 || nbBytes > 4) {
            throw new IllegalArgumentException("nbBytes must be in 1..4");
        }
        long value = 0;
        for (int i = nbBytes - 1; i >= 0; --i) {
            value = (value << 8) | ((long) buffer[offset + i] & 0xFFL);
        }
        return value;
    }

    public static void opj_write_double_BE(byte[] buffer, int offset, double value) {
        long bits = Double.doubleToRawLongBits(value);
        for (int i = 7; i >= 0; --i) {
            buffer[offset + (7 - i)] = (byte) ((bits >> (8 * i)) & 0xFFL);
        }
    }

    public static void opj_write_double_LE(byte[] buffer, int offset, double value) {
        long bits = Double.doubleToRawLongBits(value);
        for (int i = 0; i < 8; ++i) {
            buffer[offset + i] = (byte) ((bits >> (8 * i)) & 0xFFL);
        }
    }

    public static double opj_read_double_BE(byte[] buffer, int offset) {
        long bits = 0;
        for (int i = 0; i < 8; ++i) {
            bits = (bits << 8) | ((long) buffer[offset + i] & 0xFFL);
        }
        return Double.longBitsToDouble(bits);
    }

    public static double opj_read_double_LE(byte[] buffer, int offset) {
        long bits = 0;
        for (int i = 7; i >= 0; --i) {
            bits = (bits << 8) | ((long) buffer[offset + i] & 0xFFL);
        }
        return Double.longBitsToDouble(bits);
    }

    public static void opj_write_float_BE(byte[] buffer, int offset, float value) {
        int bits = Float.floatToRawIntBits(value);
        for (int i = 3; i >= 0; --i) {
            buffer[offset + (3 - i)] = (byte) ((bits >> (8 * i)) & 0xFF);
        }
    }

    public static void opj_write_float_LE(byte[] buffer, int offset, float value) {
        int bits = Float.floatToRawIntBits(value);
        for (int i = 0; i < 4; ++i) {
            buffer[offset + i] = (byte) ((bits >> (8 * i)) & 0xFF);
        }
    }

    public static float opj_read_float_BE(byte[] buffer, int offset) {
        int bits = 0;
        for (int i = 0; i < 4; ++i) {
            bits = (bits << 8) | (buffer[offset + i] & 0xFF);
        }
        return Float.intBitsToFloat(bits);
    }

    public static float opj_read_float_LE(byte[] buffer, int offset) {
        int bits = 0;
        for (int i = 3; i >= 0; --i) {
            bits = (bits << 8) | (buffer[offset + i] & 0xFF);
        }
        return Float.intBitsToFloat(bits);
    }

    /* ------------------------------------------------------------------ */
    /* Stream creation / destruction                                      */
    /* ------------------------------------------------------------------ */

    public static OpjStreamPrivate opj_stream_create(int bufferSize,
                                                     boolean isInput) {
        OpjStreamPrivate s = new OpjStreamPrivate();
        s.bufferSize = bufferSize > 0 ? bufferSize : OPJ_J2K_STREAM_CHUNK_SIZE;
        s.storedData = new byte[s.bufferSize];
        s.currentDataOffset = 0;
        s.bytesInBuffer = 0;
        s.byteOffset = 0L;
        s.userDataLength = 0L;
        if (isInput) {
            s.status |= OPJ_STREAM_STATUS_INPUT;
            s.opjSkip = Cio::opj_stream_read_skip;
            s.opjSeek = Cio::opj_stream_read_seek;
        } else {
            s.status |= OPJ_STREAM_STATUS_OUTPUT;
            s.opjSkip = Cio::opj_stream_write_skip;
            s.opjSeek = Cio::opj_stream_write_seek;
        }
        s.readFn  = Cio::opj_stream_default_read;
        s.writeFn = Cio::opj_stream_default_write;
        s.skipFn  = Cio::opj_stream_default_skip;
        s.seekFn  = Cio::opj_stream_default_seek;
        return s;
    }

    public static OpjStreamPrivate opj_stream_default_create(boolean isInput) {
        return opj_stream_create(OPJ_J2K_STREAM_CHUNK_SIZE, isInput);
    }

    public static void opj_stream_destroy(OpjStreamPrivate s) {
        if (s == null) return;
        if (s.freeUserDataFn != null && s.userData != null) {
            s.freeUserDataFn.free(s.userData);
        }
        s.storedData = null;
    }

    /* ------------------------------------------------------------------ */
    /* Stream configuration                                               */
    /* ------------------------------------------------------------------ */

    public static void opj_stream_set_read_function(OpjStreamPrivate s,
                                                    OpjStreamReadFn fn) {
        if (s == null) return;
        if ((s.status & OPJ_STREAM_STATUS_INPUT) == 0) return;
        s.readFn = fn;
    }

    public static void opj_stream_set_write_function(OpjStreamPrivate s,
                                                     OpjStreamWriteFn fn) {
        if (s == null) return;
        if ((s.status & OPJ_STREAM_STATUS_OUTPUT) == 0) return;
        s.writeFn = fn;
    }

    public static void opj_stream_set_skip_function(OpjStreamPrivate s,
                                                    OpjStreamSkipFn fn) {
        if (s == null) return;
        s.skipFn = fn;
    }

    public static void opj_stream_set_seek_function(OpjStreamPrivate s,
                                                    OpjStreamSeekFn fn) {
        if (s == null) return;
        s.seekFn = fn;
    }

    public static void opj_stream_set_user_data(OpjStreamPrivate s,
                                                Object userData,
                                                OpjStreamFreeUserDataFn freeFn) {
        if (s == null) return;
        s.userData = userData;
        s.freeUserDataFn = freeFn;
    }

    public static void opj_stream_set_user_data_length(OpjStreamPrivate s,
                                                       long length) {
        if (s == null) return;
        s.userDataLength = length;
    }

    /* ------------------------------------------------------------------ */
    /* High-level read / write / skip / seek                              */
    /* ------------------------------------------------------------------ */

    public static long opj_stream_read_data(OpjStreamPrivate s,
                                            byte[] buffer,
                                            long size,
                                            OpjEventMgr mgr) {
        if (s == null || buffer == null || size < 0) return -1;
        if (size == 0) return 0;
        if (s.readFn == null) return -1;

        long toRead = Math.min(size, buffer.length);
        long read = s.readFn.read(buffer, toRead, s.userData);
        if (read == -1) {
            s.status |= OPJ_STREAM_STATUS_END;
            opj_event_msg(mgr, EVT_INFO, "Stream reached its end !\n");
            return -1;
        }
        s.byteOffset += read;
        return read;
    }

    public static long opj_stream_write_data(OpjStreamPrivate s,
                                             byte[] buffer,
                                             long size,
                                             OpjEventMgr mgr) {
        if (s == null || buffer == null || size < 0) return -1;
        if (size == 0) return 0;
        if (s.writeFn == null) return -1;

        long toWrite = Math.min(size, buffer.length);
        long written = s.writeFn.write(buffer, toWrite, s.userData);
        if (written == -1) {
            s.status |= OPJ_STREAM_STATUS_ERROR;
            opj_event_msg(mgr, EVT_INFO, "Stream error!\n");
            return -1;
        }
        s.byteOffset += written;
        return written;
    }

    public static long opj_stream_skip(OpjStreamPrivate s,
                                       long size,
                                       OpjEventMgr mgr) {
        if (s == null || size < 0) return -1;
        if (s.opjSkip == null) return -1;
        return s.opjSkip.apply(s, size, mgr);
    }

    public static boolean opj_stream_seek(OpjStreamPrivate s,
                                          long offset,
                                          OpjEventMgr mgr) {
        if (s == null || offset < 0) return false;
        if (s.opjSeek == null) return false;
        return s.opjSeek.apply(s, offset, mgr);
    }

    public static boolean opj_stream_flush(OpjStreamPrivate s,
                                           OpjEventMgr mgr) {
        if (s == null) return false;
        if (s.bytesInBuffer == 0) return true;
        if (s.writeFn == null) return false;

        byte[] tmp = new byte[s.bytesInBuffer];
        System.arraycopy(s.storedData, s.currentDataOffset, tmp, 0, s.bytesInBuffer);
        long written = s.writeFn.write(tmp, s.bytesInBuffer, s.userData);
        if (written != s.bytesInBuffer) {
            s.status |= OPJ_STREAM_STATUS_ERROR;
            opj_event_msg(mgr, EVT_INFO, "Stream error!\n");
            return false;
        }
        s.currentDataOffset = 0;
        s.bytesInBuffer = 0;
        s.byteOffset += written;
        return true;
    }

    /* ------------------------------------------------------------------ */
    /* Read-/write-oriented skip/seek implementations                     */
    /* ------------------------------------------------------------------ */

    public static boolean opj_stream_read_seek(OpjStreamPrivate s,
                                               long offset,
                                               OpjEventMgr mgr) {
        if (s == null) return false;
        s.currentDataOffset = 0;
        s.bytesInBuffer = 0;
        if (s.seekFn == null || !s.seekFn.seek(offset, s.userData)) {
            s.status |= OPJ_STREAM_STATUS_END;
            return false;
        } else {
            s.status &= ~OPJ_STREAM_STATUS_END;
            s.byteOffset = offset;
        }
        return true;
    }

    public static boolean opj_stream_write_seek(OpjStreamPrivate s,
                                                long offset,
                                                OpjEventMgr mgr) {
        if (s == null) return false;
        if (!opj_stream_flush(s, mgr)) {
            s.status |= OPJ_STREAM_STATUS_ERROR;
            return false;
        }
        s.currentDataOffset = 0;
        s.bytesInBuffer = 0;
        if (s.seekFn == null || !s.seekFn.seek(offset, s.userData)) {
            s.status |= OPJ_STREAM_STATUS_ERROR;
            return false;
        } else {
            s.byteOffset = offset;
        }
        return true;
    }

    public static long opj_stream_read_skip(OpjStreamPrivate s,
                                            long size,
                                            OpjEventMgr mgr) {
        if (s == null || size < 0) return -1;
        if (s.skipFn == null) return -1;
        long skipped = s.skipFn.skip(size, s.userData);
        if (skipped == -1) {
            s.status |= OPJ_STREAM_STATUS_END;
            opj_event_msg(mgr, EVT_INFO, "Stream reached its end !\n");
            return -1;
        }
        s.byteOffset += skipped;
        return skipped;
    }

    public static long opj_stream_write_skip(OpjStreamPrivate s,
                                             long size,
                                             OpjEventMgr mgr) {
        if (s == null || size < 0) return -1;
        if (!opj_stream_flush(s, mgr)) {
            s.status |= OPJ_STREAM_STATUS_ERROR;
            return -1;
        }
        if (s.skipFn == null) return -1;
        long skipped = s.skipFn.skip(size, s.userData);
        if (skipped == -1) {
            s.status |= OPJ_STREAM_STATUS_ERROR;
            return -1;
        }
        s.byteOffset += skipped;
        return skipped;
    }

    /* ------------------------------------------------------------------ */
    /* Default callbacks                                                  */
    /* ------------------------------------------------------------------ */

    public static long opj_stream_default_read(byte[] buffer,
                                               long nbBytes,
                                               Object userData) {
        return -1;
    }

    public static long opj_stream_default_write(byte[] buffer,
                                                long nbBytes,
                                                Object userData) {
        return -1;
    }

    public static long opj_stream_default_skip(long nbBytes, Object userData) {
        return -1;
    }

    public static boolean opj_stream_default_seek(long nbBytes, Object userData) {
        return false;
    }
}