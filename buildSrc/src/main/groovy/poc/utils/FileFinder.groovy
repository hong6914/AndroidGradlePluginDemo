package poc.utils

import groovy.io.FileType

import java.util.zip.ZipFile


/**
 * Finds files inside the specified location
 */
class FileFinder {

    /**
     * Search directories recursively for files based on regex pattern
     * @param rootFolder
     *      The root directory for the search
     * @param regexSearchString
     *      The search pattern string
     * @return ArrayList<File> of matching files
     */
    static List<File> lookupForFiles(String rootFolder, String regexSearchString) {
        lookupForFiles(new File(rootFolder), regexSearchString)
    }

    /**
     * Same As Above
     */
    static List<File> lookupForFiles(File rootFolder, String regexSearchString) {
        if (! rootFolder.exists()) {
            throw new RuntimeException("Directory " + rootFolder.canonicalFile + " does not exist.")
        }
        if (! rootFolder.isDirectory()) {
            throw new RuntimeException("File " + rootFolder.canonicalFile + " is not a directory.")
        }

        List<File> matchingFiles = new ArrayList<File>()
        rootFolder.eachFileRecurse(FileType.FILES) {
            if (it.name ==~ regexSearchString) {
                matchingFiles.add(it)
            }
        }

        return matchingFiles
    }

    /**
     * Search directories recursively for files based on beginning and ending strings of the name
     * @param rootFolder
     *      The root directory for the search
     * @param regexSearchString
     *      The search pattern string
     * @return ArrayList<File> of matching files
     */
    static List<File> lookupForFiles(String rootFolder, String startString, String endString) {
        lookupForFiles(new File(rootFolder), startString, endString)
    }

    /**
     * Same As Above
     */
    static List<File> lookupForFiles(File rootFolder, String startString, String endString) {
        if (! rootFolder.exists()) {
            throw new RuntimeException("Directory " + rootFolder.canonicalFile + " does not exist.")
        }
        if (! rootFolder.isDirectory()) {
            throw new RuntimeException("File " + rootFolder.canonicalFile + " is not a directory.")
        }

        List<File> matchingFiles = new ArrayList<File>()
        rootFolder.eachFileRecurse(FileType.FILES) {
            if (it.name.startsWith(startString) && it.name.endsWith(endString)) {
                matchingFiles.add(it)
            }
        }

        return matchingFiles
    }

    /**
     * Search for sub directory
     */
    static List<File> lookupForDirs(String rootFolder, String regexSearchString) {
        lookupForDirs(new File(rootFolder), regexSearchString)
    }

    static List<File> lookupForDirs(File rootFolder, String regexSearchString) {
        if (! rootFolder.exists()) {
            throw new RuntimeException("Directory " + rootFolder.canonicalFile + " does not exist.")
        }
        if (! rootFolder.isDirectory()) {
            throw new RuntimeException("File " + rootFolder.canonicalFile + " is not a directory.")
        }

        List<File> matchingFiles = new ArrayList<File>()
        rootFolder.eachFileRecurse(FileType.DIRECTORIES) {
            if (it.name ==~ regexSearchString) {
                matchingFiles.add(it)
            }
        }

        return matchingFiles
    }

    /**
     * Extract embedded file from one library
     *
     */
    static String ExtractFileFromLibrary(String libraryFileName, String fileName) {
        if (libraryFileName.isEmpty())
            return null;
        def jarFile = null
        def zipFile = new ZipFile(libraryFileName)

        zipFile.entries().each { ins->
            if (! ins.directory && ins.name.equals(fileName)) {
                def tempFile = File.createTempFile('temp', '.jar')
                tempFile.deleteOnExit()                                         // The temp file will be cleaned out when the thread terminates (in theory).
                tempFile.withOutputStream { os ->
                    os << zipFile.getInputStream(ins)
                }
                jarFile = tempFile.canonicalPath
            }
        }

        return jarFile
    }

}
