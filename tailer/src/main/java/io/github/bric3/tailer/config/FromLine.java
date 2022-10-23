/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.tailer.config;

import picocli.CommandLine;

public class FromLine {
    public boolean fromStart = false;
    public long number = 10;

    public static FromLine fromStart(long lineNumber) {
        var startFromLine = new FromLine();
        startFromLine.fromStart = true;
        startFromLine.number = lineNumber;
        return startFromLine;
    }

    public static FromLine fromEnd(long lineNumber) {
        var startFromLine = new FromLine();
        startFromLine.fromStart = false;
        startFromLine.number = lineNumber;
        return startFromLine;
    }

    public static class StartFromLineConverter implements CommandLine.ITypeConverter<FromLine> {
        @Override
        public FromLine convert(String value) {
            var result = new FromLine();
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
