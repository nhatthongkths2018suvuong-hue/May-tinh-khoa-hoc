package vn.khoahoc.maytinh;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Máy tính khoa học tối giản: một Activity, không thư viện ngoài, không quyền mạng,
 * không animation. Giao diện dựng bằng code nên không cần file layout.
 */
public class MainActivity extends Activity {

    // Loại phím
    private static final int OP = 0, NUM = 1, FN = 2, POST = 3, ACT = 4;
    // Kiểu màu
    private static final int S_NUM = 0, S_OP = 1, S_FN = 2, S_CLR = 3, S_EQ = 4;

    private static final String OPS = "+−×÷^";

    private static final class Key {
        final String label, invLabel, text, invText;
        final int kind, style;
        Key(String label, String invLabel, String text, String invText, int kind, int style) {
            this.label = label; this.invLabel = invLabel;
            this.text = text; this.invText = invText;
            this.kind = kind; this.style = style;
        }
    }

    private static Key k(String l, String il, String t, String it, int kind, int style) {
        return new Key(l, il, t, it, kind, style);
    }

    private static Key n(String t) { return k(t, null, t, null, NUM, S_NUM); }

    private static final Key[][] ROWS = {
        { k("INV", null, "INV", null, ACT, S_FN),
          k("DEG", null, "DEG", null, ACT, S_FN),
          k("x!", null, "!", null, POST, S_FN),
          k("Ans", null, "Ans", null, NUM, S_FN),
          k("⌫", null, "DEL", null, ACT, S_CLR) },
        { k("sin", "sin⁻¹", "sin(", "asin(", FN, S_FN),
          k("cos", "cos⁻¹", "cos(", "acos(", FN, S_FN),
          k("tan", "tan⁻¹", "tan(", "atan(", FN, S_FN),
          k("ln", "eˣ", "ln(", "e^(", FN, S_FN),
          k("log", "10ˣ", "log(", "10^(", FN, S_FN) },
        { k("√", "∛", "√(", "∛(", FN, S_FN),
          k("x²", "x³", "²", "³", POST, S_FN),
          k("xʸ", null, "^", null, OP, S_FN),
          k("(", null, "(", null, NUM, S_FN),
          k(")", null, ")", null, NUM, S_FN) },
        { n("7"), n("8"), n("9"), k("÷", null, "÷", null, OP, S_OP), k("AC", null, "AC", null, ACT, S_CLR) },
        { n("4"), n("5"), n("6"), k("×", null, "×", null, OP, S_OP), k("π", null, "π", null, NUM, S_FN) },
        { n("1"), n("2"), n("3"), k("−", null, "−", null, OP, S_OP), k("e", null, "e", null, NUM, S_FN) },
        { n("0"), n("."), k("%", null, "%", null, POST, S_OP), k("+", null, "+", null, OP, S_OP),
          k("=", null, "=", null, ACT, S_EQ) },
    };

    private final StringBuilder expr = new StringBuilder();
    private boolean inv = false, deg = true, justEval = false;
    private double ans = 0;

    private TextView exprView, resultView, invKey, degKey;
    private ScrollView exprScroll;
    private final List<TextView> keyViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF121212);
        int pad = dp(8);
        root.setPadding(pad, pad, pad, pad);

        // --- Màn hình ---
        LinearLayout display = new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setPadding(dp(8), dp(4), dp(8), dp(4));
        root.addView(display, new LinearLayout.LayoutParams(-1, 0, 2.2f));

        exprScroll = new ScrollView(this);
        exprScroll.setVerticalScrollBarEnabled(false);
        exprView = new TextView(this);
        exprView.setTextColor(Color.WHITE);
        exprView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        exprView.setGravity(Gravity.END | Gravity.BOTTOM);
        exprScroll.addView(exprView, new ScrollView.LayoutParams(-1, -2));
        display.addView(exprScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        resultView = new TextView(this);
        resultView.setTextColor(0xFF9E9E9E);
        resultView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        resultView.setGravity(Gravity.END);
        resultView.setSingleLine(true);
        resultView.setMinHeight(dp(34));
        display.addView(resultView, new LinearLayout.LayoutParams(-1, -2));

        // --- Bàn phím ---
        LinearLayout pads = new LinearLayout(this);
        pads.setOrientation(LinearLayout.VERTICAL);
        root.addView(pads, new LinearLayout.LayoutParams(-1, 0, 6f));

        for (Key[] row : ROWS) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            pads.addView(line, new LinearLayout.LayoutParams(-1, 0, 1f));
            for (Key key : row) {
                TextView v = makeKey(key);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
                lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                line.addView(v, lp);
                keyViews.add(v);
                if (key.text.equals("INV")) invKey = v;
                if (key.text.equals("DEG")) degKey = v;
            }
        }

        setContentView(root);
        refreshKeys();
        refreshDisplay();
    }

    // ---------- Dựng phím ----------

    private TextView makeKey(final Key key) {
        TextView v = new TextView(this);
        v.setTag(key);
        v.setGravity(Gravity.CENTER);
        v.setClickable(true);
        v.setSoundEffectsEnabled(false);
        v.setHapticFeedbackEnabled(false);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, key.style == S_NUM ? 24 : 18);
        int bg, pressed, fg;
        switch (key.style) {
            case S_OP:  bg = 0xFF3B3B3B; pressed = 0xFF555555; fg = 0xFFFFB74D; break;
            case S_FN:  bg = 0xFF1F2A33; pressed = 0xFF31424F; fg = 0xFF90CAF9; break;
            case S_CLR: bg = 0xFF3A2424; pressed = 0xFF5A3535; fg = 0xFFEF9A9A; break;
            case S_EQ:  bg = 0xFFFF9800; pressed = 0xFFFFB74D; fg = Color.BLACK; break;
            default:    bg = 0xFF2B2B2B; pressed = 0xFF444444; fg = Color.WHITE; break;
        }
        v.setTextColor(fg);
        v.setBackground(background(bg, pressed));
        v.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onKey(key); }
        });
        return v;
    }

    private StateListDrawable background(int normal, int pressed) {
        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_pressed}, round(pressed));
        sl.addState(new int[]{}, round(normal));
        return sl;
    }

    private GradientDrawable round(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(12));
        return g;
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }

    // ---------- Xử lý phím ----------

    private void onKey(Key key) {
        String text = inv && key.invText != null ? key.invText : key.text;
        switch (key.kind) {
            case ACT:
                handleAction(text);
                return;
            case OP:
                insertOp(text);
                break;
            case POST:
                insertPostfix(text);
                break;
            default:
                insertPlain(text);
                break;
        }
        if (inv && key.invText != null) { inv = false; refreshKeys(); }
        refreshDisplay();
    }

    private void handleAction(String t) {
        switch (t) {
            case "INV": inv = !inv; refreshKeys(); break;
            case "DEG": deg = !deg; refreshKeys(); break;
            case "AC": expr.setLength(0); justEval = false; break;
            case "DEL": delete(); break;
            case "=": evaluate(); return;
            default: break;
        }
        refreshDisplay();
    }

    private void delete() {
        justEval = false;
        int len = expr.length();
        if (len == 0) return;
        if (expr.toString().endsWith("Ans")) { expr.setLength(len - 3); return; }
        char removed = expr.charAt(len - 1);
        expr.setLength(len - 1);
        if (removed == '(') { // xóa luôn tên hàm đứng trước dấu "("
            while (expr.length() > 0) {
                char c = expr.charAt(expr.length() - 1);
                if (Character.isLetter(c) || c == '√' || c == '∛') expr.setLength(expr.length() - 1);
                else break;
            }
        }
    }

    private void insertOp(String op) {
        if (justEval) justEval = false; // tiếp tục tính với kết quả cũ
        if (expr.length() == 0) {
            if (op.equals("−")) { expr.append(op); return; }
            expr.append("Ans");
        }
        char last = expr.charAt(expr.length() - 1);
        // cho phép dấu âm sau × ÷ ^ ( : 2×−3, 2^−3
        if (op.equals("−") && (last == '×' || last == '÷' || last == '^' || last == '(')) {
            expr.append(op);
            return;
        }
        if (last == '(' ) return; // không có toán hạng
        while (expr.length() > 0 && OPS.indexOf(expr.charAt(expr.length() - 1)) >= 0) {
            expr.setLength(expr.length() - 1);
        }
        if (expr.length() == 0) { expr.append("Ans"); }
        expr.append(op);
    }

    private void insertPostfix(String p) {
        if (expr.length() == 0) return;
        justEval = false;
        char last = expr.charAt(expr.length() - 1);
        boolean ok = Character.isDigit(last) || last == ')' || last == 'π'
                || Character.isLetter(last) || last == '!' || last == '%'
                || last == '²' || last == '³';
        if (ok) expr.append(p);
    }

    private void insertPlain(String t) {
        if (justEval) { expr.setLength(0); justEval = false; }
        char first = t.charAt(0);
        if (expr.length() > 0) {
            char last = expr.charAt(expr.length() - 1);
            // tránh dính chữ: "e" + "sin(" -> "e×sin("
            if ((Character.isLetter(last) || last == 'π') && Character.isLetter(first)) expr.append('×');
        }
        if (first == '.') {
            // mỗi số chỉ có một dấu chấm
            int i = expr.length() - 1;
            while (i >= 0 && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                if (expr.charAt(i) == '.') return;
                i--;
            }
        }
        expr.append(t);
    }

    private void evaluate() {
        if (expr.length() == 0) return;
        try {
            double v = Evaluator.eval(expr.toString(), deg, ans);
            ans = v;
            expr.setLength(0);
            expr.append(Evaluator.formatForExpr(v));
            justEval = true;
            resultView.setText("");
            exprView.setText(expr);
            scrollDown();
        } catch (Evaluator.MathError e) {
            resultView.setText("Lỗi");
        }
    }

    // ---------- Hiển thị ----------

    private void refreshKeys() {
        for (TextView v : keyViews) {
            Key key = (Key) v.getTag();
            if (key.text.equals("DEG")) v.setText(deg ? "DEG" : "RAD");
            else v.setText(inv && key.invLabel != null ? key.invLabel : key.label);
        }
        if (invKey != null) {
            invKey.setBackground(inv ? background(0xFF1565C0, 0xFF1E88E5)
                                     : background(0xFF1F2A33, 0xFF31424F));
        }
    }

    private void refreshDisplay() {
        exprView.setText(expr);
        scrollDown();
        if (expr.length() == 0 || justEval) { resultView.setText(""); return; }
        try {
            double v = Evaluator.eval(expr.toString(), deg, ans);
            String f = Evaluator.format(v);
            resultView.setText(f.equals(expr.toString()) ? "" : "= " + f);
        } catch (Evaluator.MathError e) {
            resultView.setText(""); // biểu thức chưa hoàn chỉnh: im lặng
        }
    }

    private void scrollDown() {
        exprScroll.post(new Runnable() {
            @Override public void run() { exprScroll.fullScroll(View.FOCUS_DOWN); }
        });
    }
}
