
package com.techietitans.opmodes;


import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;
import com.techietitans.libraries.DataLogger;
import com.techietitans.libraries.HardwareClass_V2;
import com.techietitans.libraries.TTCrypto;

import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;

//---VueForia-Specific-Imports---

@Autonomous(group = "TechieTitans")
//@Disabled
public class AaryanBLUE extends HardwareClass_V2{

    int currentState = 0;
    int previousState = 7;
    boolean isRunning = false;
    boolean isResetRunning = false;
    DataLogger dl;
    private ElapsedTime runtime = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
    Colors allianceColor= Colors.BLUE;
    Colors jewelColor;
    int leftStartPosition;
    int rightStartPosition;
    int startDirection = 0;
    Sides turnDirection = Sides.LEFT;
    Sides UndoturnDirection = Sides.LEFT;
    boolean logEnabled = false;
    long logTime = System.currentTimeMillis();
    boolean colorSensorsDisabled = false;
    int recoveryCount=0;
    int loopCounter =0;
    int shooterInit=0;
    double allianceSpecific;
    int allianceSpecificDistance;
    boolean shortVersion = false;
    boolean jewelEnabled = true;
    int moveDistance = 0;

    //---VueForia-Specific-Variables---
    //TODO:Move these to another class like hardware class so that we dont have to declare in all code.
    private TTCrypto vu;
    private RelicRecoveryVuMark vm;
    int collumn = 0; //0 is left 1 is middle 2 is right default to 0 so that it defaults to left row the row with least margin for error



    // Colors used in Alliance, resQ beacon and line
    public enum Colors {
        RED, BLUE, WHITE, OTHER
    }

    // Sides used in turn, edge of a line
    public enum Sides {
        LEFT, RIGHT, OTHER
    }


    /**
     * Construct the class.
     * The system calls this member when the class is instantiated.
     */
    public AaryanBLUE() {
        // Initialize base classes.
        // All via self-construction.

        // Initialize class members.
        // All via self-construction.
    }

    //*** Autonomous constants

    public static final double TURN_POWER = 0.45;
    public static final double NAV_HIGH_POWER = 0.7;
    public static final double NAV_MID_POWER = 0.4;


    @Override
    public void init() {
        //Get initialization..mainly servos.
        super.init();
        
        //init Servos
        bottom_right_hand.setPosition(GLYPH_BOTTOM_RIGHT_SERVO_OPEN); 
        bottom_left_hand.setPosition(GLYPH_BOTTOM_LEFT_SERVO_OPEN);
        top_right_hand.setPosition(GLYPH_TOP_RIGHT_SERVO_OPEN);
        top_left_hand.setPosition(GLYPH_TOP_LEFT_SERVO_OPEN);
        glyph_rotator.setPosition(GLYPH_ROTATOR_POSITION_A);
        jewel_pusher.setPosition(JEWEL_PUSHER_REST);
        jewel_pusher_arm.setPosition(JEWEL_PUSHER_ARM_REST);
        relicGrabber_base.setPosition(33.0/255.0);
        relicGrabber_claw.setPosition(0.0/255.0);
        
        // Calibrate the gyro.
        gyro.calibrate();
        // Set all drive train motors to run using encoders
        //useEncoders();
        //Turn on LED of the color sensor-Used to detect jewel.
        Color_jewel.enableLed(true);
        //Estabilish vm as "VueForia eyes"
        vu = new TTCrypto(hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName()));
        vu.activateView();//active vue
        isRunning = false;
    }

    //*****************************************************************************
    @Override
    public void init_loop() {
        vm = vu.getViewResult();
        if (vm == RelicRecoveryVuMark.RIGHT)
            collumn = 2;
        else if (vm == RelicRecoveryVuMark.CENTER)
            collumn = 1;
        else
            collumn = 0;

        telemetry.addData("Placing in ",vm);
        //Get Alliance selection
        if (gamepad1.b) {
            // RED Alliance
            allianceColor = Colors.RED;
        } else if (gamepad1.x) {
            //BLUE Alliance
            allianceColor = Colors.BLUE;
        }
        if (gamepad1.left_bumper)   {

            jewelEnabled = false;
        }

        if (gamepad1.right_bumper)  {

            jewelEnabled = true;
        }

        //allianceColor = Colors.RED;
        telemetry.addData("**** ALLIANCE ****  ", allianceColor);

        telemetry.addData("****JEWEL****  ", jewelEnabled);

        if (gamepad1.y) {
            logEnabled = true;
        }
        //logEnabled = true;
        telemetry.addData("**** Log Enabled ****", logEnabled);
        vu.close();
    }

    //*****************************************************************************
    @Override
    public void start() {

        currentState = 0;
        //TODO: Change for 2017 data points
        if (logEnabled) {
            //Set a new data logger and header of the file
//            dl = new DataLogger("Dl_TT_Auto_V4");
//            dl.addField("LoopCounter");
//            dl.addField("Bottom_G");
//            dl.newLine();
        }
    }

    //*****************************************************************************
    @Override
    public void loop() {

        //*********Start State Machine
        switch (currentState) {
            //Tasks are broken down to finite STATES. We will increment to to next state after successful
            // completion of each state.

            case 0:
                //First state
                currentState++;
                gyro.resetZAxisIntegrator();
                bottom_right_hand.setPosition(GLYPH_BOTTOM_RIGHT_SERVO_CLOSE-0.1);
                bottom_left_hand.setPosition(GLYPH_BOTTOM_LEFT_SERVO_CLOSE+0.1);
                runtime.reset();
                break;
            case 1:
                // Grab the glyph

                if (runtime.milliseconds()>1500) {
                    bottom_right_hand.setPosition(GLYPH_BOTTOM_RIGHT_SERVO_CLOSE);
                    bottom_left_hand.setPosition(GLYPH_BOTTOM_LEFT_SERVO_CLOSE);
                    runtime.reset();
                    currentState++;
                }
                break;
            case 2:
                // Lift the glyph to mid height
                //Going with timer, should change to encoder count
                lift_motor.setPower(0.3);
                if (runtime.milliseconds()>500){
                    lift_motor.setPower(0.0);
                    runtime.reset();
                    //This step is mainly for development.
                    //With this config, we can skip jewel push during development to save time and hardware
                    if (jewelEnabled == true)
                        currentState++;
                    else //TODO:Adjust when needed, if a step is added in jewel pushing.
                        currentState = 7;
                }
                break;
            case 3:
                // Lower the Jewel Arm Servo - AND -
                // Bring out Jewel servo from resting position
                jewel_pusher.setPosition(125.0/256);
                jewel_pusher_arm.setPosition(JEWEL_PUSHER_ARM_ENGAGE);
                if (runtime.milliseconds() > 3000) {
                    currentState++;
                        runtime.reset();

                }
                break;
            case 4:
                // Detect the Jewel color. Pause for 1 sec to get stable reading
                if (Color_jewel.red()>Color_jewel.blue()){
                    jewelColor= Colors.RED;
                }
                else{
                    jewelColor= Colors.BLUE;
                }
                if (runtime.milliseconds() > 1000) {
                    currentState++;
                    runtime.reset();
                }
                break;
            case 5:
                // remove the jewel
                //if color sensor and alliance color is same the move right, else left
                // (when color sensor is pointed at right side of the arm)
                if (jewelColor == allianceColor){
                    jewel_pusher.setPosition(60.0/256);
                }
                else{
                    jewel_pusher.setPosition(200.0/256);
                }

                if (runtime.milliseconds() > 2000) {
                    currentState++;
                    runtime.reset();
                }
                break;
            case 6:
                // Bring back the jewel servo
                jewel_pusher_arm.setPosition(JEWEL_PUSHER_ARM_REST);
                jewel_pusher.setPosition(JEWEL_PUSHER_REST);
                //Turn off LED of the color sensor Used to detect jewel.
                Color_jewel.enableLed(false);
                if (runtime.milliseconds()>2000){
                    currentState++;
                }
                break;
            //************END of Jewel Push***********************
            case 7:
                // Come down and move towards glyph drop zone
                //TODO: Adjust alliance specific parameters

                allianceSpecific = (allianceColor == Colors.RED) ? -0.15 + 0.052 : 0.15 - 0.052;
                //allianceSpecific if negative will move backwards
                //if positive will move forwards

                allianceSpecificDistance = (allianceColor == Colors.RED) ? 1000 : 1000;

                if (collumn ==0) { //LEFT
                    allianceSpecificDistance = allianceSpecificDistance+480;
                }else if (collumn ==1 ) {  //CENTER
                    allianceSpecificDistance = allianceSpecificDistance+637;
                }else {
                    allianceSpecificDistance = allianceSpecificDistance+505;
                }

                if (driveWithEncodersV2(allianceSpecific, allianceSpecific, allianceSpecificDistance, allianceSpecificDistance,5000)) {
                    currentState++;
                }
                break;
            case 8:
                int angle = 0;

                if (collumn ==0 ) { //LEFT
                    angle = 105;
                }else if (collumn ==1 ) { //CENTER
                    angle = 95;
                }else {
                    angle = 60;
                }

                if (gyroPointTurn(.2, Sides.RIGHT, angle)) {
                    currentState++;
                    runtime.reset();
                }
                telemetry.addData("**Completed turning **","");
                break;
            case 9:
                // Lower glyph -- Not sure if we need it
                // Lift the glyph to mid height

                lift_motor.setPower(-0.3);
                if (runtime.milliseconds() > 195) {
                    lift_motor.setPower(0.0);
                    currentState++;
                    runtime.reset();
                }
                break;

            case 10:
                // Release glyph

                bottom_right_hand.setPosition(GLYPH_BOTTOM_RIGHT_SERVO_OPEN);
                bottom_left_hand.setPosition(GLYPH_BOTTOM_LEFT_SERVO_OPEN);
                if (runtime.milliseconds() > 300) {
                    currentState++;
                }
                break;

            case 11:
                // moving back to allow time for lift motor to go down/glyph release

                if ((driveWithEncodersV2(-0.1, -0.1, 100, 100,5000)) || (runtime.milliseconds() > 5000)) {
                    currentState++;
                    runtime.reset();
                }
//                bottomRightGlyphHolder.setPosition(GLYPH_BOTTOM_RIGHT_SERVO_PUSH);
//                bottomLeftGlyphHolder.setPosition(GLYPH_BOTTOM_LEFT_SERVO_PUSH);

                break;
            case 12:
                // Move to the drop zone

                if(collumn == 0){ //LEFT
                    if ((driveWithEncodersV2(0.1, 0.1, 425, 425,5000)) || (runtime.milliseconds() > 5000)) {
                        currentState++;
                        runtime.reset();
                    }
                }else if(collumn ==1 ){ //CENTER
                    if ((driveWithEncodersV2(0.1, 0.1, 330, 330,5000)) || (runtime.milliseconds() > 5000)) {
                        currentState++;
                        runtime.reset();
                    }
                }else{
                    if ((driveWithEncodersV2(0.1, 0.1, 310, 310,5000)) || (runtime.milliseconds() > 5000)) {
                        currentState++;
                        runtime.reset();
                    }
                }

                break;
            case 13:
                telemetry.addData("**CASE 14**", "");
                if (collumn == 0) { //LEFT
                    if (driveWithEncodersV2(-.1, -.1, 750, 750,5000)) {
                        currentState++;
                    }
                } else if (collumn == 1) { //CENTER
                    if (driveWithEncodersV2(-.1, -.1, 700, 700,5000)) {
                        currentState++;
                    }
                } else if (collumn == 2) {
                    if (driveWithEncodersV2(-.1, -.1, 700, 700,5000)) {
                        currentState++;
                    }
                } else {
                    currentState = 99;
                }
                telemetry.addData("**N value when turning**", collumn);
                break;
            case 14:
                // Come back a bit

                if(collumn == 0){ //LEFT
                    if (driveWithEncodersV2(0.1, 0.1, 500, 500,5000)) {
                        currentState++;
                    }
                }else if(collumn == 1){ //CENTER
                    if (driveWithEncodersV2(0.1, 0.1, 400, 400,5000)) {
                        currentState++;
                    }
                }else{
                    if (driveWithEncodersV2(0.1, 0.1, 400, 400,5000)) {
                        currentState++;
                    }
                }
                break;


            case 99:
                // Recovery State. Any known failures will lead the state machine to this state.
                // Display in telemetry and log to the file
                left_front_motor.setPower(0);
                right_front_motor.setPower(0);
                telemetry.addData("**** ROBOT STOPPED at State: ****", previousState);
                break;

            default:
                // The autonomous actions have been accomplished (i.e. the state machine has
                // transitioned into its final state.
                break;
        }

        previousState = currentState;
        loopCounter++;

        telemetry.addData("state: ", currentState);
        telemetry.addData("Jewel : ", jewelColor);
        telemetry.addData("Red : ",Color_jewel.red());
        telemetry.addData("Blue : ",Color_jewel.blue());
        telemetry.addData("Alliance : ",allianceColor);
        telemetry.addData("Placing in ",collumn);


        // Write data to log file..if enabled and log duration has reached
        //TODO: Change for 2017 data points
        if ((logEnabled) && ((System.currentTimeMillis()- logTime)>100)){
//            dl.addField(String.valueOf(loopCounter));
//            dl.addField(String.valueOf(currentState));
//            dl.addField(String.valueOf(left_front_motor.getCurrentPosition()));
//            dl.addField(String.valueOf(left_front_motor.getPower()));
//            dl.addField(String.valueOf(right_front_motor.getCurrentPosition()));
//            dl.addField(String.valueOf(right_front_motor.getPower()));
//            dl.addField(String.valueOf(gyro.getIntegratedZValue()));
//            dl.addField(String.valueOf(ods_front.getLightDetected()));
//            dl.addField(String.valueOf(getLeftBeaconColor()));
//            dl.addField(String.valueOf(mrcolor_front.red()));
//            dl.addField(String.valueOf(mrcolor_front.blue()));
//            dl.addField(String.valueOf(mrcolor_front.green()));
//            dl.addField(String.valueOf(getRightBeaconColor()));
//            dl.addField(String.valueOf(cs.red()));
//            dl.addField(String.valueOf(cs.blue()));
//            dl.addField(String.valueOf(cs.green()));
//            dl.addField(String.valueOf(pusher_left.getPosition()));
//            dl.addField(String.valueOf(pusher_right.getPosition()));
//            dl.addField(String.valueOf(getLineColor()));
//            dl.addField(String.valueOf(mrcolor_under.red()));
//            dl.addField(String.valueOf(mrcolor_under.blue()));
//            dl.addField(String.valueOf(mrcolor_under.green()));
//            dl.addField(String.valueOf(IsPushed));
//            dl.addField(String.valueOf(pushSuccessful()));
//            dl.addField(String.valueOf(recoveryCount));
//            dl.newLine();
            //Reset counter
            logTime = System.currentTimeMillis();
        }
    }

    @Override
    public void stop() {
        //Close data logger and Adafruit
        if (logEnabled){
            dl.closeDataLogger();
        }

    }

     /*
     * ************** Helper Methods*************************
     */

    //driveWithEncoders:
    //==================
    //Drives all 4 wheel to a desired encoder count
    // it works on relative position. so, we don't need to reset encoder
    //
    boolean driveWithEncodersV2
    (double left_power
            , double right_power
            , double left_count
            , double right_count
            , int timeout
    )

    {
        if (!isRunning) {
            //This block should only execute once
            //Set starting position
            leftStartPosition = left_front_motor.getCurrentPosition();
            rightStartPosition = right_front_motor.getCurrentPosition();
            //Set motor speed
            left_front_motor.setPower(left_power);
            right_front_motor.setPower(right_power);
            left_back_motor.setPower(left_power);
            right_back_motor.setPower(right_power);
            isRunning = true;
        }

        //ToDo: add proportional slow down

        //Done - if the target is reached
        if (leftEncoder_reached(left_count) || rightEncoder_reached(right_count) || (runtime.milliseconds()>timeout)) {
            left_front_motor.setPower(0);
            right_front_motor.setPower(0);
            left_back_motor.setPower(0);
            right_back_motor.setPower(0);
            isRunning = false;
            return true;
        }
        return false;
    }


    boolean strafeWithEncoders
            (double left_power
                    , double right_power
                    , double left_count
                    , double right_count
            )

    {
        if (!isRunning) {
            //This block should only execute once
            //Set starting position
            leftStartPosition = left_front_motor.getCurrentPosition();
            rightStartPosition = right_front_motor.getCurrentPosition();
            //Set motor speed
            left_front_motor.setPower(left_power);
            right_front_motor.setPower(right_power);
            left_back_motor.setPower(left_power);
            right_back_motor.setPower(right_power);
            isRunning = true;
        }

        //ToDo: add proportional slow down

        //Done - if the target is reached
        if (leftEncoder_reached(left_count) || rightEncoder_reached(right_count)) {
            left_front_motor.setPower(0);
            right_front_motor.setPower(0);
            left_back_motor.setPower(0);
            right_back_motor.setPower(0);
            isRunning = false;
            return true;
        }
        return false;
    }

    //gyroPointTurn:
    //================
    //
    boolean gyroPointTurn(double power
            , Sides turnDirection
            , int angle
    ) {
        int progress;
        int error;
        double correction;

        if (!isRunning) {
            //This block should only execute once
            //Set starting position
            startDirection = gyro.getIntegratedZValue();
            isRunning = true;
        }

        //ToDo: add proportional slow down. This is a bit tricky
        // Power = Power*Error*P
        // IntegratedZ value behaves much like Motor encoders. It keeps increasing
        // Or decreasing from initial calibration point based on direction.
        // Progress = Abs(Current position- start position)
        // Error = Target - Progress
        // So, Target will be reached as soon as Error is below threshold

        progress = Math.abs(gyro.getIntegratedZValue() - startDirection);
        error = angle-progress;
        correction = Range.clip(error*0.1, 0,1); // P coefficient = .1
        power = power*correction;

        if (turnDirection == Sides.LEFT) {
            left_front_motor.setPower(power);
            left_back_motor.setPower(power);
            right_front_motor.setPower(-power);
            right_back_motor.setPower(-power);
        }
        if (turnDirection == Sides.RIGHT) {
            left_front_motor.setPower(-power);
            left_back_motor.setPower(-power);
            right_front_motor.setPower(power);
            right_back_motor.setPower(power);
        }
        // Target is reached if error is within threshold.. (2 degrees)
        if (error<=2) {
            left_front_motor.setPower(0);
            right_front_motor.setPower(0);
            left_back_motor.setPower(0);
            right_back_motor.setPower(0);
            isRunning = false;
            return true;
        }
        return false;
    }



    //Set both drive wheel encoder to run, if the mode is appropriate.
    public void useEncoders() {
        // perform the action on both motors.
        if (left_front_motor != null) {
            left_front_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        if (right_front_motor != null) {
            right_front_motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
    }

    //Reset both drive wheel encoders.

    public void resetEncoders() {

        left_front_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        right_front_motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
    }

    //Completion of Reset - both drive wheel encoders.

    public boolean resetComplete() {
        // Assume failure.
        boolean l_return = false;
        int pos = 0;
        if (left_front_motor != null) {
            pos = +left_front_motor.getCurrentPosition();
        }
        if (right_front_motor != null) {
            pos = +right_front_motor.getCurrentPosition();
        }

        if (pos == 0) {
            l_return = true;
            isResetRunning = false;
        }
        return l_return;
    }

    public boolean leftEncoder_reached(double count) {
        // Assume failure.
        boolean l_return = false;

        if (left_front_motor != null) {
            // Has the encoder reached the specified values?
            if (Math.abs(left_front_motor.getCurrentPosition()-leftStartPosition) >= count) {
                // Set the status to a positive indication.
                l_return = true;
            }
        }
        // Return the status.
        return l_return;
    }

    public boolean rightEncoder_reached(double count) {
        // Assume failure.
        boolean l_return = false;

        if (right_front_motor != null) {
            // Has the encoder reached the specified values?
            if (Math.abs(right_front_motor.getCurrentPosition()-rightStartPosition) >= count) {
                // Set the status to a positive indication.
                l_return = true;
            }
        }
        // Return the status.
        return l_return;
    }


}

