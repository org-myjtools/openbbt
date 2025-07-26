package org.myjtools.openbbt.core.adapters;


import org.myjtools.openbbt.core.DataType;
import org.myjtools.openbbt.core.OpenBBTException;

import java.text.*;
import java.util.*;
import java.util.function.Function;

public class NumberDataTypeAdapter<T extends Number> extends DataTypeAdapter<T> implements DataType {

    private static final DecimalFormat bigDecimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
    static {
        bigDecimalFormat.setParseBigDecimal(true);
    }
    private static final DecimalFormat regularFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);


    public NumberDataTypeAdapter(
        String name,
        Class<T> javaType,
        boolean includeDecimals,
        boolean useBigDecimal,
        Function<Number,T> converter
    ) {
        super(
            name,
            javaType,
            numericRegexPattern(includeDecimals),
            useBigDecimal ? bigDecimalFormat.toLocalizedPattern() : regularFormat.toLocalizedPattern(),
            parser(includeDecimals, converter)
        );
    }



    private static <T> Function<String,T> parser(boolean includeDecimals, Function<Number, T> converter) {
        return source -> {
            try {
               var number = includeDecimals ? bigDecimalFormat.parse(source) : regularFormat.parse(source);
               return converter.apply(number);
            } catch (ParseException e) {
                throw new OpenBBTException(e);
            }
        };
    }



    private static String numericRegexPattern(boolean includeDecimals) {
        DecimalFormat format = includeDecimals ? bigDecimalFormat : regularFormat;
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        StringBuilder pattern = new StringBuilder("-?")
            .append("\\d{1,").append(format.getGroupingSize()).append("}")
            .append("(\\").append(symbols.getGroupingSeparator()).append("?")
            .append("\\d{1,").append(format.getGroupingSize()).append("})*");
        if (includeDecimals) {
            pattern.append("\\").append(symbols.getDecimalSeparator()).append("\\d+?");
        }
        return pattern.toString();
    }





}
