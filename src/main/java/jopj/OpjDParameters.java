package jopj;

/**
 * Java equivalent of opj_dparameters_t.
 *
 * Represents decoder parameters originally defined in the OpenJPEG C library.
 */
public class OpjDParameters {

    /**
     * Set the number of highest resolution levels to be discarded.
     * The image resolution is effectively divided by 2 to the power of the number of discarded levels.
     * The reduce factor is limited by the smallest total number of decomposition levels among tiles.
     * If != 0, then original dimension divided by 2^(cp_reduce).
     * If == 0 or not used, image is decoded to the full resolution.
     */
    private int cpReduce;   // OPJ_UINT32

    /**
     * Set the maximum number of quality layers to decode.
     * If there are fewer quality layers than the specified number, all quality layers are decoded.
     * If != 0, then only the first "cpLayer" layers are decoded.
     * If == 0 or not used, all quality layers are decoded.
     */
    private int cpLayer;    // OPJ_UINT32


    // --- Command line decoder parameters (not used inside the library) ---

    /** Input file name */
    private String infile;   // char infile[OPJ_PATH_LEN];

    /** Output file name */
    private String outfile;  // char outfile[OPJ_PATH_LEN];

    /** Input file format 0: J2K, 1: JP2, 2: JPT */
    private int decodFormat;

    /** Output file format 0: PGX, 1: PxM, 2: BMP */
    private int codFormat;


    // --- Decoding area parameters ---

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
    private int tileIndex;       // OPJ_UINT32

    /** Number of tiles to decode */
    private int nbTileToDecode;  // OPJ_UINT32


    // --- JPWL decoding parameters (currently not used in v2 of OpenJPEG) ---

    /** Activates the JPWL correction capabilities */
    private boolean jpwlCorrect;   // OPJ_BOOL

    /** Expected number of components */
    private int jpwlExpComps;

    /** Maximum number of tiles */
    private int jpwlMaxTiles;


    /** Flags (bitfield in original C struct; mapped to int here) */
    private int flags;  // unsigned int in C; int is usually sufficient here


    // ---------------------------------------------------------------------
    // Constructors
    // ---------------------------------------------------------------------

    public OpjDParameters() {
        // Provide sensible defaults mirroring "zeroed" C struct
        this.cpReduce = 0;
        this.cpLayer = 0;
        this.infile = null;
        this.outfile = null;
        this.decodFormat = 0;
        this.codFormat = 0;
        this.daX0 = 0;
        this.daX1 = 0;
        this.daY0 = 0;
        this.daY1 = 0;
        this.verbose = false;
        this.tileIndex = 0;
        this.nbTileToDecode = 0;
        this.jpwlCorrect = false;
        this.jpwlExpComps = 0;
        this.jpwlMaxTiles = 0;
        this.flags = 0;
    }

    // Optionally, a copy constructor if you need to clone parameter sets.
    public OpjDParameters(OpjDParameters other) {
        this.cpReduce = other.cpReduce;
        this.cpLayer = other.cpLayer;
        this.infile = other.infile;
        this.outfile = other.outfile;
        this.decodFormat = other.decodFormat;
        this.codFormat = other.codFormat;
        this.daX0 = other.daX0;
        this.daX1 = other.daX1;
        this.daY0 = other.daY0;
        this.daY1 = other.daY1;
        this.verbose = other.verbose;
        this.tileIndex = other.tileIndex;
        this.nbTileToDecode = other.nbTileToDecode;
        this.jpwlCorrect = other.jpwlCorrect;
        this.jpwlExpComps = other.jpwlExpComps;
        this.jpwlMaxTiles = other.jpwlMaxTiles;
        this.flags = other.flags;
    }

    // ---------------------------------------------------------------------
    // Getters and setters
    // ---------------------------------------------------------------------

    public int getCpReduce() {
        return cpReduce;
    }

    public void setCpReduce(int cpReduce) {
        this.cpReduce = cpReduce;
    }

    public int getCpLayer() {
        return cpLayer;
    }

    public void setCpLayer(int cpLayer) {
        this.cpLayer = cpLayer;
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

    public boolean isJpwlCorrect() {
        return jpwlCorrect;
    }

    public void setJpwlCorrect(boolean jpwlCorrect) {
        this.jpwlCorrect = jpwlCorrect;
    }

    public int getJpwlExpComps() {
        return jpwlExpComps;
    }

    public void setJpwlExpComps(int jpwlExpComps) {
        this.jpwlExpComps = jpwlExpComps;
    }

    public int getJpwlMaxTiles() {
        return jpwlMaxTiles;
    }

    public void setJpwlMaxTiles(int jpwlMaxTiles) {
        this.jpwlMaxTiles = jpwlMaxTiles;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    @Override
    public String toString() {
        return "OpjDParameters{" +
                "cpReduce=" + cpReduce +
                ", cpLayer=" + cpLayer +
                ", infile='" + infile + '\'' +
                ", outfile='" + outfile + '\'' +
                ", decodFormat=" + decodFormat +
                ", codFormat=" + codFormat +
                ", daX0=" + daX0 +
                ", daX1=" + daX1 +
                ", daY0=" + daY0 +
                ", daY1=" + daY1 +
                ", verbose=" + verbose +
                ", tileIndex=" + tileIndex +
                ", nbTileToDecode=" + nbTileToDecode +
                ", jpwlCorrect=" + jpwlCorrect +
                ", jpwlExpComps=" + jpwlExpComps +
                ", jpwlMaxTiles=" + jpwlMaxTiles +
                ", flags=" + flags +
                '}';
    }
}
