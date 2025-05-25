
android_binary(
    name = "app",
    manifest = "AndroidManifest.xml",
    deps = [":pitchdetector"],
)

android_library(
    name = "pitchdetector",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/org/lizlooney/pitchdetector/audio:audio",
        "@maven//:androidx_core_core",
    ],
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
)
