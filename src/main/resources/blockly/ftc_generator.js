// Minimal Java code generator for the core subset.
// We build our own Generator instead of relying on a stock Java generator.

const javaGenerator = new Blockly.Generator('Java');
const Order = { ATOMIC: 0, NONE: 99 };
javaGenerator.ORDER_ATOMIC = Order.ATOMIC;

javaGenerator.scrub_ = function(block, code, thisOnly) {
  const next = block.nextConnection && block.nextConnection.targetBlock();
  const nextCode = (!thisOnly && next) ? javaGenerator.blockToCode(next) : '';
  return code + nextCode;
};

// --- Built-in blocks ---
javaGenerator.forBlock['math_number'] = b => [String(b.getFieldValue('NUM')), Order.ATOMIC];
javaGenerator.forBlock['text'] = b => [JSON.stringify(b.getFieldValue('TEXT') || ''), Order.ATOMIC];
javaGenerator.forBlock['logic_boolean'] = b => [b.getFieldValue('BOOL') === 'TRUE' ? 'true' : 'false', Order.ATOMIC];
javaGenerator.forBlock['logic_negate'] = b => {
  const v = javaGenerator.valueToCode(b, 'BOOL', Order.NONE) || 'false';
  return ['!(' + v + ')', Order.ATOMIC];
};
javaGenerator.forBlock['logic_compare'] = b => {
  const ops = { EQ:'==', NEQ:'!=', LT:'<', LTE:'<=', GT:'>', GTE:'>=' };
  const a = javaGenerator.valueToCode(b, 'A', Order.NONE) || '0';
  const c = javaGenerator.valueToCode(b, 'B', Order.NONE) || '0';
  return ['(' + a + ' ' + ops[b.getFieldValue('OP')] + ' ' + c + ')', Order.ATOMIC];
};
javaGenerator.forBlock['logic_operation'] = b => {
  const op = b.getFieldValue('OP') === 'AND' ? '&&' : '||';
  const a = javaGenerator.valueToCode(b, 'A', Order.NONE) || 'false';
  const c = javaGenerator.valueToCode(b, 'B', Order.NONE) || 'false';
  return ['(' + a + ' ' + op + ' ' + c + ')', Order.ATOMIC];
};
javaGenerator.forBlock['math_arithmetic'] = b => {
  const ops = { ADD:'+', MINUS:'-', MULTIPLY:'*', DIVIDE:'/' };
  const a = javaGenerator.valueToCode(b, 'A', Order.NONE) || '0';
  const c = javaGenerator.valueToCode(b, 'B', Order.NONE) || '0';
  const op = ops[b.getFieldValue('OP')] || '+';
  if (op) return ['(' + a + ' ' + op + ' ' + c + ')', Order.ATOMIC];
  return ['Math.pow(' + a + ', ' + c + ')', Order.ATOMIC];
};
javaGenerator.forBlock['controls_if'] = b => {
  let n = 0, code = '';
  do {
    const cond = javaGenerator.valueToCode(b, 'IF' + n, Order.NONE) || 'false';
    const body = javaGenerator.statementToCode(b, 'DO' + n);
    code += (n === 0 ? 'if (' : 'else if (') + cond + ') {\n' + body + '}\n';
    n++;
  } while (b.getInput('IF' + n));
  if (b.getInput('ELSE')) code += 'else {\n' + javaGenerator.statementToCode(b, 'ELSE') + '}\n';
  return code;
};
javaGenerator.forBlock['controls_whileUntil'] = b => {
  let cond = javaGenerator.valueToCode(b, 'BOOL', Order.NONE) || 'false';
  if (b.getFieldValue('MODE') === 'UNTIL') cond = '!(' + cond + ')';
  return 'while (' + cond + ') {\n' + javaGenerator.statementToCode(b, 'DO') + '}\n';
};
javaGenerator.forBlock['controls_for'] = b => {
  const v = b.getFieldValue('VAR');
  const from = javaGenerator.valueToCode(b, 'FROM', Order.NONE) || '0';
  const to   = javaGenerator.valueToCode(b, 'TO',   Order.NONE) || '0';
  const by   = javaGenerator.valueToCode(b, 'BY',   Order.NONE) || '1';
  return 'for (int ' + v + ' = ' + from + '; ' + v + ' <= ' + to + '; ' + v + ' += ' + by + ') {\n'
       + javaGenerator.statementToCode(b, 'DO') + '}\n';
};
javaGenerator.forBlock['variables_get'] = b => [b.getField('VAR').getText(), Order.ATOMIC];
javaGenerator.forBlock['variables_set'] = b => {
  const name = b.getField('VAR').getText();
  const val = javaGenerator.valueToCode(b, 'VALUE', Order.NONE) || '0';
  return name + ' = ' + val + ';\n';
};

// --- FTC blocks ---
javaGenerator.forBlock['ftc_gamepad_button'] =
  b => [b.getFieldValue('PAD') + '.' + b.getFieldValue('BTN'), Order.ATOMIC];
javaGenerator.forBlock['ftc_gamepad_axis'] =
  b => [b.getFieldValue('PAD') + '.' + b.getFieldValue('AXIS'), Order.ATOMIC];
javaGenerator.forBlock['ftc_motor_setpower'] = b => {
  const name = b.getFieldValue('NAME');
  const pow = javaGenerator.valueToCode(b, 'POWER', Order.NONE) || '0';
  return name + '.setPower(' + pow + ');\n';
};
javaGenerator.forBlock['ftc_servo_setposition'] = b => {
  const name = b.getFieldValue('NAME');
  const pos = javaGenerator.valueToCode(b, 'POS', Order.NONE) || '0';
  return name + '.setPosition(' + pos + ');\n';
};
javaGenerator.forBlock['ftc_telemetry_adddata'] = b => {
  const cap = javaGenerator.valueToCode(b, 'CAPTION', Order.NONE) || '""';
  const val = javaGenerator.valueToCode(b, 'VALUE', Order.NONE) || '""';
  return 'telemetry.addData(' + cap + ', ' + val + ');\n';
};
javaGenerator.forBlock['ftc_telemetry_update']     = () => 'telemetry.update();\n';
javaGenerator.forBlock['ftc_opmode_waitforstart']  = () => 'waitForStart();\n';
javaGenerator.forBlock['ftc_opmode_opmodeisactive']= () => ['opModeIsActive()', Order.ATOMIC];

// --- Motor extras ---
javaGenerator.forBlock['ftc_motor_setmode'] = b =>
  b.getFieldValue('NAME') + '.setMode(DcMotor.RunMode.' + b.getFieldValue('MODE') + ');\n';
javaGenerator.forBlock['ftc_motor_setdirection'] = b =>
  b.getFieldValue('NAME') + '.setDirection(DcMotorSimple.Direction.' + b.getFieldValue('DIR') + ');\n';
javaGenerator.forBlock['ftc_motor_setzeropower'] = b =>
  b.getFieldValue('NAME') + '.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.' + b.getFieldValue('BEH') + ');\n';
javaGenerator.forBlock['ftc_motor_settarget'] = b => {
  const v = javaGenerator.valueToCode(b, 'POS', Order.NONE) || '0';
  return b.getFieldValue('NAME') + '.setTargetPosition((int)(' + v + '));\n';
};
javaGenerator.forBlock['ftc_motor_getposition'] =
  b => [b.getFieldValue('NAME') + '.getCurrentPosition()', Order.ATOMIC];
javaGenerator.forBlock['ftc_motor_isbusy'] =
  b => [b.getFieldValue('NAME') + '.isBusy()', Order.ATOMIC];

// --- CRServo ---
javaGenerator.forBlock['ftc_crservo_setpower'] = b => {
  const v = javaGenerator.valueToCode(b, 'POWER', Order.NONE) || '0';
  return b.getFieldValue('NAME') + '.setPower(' + v + ');\n';
};

// --- IMU ---
javaGenerator.forBlock['ftc_imu_yaw'] = b => [
  b.getFieldValue('NAME') + '.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES)',
  Order.ATOMIC
];
javaGenerator.forBlock['ftc_imu_resetyaw'] = b => b.getFieldValue('NAME') + '.resetYaw();\n';

// --- Sensors ---
javaGenerator.forBlock['ftc_color_channel'] =
  b => [b.getFieldValue('NAME') + '.' + b.getFieldValue('CH') + '()', Order.ATOMIC];
javaGenerator.forBlock['ftc_distance_cm'] =
  b => [b.getFieldValue('NAME') + '.getDistance(DistanceUnit.CM)', Order.ATOMIC];
javaGenerator.forBlock['ftc_touch_pressed'] =
  b => [b.getFieldValue('NAME') + '.isPressed()', Order.ATOMIC];

// --- Utility ---
javaGenerator.forBlock['ftc_sleep'] = b => {
  const v = javaGenerator.valueToCode(b, 'MS', Order.NONE) || '0';
  return 'sleep((long)(' + v + '));\n';
};
javaGenerator.forBlock['ftc_timer_new'] = () => ['new ElapsedTime()', Order.ATOMIC];
javaGenerator.forBlock['ftc_timer_reset'] = b => b.getFieldValue('NAME') + '.reset();\n';
javaGenerator.forBlock['ftc_timer_seconds'] =
  b => [b.getFieldValue('NAME') + '.seconds()', Order.ATOMIC];

javaGenerator.forBlock['math_single'] = b => {
  const v = javaGenerator.valueToCode(b, 'NUM', Order.NONE) || '0';
  const op = b.getFieldValue('OP');
  const map = {
    ABS:'Math.abs(' + v + ')',
    ROOT:'Math.sqrt(' + v + ')',
    NEG:'(-(' + v + '))',
    SIN:'Math.sin(Math.toRadians(' + v + '))',
    COS:'Math.cos(Math.toRadians(' + v + '))',
    TAN:'Math.tan(Math.toRadians(' + v + '))'
  };
  return [map[op], Order.ATOMIC];
};
javaGenerator.forBlock['math_minmax'] = b => {
  const a = javaGenerator.valueToCode(b, 'A', Order.NONE) || '0';
  const c = javaGenerator.valueToCode(b, 'B', Order.NONE) || '0';
  return ['Math.' + b.getFieldValue('OP').toLowerCase() + '(' + a + ', ' + c + ')', Order.ATOMIC];
};
javaGenerator.forBlock['math_modulo'] = b => {
  const a = javaGenerator.valueToCode(b, 'A', Order.NONE) || '0';
  const c = javaGenerator.valueToCode(b, 'B', Order.NONE) || '1';
  return ['(' + a + ' % ' + c + ')', Order.ATOMIC];
};
javaGenerator.forBlock['math_constant_pi'] = () => ['Math.PI', Order.ATOMIC];
javaGenerator.forBlock['math_random'] = () => ['Math.random()', Order.ATOMIC];
javaGenerator.forBlock['text_join2'] = b => {
  const a = javaGenerator.valueToCode(b, 'A', Order.NONE) || '""';
  const c = javaGenerator.valueToCode(b, 'B', Order.NONE) || '""';
  return ['(' + a + ' + ' + c + ')', Order.ATOMIC];
};
javaGenerator.forBlock['controls_flow_statements'] =
  b => (b.getFieldValue('FLOW') === 'BREAK' ? 'break;\n' : 'continue;\n');

window.javaGenerator = javaGenerator;
