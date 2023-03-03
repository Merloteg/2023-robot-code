package frc.robot.commands

import edu.wpi.first.math.controller.ProfiledPIDController
import edu.wpi.first.networktables.NetworkTableInstance
import edu.wpi.first.wpilibj.XboxController
import edu.wpi.first.wpilibj2.command.Command
import edu.wpi.first.wpilibj2.command.CommandBase
import frc.robot.constants.ArmConstants
import frc.robot.subsystems.PickAndPlaceSubsystem

class SetPickAndPlacePosition(val continuous: Boolean ,val subsystem: PickAndPlaceSubsystem,
                          val elevatorSupplier: ()->Double,
                          val elbowSupplier: ()->Double,
                          val wristSupplier:()->Double,
                          val intakeSupplier: ()->Double,
                          ) : CommandBase() {
    init {
        addRequirements(subsystem)
    }
    var elevatorPid = ProfiledPIDController(
    ArmConstants.elevatorP,
    ArmConstants.elevatorI,
    ArmConstants.elevatorD, ArmConstants.elevatorTrapezoidConstraints, 0.02)

    var elbowPid = ProfiledPIDController(
        ArmConstants.elbowP,
        ArmConstants.elbowI,
        ArmConstants.elbowD, ArmConstants.elbowTrapezoidConstraints, 0.02)
    var wristPid = ProfiledPIDController(
        ArmConstants.wristP,
        ArmConstants.wristI,
        ArmConstants.wristD, ArmConstants.wristTrapezoidConstraints, 0.02)

    val elbowFeedforward = ArmConstants.elbowFF

    object Telemetry{
        val desiredElbow = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("DesiredElbow").publish()
        val desiredWrist = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("DesiredWrist").publish()
        val desiredElevator = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("DesiredElevator").publish()
        val desiredIntake = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("DesiredIntake").publish()

        val elbowPidPosError = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("ElbowPidError").publish()
        val elevatorPidPosError = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("ElevatorPidError").publish()
        val wristPidPosError = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("WristPidError").publish()

        val elbowPidVelocityError = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("ElbowPidError").publish()
        val elevatorPidVelocityError = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("ElevatorPidError").publish()
        val wristPidVelocityError = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("WristPidError").publish()

        val elbowVoltage = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("ElbowVolts").publish()
        val wristVoltage = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("WristVolts").publish()
        val elevatorVoltage = NetworkTableInstance.getDefault().getTable("Arm").getDoubleTopic("ElevatorVolts").publish()
    }

    override fun execute() {

        subsystem.elevatorVoltage = elevatorPid.calculate(subsystem.elevatorPositionMeters,elevatorSupplier().coerceIn(ArmConstants.elevatorMinHeight, ArmConstants.elevatorMaxHeight))
        subsystem.elbowVoltage = elbowPid.calculate(subsystem.elbowPositionRadians,elbowSupplier().coerceIn(ArmConstants.elbowMinRotation, ArmConstants.elbowMaxRotation))
        subsystem.wristVoltage = wristPid.calculate(subsystem.absoluteWristPosition,wristSupplier().coerceIn(ArmConstants.wristMinRotation, ArmConstants.wristMaxRotation))
        subsystem.intakesVoltage = intakeSupplier()

        // TODO: Add telemetry: desired states (from suppliers), pid errors, voltages
        Telemetry.desiredWrist.set(wristSupplier())
        Telemetry.desiredElbow.set(elbowSupplier())
        Telemetry.desiredElevator.set(elevatorSupplier())
        Telemetry.desiredIntake.set(intakeSupplier())
        Telemetry.elbowPidPosError.set(elbowPid.positionError)
        Telemetry.elevatorPidPosError.set(elevatorPid.positionError)
        Telemetry.wristPidPosError.set(wristPid.positionError)
        Telemetry.elbowPidVelocityError.set(elbowPid.velocityError)
        Telemetry.elevatorPidVelocityError.set(elevatorPid.velocityError)
        Telemetry.wristPidVelocityError.set(wristPid.velocityError)
        Telemetry.elbowVoltage.set(subsystem.elbowVoltage)
        Telemetry.wristVoltage.set(subsystem.wristVoltage)
        Telemetry.elevatorVoltage.set(subsystem.elevatorVoltage)
    }

    override fun isFinished(): Boolean {
        return !continuous && (elbowPid.atGoal() && elevatorPid.atGoal() && wristPid.atGoal())
    }
}

fun NTPnP(pnp: PickAndPlaceSubsystem, controller: XboxController): Command {
    var height = 0.5
    return SetPickAndPlacePosition(
        true,
        pnp,
        {
            height += controller.leftX / 25;
            height
        }, // elevator
        { 0.0 }, // eblbow
        { Math.PI / 2.0 }, // wrist
        { controller.leftTriggerAxis * 12.0 } // intake
    )
}