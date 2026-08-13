// Georgy Treshchev 2024.

#pragma once

#include "CoreMinimal.h"
#include "Subsystems/GameInstanceSubsystem.h"
#include "AndroidNativeSubsystem.generated.h"

/**
 * Sensor kinds exposed for streaming
 */
UENUM(BlueprintType, Category = "Android Native Library|Enumerators")
enum class EAndroidSensorType : uint8
{
	Accelerometer,
	Gyroscope,
	MagneticField,
	Light,
	Proximity
};

DECLARE_DYNAMIC_MULTICAST_DELEGATE(FAndroidNativeSimpleEvent);
DECLARE_DYNAMIC_MULTICAST_DELEGATE_FourParams(FAndroidNativeSensorEvent, EAndroidSensorType, SensorType, float, X, float, Y, float, Z);

/**
 * Game-instance subsystem that surfaces continuous Android events (app lifecycle and
 * sensor streams) as Blueprint-assignable delegates. Get it via
 * GetGameInstance()->GetSubsystem<UAndroidNativeSubsystem>() (or the Blueprint
 * "Get Android Native Subsystem" node) and bind to the events.
 *
 * Requires an on-device Unreal build to compile and verify.
 */
UCLASS()
class ANDROIDNATIVE_API UAndroidNativeSubsystem : public UGameInstanceSubsystem
{
	GENERATED_BODY()

public:
	UPROPERTY(BlueprintAssignable, Category = "Android Native Library|Lifecycle")
	FAndroidNativeSimpleEvent OnAppResumed;

	UPROPERTY(BlueprintAssignable, Category = "Android Native Library|Lifecycle")
	FAndroidNativeSimpleEvent OnAppPaused;

	UPROPERTY(BlueprintAssignable, Category = "Android Native Library|Lifecycle")
	FAndroidNativeSimpleEvent OnAppStopped;

	UPROPERTY(BlueprintAssignable, Category = "Android Native Library|Sensors")
	FAndroidNativeSensorEvent OnSensorChanged;

	/** Start streaming a sensor. Returns false if the sensor is unavailable. */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Sensors")
	bool StartSensor(EAndroidSensorType SensorType);

	/** Stop streaming a previously started sensor. */
	UFUNCTION(BlueprintCallable, Category = "Android Native Library|Sensors")
	void StopSensor(EAndroidSensorType SensorType);

	// UGameInstanceSubsystem
	virtual void Initialize(FSubsystemCollectionBase& Collection) override;
	virtual void Deinitialize() override;

	// Invoked (on the game thread) by the JNI callbacks
	void BroadcastLifecycle(int32 LifecycleEvent);
	void BroadcastSensor(int32 AndroidSensorType, float X, float Y, float Z);
};
