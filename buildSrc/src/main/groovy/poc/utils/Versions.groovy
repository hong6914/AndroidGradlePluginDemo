package poc.utils

import org.gradle.util.GradleVersion

/**
 * Utilities to deal with versions of stuff
 */
class Versions {

    static String getGradleVersion() {
        return GradleVersion.current().getVersion();
    }

    static String getAndroidGradlePluginVersion() {
        return com.android.builder.Version.ANDROID_GRADLE_PLUGIN_VERSION;
    }

    static boolean isPluginVersion3orAbove() {
        return versionCompare(getAndroidGradlePluginVersion(), "3.0.0") >= 0;
    }

    static boolean meetMinimumVersionsRequirement() {
        return  versionCompare(getGradleVersion(), "2.14.1") >= 0 &&
                versionCompare(getAndroidGradlePluginVersion(), "2.1.0") >= 0;
    }

    /**
     * Compare two version strings.
     *
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
     *
     * @param str1 a string of ordinal numbers separated by decimal points.
     * @param str2 a string of ordinal numbers separated by decimal points.
     * @return a negative integer if str1 is _numerically_ less than str2.
     *         a positive integer if str1 is _numerically_ greater than str2.
     *         zero if the strings are _numerically_ equal.
     */
    static int versionCompare(String str1, String str2) {
        String[] vals1 = str1.split("-")[0].split("\\.");
        String[] vals2 = str2.split("-")[0].split("\\.");
        int i = 0;

        // set index to first non-equal ordinal or length of shortest version string
        while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
            i++;
        }

        // compare first non-equal ordinal number
        if (i < vals1.length && i < vals2.length) {
            int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
            return Integer.signum(diff);
        }

        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        else {
            return Integer.signum(vals1.length - vals2.length);
        }
    }
}
