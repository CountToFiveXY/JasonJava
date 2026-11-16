package jasonjava.macro;

import java.util.List;

/** Builds the eight-ad macro used by the REFILL_ADS remote task. */
public final class RefillAds extends MacroRun {
    private static final int AD_COUNT = 8;

    public static void main(String[] args) {
        new RefillAds().combine();
    }

    @Override
    protected void buildMacro(List<String> macro) {
        combiner.addMacro(macro, MacroCombiner.AD, AD_COUNT);
    }

    @Override
    protected String outputName() {
        return "refill-ads";
    }
}
