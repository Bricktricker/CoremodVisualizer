# Coremod Visualizer
This tool allows you to view the transformed bytecode from coremods for classes loaded by the [MinecraftForge](https://github.com/MInecraftForge/MinecraftForge) mod loader. It allows you to view the bytecode and tries to decompile it with [ForgeFlower](https://github.com/MinecraftForge/ForgeFlower). Coremod Visualizer is designed to work with [modlauncher](https://github.com/cpw/modlauncher) 5.1.0.

## Usage
1. Clone the repo and build the jar with `gradlew build`
2. Launch the jar, but make sure to add the tool.jar from the JDK to the classpath with `-cp "/path/to/jdk/lib/tools.jar;."`
3. Launch Minecraft with the `-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005` JVM args.
4. Wait until all needed classes are loaded. Then select the class you want to view on the left. You may have to wait a bit until ForgeFlower has decompiled the class.

## How does this work
Coremod Visualizer attatches a debuger to minecraft to stop the VM when a class is loaded by the [ClassTransformer](https://github.com/cpw/modlauncher/blob/master/src/main/java/cpw/mods/modlauncher/ClassTransformer.java).
More specificaly when the transformed class is returned [here](https://github.com/cpw/modlauncher/blob/79f13f769a8102c3426e4bef47930503cb2b710f/src/main/java/cpw/mods/modlauncher/ClassTransformer.java#L66), [here](https://github.com/cpw/modlauncher/blob/79f13f769a8102c3426e4bef47930503cb2b710f/src/main/java/cpw/mods/modlauncher/ClassTransformer.java#L89) and [here](https://github.com/cpw/modlauncher/blob/79f13f769a8102c3426e4bef47930503cb2b710f/src/main/java/cpw/mods/modlauncher/ClassTransformer.java#L125). The classbytes are then copied and stored for the user to view.