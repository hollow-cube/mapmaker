package net.hollowcube.command.util;

public enum WordType {

    ALPHANUMERIC {
        @Override
        boolean test(char c) {
            return c >= '0' && c <= '9'
                    || c >= 'A' && c <= 'Z'
                    || c >= 'a' && c <= 'z';
        }
    },
    BRIGADIER {
        @Override
        boolean test(char c) {
            return c >= '0' && c <= '9'
                    || c >= 'A' && c <= 'Z'
                    || c >= 'a' && c <= 'z'
                    || c == '_' || c == '-'
                    || c == '.' || c == '+';
        }
    },
    GREEDY {
        @Override
        boolean test(char c) {
            // Maybe need to be smarter than this, not sure.
            return c != ' ';
        }
    };

    abstract boolean test(char c);

}
