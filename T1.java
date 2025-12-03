package jopj;

/**
 * Java skeleton for t1.c / t1.h
 *
 * Tier-1 coder / decoder for JPEG2000 (codeblock-level).
 */
public final class T1 {

    private T1() {
    }

    /**
     * Java equivalent of opj_t1_t.
     * Extend with fields like:
     *  - codeblock size
     *  - coefficient buffers
     *  - MQ coder state, etc.
     */
    public static final class OpjT1 {
        public boolean isEncoder;
        // e.g. int width, height;
        // int[] coeffs;
        // jopj.Mqc.OpjMqc mqc;
    }

    /**
     * C: opj_t1_t* opj_t1_create(OPJ_BOOL isEncoder);
     */
    public static OpjT1 opj_t1_create(boolean isEncoder) {
        OpjT1 t1 = new OpjT1();
        t1.isEncoder = isEncoder;
        return t1;
    }

    /**
     * C: void opj_t1_destroy(opj_t1_t *p_t1);
     */
    public static void opj_t1_destroy(OpjT1 t1) {
        // Nothing special in pure Java; add cleanup if you allocate native resources.
    }

    /* ------------------------------------------------------------------ */
    /* Placeholders for actual tier-1 decode / encode methods             */
    /* ------------------------------------------------------------------ */

    // When porting t1.c, you’ll place methods here such as:
    //
    //  - decodeCodeblock(OpjT1 t1, byte[] compressed, int width, int height, int[] outCoeffs)
    //  - encodeCodeblock(OpjT1 t1, int[] coeffs, int width, int height, byte[] outBuffer)
    //
    // These will use jopj.Mqc and jopj.Dwt logic behind the scenes.
}