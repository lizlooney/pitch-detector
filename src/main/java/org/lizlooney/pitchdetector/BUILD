
android_binary(
    name = "app",
    manifest = "AndroidManifest.xml",
    deps = [":pitch_detector_activity"],
)

android_library(
    name = "pitch_detector_activity",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/org/lizlooney/pitchdetector/audio:audio",
        "@maven//:androidx_core_core",
    ],
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
)
