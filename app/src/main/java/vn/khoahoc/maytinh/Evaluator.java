package vn.khoahoc.maytinh;

import java.math.BigDecimal;
import java.math.MathContext;

/** Bộ phân tích & tính biểu thức (đệ quy xuống), không phụ thuộc thư viện ngoài. */
final class Evaluator {

    static final class MathError extends Exception {
        MathError(String m) { super(m); }
    }

    private final String s;
    private final boolean deg;
    private final double ans;
    private int pos = 0;

    private Evaluator(String s, boolean deg, double ans) {
        this.s = s;
        this.deg = deg;
        this.ans = ans;
    }

    static double eval(String expr, boolean deg, double ans) throws MathError {
        Evaluator ev = new Evaluator(closeParens(expr), deg, ans);
        double v = ev.parseExpr();
        if (ev.pos != ev.s.length()) throw new MathError("syntax");
        return check(v);
    }

    /** Tự đóng các dấu ngoặc còn thiếu. */
    static String closeParens(String e) {
        int open = 0;
        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);
            if (c == '(') open++;
            else if (c == ')') open--;
        }
        StringBuilder sb = new StringBuilder(e);
        while (open-- > 0) sb.append(')');
        return sb.toString();
    }

    // ---------- Ngữ pháp ----------
    // expr    := term (('+'|'-') term)*
    // term    := unary (('×'|'÷'|ngầm định) unary)*
    // unary   := ('+'|'-') unary | power
    // power   := postfix ('^' unary)?
    // postfix := primary ('!' | '%' | '²' | '³')*

    private double parseExpr() throws MathError {
        double v = parseTerm();
        while (true) {
            char c = peek();
            if (c == '+') { pos++; v += parseTerm(); }
            else if (c == '-' || c == '−') { pos++; v -= parseTerm(); }
            else return v;
        }
    }

    private double parseTerm() throws MathError {
        double v = parseUnary();
        while (true) {
            char c = peek();
            if (c == '×' || c == '*') { pos++; v *= parseUnary(); }
            else if (c == '÷' || c == '/') {
                pos++;
                double d = parseUnary();
                if (d == 0) throw new MathError("div0");
                v /= d;
            } else if (startsPrimary(c)) {
                v *= parseUnary(); // nhân ngầm: 2π, 3(4), 2sin(x)
            } else return v;
        }
    }

    private double parseUnary() throws MathError {
        char c = peek();
        if (c == '-' || c == '−') { pos++; return -parseUnary(); }
        if (c == '+') { pos++; return parseUnary(); }
        return parsePower();
    }

    private double parsePower() throws MathError {
        double base = parsePostfix();
        if (peek() == '^') {
            pos++;
            double exp = parseUnary();
            return check(Math.pow(base, exp));
        }
        return base;
    }

    private double parsePostfix() throws MathError {
        double v = parsePrimary();
        while (true) {
            char c = peek();
            if (c == '!') { pos++; v = factorial(v); }
            else if (c == '%') { pos++; v = v / 100.0; }
            else if (c == '²') { pos++; v = v * v; }
            else if (c == '³') { pos++; v = v * v * v; }
            else return v;
        }
    }

    private double parsePrimary() throws MathError {
        char c = peek();
        if (c == '(') {
            pos++;
            double v = parseExpr();
            if (peek() != ')') throw new MathError("paren");
            pos++;
            return v;
        }
        if (c == 'π') { pos++; return Math.PI; }
        if (c == '√' || c == '∛') {
            pos++;
            double a = parseArg();
            if (c == '√') {
                if (a < 0) throw new MathError("domain");
                return Math.sqrt(a);
            }
            return Math.cbrt(a);
        }
        if (Character.isLetter(c)) {
            int start = pos;
            while (pos < s.length() && Character.isLetter(s.charAt(pos)) && s.charAt(pos) != 'π') pos++;
            String name = s.substring(start, pos);
            switch (name) {
                case "e": return Math.E;
                case "Ans": return ans;
                default: break;
            }
            return applyFunction(name, parseArg());
        }
        if (Character.isDigit(c) || c == '.') return parseNumber();
        throw new MathError("syntax");
    }

    private double parseArg() throws MathError {
        if (peek() != '(') throw new MathError("syntax");
        pos++;
        double v = parseExpr();
        if (peek() != ')') throw new MathError("paren");
        pos++;
        return v;
    }

    private double parseNumber() throws MathError {
        int start = pos;
        boolean dot = false;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (Character.isDigit(c)) pos++;
            else if (c == '.' && !dot) { dot = true; pos++; }
            else break;
        }
        String t = s.substring(start, pos);
        if (t.equals(".")) throw new MathError("syntax");
        return Double.parseDouble(t);
    }

    private double applyFunction(String name, double a) throws MathError {
        switch (name) {
            case "sin": return clean(Math.sin(toRad(a)));
            case "cos": return clean(Math.cos(toRad(a)));
            case "tan": {
                double r = toRad(a);
                if (Math.abs(Math.cos(r)) < 1e-12) throw new MathError("domain");
                return clean(Math.tan(r));
            }
            case "asin": if (a < -1 || a > 1) throw new MathError("domain"); return fromRad(Math.asin(a));
            case "acos": if (a < -1 || a > 1) throw new MathError("domain"); return fromRad(Math.acos(a));
            case "atan": return fromRad(Math.atan(a));
            case "ln":   if (a <= 0) throw new MathError("domain"); return Math.log(a);
            case "log":  if (a <= 0) throw new MathError("domain"); return Math.log10(a);
            default: throw new MathError("syntax");
        }
    }

    private double toRad(double x) { return deg ? Math.toRadians(x) : x; }
    private double fromRad(double x) { return deg ? Math.toDegrees(x) : x; }

    private static double clean(double v) { return Math.abs(v) < 1e-14 ? 0 : v; }

    private static double factorial(double v) throws MathError {
        if (v < 0 || v != Math.floor(v) || v > 170) throw new MathError("domain");
        double r = 1;
        for (int i = 2; i <= (int) v; i++) r *= i;
        return r;
    }

    private static double check(double v) throws MathError {
        if (Double.isNaN(v) || Double.isInfinite(v)) throw new MathError("domain");
        return v;
    }

    private static boolean startsPrimary(char c) {
        return Character.isDigit(c) || c == '.' || c == '(' || c == 'π'
                || c == '√' || c == '∛' || Character.isLetter(c);
    }

    private char peek() { return pos < s.length() ? s.charAt(pos) : '\0'; }

    // ---------- Định dạng kết quả ----------

    /** Hiển thị kết quả: tối đa 12 chữ số có nghĩa. */
    static String format(double v) {
        if (v == 0) return "0";
        BigDecimal bd = new BigDecimal(v).round(new MathContext(12)).stripTrailingZeros();
        double a = Math.abs(v);
        String t = (a >= 1e12 || a < 1e-6) ? bd.toString().replace("E+", "E") : bd.toPlainString();
        return t.replace('-', '−');
    }

    /** Dạng có thể đọc lại được bởi bộ phân tích (dùng khi bấm "="). */
    static String formatForExpr(double v) {
        return format(v).replace("E-", "×10^−").replace("E", "×10^");
    }
}
