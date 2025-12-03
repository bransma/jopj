package jopj;

public class OpjImage
{
    public int x0;
    public int y0;
    public int x1;
    public int y1;

    public int numcomps;
    public OpjImageComp[] comps;

    public OpjColorSpace color_space = OpjColorSpace.OPJ_CLRSPC_UNKNOWN;

    public OpjImage() {
    }

        /* ==========================================================
       Color space enum (from opj_color_space)
       ========================================================== */

    public enum OpjColorSpace {
        OPJ_CLRSPC_UNKNOWN,
        OPJ_CLRSPC_SRGB,
        OPJ_CLRSPC_GRAY,
        OPJ_CLRSPC_SYCC,
        OPJ_CLRSPC_EYCC,
        OPJ_CLRSPC_CMYK
        // add others if you ever need them
    }

    /* ==========================================================
       Component parameter "template" (opj_image_cmptparm_t)
       ========================================================== */

    /**
     * Java equivalent of opj_image_cmptparm_t.
     * Describes how a component should be created.
     */
    public static final class OpjImageCmptParm {
        public int dx;      // subsampling factor in x
        public int dy;      // subsampling factor in y
        public int w;       // width in samples
        public int h;       // height in samples
        public int x0;      // top-left sample position (image ref)
        public int y0;      // top-left sample position (image ref)
        public int prec;    // precision (bits per sample)
        public int bpp;     // number of stored bits (usually == prec)
        public boolean sgnd; // true if signed

        public OpjImageCmptParm() {
        }
    }

    /* ==========================================================
       Image component (opj_image_comp_t)
       ========================================================== */

    /**
     * Java equivalent of opj_image_comp_t.
     * Holds actual sample data and geometric info for one component.
     */
    public static final class OpjImageComp {
        public int dx;
        public int dy;
        public int w;
        public int h;
        public int x0;
        public int y0;
        public int x1;
        public int y1;
        public int prec;
        public int bpp;
        public boolean sgnd;

        /** Number of highest resolution level decoded (for multi-res). */
        public int resnoDecoded;

        /** Downsampling factor (used during decoding). */
        public int factor;

        /** Sample data: one int per sample. */
        public int[] data;

        public OpjImageComp() {
        }
    }

        /* ==========================================================
       Image data allocator (opj_image_data_alloc/free)
       ========================================================== */

    /**
     * Allocate an int[] for component data, with overflow checks.
     */
    private static int[] opj_image_data_alloc_int(int w, int h) {
        if (h != 0) {
            long num = (long) w * (long) h;
            if (num > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Component buffer too large: " + w + "x" + h);
            }
            return new int[(int) num];
        }
        return new int[0];
    }

    /* ==========================================================
       opj_image_create0
       ========================================================== */

    /**
     * C: opj_image_t* opj_image_create0(void);
     *
     * Create an empty image with all fields zero/NULL.
     */
    public static OpjImage opj_image_create0() {
        return new OpjImage();
    }

    /* ==========================================================
       opj_image_create
       ========================================================== */

    /**
     * C: opj_image_t* opj_image_create(OPJ_UINT32 numcmpts,
     *                                  opj_image_cmptparm_t *cmptparms,
     *                                  OPJ_COLOR_SPACE clrspc);
     *
     * Create an image with given number of components and their parameters.
     * Component data arrays are allocated and initialized to zero.
     */
    public static OpjImage opj_image_create(int numcmpts,
                                            OpjImageCmptParm[] cmptparms,
                                            OpjColorSpace clrspc) {
        if (numcmpts <= 0) {
            throw new IllegalArgumentException("numcmpts must be > 0");
        }
        if (cmptparms == null || cmptparms.length < numcmpts) {
            throw new IllegalArgumentException("cmptparms is null or too short");
        }

        OpjImage image = new OpjImage();
        image.color_space = clrspc;
        image.numcomps = numcmpts;
        image.comps = new OpjImageComp[numcmpts];

        // For now, set the image reference origin to 0,0 by default.
        // Higher-level code can adjust x0,y0,x1,y1 after creation.
        image.x0 = 0;
        image.y0 = 0;
        image.x1 = 0;
        image.y1 = 0;

        for (int compno = 0; compno < numcmpts; ++compno) {
            OpjImageCmptParm src = cmptparms[compno];
            OpjImageComp comp = new OpjImageComp();

            comp.dx   = src.dx;
            comp.dy   = src.dy;
            comp.w    = src.w;
            comp.h    = src.h;
            comp.x0   = src.x0;
            comp.y0   = src.y0;
            comp.prec = src.prec;
            comp.bpp  = src.bpp;
            comp.sgnd = src.sgnd;

            // Derive x1,y1 from x0,y0,w,h
            comp.x1 = comp.x0 + comp.w;
            comp.y1 = comp.y0 + comp.h;

            // Allocate data with overflow checks
            comp.data = opj_image_data_alloc_int(comp.w, comp.h);

            // Optionally zero (Java already zeroes new arrays)
            // Arrays.fill(comp.data, 0);

            image.comps[compno] = comp;

            // Update global image bounds (min x0,y0; max x1,y1 over comps)
            if (compno == 0) {
                image.x0 = comp.x0;
                image.y0 = comp.y0;
                image.x1 = comp.x1;
                image.y1 = comp.y1;
            } else {
                if (comp.x0 < image.x0) image.x0 = comp.x0;
                if (comp.y0 < image.y0) image.y0 = comp.y0;
                if (comp.x1 > image.x1) image.x1 = comp.x1;
                if (comp.y1 > image.y1) image.y1 = comp.y1;
            }
        }

        return image;
    }

    /* ==========================================================
       opj_image_destroy
       ========================================================== */

    /**
     * C: void opj_image_destroy(opj_image_t *image);
     *
     * In Java, we just clear references to help GC.
     */
    public static void opj_image_destroy(OpjImage image) {
        if (image == null) return;
        if (image.comps != null) {
            for (int i = 0; i < image.comps.length; ++i) {
                OpjImageComp c = image.comps[i];
                if (c != null) {
                    c.data = null;
                    image.comps[i] = null;
                }
            }
            image.comps = null;
        }
    }

    /* ==========================================================
       opj_image_comp_header_update (stub)
       ========================================================== */

    /**
     * C: void opj_image_comp_header_update(opj_image_t *p_image,
     *                                      const struct opj_cp *p_cp);
     *
     * In OpenJPEG, this uses the coding parameters (cp->tcps/tccps) to
     * adjust the component headers (dx,dy, subsampling, etc.).
     *
     * Here, we keep it as a stub until your cp/tcp structures are fully
     * defined in Java. You can later implement it by reading from your
     * Java equivalent of opj_cp_t.
     */
    public static void opj_image_comp_header_update(OpjImage image,
                                                    Object cp /* TODO: define cp type */) {
        // TODO: When your jopj.J2K.OpjCp / Pi.OpjCp contains the necessary
        // component coding parameters, implement whatever adjustments
        // are required (subsampling factors, ROI shifts, etc.).
    }

    /* ==========================================================
       opj_copy_image_header
       ========================================================== */

    /**
     * C: void opj_copy_image_header(const opj_image_t* p_image_src,
     *                               opj_image_t* p_image_dest);
     *
     * Copy image geometry and component header info from src to dest,
     * but DO NOT copy the sample data.
     */
    public static void opj_copy_image_header(OpjImage src, OpjImage dest) {
        if (src == null || dest == null) {
            throw new IllegalArgumentException("src/dest must not be null");
        }

        dest.x0 = src.x0;
        dest.y0 = src.y0;
        dest.x1 = src.x1;
        dest.y1 = src.y1;

        dest.color_space = src.color_space;
        dest.numcomps = src.numcomps;

        if (src.comps == null || src.numcomps == 0) {
            dest.comps = new OpjImageComp[0];
            return;
        }

        dest.comps = new OpjImageComp[src.numcomps];
        for (int i = 0; i < src.numcomps; ++i) {
            OpjImageComp sc = src.comps[i];
            OpjImageComp dc = new OpjImageComp();

            dc.dx   = sc.dx;
            dc.dy   = sc.dy;
            dc.w    = sc.w;
            dc.h    = sc.h;
            dc.x0   = sc.x0;
            dc.y0   = sc.y0;
            dc.x1   = sc.x1;
            dc.y1   = sc.y1;
            dc.prec = sc.prec;
            dc.bpp  = sc.bpp;
            dc.sgnd = sc.sgnd;

            dc.resnoDecoded = sc.resnoDecoded;
            dc.factor       = sc.factor;

            // Do NOT copy the data buffer; leave it null.
            dc.data = null;

            dest.comps[i] = dc;
        }
    }
}
