package jopj;

public class OpjPrecision
{
    /**
     * Java equivalent of opj_precision_mode.
     */
    public enum OpjPrecisionMode {
        OPJ_PREC_MODE_CLIP,
        OPJ_PREC_MODE_SCALE
    }

    /** Precision value (C: OPJ_UINT32) */
    private int prec;

    /** Precision mode (clip or scale) */
    private OpjPrecisionMode mode;

    public OpjPrecision() {
        // Zero-initialized C struct equivalent
        this.prec = 0;
        this.mode = OpjPrecisionMode.OPJ_PREC_MODE_CLIP;
    }

    public OpjPrecision(int prec, OpjPrecisionMode mode) {
        this.prec = prec;
        this.mode = mode;
    }

    public int getPrec() {
        return prec;
    }

    public void setPrec(int prec) {
        this.prec = prec;
    }

    public OpjPrecisionMode getMode() {
        return mode;
    }

    public void setMode(OpjPrecisionMode mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "OpjPrecision{" +
                "prec=" + prec +
                ", mode=" + mode +
                '}';
    }
}
