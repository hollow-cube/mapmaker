package net.hollowcube.luau.docs.types;

import java.util.ArrayList;
import java.util.List;

/// Recursive-descent parser for the Luau `type` production from <https://luau.org/grammar/>.
/// Hand-written so error messages can pinpoint the exact byte offset within the input
/// expression.
///
/// Supports: named/qualified-named types with type-argument lists, generic instantiations,
/// generic packs (`T...`) and variadics (`...T`), optionals (`T?`), unions (`T | U`),
/// intersections (`T & U`), table types (array, fields, indexer), function types with named
/// params and multi-return, string and boolean singletons, and `typeof(…)` (preserved raw).
///
/// Read/write table-field modifiers are explicitly rejected with a clear error in v1.
public final class LuauTypeParser {

    private LuauTypeParser() {
    }

    public static LuauType parse(String src) {
        var tokens = Lexer.tokenize(src);
        var parser = new Parser(tokens, src.length());
        var type = parser.parseType();
        parser.expect(TokenType.EOF, "expected end of input");
        return type;
    }

    // ===================== Tokens =====================

    enum TokenType {
        NAME, STRING, MODULE_REF,
        LT, GT, LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE,
        QUESTION, PIPE, AMP, COMMA, COLON, SEMI, DOT, DOT_DOT_DOT, ARROW,
        // Keywords (lexer reclassifies NAME)
        K_NIL, K_TRUE, K_FALSE, K_TYPEOF, K_READ, K_WRITE,
        EOF
    }

    record Token(TokenType type, String text, int offset) {}

    // ===================== Lexer =====================

    private static final class Lexer {
        private final String src;
        private int pos = 0;

        Lexer(String src) {
            this.src = src;
        }

        static List<Token> tokenize(String src) {
            var lex = new Lexer(src);
            var out = new ArrayList<Token>();
            while (true) {
                var tok = lex.next();
                out.add(tok);
                if (tok.type == TokenType.EOF) break;
            }
            return out;
        }

        Token next() {
            skipWhitespace();
            if (pos >= src.length()) return new Token(TokenType.EOF, "", pos);
            int start = pos;
            char c = src.charAt(pos);
            switch (c) {
                case '<':
                    pos++;
                    return new Token(TokenType.LT, "<", start);
                case '>':
                    pos++;
                    return new Token(TokenType.GT, ">", start);
                case '(':
                    pos++;
                    return new Token(TokenType.LPAREN, "(", start);
                case ')':
                    pos++;
                    return new Token(TokenType.RPAREN, ")", start);
                case '[':
                    pos++;
                    return new Token(TokenType.LBRACKET, "[", start);
                case ']':
                    pos++;
                    return new Token(TokenType.RBRACKET, "]", start);
                case '{':
                    pos++;
                    return new Token(TokenType.LBRACE, "{", start);
                case '}':
                    pos++;
                    return new Token(TokenType.RBRACE, "}", start);
                case '?':
                    pos++;
                    return new Token(TokenType.QUESTION, "?", start);
                case '|':
                    pos++;
                    return new Token(TokenType.PIPE, "|", start);
                case '&':
                    pos++;
                    return new Token(TokenType.AMP, "&", start);
                case ',':
                    pos++;
                    return new Token(TokenType.COMMA, ",", start);
                case ':':
                    pos++;
                    return new Token(TokenType.COLON, ":", start);
                case ';':
                    pos++;
                    return new Token(TokenType.SEMI, ";", start);
                case '.':
                    if (pos + 2 < src.length() && src.charAt(pos + 1) == '.' && src.charAt(pos + 2) == '.') {
                        pos += 3;
                        return new Token(TokenType.DOT_DOT_DOT, "...", start);
                    }
                    pos++;
                    return new Token(TokenType.DOT, ".", start);
                case '-':
                    if (pos + 1 < src.length() && src.charAt(pos + 1) == '>') {
                        pos += 2;
                        return new Token(TokenType.ARROW, "->", start);
                    }
                    throw new LuauParseException(start, "unexpected '-' (expected '->')");
                case '"':
                case '\'':
                    return readString(c, start);
                case '@':
                    return readModuleRef(start);
            }
            if (Character.isLetter(c) || c == '_') {
                return readName(start);
            }
            throw new LuauParseException(start, "unexpected character '" + c + "'");
        }

        private Token readModuleRef(int start) {
            pos++; // consume '@'
            int contentStart = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '/' || c == '-') pos++;
                else break;
            }
            if (pos == contentStart) {
                throw new LuauParseException(start, "expected module name after '@'");
            }
            return new Token(TokenType.MODULE_REF, src.substring(start, pos), start);
        }

        private Token readString(char quote, int start) {
            pos++; // consume opening quote
            var sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == '\\') {
                    if (pos + 1 >= src.length())
                        throw new LuauParseException(pos, "unterminated escape in string literal");
                    char e = src.charAt(pos + 1);
                    sb.append(switch (e) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        case 'r' -> '\r';
                        case '"' -> '"';
                        case '\'' -> '\'';
                        case '\\' -> '\\';
                        default -> throw new LuauParseException(pos, "unknown escape '\\" + e + "'");
                    });
                    pos += 2;
                } else if (c == quote) {
                    pos++;
                    return new Token(TokenType.STRING, sb.toString(), start);
                } else {
                    sb.append(c);
                    pos++;
                }
            }
            throw new LuauParseException(start, "unterminated string literal");
        }

        private Token readName(int start) {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isLetterOrDigit(c) || c == '_') pos++;
                else break;
            }
            String text = src.substring(start, pos);
            TokenType kw = switch (text) {
                case "nil" -> TokenType.K_NIL;
                case "true" -> TokenType.K_TRUE;
                case "false" -> TokenType.K_FALSE;
                case "typeof" -> TokenType.K_TYPEOF;
                case "read" -> TokenType.K_READ;
                case "write" -> TokenType.K_WRITE;
                default -> TokenType.NAME;
            };
            return new Token(kw, text, start);
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }
    }

    // ===================== Parser =====================

    private static final class Parser {
        private final List<Token> tokens;
        private final int srcLength;
        private int idx = 0;

        Parser(List<Token> tokens, int srcLength) {
            this.tokens = tokens;
            this.srcLength = srcLength;
        }

        // ---- token helpers ----

        Token peek() {
            return tokens.get(idx);
        }

        Token peek(int ahead) {
            return tokens.get(Math.min(idx + ahead, tokens.size() - 1));
        }

        boolean check(TokenType t) {
            return peek().type == t;
        }

        boolean match(TokenType t) {
            if (check(t)) {
                idx++;
                return true;
            }
            return false;
        }

        Token expect(TokenType t, String message) {
            if (!check(t)) {
                throw new LuauParseException(peek().offset, message + " (got '" + peek().text + "')");
            }
            return tokens.get(idx++);
        }

        int currentOffset() {
            return idx < tokens.size() ? peek().offset : srcLength;
        }

        // ---- entry: parseType handles unions and intersections ----

        LuauType parseType() {
            var first = parseOptional();
            if (check(TokenType.PIPE)) {
                var alts = new ArrayList<LuauType>();
                alts.add(first);
                while (match(TokenType.PIPE)) alts.add(parseOptional());
                return new LuauType.Union(alts);
            }
            if (check(TokenType.AMP)) {
                var conjs = new ArrayList<LuauType>();
                conjs.add(first);
                while (match(TokenType.AMP)) conjs.add(parseOptional());
                return new LuauType.Intersection(conjs);
            }
            return first;
        }

        LuauType parseOptional() {
            var simple = parseSimple();
            if (match(TokenType.QUESTION)) return new LuauType.Optional(simple);
            return simple;
        }

        LuauType parseSimple() {
            var tok = peek();
            return switch (tok.type) {
                case K_NIL -> {
                    idx++;
                    yield new LuauType.Named(null, "nil", List.of());
                }
                case K_TRUE -> {
                    idx++;
                    yield new LuauType.BoolLiteral(true);
                }
                case K_FALSE -> {
                    idx++;
                    yield new LuauType.BoolLiteral(false);
                }
                case STRING -> {
                    idx++;
                    yield new LuauType.StringLiteral(tok.text);
                }
                case K_TYPEOF -> parseTypeof();
                case LBRACE -> parseTable();
                case LPAREN, LT -> parseFunction();
                case DOT_DOT_DOT -> {
                    throw new LuauParseException(tok.offset,
                        "variadic '...' is only valid inside function param/return lists or pack arguments");
                }
                case NAME, MODULE_REF -> parseNamed();
                default -> throw new LuauParseException(tok.offset,
                    "expected a type (got '" + tok.text + "')");
            };
        }

        LuauType parseTypeof() {
            int start = peek().offset;
            expect(TokenType.K_TYPEOF, "expected 'typeof'");
            expect(TokenType.LPAREN, "expected '(' after 'typeof'");
            int exprStart = peek().offset;
            int depth = 1;
            int end = exprStart;
            while (idx < tokens.size() && depth > 0) {
                var t = peek();
                if (t.type == TokenType.LPAREN) depth++;
                else if (t.type == TokenType.RPAREN) {
                    depth--;
                    if (depth == 0) {
                        end = t.offset;
                        break;
                    }
                } else if (t.type == TokenType.EOF) break;
                idx++;
            }
            if (depth != 0) throw new LuauParseException(start, "unterminated 'typeof(…)'");
            // We don't preserve the raw source string here (we have tokens). Reconstruct best-effort.
            var sb = new StringBuilder();
            int saved = idx;
            // walk back to exprStart token index
            int exprIdx = idx;
            while (exprIdx > 0 && tokens.get(exprIdx - 1).offset >= exprStart) exprIdx--;
            for (int i = exprIdx; i < idx; i++) {
                if (i > exprIdx) sb.append(' ');
                sb.append(tokens.get(i).text);
            }
            expect(TokenType.RPAREN, "expected ')' to close typeof");
            return new LuauType.TypeOf(sb.toString());
        }

        LuauType parseTable() {
            int start = peek().offset;
            expect(TokenType.LBRACE, "expected '{'");
            // Empty table?
            if (match(TokenType.RBRACE)) {
                return new LuauType.Table(List.of(), null, null, null);
            }
            // Distinguish array form `{ T }` from record form `{ name: T, … }` and indexer `{ [K]: V }`.
            // Array form has exactly one type and no `:` separator.
            // Try to detect record/indexer form by lookahead: NAME ':' or '[' '...] ':'.
            if (looksLikeFieldOrIndexer()) {
                return parseRecordTable(start);
            }
            // Array form
            var element = parseType();
            expect(TokenType.RBRACE, "expected '}' to close array table type");
            return new LuauType.Table(List.of(), element, null, null);
        }

        boolean looksLikeFieldOrIndexer() {
            // We're positioned just after `{` and before the first element.
            // Field form starts with NAME (or `read`/`write`) ':', possibly with NAME '?' ':'.
            // Indexer form starts with `[`.
            if (check(TokenType.LBRACKET)) return true;
            if (check(TokenType.K_READ) || check(TokenType.K_WRITE)) return true;
            // Look at NAME possibly followed by ?, then `:`
            if (!check(TokenType.NAME)) return false;
            int save = idx;
            try {
                idx++; // consume NAME
                if (check(TokenType.QUESTION)) idx++;
                return check(TokenType.COLON);
            } finally {
                idx = save;
            }
        }

        LuauType parseRecordTable(int startOffset) {
            var fields = new ArrayList<LuauType.TableField>();
            LuauType indexerKey = null;
            LuauType indexerValue = null;
            while (!check(TokenType.RBRACE)) {
                if (check(TokenType.K_READ) || check(TokenType.K_WRITE)) {
                    throw new LuauParseException(peek().offset,
                        "'read'/'write' table-field modifiers are not supported in v1");
                }
                if (match(TokenType.LBRACKET)) {
                    var k = parseType();
                    expect(TokenType.RBRACKET, "expected ']' to close indexer key");
                    expect(TokenType.COLON, "expected ':' after indexer key");
                    var v = parseType();
                    if (indexerKey != null) {
                        throw new LuauParseException(peek().offset, "table may declare at most one indexer");
                    }
                    indexerKey = k;
                    indexerValue = v;
                } else if (check(TokenType.NAME)) {
                    var nameTok = expect(TokenType.NAME, "expected field name");
                    if (match(TokenType.QUESTION)) {
                        // Optional field — model as union with nil
                        expect(TokenType.COLON, "expected ':' after field name");
                        var fieldType = parseType();
                        fields.add(new LuauType.TableField(nameTok.text,
                            new LuauType.Optional(fieldType)));
                    } else {
                        expect(TokenType.COLON, "expected ':' after field name");
                        fields.add(new LuauType.TableField(nameTok.text, parseType()));
                    }
                } else {
                    throw new LuauParseException(peek().offset,
                        "expected field name or '[…]' indexer (got '" + peek().text + "')");
                }
                if (!match(TokenType.COMMA) && !match(TokenType.SEMI)) break;
            }
            expect(TokenType.RBRACE, "expected '}' to close table type");
            return new LuauType.Table(List.copyOf(fields), null, indexerKey, indexerValue);
        }

        LuauType parseFunction() {
            // Optional generic prefix
            if (check(TokenType.LT)) {
                // Skip generics for now — they're declared via @luaGeneric, not inline.
                // A robust implementation would parse and attach to the Function node.
                throw new LuauParseException(peek().offset,
                    "inline generic prefix on function types is not supported in v1; declare generics with @luaGeneric");
            }
            int startOffset = peek().offset;
            expect(TokenType.LPAREN, "expected '('");
            var params = new ArrayList<LuauType.Param>();
            LuauType.Variadic varargs = null;
            if (!check(TokenType.RPAREN)) {
                while (true) {
                    if (check(TokenType.DOT_DOT_DOT)) {
                        idx++;
                        var element = parseType();
                        varargs = new LuauType.Variadic(element);
                        break;
                    }
                    // Named param vs unnamed: NAME ':' lookahead, or NAME '?' ':'.
                    String paramName = null;
                    if (check(TokenType.NAME)) {
                        int save = idx;
                        idx++;
                        boolean optional = match(TokenType.QUESTION);
                        if (check(TokenType.COLON)) {
                            paramName = tokens.get(save).text;
                            idx++; // consume COLON
                            var pt = parseType();
                            if (optional) pt = new LuauType.Optional(pt);
                            params.add(new LuauType.Param(paramName, pt));
                        } else {
                            idx = save;
                            params.add(new LuauType.Param(null, parseType()));
                        }
                    } else {
                        params.add(new LuauType.Param(null, parseType()));
                    }
                    if (!match(TokenType.COMMA)) break;
                }
            }
            expect(TokenType.RPAREN, "expected ')' to close function param list");
            expect(TokenType.ARROW, "expected '->' in function type");
            var returns = parseReturnType();
            return new LuauType.Function(List.copyOf(params), varargs, returns);
        }

        List<LuauType> parseReturnType() {
            // Either a single type or a parenthesized type list.
            if (check(TokenType.LPAREN)) {
                // Could be a multi-return `(A, B)` OR a function-returning-a-function `(P) -> R`.
                // Disambiguate via lookahead for `->` after the closing `)`.
                int save = idx;
                if (looksLikeFunctionType()) {
                    // It's another function type — parse as a single type.
                    return List.of(parseType());
                }
                idx++; // consume LPAREN
                var out = new ArrayList<LuauType>();
                if (!check(TokenType.RPAREN)) {
                    while (true) {
                        out.add(parseType());
                        if (!match(TokenType.COMMA)) break;
                    }
                }
                expect(TokenType.RPAREN, "expected ')' to close return type list");
                return out;
            }
            return List.of(parseType());
        }

        /// Lookahead helper: from a `(`, scan to its matching `)` and check whether the next
        /// token is `->`. Used to distinguish function types from multi-return lists.
        boolean looksLikeFunctionType() {
            if (!check(TokenType.LPAREN)) return false;
            int depth = 0;
            int i = idx;
            while (i < tokens.size()) {
                var t = tokens.get(i);
                if (t.type == TokenType.LPAREN) depth++;
                else if (t.type == TokenType.RPAREN) {
                    depth--;
                    if (depth == 0) {
                        return i + 1 < tokens.size() && tokens.get(i + 1).type == TokenType.ARROW;
                    }
                } else if (t.type == TokenType.EOF) return false;
                i++;
            }
            return false;
        }

        LuauType parseNamed() {
            String moduleName = null;
            String typeName;
            int firstOffset = peek().offset;
            if (check(TokenType.MODULE_REF)) {
                var modTok = peek();
                idx++;
                moduleName = modTok.text;
                expect(TokenType.DOT, "expected '.' after module reference '" + modTok.text + "'");
                var sub = expect(TokenType.NAME, "expected type name after '.'");
                typeName = sub.text;
            } else {
                var first = expect(TokenType.NAME, "expected type name");
                typeName = first.text;
                if (match(TokenType.DOT)) {
                    var sub = expect(TokenType.NAME, "expected type name after '.'");
                    moduleName = first.text;
                    typeName = sub.text;
                }
            }
            // Generic-pack reference form: NAME '...'
            if (match(TokenType.DOT_DOT_DOT)) {
                if (moduleName != null) {
                    throw new LuauParseException(firstOffset,
                        "qualified generic-pack reference is not supported");
                }
                return new LuauType.GenericPack(typeName);
            }
            List<LuauType.TypeArg> args = List.of();
            if (match(TokenType.LT)) {
                args = parseTypeArgList();
                expect(TokenType.GT, "expected '>' to close type argument list");
            }
            return new LuauType.Named(moduleName, typeName, args);
        }

        List<LuauType.TypeArg> parseTypeArgList() {
            var out = new ArrayList<LuauType.TypeArg>();
            if (check(TokenType.GT)) return out;
            while (true) {
                out.add(parseTypeArg());
                if (!match(TokenType.COMMA)) break;
            }
            return out;
        }

        LuauType.TypeArg parseTypeArg() {
            // Pack form: '(' typeList ')' or '...' T or NAME '...'
            if (check(TokenType.LPAREN)) {
                idx++;
                var types = new ArrayList<LuauType>();
                LuauType.Variadic tail = null;
                if (!check(TokenType.RPAREN)) {
                    while (true) {
                        if (check(TokenType.DOT_DOT_DOT)) {
                            idx++;
                            tail = new LuauType.Variadic(parseType());
                            break;
                        }
                        types.add(parseType());
                        if (!match(TokenType.COMMA)) break;
                    }
                }
                expect(TokenType.RPAREN, "expected ')' to close type pack");
                return new LuauType.TypeArg.Pack(List.copyOf(types), tail);
            }
            if (check(TokenType.DOT_DOT_DOT)) {
                idx++;
                var element = parseType();
                return new LuauType.TypeArg.Pack(List.of(),
                    new LuauType.Variadic(element));
            }
            // NAME '...' as a single arg
            if (check(TokenType.NAME) && peek(1).type == TokenType.DOT_DOT_DOT) {
                var nameTok = peek();
                idx++; // NAME
                idx++; // ...
                return new LuauType.TypeArg.Pack(List.of(),
                    new LuauType.Variadic(new LuauType.GenericPack(nameTok.text)));
            }
            return new LuauType.TypeArg.Single(parseType());
        }
    }
}
