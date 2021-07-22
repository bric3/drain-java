package com.github.bric3.drain;

import picocli.CommandLine;

class FromLine {
    boolean fromStart = false;
    long number = 10;

    static FromLine fromStart(long lineNumber) {
        FromLine startFromLine = new FromLine();
        startFromLine.fromStart = true;
        startFromLine.number = lineNumber;
        return startFromLine;
    }

    static FromLine fromEnd(long lineNumber) {
        FromLine startFromLine = new FromLine();
        startFromLine.fromStart = false;
        startFromLine.number = lineNumber;
        return startFromLine;
    }

    static class StartFromLineConverter implements CommandLine.ITypeConverter<FromLine> {
        @Override
        public FromLine convert(String value) {
            FromLine result = new FromLine();
            if (value.charAt(0) == '+') {
                result.fromStart = true;
                value = value.substring(1);
            }
            result.number = Long.parseLong(value);
            if (result.number < 0) {
                throw new CommandLine.TypeConversionException(
                        "invalid number of lines '" + value + "': must be 0 or positive number."
                );
            }
            return result;
        }
    }
}
