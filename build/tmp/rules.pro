
-injars 'C:\IntelijProjects\autoswap\build\libs\autoswap-1.0.0.jar'
-outjars 'C:\IntelijProjects\autoswap\build\libs\autoswap-1.0.0-obf.jar'
-libraryjars 'C:\Program Files\Java\jdk-21/jmods'
-dontwarn **
-dontnote **
-optimize
-repackageclasses 'net.yourname.autoswap.internal'
-allowaccessmodification
-keepattributes Exceptions,Signature,InnerClasses,EnclosingMethod,Deprecated,Annotation,SourceFile,LineNumberTable
-keepattributes *Mixin*,*Inject*,*Redirect*,*ModifyArg*,*ModifyArgs*,*ModifyConstant*,*ModifyVariable*,*At*,*Shadow*,*Overwrite*
-keepdirectories assets/**,data/**
-keep class assets.** { *; }
-keep public class net.yourname.autoswap.AutoSwapMod { *; }
-keep public class net.yourname.autoswap.AutoSwapClient { *; }
-keep public class net.yourname.autoswap.ModMenuIntegration { *; }
-keep class net.yourname.autoswap.config.** { *; }
-keep class net.yourname.autoswap.screen.** { *; }
-keep class net.yourname.autoswap.mixin.** { *; }
        