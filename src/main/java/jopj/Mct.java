package jopj;

/**
 * Java skeleton for mct.c / mct.h
 *
 * Multi-component transforms (e.g., RGB <-> YCbCr).
 */
public final class Mct {

    private Mct() {
    }

    /**
     * C: OPJ_FLOAT64 opj_mct_getnorm(OPJ_UINT32 compno);
     */
    public static double opj_mct_getnorm(long compno) {
        // TODO: port actual norms; for now, neutral factor.
        return 1.0;
    }

    /**
     * C: OPJ_FLOAT64 opj_mct_getnorm_real(OPJ_UINT32 compno);
     */
    public static double opj_mct_getnorm_real(long compno) {
        // TODO: high-precision norms.
        return opj_mct_getnorm(compno);
    }

    /**
     * C: const OPJ_FLOAT64 * opj_mct_get_mct_norms(void);
     *
     * Java: return a copy of the norm array (stubbed).
     */
    public static double[] opj_mct_get_mct_norms() {
        // e.g., one value per component for standard RGB->YCC.
        return new double[] {1.0, 1.0, 1.0};
    }

    /**
     * C: const OPJ_FLOAT64 * opj_mct_get_mct_norms_real(void);
     */
    public static double[] opj_mct_get_mct_norms_real() {
        return opj_mct_get_mct_norms();
    }

    /* ------------------------------------------------------------------ */
    /* Placeholders for actual MCT forward/inverse routines                */
    /* ------------------------------------------------------------------ */

    // As you port mct.c, you will add methods like:
    //  - forward MCT on int[]/float[]
    //  - inverse MCT
    //  and call them from jopj.J2K tier-1 / tile-level reconstruction.
}