package jopj;

import java.util.Objects;

/**
 * Minimal JP2 parser: just enough to locate and extract the embedded
 * JPEG 2000 code stream from a JP2 file (the "jp2c" box).
 * This does NOT parse metadata, color profiles, or anything else.
 */
public final class Jp2Parser {

    private Jp2Parser() {
    }

    /** Basic classification of input bytes. */
    public enum J2kFormat {
        RAW_J2K,
        JP2,
        UNKNOWN
    }

    /**
     * Sniff the input and classify as RAW_J2K, JP2, or UNKNOWN.
     * - RAW_J2K: starts with SOC (0xFF4F)
     * - JP2: has JP2 signature box at offset 0
     */
    public static J2kFormat sniffFormat(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        if (data.length >= 12) {
            // JP2 signature box at offset 0:
            // length (0x0000000C), type 'jP  ' (0x6A502020),
            // contents 0D 0A 87 0A
            int len = readUInt32BE(data, 0);
            int type = readUInt32BE(data, 4);
            if (len == 12 && type == 0x6A502020) { // "jP  "
                return J2kFormat.JP2;
            }
        }
        if (data.length >= 2) {
            int b0 = data[0] & 0xFF;
            int b1 = data[1] & 0xFF;
            // JPEG 2000 codestream SOC marker
            if (b0 == 0xFF && b1 == 0x4F) {
                return J2kFormat.RAW_J2K;
            }
        }
        return J2kFormat.UNKNOWN;
    }

    /**
     * Extract the raw jopj.J2K codestream from a JP2 file.
     *
     * @param jp2Bytes full bytes of a JP2 file
     * @return a new byte[] containing only the codestream (jp2c box contents)
     * @throws IllegalArgumentException if this does not look like a valid JP2
     *                                  or no jp2c box is found.
     */
    public static byte[] extractCodestream(byte[] jp2Bytes) {
        Objects.requireNonNull(jp2Bytes, "jp2Bytes must not be null");

        if (sniffFormat(jp2Bytes) != J2kFormat.JP2) {
            throw new IllegalArgumentException("Input is not recognized as JP2");
        }

        int offset = 0;
        final int len = jp2Bytes.length;

        // --- 1. Parse signature box (must be first) ---
        if (len < 12) {
            throw new IllegalArgumentException("Truncated JP2 file (no signature box)");
        }
        int sigLength = readUInt32BE(jp2Bytes, offset);
        int sigType = readUInt32BE(jp2Bytes, offset + 4);
        if (sigLength != 12 || sigType != 0x6A502020) { // "jP  "
            throw new IllegalArgumentException("Invalid JP2 signature box");
        }
        // Optionally check signature contents (0D 0A 87 0A)
        // but we can skip strict checking if we want.
        offset += sigLength;

        // --- 2. Iterate over boxes until we find "jp2c" ---
        while (offset + 8 <= len) {
            long boxLength = readUInt32BE(jp2Bytes, offset) & 0xFFFFFFFFL;
            int boxType = readUInt32BE(jp2Bytes, offset + 4);
            long headerSize = 8L;
            long dataStart = offset + headerSize;
            long dataLength;

            if (boxLength == 0) {
                // Box extends to end of file
                dataLength = len - (offset + headerSize);
                boxLength = dataLength + headerSize;
            } else if (boxLength == 1) {
                // Extended length: 64-bit size in next 8 bytes
                if (offset + 16 > len) {
                    throw new IllegalArgumentException("Truncated extended-length box");
                }
                long extLen = readUInt64BE(jp2Bytes, offset + 8);
                headerSize = 16L;
                dataStart = offset + headerSize;
                dataLength = extLen - headerSize;
                boxLength = extLen;
            } else {
                dataLength = boxLength - headerSize;
            }

            if (boxLength < headerSize || offset + boxLength > len) {
                throw new IllegalArgumentException("Invalid box length in JP2");
            }

            // If this is the Contiguous Codestream box ('jp2c'), grab it
            if (boxType == 0x6A703263) { // 'j','p','2','c'
                if (dataLength <= 0) {
                    throw new IllegalArgumentException("Empty jp2c box in JP2");
                }
                if (dataStart + dataLength > len) {
                    throw new IllegalArgumentException("Truncated jp2c box in JP2");
                }

                byte[] codestream = new byte[(int) dataLength];
                System.arraycopy(jp2Bytes, (int) dataStart, codestream, 0, (int) dataLength);
                return codestream;
            }

            // Move to next box
            offset += (int) boxLength;
        }

        throw new IllegalArgumentException("No jp2c (contiguous codestream) box found in JP2");
    }

    /* ===================================================================== */
    /* Helpers                                                               */
    /* ===================================================================== */

    private static int readUInt32BE(byte[] data, int offset) {
        if (offset + 4 > data.length) {
            throw new IllegalArgumentException("readUInt32BE out of bounds");
        }
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static long readUInt64BE(byte[] data, int offset) {
        if (offset + 8 > data.length) {
            throw new IllegalArgumentException("readUInt64BE out of bounds");
        }
        return ((long) (data[offset] & 0xFF) << 56)
                | ((long) (data[offset + 1] & 0xFF) << 48)
                | ((long) (data[offset + 2] & 0xFF) << 40)
                | ((long) (data[offset + 3] & 0xFF) << 32)
                | ((long) (data[offset + 4] & 0xFF) << 24)
                | ((long) (data[offset + 5] & 0xFF) << 16)
                | ((long) (data[offset + 6] & 0xFF) << 8)
                | ((long) (data[offset + 7] & 0xFF));
    }
}
