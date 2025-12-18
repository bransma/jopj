package jopj;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;

/**
 * Narrow-path JPEG 2000 decode API:
 *   byte[] codestream -> DecodedImage (planar int samples).
 * No CLI, no file I/O. This is what your DICOM pipeline should call.
 */
public abstract class OpjDecompress {

    public OpjDecompress() {
    }

    public static void main(String[] args)
    {

         String path = args[0];
         byte[] jp2k_data;

         try
         {
             jp2k_data = readData(path);
             DecodedImage img = OpjDecompress.decode(jp2k_data);
             System.out.println(img);
         }
         catch (IOException ioe)
         {
             ioe.printStackTrace();
             System.exit(-1);
         }
    }

    public static byte[] readData(String fileName) throws IOException
    {
        RandomAccessFile in = new RandomAccessFile(fileName, "r");
        byte[] rawData = new byte[(int) in.length()];
        in.readFully(rawData);
        in.close();
        return rawData;
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
        Objects.requireNonNull(input, "code stream must not be null");

        Jp2Parser.J2kFormat format = Jp2Parser.sniffFormat(input);
        OpjStream stream;
        OpjCodec codec;
        OpjImage image = null;

        byte[] codestream;
        switch (format)
        {
            case RAW_J2K ->
            {
                codestream = input;
                // Wrap byte[] in a jopj.OpjStream
                stream = OpjStream.createFromByteArray(codestream);

                // Create codec for raw jopj.J2K (or JP2 if you prefer)
                codec = OpenJpeg.opj_create_decompress(OpenJpeg.OpjCodecFormat.OPJ_CODEC_J2K);
            }
            case JP2 ->
            {
                codestream = Jp2Parser.extractCodestream(input);
                // Wrap byte[] in a jopj.OpjStream
                stream = OpjStream.createFromByteArray(codestream);

                // Create codec for raw jopj.J2K (or JP2 if you prefer)
                codec = OpenJpeg.opj_create_decompress(OpenJpeg.OpjCodecFormat.OPJ_CODEC_JP2);
            }
            default -> throw new UnsupportedOperationException("Unsupported JPEG 2000 format");
        }

        try {
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
            OpenJpeg.opj_destroy_codec(codec);
            Cio.opj_stream_destroy(stream);
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