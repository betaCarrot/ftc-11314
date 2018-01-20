package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDevice;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchImpl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.RelicRecoveryVuMark;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackables;

import java.util.Locale;

/**
 * Created by samsung on 2017/12/3.
 */
@Autonomous(name="nearBlue", group="Linear Opmode")
//@Disabled
public class nearBlue extends LinearOpMode {
    private static final int BLUE = 3;
    private ElapsedTime runtime = new ElapsedTime();

    boolean jewelFinished;
    byte[] colorCcache;
    int colorCNumber;
    I2cDevice colorC;
    I2cDeviceSynch colorCreader;


    BNO055IMU gyro;
    Orientation angles;
    Acceleration gravity;

    static final double     P_TURN_COEFF            = 0.07;     // Larger is more responsive, but also less stable
    static final double     P_DRIVE_COEFF           = 0.15;     // Larger is more responsive, but also less stable

    int VuMarkID = -1;
    boolean VuMarkFinished = false;
    ClosableVuforiaLocalizer vuforia;
    VuforiaTrackables relicTrackables;
    VuforiaTrackable relicTemplate;
    RelicRecoveryVuMark vuMark;

    private DcMotor LeftBackMotor;
    private DcMotor RightBackMotor;
    private DcMotor LeftFrontMotor;
    private DcMotor RightFrontMotor;
    private Servo jewelArm;
    private Servo jewelTurn;
    private DcMotor LiftMotor;
    private DcMotor LiftMotor2;
    private CRServo ClawL;
    private CRServo ClawR;
    private CRServo IntakeR;
    private CRServo IntakeL;
    private Servo IntLnkR;
    private Servo IntLnkL;


    static final double     COUNTS_PER_MOTOR_REV    = 1120 ;
    static final double     DRIVE_GEAR_REDUCTION    = 0.5 ;
    static final double     WHEEL_DIAMETER_INCHES   = 4.0 ;
    static final double     COUNTS_PER_INCH         = (COUNTS_PER_MOTOR_REV * DRIVE_GEAR_REDUCTION) /
            (WHEEL_DIAMETER_INCHES * 3.1415);
    static final double COUNTS_PER_INCH_LIFT = (1120)/(6*3.1415);

    static final double     DRIVE_SPEED             = 0.3;
    static final double     TURN_SPEED             = 0.3;

    static final double CENTER_DISTANCE = 31;
    static final double OFFSET = 7.5;
    static final double FORWARD_DISTANCE= 6;
    static final double BACKWARD_DISTANCE = 6;
    static final double PUSH_DISTANCE = 11;
    static final double BACKOFF_DISTANCE = 8;

    public void runOpMode() {
        motorInit();
        initializeVuMark(true);
        initializeColorSensors();
        initializeGyro();
        jewelArm = hardwareMap.servo.get("jewelArm");
        jewelTurn = hardwareMap.servo.get("jewelTurn");
        jewelTurn.setPosition(1);
        jewelArm.setPosition(0);
        IntLnkL.setPosition(0.8);
        telemetry.update();
        waitForStart();

        lift(DRIVE_SPEED,5);

        runtime.reset();
        while(opModeIsActive()&&runtime.milliseconds()<1000) {
            jewelArm.setPosition(0.8);
        }
        runtime.reset();
        while(opModeIsActive()&&runtime.milliseconds()<1000) {
            jewelTurn.setPosition(0.45);
        }
        runtime.reset();
        while(opModeIsActive()&&runtime.milliseconds()<1000) {
            jewelArm.setPosition(0.92);
        }
        while(opModeIsActive()&&runtime.milliseconds()<500){
            readColorNumberC();
        }
        double currentPosition = 0.39;
        if (colorCNumber != 0) {
            hitJewel();
        }
        while (opModeIsActive()&&!jewelFinished) {
            runtime.reset();
            while(opModeIsActive()&&runtime.milliseconds()<500){
                readColorNumberC();
            }
            if (currentPosition<0.35) {
                runtime.reset();
                while(opModeIsActive()&&runtime.milliseconds()<1000) {
                    jewelTurn.setPosition(0.45);
                }
                runtime.reset();
                while(opModeIsActive()&&runtime.milliseconds()<1000) {
                    jewelArm.setPosition(0.8);
                }
                runtime.reset();
                while(opModeIsActive()&&runtime.milliseconds()<1000) {
                    jewelTurn.setPosition(1);
                }
                jewelArm.setPosition(0.2);
                break;
            }
            if (colorCNumber != 0){
                hitJewel();
                break;
            }
            currentPosition-=0.01;
            jewelTurn.setPosition(currentPosition);
            displayColorNumber();
            //composeTelemetry();
            telemetry.update();
        }
        jewelArm.setPosition(0.2);
        drive(0, 0, 0);
        telemetry.clearAll();
        telemetry.update();

        runtime.reset();
        while(opModeIsActive()&&!VuMarkFinished){
            findVuMark();
            if(runtime.milliseconds()>2000){
                deactiveVuMark();
                VuMarkID=2;
                VuMarkFinished=true;
                break;
            }
            displayVuMarkID();
            //composeTelemetry();
            telemetry.update();
        }
        /*
        if(!VuMarkFinished) {
            gyroDrive(DRIVE_SPEED, 1.5);
        }

       /*runtime.reset();
        distanceTraveled=0.0;
        while (!VuMarkFinished) {
            gyroDrive(DRIVE_SPEED, -2);
            distanceTraveled += 2;
            if (distanceTraveled >= 8) {
                VuMarkFinished = true;
                VuMarkID = 2;
                deactiveVuMark();
                break;
            }
            runtime.reset();
            while (runtime.milliseconds() < 1000) {
                findVuMark();
            }
            displayVuMarkID();
            //composeTelemetry();
            telemetry.update();
        }
        if(distanceTraveled>0.0){
            distanceTraveled+=OFFSET;
        }
        telemetry.addData("DistanceTraveled: ",distanceTraveled);
        composeTelemetry();

        int iter = 0;
        int limit = 2;
        while(!VuMarkFinished){
            runtime.reset();
            while(runtime.milliseconds()<1000) {
                findVuMark();
            }
            displayVuMarkID();
            telemetry.addData("iter: ",iter);
            telemetry.update();
            turn(300,-TURN_SPEED);
            iter++;
            if(VuMarkFinished){
                gyroTurn(TURN_SPEED,0.0);
                break;
            }
            if(iter>limit){
                deactiveVuMark();
                gyroTurn(TURN_SPEED,0.0);
                VuMarkID=2;
                VuMarkFinished=true;
                break;
            }
        }
        */
        if (VuMarkID == 1) {
            gyroDrive(DRIVE_SPEED, CENTER_DISTANCE - OFFSET);
        } else if (VuMarkID == 2) {
            gyroDrive(DRIVE_SPEED, CENTER_DISTANCE);
        } else {
            gyroDrive(DRIVE_SPEED, CENTER_DISTANCE  + OFFSET);
        }
        telemetry.update();
        gyroTurn(TURN_SPEED,-90.1,1);
        gyroTurn(TURN_SPEED,-90.0,1);
        runtime.reset();
        gyroDrive(DRIVE_SPEED, FORWARD_DISTANCE);
        ClawL.setPower(-0.7);
        ClawR.setPower(-0.7);
        runtime.reset();
        while (opModeIsActive()&&runtime.milliseconds() < 1000) {

        }
        gyroDrive(DRIVE_SPEED,-BACKWARD_DISTANCE);
        gyroTurn(TURN_SPEED,92.0,3);
        gyroDrive(DRIVE_SPEED,-PUSH_DISTANCE);
        gyroDrive(DRIVE_SPEED,BACKOFF_DISTANCE);
        ClawL.setPower(0);
        ClawR.setPower(0);
        if (VuMarkID == 1) {
            gyroStraffe(DRIVE_SPEED, OFFSET);
            turn(700,TURN_SPEED);
        }
        if(VuMarkID==2){
            turn(700,TURN_SPEED);
        }
        if (VuMarkID == 3) {
            gyroStraffe(DRIVE_SPEED, -OFFSET);
            turn(700,-TURN_SPEED);
        }
        /*
        gyroDrive(DRIVE_SPEED,PUSH_DISTANCE);
        gyroDrive(DRIVE_SPEED,-BACKOFF_DISTANCE);
        ClawL.setPower(0);
        ClawR.setPower(0);
        if (VuMarkID == 1) {
            gyroStraffe(DRIVE_SPEED, -OFFSET);
            turn(700,TURN_SPEED);
        }
        if(VuMarkID==2){
            turn(700,TURN_SPEED);
        }
        if (VuMarkID == 3) {
            gyroStraffe(DRIVE_SPEED, OFFSET);
            turn(700,-TURN_SPEED);
        }
        */
        telemetry.update();
    }

    public void findVuMark(){
        vuMark = RelicRecoveryVuMark.from(relicTemplate);
        if (vuMark != RelicRecoveryVuMark.UNKNOWN) {
            if (vuMark == RelicRecoveryVuMark.LEFT) VuMarkID = 1;
            else if (vuMark == RelicRecoveryVuMark.CENTER) VuMarkID = 2;
            else VuMarkID = 3;
            VuMarkFinished = true;
            deactiveVuMark();
        }
    }

    public void deactiveVuMark(){
        relicTrackables.deactivate();
        vuforia.close();
    }

    public void initializeVuMark(boolean cameraEnabled){
        VuforiaLocalizer.Parameters parameters;
        if(cameraEnabled)
            parameters = new VuforiaLocalizer.Parameters(hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName()));
        else
            parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = "AbvEHy7/////AAAAGRmT0kowlEESsHWO52+1yK4RGV8QifRa3mmkwcy05kelV2/3fGxhhkPmURHfIgTA9Y2lw4zf1WZ5DkPv5DVfLSYy/cafwHkCriSeJep+5xwa2qRpMi3aDAncmXGB5ZoWw3hHK5upIXBj/aX9q9cNKN2ZcE05dMGaJp7ykHLfYXeVhL7fepFvOhLj8szG5kzE7myp2Lc2pgaDb8iGGhijznojUSg0GcSIQC7cEFRacsCUN7f4cIhHdK2c+Lv3sZP+NlyRliGoz8ICS4AgKDTXRVIFIDZ0h96T4rGjFxInT5JsK2tGqilGckVRcDXnGZPG6KR3LIdaa2T72mSoww/GusZhWrYSiS15oge6kr1Vv2UJ";

        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
        this.vuforia = new ClosableVuforiaLocalizer(parameters);

        relicTrackables = this.vuforia.loadTrackablesFromAsset("RelicVuMark");
        relicTemplate = relicTrackables.get(0);

        relicTrackables.activate();
    }

    public void displayVuMarkID(){
        if(VuMarkID==1) telemetry.addData("VuMarkID", "LEFT");
        else if(VuMarkID==2) telemetry.addData("VuMarkID", "CENTER");
        else if(VuMarkID==3) telemetry.addData("VuMarkID", "RIGHT");
        else telemetry.addData("VuMarkID", "UNKNOWN");
    }

    public void drive(double x, double y, double r) {
        double frValue = y - x -  r;
        double flValue = y + x + r;
        double rrValue = y + x - r;
        double rlValue = y - x + r;

        double maxValue = Math.max(
                Math.max(Math.abs(flValue), Math.abs(rlValue)),
                Math.max(Math.abs(frValue), Math.abs(rrValue))
        );
        if (maxValue > 1.0) {
            flValue /= maxValue;
            rlValue /= maxValue;
            frValue /= maxValue;
            rrValue /= maxValue;
        }
        LeftFrontMotor.setPower(flValue);
        RightFrontMotor.setPower(frValue);
        LeftBackMotor.setPower(rlValue);
        RightBackMotor.setPower(rrValue);
    }

    public void jewelForward(){
        jewelTurn.setPosition(1);
    }

    public void jewelBackward(){
        jewelTurn.setPosition(0);
    }

    public void hitJewel(){
        if(colorCNumber==BLUE) {
            jewelForward();
            runtime.reset();
            while(opModeIsActive()&&runtime.milliseconds()<2000){

            }
        }
        else {
            jewelBackward();
            runtime.reset();
            while(opModeIsActive()&&runtime.milliseconds()<2000){

            }
        }
        jewelArm.setPosition(0.2);
        jewelFinished = true;
        //colorCreader.disengage();
    }
    public void displayColorNumber(){
        telemetry.addData("Color C: " ,colorCNumber);
    }

    public void readColorNumberC(){
        colorCcache = colorCreader.read(0x04, 1);
        colorCNumber = colorCcache[0];
    }

    public void initializeColorSensors() {
        colorC = hardwareMap.i2cDevice.get("color sensor C");
        colorCreader = new I2cDeviceSynchImpl(colorC, I2cAddr.create8bit(0x3c), false);
        colorCreader.engage();
        colorCreader.write8(3, 0);
    }

    public void lift(double speed, double distance){
        int moveCounts;
        int target;

        moveCounts = (int)(distance * COUNTS_PER_INCH_LIFT);
        target = LiftMotor.getCurrentPosition()+moveCounts;
        LiftMotor.setTargetPosition(target);
        LiftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        speed = Range.clip(Math.abs(speed), 0.0, 1.0);
        LiftMotor.setPower(speed);

        while (opModeIsActive() &&LiftMotor.isBusy()) {
            double max = speed;
            if (max > 1.0)
            {
                speed/=max;
            }
            LiftMotor.setPower(speed);
        }
        LiftMotor.setPower(0);
        LiftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public void gyroDrive ( double speed, double distance)
    {
        int     newLeftFrontTarget;
        int     newLeftBackTarget;
        int     newRightFrontTarget;
        int     newRightBackTarget;
        int     moveCounts;
        double  max;

        if (opModeIsActive()) {
            moveCounts = (int)(distance * COUNTS_PER_INCH);
            newLeftFrontTarget = LeftFrontMotor.getCurrentPosition() + moveCounts;
            newLeftBackTarget = LeftBackMotor.getCurrentPosition()+moveCounts;
            newRightFrontTarget = RightFrontMotor.getCurrentPosition()+moveCounts;
            newRightBackTarget = RightBackMotor.getCurrentPosition()+moveCounts;

            LeftFrontMotor.setTargetPosition(newLeftFrontTarget);
            LeftBackMotor.setTargetPosition(newLeftBackTarget);
            RightFrontMotor.setTargetPosition(newRightFrontTarget);
            RightBackMotor.setTargetPosition(newRightBackTarget);

            LeftFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            LeftBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            RightFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            RightBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            speed = Range.clip(Math.abs(speed), 0.0, 1.0);
            LeftFrontMotor.setPower(speed);
            LeftBackMotor.setPower(speed);
            RightFrontMotor.setPower(speed);
            RightBackMotor.setPower(speed);

            while (opModeIsActive() &&
                    (LeftFrontMotor.isBusy() && LeftBackMotor.isBusy()&&RightFrontMotor.isBusy()&&RightBackMotor.isBusy())) ;

            max = speed;
            if (max > 1.0)
            {
                speed/=max;
            }
            drive(0,speed,0);

            telemetry.addData("Target",  "%7d:%7d:%7d:%7d",      newLeftFrontTarget,  newLeftBackTarget,newRightFrontTarget,newRightBackTarget);
            telemetry.addData("Actual",  "%7d:%7d:%7d:%7d",      LeftFrontMotor.getCurrentPosition(),LeftBackMotor.getCurrentPosition(),RightFrontMotor.getCurrentPosition(),RightBackMotor.getCurrentPosition());
            telemetry.addData("Speed",   "%5.2f",  speed);
            composeTelemetry();
            telemetry.update();
        }

        LeftFrontMotor.setPower(0);
        LeftBackMotor.setPower(0);
        RightFrontMotor.setPower(0);
        RightBackMotor.setPower(0);

        LeftFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        LeftBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        RightFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        RightBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public void gyroStraffe ( double speed,double distance)
    {

        int     newLeftFrontTarget;
        int     newLeftBackTarget;
        int     newRightFrontTarget;
        int     newRightBackTarget;
        int     moveCounts;
        double  max;

        if (opModeIsActive()) {
            moveCounts = (int)(distance * COUNTS_PER_INCH);
            newLeftFrontTarget = LeftFrontMotor.getCurrentPosition() - moveCounts;
            newLeftBackTarget = LeftBackMotor.getCurrentPosition()+moveCounts;
            newRightFrontTarget = RightFrontMotor.getCurrentPosition()+moveCounts;
            newRightBackTarget = RightBackMotor.getCurrentPosition()-moveCounts;

            LeftFrontMotor.setTargetPosition(newLeftFrontTarget);
            LeftBackMotor.setTargetPosition(newLeftBackTarget);
            RightFrontMotor.setTargetPosition(newRightFrontTarget);
            RightBackMotor.setTargetPosition(newRightBackTarget);

            LeftFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            LeftBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            RightFrontMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            RightBackMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            speed = Range.clip(Math.abs(speed), 0.0, 1.0);
            LeftFrontMotor.setPower(speed);
            LeftBackMotor.setPower(speed);
            RightFrontMotor.setPower(speed);
            RightBackMotor.setPower(speed);

            while (opModeIsActive() &&
                    (LeftFrontMotor.isBusy() && LeftBackMotor.isBusy()&&RightFrontMotor.isBusy()&&RightBackMotor.isBusy())) ;

            max = speed;
            if (max > 1.0)
            {
                speed/=max;
            }

            drive(speed,0,0);
            telemetry.addData("Target",  "%7d:%7d:%7d:%7d",      newLeftFrontTarget,  newLeftBackTarget,newRightFrontTarget,newRightBackTarget);
            telemetry.addData("Actual",  "%7d:%7d:%7d:%7d",      LeftFrontMotor.getCurrentPosition(),LeftBackMotor.getCurrentPosition(),RightFrontMotor.getCurrentPosition(),RightBackMotor.getCurrentPosition());
            telemetry.addData("Speed",   "%5.2f",  speed);
            composeTelemetry();
            telemetry.update();
        }

        LeftFrontMotor.setPower(0);
        LeftBackMotor.setPower(0);
        RightFrontMotor.setPower(0);
        RightBackMotor.setPower(0);

        LeftFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        LeftBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        RightFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        RightBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }


    public void motorInit()
    {
        LeftBackMotor = hardwareMap.dcMotor.get("rl");
        LeftFrontMotor = hardwareMap.dcMotor.get("fl");

        RightFrontMotor = hardwareMap.dcMotor.get("fr");
        RightBackMotor = hardwareMap.dcMotor.get("rr");

        RightFrontMotor.setDirection(DcMotor.Direction.REVERSE);
        RightBackMotor.setDirection(DcMotor.Direction.REVERSE);

        LiftMotor = hardwareMap.dcMotor.get("LiftMotor");
        LiftMotor2 = hardwareMap.dcMotor.get("LiftMotor2");

        ClawR = hardwareMap.crservo.get("ClawR");
        ClawL = hardwareMap.crservo.get("ClawL");

        IntLnkL = hardwareMap.servo.get("IntLnkL");
        IntLnkR = hardwareMap.servo.get("IntLnkR");

        IntakeR = hardwareMap.crservo.get("IntakeR");
        IntakeL = hardwareMap.crservo.get("IntakeL");

        LeftBackMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        LeftFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        RightBackMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        RightFrontMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        LeftFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        LeftBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        RightFrontMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        RightBackMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        LeftFrontMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        LeftBackMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        RightFrontMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        RightBackMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
    }

    public void gyroTurn (  double speed, double angle, double HEADING_THRESHOLD) {

        // keep looping while we are still active, and not on heading.
        while (opModeIsActive() && !onHeading(speed, angle, P_TURN_COEFF,HEADING_THRESHOLD)) {
            // Update telemetry & Allow time for other processes to run.
            telemetry.update();
        }
    }
    /**
     * Perform one cycle of closed loop heading control.
     *
     * @param speed     Desired speed of turn.
     * @param angle     Absolute Angle (in Degrees) relative to last gyro reset.
     *                  0 = fwd. +ve is CCW from fwd. -ve is CW from forward.
     *                  If a relative angle is required, add/subtract from current heading.
     * @param PCoeff    Proportional Gain coefficient
     * @return
     */
    boolean onHeading(double speed, double angle, double PCoeff, double HEADING_THRESHOLD) {
        double   error ;
        double   steer ;
        boolean  onTarget = false ;

        // determine turn power based on +/- error
        error = getError(angle);

        if (Math.abs(error) <= HEADING_THRESHOLD) {
            steer = 0.0;
            speed = 0.0;
            onTarget = true;
        }
        else {
            steer = getSteer(error, PCoeff);
            speed *=steer;
        }

        // Send desired speeds to motors.
        drive(0,0,speed);

        // Display it for the driver.
        telemetry.addData("Target", "%5.2f", angle);
        telemetry.addData("Heading", -gyro.getAngularOrientation().firstAngle);
        telemetry.addData("Err/St", "%5.2f/%5.2f", error, steer);
        telemetry.addData("Speed.", "%5.2f", speed);

        return onTarget;
    }

    /**
     * getError determines the error between the target angle and the robot's current heading
     * @param   targetAngle  Desired angle (relative to global reference established at last Gyro Reset).
     * @return  error angle: Degrees in the range +/- 180. Centered on the robot's frame of reference
     *          +ve error means the robot should turn LEFT (CCW) to reduce error.
     */
    public double getError(double targetAngle) {

        double robotError;

        // calculate error in -179 to +180 range  (
        robotError = targetAngle + gyro.getAngularOrientation().firstAngle;
        while (robotError > 180)  robotError -= 360;
        while (robotError <= -180) robotError += 360;
        return robotError;
    }

    /**
     * returns desired steering force.  +/- 1 range.  +ve = steer left
     * @param error   Error angle in robot relative degrees
     * @param PCoeff  Proportional Gain Coefficient
     * @return
     */
    public double getSteer(double error, double PCoeff) {
        return Range.clip(error * PCoeff, -1, 1);
    }


    public void straffe(int ms, double power){
        runtime.reset();
        while(opModeIsActive()) {
            drive(power, 0, 0);
            if (runtime.milliseconds() >= ms){
                drive(0,0,0);
                runtime.reset();
                break;
            }
        }
    }

    public void driveForward(int ms, double power){
        runtime.reset();
        while(opModeIsActive()) {
            drive(0, power, 0);
            if (runtime.milliseconds() >= ms){
                drive(0,0,0);
                runtime.reset();
                break;
            }
        }
    }

    public void driveBackward(int ms, double power){
        runtime.reset();
        while(opModeIsActive()) {
            drive(0, -power, 0);
            if (runtime.milliseconds() >= ms){
                runtime.reset();
                drive(0,0,0);
                break;
            }
        }
    }

    public void turn(int ms, double power){
        runtime.reset();
        while(opModeIsActive()) {
            drive(0, 0, power);
            if (runtime.milliseconds() >= ms){
                runtime.reset();
                drive(0,0,0);
                break;
            }
        }
    }

    public void initializeGyro(){
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        //parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled      = true;
        parameters.loggingTag          = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();

        // Retrieve and initialize the IMU. We expect the IMU to be attached to an I2C port
        // on a Core Device Interface Module, configured to be a sensor of type "AdaFruit IMU",
        // and named "imu".
        gyro = hardwareMap.get(BNO055IMU.class, "gyro");
        gyro.initialize(parameters);

        // Set up our telemetry dashboard
        composeTelemetry();
    }

    void composeTelemetry() {

        // At the beginning of each telemetry update, grab a bunch of data
        // from the IMU that we will then display in separate lines.
        telemetry.addAction(new Runnable() { @Override public void run()
        {
            // Acquiring the angles is relatively expensive; we don't want
            // to do that in each of the three items that need that info, as that's
            // three times the necessary expense.
            angles   = gyro.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
            gravity  = gyro.getGravity();
        }
        });

        telemetry.addLine()
                .addData("status", new Func<String>() {
                    @Override public String value() {
                        return gyro.getSystemStatus().toShortString();
                    }
                })
                .addData("calib", new Func<String>() {
                    @Override public String value() {
                        return gyro.getCalibrationStatus().toString();
                    }
                });

        telemetry.addLine()
                .addData("heading", new Func<String>() {
                    @Override public String value() {
                        return formatAngle(angles.angleUnit, angles.firstAngle);
                    }
                })
                .addData("roll", new Func<String>() {
                    @Override public String value() {
                        return formatAngle(angles.angleUnit, angles.secondAngle);
                    }
                })
                .addData("pitch", new Func<String>() {
                    @Override public String value() {
                        return formatAngle(angles.angleUnit, angles.thirdAngle);
                    }
                });

        telemetry.addLine()
                .addData("grvty", new Func<String>() {
                    @Override public String value() {
                        return gravity.toString();
                    }
                })
                .addData("mag", new Func<String>() {
                    @Override public String value() {
                        return String.format(Locale.getDefault(), "%.3f",
                                Math.sqrt(gravity.xAccel*gravity.xAccel
                                        + gravity.yAccel*gravity.yAccel
                                        + gravity.zAccel*gravity.zAccel));
                    }
                });
    }

    String formatAngle(AngleUnit angleUnit, double angle) {
        return formatDegrees(AngleUnit.DEGREES.fromUnit(angleUnit, angle));
    }

    String formatDegrees(double degrees){
        return String.format(Locale.getDefault(), "%.1f", AngleUnit.DEGREES.normalize(degrees));
    }

}
