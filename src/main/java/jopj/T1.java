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

    public static boolean opj_t1_decode_cblk(
            OpjT1 t1,
            Tcd.OpjTcdCblkDec cblk,
            long orient,
            long roishift,
            long cblksty,
            Cio.OpjEventMgr eventMgr,
            boolean checkPterm) {

        if (t1 == null || cblk == null) {
            return false;
        }

        Mqc.OpjMqc mqc = t1.mqc;  // MQC component

        int bpnoPlusOne;
        int passtype;
        int segno, passno;
        byte[] cblkdata = null;
        int cblkdataIndex = 0;
        byte type = T1_TYPE_MQ;   // BYPASS vs MQ; default MQ
        int[] originalT1Data = null;

        // If you eventually port the LUTs, you can cache them here
        // mqc.lutCtxnoZcOrient = ... based on orient;

        // Allocate coefficient / flag buffers for this codeblock
        int w = cblk.x1 - cblk.x0;
        int h = cblk.y1 - cblk.y0;
        if (!opj_t1_allocate_buffers(t1, w, h)) {
            return false;
        }

        // Compute initial bit-plane (B') = roishift + numbps
        bpnoPlusOne = (int) (roishift + cblk.numbps);
        if (bpnoPlusOne >= 31) {
            if (eventMgr != null) {
                Cio.opj_event_msg(
                        eventMgr,
                        Cio.EVT_WARNING,
                        String.format("opj_t1_decode_cblk(): unsupported bpno_plus_one = %d >= 31\n",
                                bpnoPlusOne)
                );
            }
            return false;
        }

        // In OpenJPEG, decoding always starts at passtype=2 (cleanup pass)
        passtype = 2;

        // Initial MQ context setup
        Mqc.opj_mqc_resetstates(mqc);
        Mqc.opj_mqc_setstate(mqc, T1_CTXNO_UNI, 0, 46);
        Mqc.opj_mqc_setstate(mqc, T1_CTXNO_AGG, 0, 3);
        Mqc.opj_mqc_setstate(mqc, T1_CTXNO_ZC, 0, 4);

        // If codeblock is flagged corrupted, nothing to decode
        if (cblk.corrupted) {
            // In C there’s an assert(cblk->numchunks == 0)
            return true;
        }

        /*
         * Concatenate chunk data if:
         *  - more than one chunk, OR
         *  - we are forced to use a private buffer (multithread/subtiles)
         */
        if (cblk.numchunks > 1 || (t1.mustUseCblkDataBuffer && cblk.numchunks > 0)) {
            int cblkLen = 0;
            for (int i = 0; i < cblk.numchunks; ++i) {
                cblkLen += cblk.chunks[i].len;
            }

            int needed = cblkLen + OPJ_COMMON_CBLK_DATA_EXTRA;
            if (t1.cblkDataBuffer == null || needed > t1.cblkDataBufferSize) {
                t1.cblkDataBuffer = new byte[needed];
                t1.cblkDataBufferSize = needed;
            }

            cblkdata = t1.cblkDataBuffer;
            int dst = 0;
            for (int i = 0; i < cblk.numchunks; ++i) {
                Tcd.OpjTcdSegDataChunk chunk = cblk.chunks[i];
                if (chunk.data != null && chunk.len > 0) {
                    System.arraycopy(chunk.data, 0, cblkdata, dst, chunk.len);
                    dst += chunk.len;
                }
            }
            // zero the extra guard bytes
            for (int i = 0; i < OPJ_COMMON_CBLK_DATA_EXTRA; ++i) {
                cblkdata[dst + i] = 0;
            }
        } else if (cblk.numchunks == 1 && cblk.chunks[0].data != null) {
            // Simple case: single chunk, no need to copy
            Tcd.OpjTcdSegDataChunk chunk0 = cblk.chunks[0];
            cblkdata = chunk0.data;
            cblkdataIndex = 0;
            // We assume the chunk already has enough guard bytes, or you can
            // copy into t1.cblkDataBuffer as above if you want strict safety.
        } else {
            // Nothing to decode – avoid null dereference
            return true;
        }

        // For subtile decoding, decode directly into cblk.decodedData
        if (cblk.decodedData != null) {
            originalT1Data = t1.data;
            t1.data = cblk.decodedData;
        }

        // --- Main segment loop ---
        for (segno = 0; segno < cblk.realNumSegs; ++segno) {
            Tcd.OpjTcdSeg seg = cblk.segs[segno];

            // Decide BYPASS vs MQ coding for this segment
            boolean lazy = ( (cblksty & J2K_CCP_CBLKSTY_LAZY) != 0 );
            boolean bypassCondition = (bpnoPlusOne <= (cblk.numbps - 4)) &&
                    (passtype < 2) &&
                    lazy;
            type = (byte) (bypassCondition ? T1_TYPE_RAW : T1_TYPE_MQ);

            if (type == T1_TYPE_RAW) {
                // RAW (bypass) mode: bit-IO based decode
                Mqc.opj_mqc_raw_init_dec(
                        mqc,
                        cblkdata,
                        cblkdataIndex,
                        seg.len,
                        OPJ_COMMON_CBLK_DATA_EXTRA
                );
            } else {
                // MQ arithmetic decoder initialization
                Mqc.opj_mqc_init_dec(
                        mqc,
                        cblkdata,
                        cblkdataIndex,
                        seg.len,
                        OPJ_COMMON_CBLK_DATA_EXTRA
                );
            }
            cblkdataIndex += seg.len;

            // --- Pass loop within this segment ---
            for (passno = 0;
                 (passno < seg.realNumPasses) && (bpnoPlusOne >= 1);
                 ++passno) {

                switch (passtype) {
                    case 0: // significance pass
                        if (type == T1_TYPE_RAW) {
                            opj_t1_dec_sigpass_raw(t1, bpnoPlusOne, (int) cblksty);
                        } else {
                            opj_t1_dec_sigpass_mqc(t1, bpnoPlusOne, (int) cblksty);
                        }
                        break;
                    case 1: // refinement pass
                        if (type == T1_TYPE_RAW) {
                            opj_t1_dec_refpass_raw(t1, bpnoPlusOne);
                        } else {
                            opj_t1_dec_refpass_mqc(t1, bpnoPlusOne);
                        }
                        break;
                    case 2: // cleanup pass
                        opj_t1_dec_clnpass(t1, bpnoPlusOne, (int) cblksty);
                        break;
                    default:
                        // Should never happen
                        break;
                }

                // RESET coding style: reset MQ contexts at each pass
                if ( ( (cblksty & J2K_CCP_CBLKSTY_RESET) != 0 ) &&
                        type == T1_TYPE_MQ ) {
                    Mqc.opj_mqc_resetstates(mqc);
                    Mqc.opj_mqc_setstate(mqc, T1_CTXNO_UNI, 0, 46);
                    Mqc.opj_mqc_setstate(mqc, T1_CTXNO_AGG, 0, 3);
                    Mqc.opj_mqc_setstate(mqc, T1_CTXNO_ZC, 0, 4);
                }

                // Cycle passtype: 0 → 1 → 2 → back to 0, decrement bit-plane
                passtype++;
                if (passtype == 3) {
                    passtype = 0;
                    bpnoPlusOne--;
                }
            }

            // Finish MQ / RAW decoding for this segment
            Mqc.opq_mqc_finish_dec(mqc);
        } // segno loop

        // Optional PTERM (termination) consistency check
        if (checkPterm) {
            if (mqc.bp + 2 < mqc.end) {
                if (eventMgr != null) {
                    Cio.opj_event_msg(
                            eventMgr,
                            Cio.EVT_WARNING,
                            String.format(
                                    "PTERM check failure: %d remaining bytes in code block (%d used / %d)\n",
                                    (mqc.end - mqc.bp) - 2,
                                    (mqc.bp - mqc.start),
                                    (mqc.end - mqc.start)
                            )
                    );
                }
            } else if (mqc.endOfByteStreamCounter > 2) {
                if (eventMgr != null) {
                    Cio.opj_event_msg(
                            eventMgr,
                            Cio.EVT_WARNING,
                            String.format(
                                    "PTERM check failure: %d synthesized 0xFF markers read\n",
                                    mqc.endOfByteStreamCounter
                            )
                    );
                }
            }
        }

        // Restore original t1->data if we hacked it for subtile decoding
        if (cblk.decodedData != null && originalT1Data != null) {
            t1.data = originalT1Data;
        }

        return true;
    }}