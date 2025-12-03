package jopj;

import java.util.Objects;

/**
 * Narrow-path JPEG 2000 decode API:
 *
 *   byte[] codestream -> DecodedImage (planar int samples).
 *
 * No CLI, no file I/O. This is what your DICOM pipeline should call.
 */
public final class OpjDecompress {

    private OpjDecompress() {
    }

    public static final class DecodedImage {
        public int width;
        public int height;
        public int numComponents;
        public int bitsPerSample;
        public boolean signed;
        /** components[c][y * width + x] */
        public int[][] components;
    }

    public static DecodedImage decode(byte[] input) {
        Objects.requireNonNull(input, "codestream must not be null");

        Jp2Parser.J2kFormat format = Jp2Parser.sniffFormat(input);

        byte[] codestream;
        switch (format) {
            case RAW_J2K:
                codestream = input;
                break;
            case JP2:
                codestream = Jp2Parser.extractCodestream(input);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported JPEG 2000 format");
        }
        OpjStream stream = null;
        OpenJpeg.OpjCodec codec = null;
        OpjImage image = null;

        try {
            // Wrap byte[] in an jopj.OpjStream
            stream = OpjStream.createFromByteArray(codestream);

            // Create codec for raw jopj.J2K (or JP2 if you prefer)
            codec = OpenJpeg.opj_create_decompress(OpenJpeg.OpjCodecFormat.OPJ_CODEC_J2K);
            if (codec == null) {
                throw new RuntimeException("opj_create_decompress returned null");
            }

            // Default decoder parameters
            OpenJpeg.OpjDParameters params = new OpenJpeg.OpjDParameters();
            OpenJpeg.opj_set_default_decoder_parameters(params);

            if (!OpenJpeg.opj_setup_decoder(codec, params)) {
                throw new RuntimeException("opj_setup_decoder failed");
            }

            // Header + image struct
            image = new OpjImage();
            if (!OpenJpeg.opj_read_header(stream, codec, image)) {
                throw new RuntimeException("opj_read_header failed");
            }

            // Full image decode
            if (!OpenJpeg.opj_decode(codec, stream, image)) {
                throw new RuntimeException("opj_decode failed");
            }

            if (!OpenJpeg.opj_end_decompress(codec, stream)) {
                // You can either treat this as fatal or just log it
                throw new RuntimeException("opj_end_decompress failed");
            }

            return toDecodedImage(image);

        } finally {
            if (codec != null) {
                OpenJpeg.opj_destroy_codec(codec);
            }
            if (stream != null) {
                // You can either call jopj.Cio.opj_stream_destroy or a wrapper
                Cio.opj_stream_destroy(stream);
            }
            if (image != null) {
                OpenJpeg.opj_image_destroy(image);
            }
        }
    }

    private static DecodedImage toDecodedImage(OpjImage img) {
        if (img.comps == null || img.numcomps <= 0) {
            throw new IllegalStateException("No components in decoded image");
        }

        DecodedImage out = new DecodedImage();
        out.numComponents = img.numcomps;

        OpjImage.OpjImageComp first = img.comps[0];
        out.width  = first.w;
        out.height = first.h;
        out.bitsPerSample = first.prec;
        out.signed = first.sgnd;

        int n = out.width * out.height;
        out.components = new int[out.numComponents][];

        for (int c = 0; c < out.numComponents; ++c) {
            OpjImage.OpjImageComp comp = img.comps[c];

            if (comp.w != out.width || comp.h != out.height) {
                throw new IllegalStateException("Subsampled components not supported yet");
            }
            if (comp.data == null || comp.data.length < n) {
                throw new IllegalStateException("Component " + c + " has invalid data buffer");
            }

            int[] plane = new int[n];
            System.arraycopy(comp.data, 0, plane, 0, n);
            out.components[c] = plane;
        }

        return out;
    }
}