package net.hollowcube.sqlgen;

/// snake_case (Postgres) to Java identifiers, and back where the generator needs a stable
/// constant name.
final class Names {

    /// `head_db` -> `headDb`. Already-camel input is returned unchanged.
    static String camel(String name) {
        var out = new StringBuilder(name.length());
        boolean upper = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                upper = out.length() > 0;
                continue;
            }
            out.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return out.isEmpty() ? name : out.toString();
    }

    /// `head_db` -> `HeadDb`, `getRandomHeads` -> `GetRandomHeads`.
    static String pascal(String name) {
        var camel = camel(name);
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /// `getRandomHeads` -> `GET_RANDOM_HEADS`; the name of the emitted SQL constant.
    static String constant(String name) {
        var out = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0 && out.charAt(out.length() - 1) != '_') out.append('_');
            out.append(c == '_' ? '_' : Character.toUpperCase(c));
        }
        return out.toString();
    }

    private Names() {
    }
}
