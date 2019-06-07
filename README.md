# MCMultiPart
A universal multipart API for modern Minecraft.

### Adding MCMultiPart to your workspace

To add MCMultiPart to your dev environment and be able to use it in your mods, you need to add the following lines to the buildscript, replacing `<mcmp_version>` with the version you want to use:

    repositories {
        maven { url "https://cdn.hrzn.studio/maven/" }
    }
    dependencies {
        deobfCompile "com.hrznstudio:mcmultipart:<mcmp_version>"
    }