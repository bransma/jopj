package jopj;

/**
 * Java equivalent of opj_decompress_params.
 *
 * This class wraps the core OpenJPEG decoder parameters (OpjDParameters)
 * and extends them with higher-level decompression options.
 */
public class OpjDecompressParameters {

    /** Core library parameters */
    private OpjDParameters core;

    /** Input file name */
    private String infile;

    /** Output file name */
    private String outfile;

    /** Input file format: 0 = J2K, 1 = JP2, 2 = JPT */
    private int decodFormat;

    /** Output file format: 0 = PGX, 1 = PxM, 2 = BMP */
    private int codFormat;

    /** Index file name */
    private String indexFilename;

    // --- Decoding area ---

    /** Decoding area left boundary */
    private int daX0;   // OPJ_UINT32

    /** Decoding area right boundary */
    private int daX1;   // OPJ_UINT32

    /** Decoding area top boundary */
    private int daY0;   // OPJ_UINT32

    /** Decoding area bottom boundary */
    private int daY1;   // OPJ_UINT32

    /** Verbose mode */
    private boolean verbose;  // OPJ_BOOL


    // --- Tile selection ---

    /** Tile number of the decoded tile */
    private int tileIndex;    // OPJ_UINT32

    /** Number of tiles to decode */
    private int nbTileToDecode; // OPJ_UINT32


    // --- Precision handling ---

    /**
     * Precision array.
     * C equivalent: opj_precision* precision
     *
     * Typically, this would be a small struct; represent it as an array.
     */
    private OpjPrecision[] precision;

    /** Number of precision entries */
    private int nbPrecision;   // OPJ_UINT32


    // --- Misc decoder options ---

    /** Force output colorspace to RGB */
    private boolean forceRgb;

    /** Upsample components according to dx/dy */
    private boolean upsample;

    /** Split output components into separate files */
    private boolean splitPnm;

    /** Number of threads */
    private int numThreads;

    /** Quiet mode */
    private boolean quiet;

    /** Allow partial decode */
    private boolean allowPartial;

    /** Number of components to decode */
    private int numComps;  // OPJ_UINT32

    /**
     * Indices of components to decode.
     * C equivalent: OPJ_UINT32* comps_indices
     */
    private int[] compsIndices;


    // ---------------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------------

    public OpjDecompressParameters() {
        // Mirrors a zero-initialized C struct
        this.core = new OpjDParameters();

        this.infile = null;
        this.outfile = null;
        this.indexFilename = null;

        this.decodFormat = 0;
        this.codFormat = 0;

        this.daX0 = 0;
        this.daX1 = 0;
        this.daY0 = 0;
        this.daY1 = 0;

        this.verbose = false;

        this.tileIndex = 0;
        this.nbTileToDecode = 0;

        this.precision = null;
        this.nbPrecision = 0;

        this.forceRgb = false;
        this.upsample = false;
        this.splitPnm = false;
        this.numThreads = 1;
        this.quiet = false;
        this.allowPartial = false;

        this.numComps = 0;
        this.compsIndices = null;
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public OpjDParameters getCore() {
        return core;
    }

    public void setCore(OpjDParameters core) {
        this.core = core;
    }

    public String getInfile() {
        return infile;
    }

    public void setInfile(String infile) {
        this.infile = infile;
    }

    public String getOutfile() {
        return outfile;
    }

    public void setOutfile(String outfile) {
        this.outfile = outfile;
    }

    public int getDecodFormat() {
        return decodFormat;
    }

    public void setDecodFormat(int decodFormat) {
        this.decodFormat = decodFormat;
    }

    public int getCodFormat() {
        return codFormat;
    }

    public void setCodFormat(int codFormat) {
        this.codFormat = codFormat;
    }

    public String getIndexFilename() {
        return indexFilename;
    }

    public void setIndexFilename(String indexFilename) {
        this.indexFilename = indexFilename;
    }

    public int getDaX0() {
        return daX0;
    }

    public void setDaX0(int daX0) {
        this.daX0 = daX0;
    }

    public int getDaX1() {
        return daX1;
    }

    public void setDaX1(int daX1) {
        this.daX1 = daX1;
    }

    public int getDaY0() {
        return daY0;
    }

    public void setDaY0(int daY0) {
        this.daY0 = daY0;
    }

    public int getDaY1() {
        return daY1;
    }

    public void setDaY1(int daY1) {
        this.daY1 = daY1;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public int getTileIndex() {
        return tileIndex;
    }

    public void setTileIndex(int tileIndex) {
        this.tileIndex = tileIndex;
    }

    public int getNbTileToDecode() {
        return nbTileToDecode;
    }

    public void setNbTileToDecode(int nbTileToDecode) {
        this.nbTileToDecode = nbTileToDecode;
    }

    public OpjPrecision[] getPrecision() {
        return precision;
    }

    public void setPrecision(OpjPrecision[] precision) {
        this.precision = precision;
        this.nbPrecision = (precision != null) ? precision.length : 0;
    }

    public int getNbPrecision() {
        return nbPrecision;
    }

    public boolean isForceRgb() {
        return forceRgb;
    }

    public void setForceRgb(boolean forceRgb) {
        this.forceRgb = forceRgb;
    }

    public boolean isUpsample() {
        return upsample;
    }

    public void setUpsample(boolean upsample) {
        this.upsample = upsample;
    }

    public boolean isSplitPnm() {
        return splitPnm;
    }

    public void setSplitPnm(boolean splitPnm) {
        this.splitPnm = splitPnm;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public boolean isAllowPartial() {
        return allowPartial;
    }

    public void setAllowPartial(boolean allowPartial) {
        this.allowPartial = allowPartial;
    }

    public int getNumComps() {
        return numComps;
    }

    public void setNumComps(int numComps) {
        this.numComps = numComps;
    }

    public int[] getCompsIndices() {
        return compsIndices;
    }

    public void setCompsIndices(int[] compsIndices) {
        this.compsIndices = compsIndices;
        this.numComps = (compsIndices != null) ? compsIndices.length : 0;
    }
}
