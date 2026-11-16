package jasonjava.macro;

import jasonjava.macro.utils.FileContentHandler;
import jasonjava.macro.utils.FilePrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;

public class MacroCombiner {

    private final FileContentHandler fileContentHandler = new FileContentHandler();

    static final String RESET = "reset";
    static final String PRESET = "preset";
    static final String WINDOWED = "window";
    static final String AD = "ad";
    static final String BASE = "base";
    static final String NEWS = "news";
    static final Map<String, Integer> classFuels = Map.of(
            "D4", 2,
            "D5", 3,
            "C4", 5,
            "C5", 6,
            "B5", 7,
            "B6", 8,
            "A5", 11,
            "A6", 12
    );

    private static final String MACRO_PATH = "macrofiles/";
    private static final String MACRO_SUFFIX = ".mcr";

    private static final Path OUTPUT_DIRECTORY = Path.of(
            System.getenv().getOrDefault("JASON_MACRO_OUTPUT_DIR", "generated-macros")
    );

    void reset() {
        writeMacro("reset", resets());
    }

    List<String> resets() {
        List<String> macro = new ArrayList<>();
        addMacro(macro, RESET, 1);
        addMacro(macro, NEWS, 1);
        addMacro(macro, PRESET, 1);
        addMacro(macro, WINDOWED, 1);

        return macro;
    }

    void addMacro(List<String> macro, String macroName, int times) {
        macro.addAll(multiplySingleMacro(macroName, times));
    }

    void writeMacro(String fileName, List<String> macro) {
        FilePrinter.overrideFile(
                macro,
                OUTPUT_DIRECTORY.resolve(fileName + MACRO_SUFFIX).toString()
        );
    }

    private List<String> multiplySingleMacro(String macroName, int times) {
        return fileContentHandler.multiplySingleFile(
                MACRO_PATH + macroName + MACRO_SUFFIX,
                times
        );
    }
}
