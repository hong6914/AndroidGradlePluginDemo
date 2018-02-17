package poc.utils

/**
 * Platform based utilities for the plugin
 */
class Platform {

    /**
     * Is the plugin running on a windows host?
     * @return
     *      true if running on windows host
     */
    static boolean isRunningOnWindows() {
        return System.getProperty("os.name").startsWith("Windows")
    }

    /**
     * Is the plugin running on a mac host?
     * @return
     *      true if running on mac host
     */
    static boolean isRunningOnMac() {
        return System.getProperty("os.name").startsWith("Mac")
    }

    /**
     * Is the plugin running on a linux host?
     * @return
     *      true if running on linux host
     */
    static boolean isRunningOnLinux() {
        return System.getProperty("os.name").startsWith("Linux")
    }
}
