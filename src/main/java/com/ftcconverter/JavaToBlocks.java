package com.ftcconverter;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Walks a Java OpMode and emits Blockly XML for the core subset.
 * Anything we don't recognize becomes an FTC raw-Java comment block (text) so we
 * never silently drop code.
 */
public class JavaToBlocks {

    public static String convert(String javaSrc) {
        CompilationUnit cu = StaticJavaParser.parse(javaSrc);
        BlockStmt body = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .flatMap(c -> c.getMethods().stream())
                .filter(m -> m.getNameAsString().equals("runOpMode")
                          || m.getNameAsString().equals("loop"))
                .findFirst()
                .flatMap(MethodDeclaration::getBody)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Couldn't find runOpMode() or loop() method"));

        StringBuilder xml = new StringBuilder();
        xml.append("<xml xmlns=\"https://developers.google.com/blockly/xml\">");
        String chain = stmtsToXml(body.getStatements());
        if (!chain.isEmpty()) {
            // wrap first block with x/y so it lands in the workspace
            int i = chain.indexOf('>');
            xml.append(chain, 0, i).append(" x=\"20\" y=\"20\"").append(chain.substring(i));
        }
        xml.append("</xml>");
        return xml.toString();
    }

    // ---------- Statements ----------
    private static String stmtsToXml(List<Statement> stmts) {
        List<String> blocks = new ArrayList<>();
        for (Statement s : stmts) {
            String b = stmtToXml(s);
            if (b != null) blocks.add(b);
        }
        return chain(blocks);
    }

    private static String chain(List<String> blocks) {
        if (blocks.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int open = 0;
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) { sb.append("<next>"); open++; }
            sb.append(blocks.get(i));
        }
        for (int i = 0; i < open; i++) sb.append("</next>");
        return sb.toString();
    }

    private static String stmtToXml(Statement s) {
        if (s.isExpressionStmt()) return exprStmtToXml(s.asExpressionStmt().getExpression());
        if (s.isIfStmt())         return ifToXml(s.asIfStmt());
        if (s.isWhileStmt())      return whileToXml(s.asWhileStmt());
        if (s.isForStmt())        return forToXml(s.asForStmt());
        if (s.isBlockStmt())      return stmtsToXml(s.asBlockStmt().getStatements());
        if (s.isBreakStmt())      return flow("BREAK");
        if (s.isContinueStmt())   return flow("CONTINUE");
        return rawComment(s.toString());
    }

    private static String flow(String kind) {
        return "<block type=\"controls_flow_statements\"><field name=\"FLOW\">" + kind + "</field></block>";
    }

    private static String exprStmtToXml(Expression e) {
        if (e.isMethodCallExpr())   return methodCallStmtToXml(e.asMethodCallExpr());
        if (e.isAssignExpr())       return assignToXml(e.asAssignExpr());
        if (e.isVariableDeclarationExpr()) {
            // turn `int x = 5;` into a variable set
            var v = e.asVariableDeclarationExpr().getVariable(0);
            String name = v.getNameAsString();
            String val = v.getInitializer().map(JavaToBlocks::exprToValueXml).orElse(num("0"));
            return varSet(name, val);
        }
        return rawComment(e.toString() + ";");
    }

    private static String assignToXml(AssignExpr a) {
        if (a.getTarget().isNameExpr()) {
            return varSet(a.getTarget().asNameExpr().getNameAsString(),
                          exprToValueXml(a.getValue()));
        }
        return rawComment(a.toString() + ";");
    }

    private static String methodCallStmtToXml(MethodCallExpr m) {
        String scope = m.getScope().map(Node::toString).orElse("");
        String name = m.getNameAsString();
        boolean simpleScope = !scope.isEmpty() && !scope.contains(".");

        if (simpleScope) {
            switch (name) {
                case "setPower":
                    return block("ftc_motor_setpower", field("NAME", scope),
                            value("POWER", exprToValueXml(m.getArgument(0))));
                case "setPosition":
                    return block("ftc_servo_setposition", field("NAME", scope),
                            value("POS", exprToValueXml(m.getArgument(0))));
                case "setTargetPosition":
                    return block("ftc_motor_settarget", field("NAME", scope),
                            value("POS", exprToValueXml(m.getArgument(0))));
                case "setMode": {
                    String mode = enumTail(m.getArgument(0).toString());
                    return block("ftc_motor_setmode", field("NAME", scope), field("MODE", mode));
                }
                case "setDirection": {
                    String dir = enumTail(m.getArgument(0).toString());
                    return block("ftc_motor_setdirection", field("NAME", scope), field("DIR", dir));
                }
                case "setZeroPowerBehavior": {
                    String beh = enumTail(m.getArgument(0).toString());
                    return block("ftc_motor_setzeropower", field("NAME", scope), field("BEH", beh));
                }
                case "resetYaw":
                    return block("ftc_imu_resetyaw", field("NAME", scope));
                case "reset":
                    return block("ftc_timer_reset", field("NAME", scope));
            }
        }
        if (scope.equals("telemetry") && name.equals("addData") && m.getArguments().size() == 2) {
            return block("ftc_telemetry_adddata",
                    value("CAPTION", exprToValueXml(m.getArgument(0))),
                    value("VALUE",   exprToValueXml(m.getArgument(1))));
        }
        if (scope.equals("telemetry") && name.equals("update"))
            return block("ftc_telemetry_update");
        if (scope.isEmpty() && name.equals("waitForStart"))
            return block("ftc_opmode_waitforstart");
        if (scope.isEmpty() && name.equals("sleep") && m.getArguments().size() == 1)
            return block("ftc_sleep", value("MS", exprToValueXml(m.getArgument(0))));
        return rawComment(m.toString() + ";");
    }

    private static String enumTail(String s) {
        int dot = s.lastIndexOf('.');
        return dot >= 0 ? s.substring(dot + 1) : s;
    }

    private static String ifToXml(IfStmt is) {
        // flatten else-if chain
        List<Expression> conds = new ArrayList<>();
        List<Statement> thens = new ArrayList<>();
        Statement elseStmt = null;
        IfStmt cur = is;
        while (true) {
            conds.add(cur.getCondition());
            thens.add(cur.getThenStmt());
            if (cur.getElseStmt().isPresent()) {
                Statement e = cur.getElseStmt().get();
                if (e.isIfStmt()) { cur = e.asIfStmt(); continue; }
                elseStmt = e;
            }
            break;
        }
        StringBuilder mutation = new StringBuilder("<mutation");
        if (conds.size() > 1) mutation.append(" elseif=\"").append(conds.size() - 1).append("\"");
        if (elseStmt != null) mutation.append(" else=\"1\"");
        mutation.append("/>");

        StringBuilder sb = new StringBuilder();
        sb.append("<block type=\"controls_if\">").append(mutation);
        for (int i = 0; i < conds.size(); i++) {
            sb.append(value("IF" + i, exprToValueXml(conds.get(i))));
            sb.append(statement("DO" + i, oneStmt(thens.get(i))));
        }
        if (elseStmt != null) sb.append(statement("ELSE", oneStmt(elseStmt)));
        sb.append("</block>");
        return sb.toString();
    }

    private static String whileToXml(WhileStmt w) {
        return "<block type=\"controls_whileUntil\">"
             + "<field name=\"MODE\">WHILE</field>"
             + value("BOOL", exprToValueXml(w.getCondition()))
             + statement("DO", oneStmt(w.getBody()))
             + "</block>";
    }

    private static String forToXml(ForStmt f) {
        // Best-effort: for (int i = A; i <= B; i++) / i += k
        String var = "i", from = num("0"), to = num("10"), by = num("1");
        if (!f.getInitialization().isEmpty()
                && f.getInitialization().get(0).isVariableDeclarationExpr()) {
            var v = f.getInitialization().get(0).asVariableDeclarationExpr().getVariable(0);
            var = v.getNameAsString();
            from = v.getInitializer().map(JavaToBlocks::exprToValueXml).orElse(num("0"));
        }
        if (f.getCompare().isPresent() && f.getCompare().get().isBinaryExpr()) {
            to = exprToValueXml(f.getCompare().get().asBinaryExpr().getRight());
        }
        if (!f.getUpdate().isEmpty()) {
            Expression u = f.getUpdate().get(0);
            if (u.isAssignExpr() && u.asAssignExpr().getOperator() == AssignExpr.Operator.PLUS) {
                by = exprToValueXml(u.asAssignExpr().getValue());
            }
        }
        return "<block type=\"controls_for\">"
             + "<field name=\"VAR\">" + esc(var) + "</field>"
             + value("FROM", from) + value("TO", to) + value("BY", by)
             + statement("DO", oneStmt(f.getBody()))
             + "</block>";
    }

    private static String oneStmt(Statement s) {
        if (s.isBlockStmt()) return stmtsToXml(s.asBlockStmt().getStatements());
        String b = stmtToXml(s);
        return b == null ? "" : b;
    }

    // ---------- Expressions (value blocks) ----------
    private static String exprToValueXml(Expression e) {
        if (e.isIntegerLiteralExpr() || e.isDoubleLiteralExpr() || e.isLongLiteralExpr())
            return num(e.toString());
        if (e.isUnaryExpr() && e.asUnaryExpr().getOperator() == UnaryExpr.Operator.MINUS
                && e.asUnaryExpr().getExpression().isIntegerLiteralExpr())
            return num(e.toString());
        if (e.isStringLiteralExpr())
            return "<block type=\"text\"><field name=\"TEXT\">"
                    + esc(e.asStringLiteralExpr().asString()) + "</field></block>";
        if (e.isBooleanLiteralExpr())
            return "<block type=\"logic_boolean\"><field name=\"BOOL\">"
                    + (e.asBooleanLiteralExpr().getValue() ? "TRUE" : "FALSE") + "</field></block>";
        if (e.isNameExpr())
            return "<block type=\"variables_get\"><field name=\"VAR\">"
                    + esc(e.asNameExpr().getNameAsString()) + "</field></block>";
        if (e.isFieldAccessExpr()) return fieldAccessToValue(e.asFieldAccessExpr());
        if (e.isBinaryExpr())      return binaryToValue(e.asBinaryExpr());
        if (e.isUnaryExpr() && e.asUnaryExpr().getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT)
            return "<block type=\"logic_negate\">"
                    + value("BOOL", exprToValueXml(e.asUnaryExpr().getExpression()))
                    + "</block>";
        if (e.isEnclosedExpr()) return exprToValueXml(e.asEnclosedExpr().getInner());
        if (e.isMethodCallExpr())   return methodCallValueToXml(e.asMethodCallExpr());
        if (e.isObjectCreationExpr()
                && e.asObjectCreationExpr().getType().getNameAsString().equals("ElapsedTime"))
            return "<block type=\"ftc_timer_new\"/>";
        return rawComment(e.toString());
    }

    private static String methodCallValueToXml(MethodCallExpr m) {
        String name = m.getNameAsString();
        String scope = m.getScope().map(Node::toString).orElse("");
        boolean simpleScope = !scope.isEmpty() && !scope.contains(".");

        if (scope.isEmpty() && name.equals("opModeIsActive"))
            return "<block type=\"ftc_opmode_opmodeisactive\"/>";

        // Math.*
        if (scope.equals("Math")) {
            String op = switch (name) {
                case "abs" -> "ABS"; case "sqrt" -> "ROOT";
                case "sin" -> "SIN"; case "cos" -> "COS"; case "tan" -> "TAN";
                default -> null;
            };
            if (op != null && m.getArguments().size() == 1) {
                Expression arg = m.getArgument(0);
                // Math.sin(Math.toRadians(x)) → unwrap
                if (arg.isMethodCallExpr()
                        && arg.asMethodCallExpr().getNameAsString().equals("toRadians"))
                    arg = arg.asMethodCallExpr().getArgument(0);
                return "<block type=\"math_single\"><field name=\"OP\">" + op + "</field>"
                     + value("NUM", exprToValueXml(arg)) + "</block>";
            }
            if ((name.equals("min") || name.equals("max")) && m.getArguments().size() == 2) {
                return "<block type=\"math_minmax\"><field name=\"OP\">"
                     + name.toUpperCase() + "</field>"
                     + value("A", exprToValueXml(m.getArgument(0)))
                     + value("B", exprToValueXml(m.getArgument(1))) + "</block>";
            }
            if (name.equals("random") && m.getArguments().isEmpty())
                return "<block type=\"math_random\"/>";
        }

        if (simpleScope) {
            switch (name) {
                case "getCurrentPosition":
                    return block("ftc_motor_getposition", field("NAME", scope));
                case "isBusy":
                    return block("ftc_motor_isbusy", field("NAME", scope));
                case "getDistance":
                    return block("ftc_distance_cm", field("NAME", scope));
                case "isPressed":
                    return block("ftc_touch_pressed", field("NAME", scope));
                case "seconds":
                    return block("ftc_timer_seconds", field("NAME", scope));
                case "red": case "green": case "blue": case "alpha":
                    return block("ftc_color_channel", field("NAME", scope), field("CH", name));
            }
        }
        // IMU: imu.getRobotYawPitchRollAngles().getYaw(...)
        if (name.equals("getYaw") && m.getScope().isPresent()
                && m.getScope().get().isMethodCallExpr()) {
            MethodCallExpr inner = m.getScope().get().asMethodCallExpr();
            if (inner.getNameAsString().equals("getRobotYawPitchRollAngles")
                    && inner.getScope().isPresent()) {
                return block("ftc_imu_yaw", field("NAME", inner.getScope().get().toString()));
            }
        }
        return rawComment(m.toString());
    }

    private static String fieldAccessToValue(FieldAccessExpr fa) {
        String scope = fa.getScope().toString();
        String name = fa.getNameAsString();
        if (scope.equals("Math") && name.equals("PI")) return "<block type=\"math_constant_pi\"/>";
        if (scope.equals("gamepad1") || scope.equals("gamepad2")) {
            boolean axis = name.contains("stick") || name.contains("trigger");
            String type = axis ? "ftc_gamepad_axis" : "ftc_gamepad_button";
            String fieldName = axis ? "AXIS" : "BTN";
            return "<block type=\"" + type + "\">"
                 + "<field name=\"PAD\">" + scope + "</field>"
                 + "<field name=\"" + fieldName + "\">" + name + "</field>"
                 + "</block>";
        }
        return rawComment(fa.toString());
    }

    private static String binaryToValue(BinaryExpr b) {
        String op = b.getOperator().asString();
        String type, opField, opName;
        switch (op) {
            case "%":
                return "<block type=\"math_modulo\">"
                     + value("A", exprToValueXml(b.getLeft()))
                     + value("B", exprToValueXml(b.getRight())) + "</block>";
            case "+": case "-": case "*": case "/":
                type = "math_arithmetic";
                opName = switch (op) { case "+" -> "ADD"; case "-" -> "MINUS";
                                       case "*" -> "MULTIPLY"; default -> "DIVIDE"; };
                opField = "OP";
                break;
            case "==": case "!=": case "<": case "<=": case ">": case ">=":
                type = "logic_compare";
                opName = switch (op) { case "==" -> "EQ"; case "!=" -> "NEQ";
                                       case "<" -> "LT"; case "<=" -> "LTE";
                                       case ">" -> "GT"; default -> "GTE"; };
                opField = "OP";
                break;
            case "&&": case "||":
                type = "logic_operation";
                opName = op.equals("&&") ? "AND" : "OR";
                opField = "OP";
                break;
            default:
                return rawComment(b.toString());
        }
        return "<block type=\"" + type + "\">"
             + "<field name=\"" + opField + "\">" + opName + "</field>"
             + value("A", exprToValueXml(b.getLeft()))
             + value("B", exprToValueXml(b.getRight()))
             + "</block>";
    }

    // ---------- helpers ----------
    private static String num(String v) {
        return "<block type=\"math_number\"><field name=\"NUM\">" + v + "</field></block>";
    }
    private static String varSet(String name, String valXml) {
        return "<block type=\"variables_set\">"
             + "<field name=\"VAR\">" + esc(name) + "</field>"
             + value("VALUE", valXml)
             + "</block>";
    }
    private static String block(String type, String... children) {
        StringBuilder sb = new StringBuilder("<block type=\"").append(type).append("\">");
        for (String c : children) sb.append(c);
        return sb.append("</block>").toString();
    }
    private static String field(String name, String value) {
        return "<field name=\"" + name + "\">" + esc(value) + "</field>";
    }
    private static String value(String name, String inner) {
        return "<value name=\"" + name + "\">" + (inner == null ? "" : inner) + "</value>";
    }
    private static String statement(String name, String inner) {
        if (inner == null || inner.isEmpty()) return "";
        return "<statement name=\"" + name + "\">" + inner + "</statement>";
    }
    private static String rawComment(String text) {
        // unrecognized code → text block so nothing is silently dropped
        return "<block type=\"text\"><field name=\"TEXT\">"
                + esc("/* unconverted: " + text.replace("\n", " ") + " */") + "</field></block>";
    }
    private static String esc(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
}
