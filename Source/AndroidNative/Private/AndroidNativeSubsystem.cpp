// Georgy Treshchev 2024.

#include "AndroidNativeSubsystem.h"

#include "AndroidNativeDefines.h"
#include "AndroidNativeUtils.h"
#include "Async/Async.h"

#if PLATFORM_ANDROID
#include "Android/AndroidApplication.h"
#include "Android/AndroidJNI.h"
#endif

static const ANSICHAR* AN_SubsystemDeviceInfoClassName = "com/Plugins/AndroidNative/DeviceInfo";

// Active subsystem instance the JNI callbacks broadcast into.
static UAndroidNativeSubsystem* GActiveAndroidNativeSubsystem = nullptr;

// android.hardware.Sensor.TYPE_* constants
static int32 AN_ToAndroidSensorType(EAndroidSensorType SensorType)
{
	switch (SensorType)
	{
	case EAndroidSensorType::Accelerometer: return 1;  // TYPE_ACCELEROMETER
	case EAndroidSensorType::MagneticField: return 2;  // TYPE_MAGNETIC_FIELD
	case EAndroidSensorType::Gyroscope:     return 4;  // TYPE_GYROSCOPE
	case EAndroidSensorType::Light:         return 5;  // TYPE_LIGHT
	case EAndroidSensorType::Proximity:     return 8;  // TYPE_PROXIMITY
	default:                                return 1;
	}
}

static EAndroidSensorType AN_FromAndroidSensorType(int32 AndroidSensorType)
{
	switch (AndroidSensorType)
	{
	case 2: return EAndroidSensorType::MagneticField;
	case 4: return EAndroidSensorType::Gyroscope;
	case 5: return EAndroidSensorType::Light;
	case 8: return EAndroidSensorType::Proximity;
	case 1:
	default: return EAndroidSensorType::Accelerometer;
	}
}

void UAndroidNativeSubsystem::Initialize(FSubsystemCollectionBase& Collection)
{
	Super::Initialize(Collection);
	GActiveAndroidNativeSubsystem = this;
}

void UAndroidNativeSubsystem::Deinitialize()
{
	if (GActiveAndroidNativeSubsystem == this)
	{
		GActiveAndroidNativeSubsystem = nullptr;
	}
	Super::Deinitialize();
}

bool UAndroidNativeSubsystem::StartSensor(EAndroidSensorType SensorType)
{
	return AndroidNativeUtils::CallJavaStaticMethod<bool>(AN_SubsystemDeviceInfoClassName, "StartSensor", FAndroidGameActivity(), AN_ToAndroidSensorType(SensorType));
}

void UAndroidNativeSubsystem::StopSensor(EAndroidSensorType SensorType)
{
	AndroidNativeUtils::CallJavaStaticMethod<void>(AN_SubsystemDeviceInfoClassName, "StopSensor", FAndroidGameActivity(), AN_ToAndroidSensorType(SensorType));
}

void UAndroidNativeSubsystem::BroadcastLifecycle(int32 LifecycleEvent)
{
	switch (LifecycleEvent)
	{
	case 1: OnAppResumed.Broadcast(); break;
	case 2: OnAppPaused.Broadcast(); break;
	case 3: OnAppStopped.Broadcast(); break;
	default: break;
	}
}

void UAndroidNativeSubsystem::BroadcastSensor(int32 AndroidSensorType, float X, float Y, float Z)
{
	OnSensorChanged.Broadcast(AN_FromAndroidSensorType(AndroidSensorType), X, Y, Z);
}

#if PLATFORM_ANDROID
extern "C" JNIEXPORT void JNICALL Java_com_Plugins_AndroidNative_DeviceInfo_nativeOnLifecycle(
	JNIEnv* /*Env*/, jclass /*Clazz*/, jint LifecycleEvent)
{
	const int32 Event = static_cast<int32>(LifecycleEvent);
	AsyncTask(ENamedThreads::GameThread, [Event]()
	{
		if (GActiveAndroidNativeSubsystem != nullptr)
		{
			GActiveAndroidNativeSubsystem->BroadcastLifecycle(Event);
		}
	});
}

extern "C" JNIEXPORT void JNICALL Java_com_Plugins_AndroidNative_DeviceInfo_nativeOnSensorChanged(
	JNIEnv* /*Env*/, jclass /*Clazz*/, jint SensorType, jfloat X, jfloat Y, jfloat Z)
{
	const int32 Type = static_cast<int32>(SensorType);
	const float Vx = static_cast<float>(X);
	const float Vy = static_cast<float>(Y);
	const float Vz = static_cast<float>(Z);

	AsyncTask(ENamedThreads::GameThread, [Type, Vx, Vy, Vz]()
	{
		if (GActiveAndroidNativeSubsystem != nullptr)
		{
			GActiveAndroidNativeSubsystem->BroadcastSensor(Type, Vx, Vy, Vz);
		}
	});
}
#endif
