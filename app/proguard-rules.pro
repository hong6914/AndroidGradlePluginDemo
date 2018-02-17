# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\apps\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-verbose

# Optimization is turned off by default. Dex does not like code run
# through the ProGuard optimize and preverify steps (and performs some
# of these optimizations on its own).

# Optimization can causes errors:
# Error: Found NEW without a DUP in method android.support.v4.g.a.a.<clinit>

#-dontoptimize
#-dontobfuscate

#-dontshrink

#-dontusemixedcaseclassnames ## avoid problems on Windows hosts
#-useuniqueclassmembernames  ## avoid problems with duplicate methods
#-keeppackagenames
#-dontpreverify
#-keepattributes #*Annotation*
#-keepparameternames
#-skipnonpubliclibraryclasses
-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}

# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**


# keep ACE ranges and hook point
#-keep public class de.foo.android.gradle.libraryprojects.library2.*

-keepclassmembers class com.example.simplesimon.SimpleSimon {
   public static ** CheckPass(java.lang.String);
}

# keep reaction classes and reaction/hook members
-keep public class com.example.helloworld.MainActivity
-keepclassmembers class com.example.helloworld.MainActivity  {
   *** on*(*); # keep onStart ... 
}

#-keep public class de.foo.android.gradle.libraryprojects.*.*
#-keepclassmembers class de.foo.android.gradle.libraryprojects.*.*  {
#}

-dontwarn **

