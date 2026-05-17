// Custom FTC block definitions (core subset).
// Extend this file to add more SDK blocks (IMU, vision, encoders, etc.).

Blockly.defineBlocksWithJsonArray([
  {
    type: "ftc_gamepad_button",
    message0: "%1 . %2",
    args0: [
      { type: "field_dropdown", name: "PAD", options: [["gamepad1","gamepad1"],["gamepad2","gamepad2"]] },
      { type: "field_dropdown", name: "BTN", options: [
        ["a","a"],["b","b"],["x","x"],["y","y"],
        ["dpad_up","dpad_up"],["dpad_down","dpad_down"],["dpad_left","dpad_left"],["dpad_right","dpad_right"],
        ["left_bumper","left_bumper"],["right_bumper","right_bumper"],
        ["start","start"],["back","back"]
      ]}
    ],
    output: "Boolean",
    colour: 20
  },
  {
    type: "ftc_gamepad_axis",
    message0: "%1 . %2",
    args0: [
      { type: "field_dropdown", name: "PAD", options: [["gamepad1","gamepad1"],["gamepad2","gamepad2"]] },
      { type: "field_dropdown", name: "AXIS", options: [
        ["left_stick_x","left_stick_x"],["left_stick_y","left_stick_y"],
        ["right_stick_x","right_stick_x"],["right_stick_y","right_stick_y"],
        ["left_trigger","left_trigger"],["right_trigger","right_trigger"]
      ]}
    ],
    output: "Number",
    colour: 20
  },
  {
    type: "ftc_motor_setpower",
    message0: "set motor %1 power to %2",
    args0: [
      { type: "field_input", name: "NAME", text: "leftDrive" },
      { type: "input_value", name: "POWER", check: "Number" }
    ],
    previousStatement: null, nextStatement: null, colour: 60
  },
  {
    type: "ftc_servo_setposition",
    message0: "set servo %1 position to %2",
    args0: [
      { type: "field_input", name: "NAME", text: "claw" },
      { type: "input_value", name: "POS", check: "Number" }
    ],
    previousStatement: null, nextStatement: null, colour: 80
  },
  {
    type: "ftc_telemetry_adddata",
    message0: "telemetry addData caption %1 value %2",
    args0: [
      { type: "input_value", name: "CAPTION", check: "String" },
      { type: "input_value", name: "VALUE" }
    ],
    previousStatement: null, nextStatement: null, colour: 290
  },
  {
    type: "ftc_telemetry_update",
    message0: "telemetry update",
    previousStatement: null, nextStatement: null, colour: 290
  },
  {
    type: "ftc_opmode_waitforstart",
    message0: "waitForStart",
    previousStatement: null, nextStatement: null, colour: 0
  },
  {
    type: "ftc_opmode_opmodeisactive",
    message0: "opModeIsActive",
    output: "Boolean", colour: 0
  },

  // --- Motor extras ---
  {
    type: "ftc_motor_setmode",
    message0: "set motor %1 mode to %2",
    args0: [
      { type: "field_input", name: "NAME", text: "leftDrive" },
      { type: "field_dropdown", name: "MODE", options: [
        ["RUN_USING_ENCODER","RUN_USING_ENCODER"],
        ["RUN_WITHOUT_ENCODER","RUN_WITHOUT_ENCODER"],
        ["STOP_AND_RESET_ENCODER","STOP_AND_RESET_ENCODER"],
        ["RUN_TO_POSITION","RUN_TO_POSITION"]
      ]}
    ],
    previousStatement: null, nextStatement: null, colour: 60
  },
  {
    type: "ftc_motor_setdirection",
    message0: "set motor %1 direction %2",
    args0: [
      { type: "field_input", name: "NAME", text: "leftDrive" },
      { type: "field_dropdown", name: "DIR", options: [["FORWARD","FORWARD"],["REVERSE","REVERSE"]] }
    ],
    previousStatement: null, nextStatement: null, colour: 60
  },
  {
    type: "ftc_motor_setzeropower",
    message0: "set motor %1 zero-power %2",
    args0: [
      { type: "field_input", name: "NAME", text: "leftDrive" },
      { type: "field_dropdown", name: "BEH", options: [["BRAKE","BRAKE"],["FLOAT","FLOAT"]] }
    ],
    previousStatement: null, nextStatement: null, colour: 60
  },
  {
    type: "ftc_motor_settarget",
    message0: "set motor %1 target position %2",
    args0: [
      { type: "field_input", name: "NAME", text: "lift" },
      { type: "input_value", name: "POS", check: "Number" }
    ],
    previousStatement: null, nextStatement: null, colour: 60
  },
  {
    type: "ftc_motor_getposition",
    message0: "motor %1 current position",
    args0: [{ type: "field_input", name: "NAME", text: "lift" }],
    output: "Number", colour: 60
  },
  {
    type: "ftc_motor_isbusy",
    message0: "motor %1 is busy",
    args0: [{ type: "field_input", name: "NAME", text: "lift" }],
    output: "Boolean", colour: 60
  },

  // --- CRServo ---
  {
    type: "ftc_crservo_setpower",
    message0: "set CRServo %1 power to %2",
    args0: [
      { type: "field_input", name: "NAME", text: "intake" },
      { type: "input_value", name: "POWER", check: "Number" }
    ],
    previousStatement: null, nextStatement: null, colour: 80
  },

  // --- IMU ---
  {
    type: "ftc_imu_yaw",
    message0: "IMU %1 yaw (deg)",
    args0: [{ type: "field_input", name: "NAME", text: "imu" }],
    output: "Number", colour: 200
  },
  {
    type: "ftc_imu_resetyaw",
    message0: "IMU %1 reset yaw",
    args0: [{ type: "field_input", name: "NAME", text: "imu" }],
    previousStatement: null, nextStatement: null, colour: 200
  },

  // --- Sensors ---
  {
    type: "ftc_color_channel",
    message0: "color sensor %1 %2",
    args0: [
      { type: "field_input", name: "NAME", text: "color" },
      { type: "field_dropdown", name: "CH",
        options: [["red","red"],["green","green"],["blue","blue"],["alpha","alpha"]] }
    ],
    output: "Number", colour: 200
  },
  {
    type: "ftc_distance_cm",
    message0: "distance sensor %1 (cm)",
    args0: [{ type: "field_input", name: "NAME", text: "distance" }],
    output: "Number", colour: 200
  },
  {
    type: "ftc_touch_pressed",
    message0: "touch sensor %1 pressed",
    args0: [{ type: "field_input", name: "NAME", text: "touch" }],
    output: "Boolean", colour: 200
  },

  // --- Utility ---
  {
    type: "ftc_sleep",
    message0: "sleep %1 ms",
    args0: [{ type: "input_value", name: "MS", check: "Number" }],
    previousStatement: null, nextStatement: null, colour: 0
  },
  {
    type: "ftc_timer_new",
    message0: "new ElapsedTime",
    output: null, colour: 200
  },
  {
    type: "ftc_timer_reset",
    message0: "timer %1 reset",
    args0: [{ type: "field_input", name: "NAME", text: "runtime" }],
    previousStatement: null, nextStatement: null, colour: 200
  },
  {
    type: "ftc_timer_seconds",
    message0: "timer %1 seconds",
    args0: [{ type: "field_input", name: "NAME", text: "runtime" }],
    output: "Number", colour: 200
  },
  {
    type: "math_single",
    message0: "%1 %2",
    args0: [
      { type: "field_dropdown", name: "OP", options: [
        ["abs","ABS"],["sqrt","ROOT"],["-","NEG"],
        ["sin°","SIN"],["cos°","COS"],["tan°","TAN"]
      ]},
      { type: "input_value", name: "NUM", check: "Number" }
    ],
    output: "Number", colour: 230
  },
  {
    type: "math_minmax",
    message0: "%1 ( %2 , %3 )",
    args0: [
      { type: "field_dropdown", name: "OP", options: [["min","MIN"],["max","MAX"]] },
      { type: "input_value", name: "A", check: "Number" },
      { type: "input_value", name: "B", check: "Number" }
    ],
    output: "Number", colour: 230
  },
  {
    type: "math_modulo",
    message0: "%1 mod %2",
    args0: [
      { type: "input_value", name: "A", check: "Number" },
      { type: "input_value", name: "B", check: "Number" }
    ],
    output: "Number", colour: 230
  },
  {
    type: "math_constant_pi",
    message0: "π",
    output: "Number", colour: 230
  },
  {
    type: "math_random",
    message0: "random",
    output: "Number", colour: 230
  },
  {
    type: "text_join2",
    message0: "%1 + %2",
    args0: [
      { type: "input_value", name: "A" },
      { type: "input_value", name: "B" }
    ],
    output: "String", colour: 160
  },
  {
    type: "controls_flow_statements",
    message0: "%1",
    args0: [{ type: "field_dropdown", name: "FLOW",
              options: [["break","BREAK"],["continue","CONTINUE"]] }],
    previousStatement: null, colour: 120
  }
]);
