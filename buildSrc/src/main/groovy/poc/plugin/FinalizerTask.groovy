package poc.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.Task
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import poc.utils.FileFinder
import poc.utils.utilities

/**
 *
 */
class FinalizerTask extends DefaultTask {

    Project parentProject
    Task parentTask                                                             // dex transformation task
    Task resourceProcessTask                                                    // processResources task that generates the compiled .ap_ file
    Task jniTask

    // inputs dex file and changes it in place
    String variantName
    String dexFilePath
    File apFile
    Set<File> jniFolderSet
    boolean isLibraryMode


    @TaskAction
    void finalize() {

        // For Android Gradle Plugin prior to v3.0, the .ap_ file is stored at ${project.buildDir}/intermediates/res/
        // For Android Gradle Plugin          v3.0, the .ap_ file is stored at ${project.buildDir}/intermediates/res/${variantName}/
        // We search for it from ${project.buildDir} anyway.

        def apFiles = FileFinder.lookupForFiles(parentProject.buildDir, ".*${variantName}.ap_")
        if (apFiles.size() == 0) {
            throw new RuntimeException("Cannot find .ap_ for ${variantName} build under " + parentProject.buildDir.canonicalPath)
        } else if (apFiles.size() > 1) {
            throw new RuntimeException("More than one .ap_ found for ${variantName} build under " + parentProject.buildDir.canonicalPath)
        }
        apFile = apFiles[0]

        jniFolderSet = jniTask.getOutputs().getFiles() as Set<File>

        // Look up for classes.dex from parent task's outputs
        def dexOutputs = parentTask.getOutputs()
        if (false == dexOutputs.hasOutput || dexOutputs.getFiles().size() == 0) {
            throw new RuntimeException("Task " + parentTask.name + " has NO outputs")
        }

        def classesDexSet = new ArrayList<File>()
        dexOutputs.getFiles().each {
            classesDexSet += FileFinder.lookupForFiles(it, "classes", ".dex")
        }

        if (classesDexSet.size() == 0) {
            throw new RuntimeException("No classes.dex found ")
        }
        dexFilePath = classesDexSet.collect{ it.absolutePath }.join("${File.pathSeparator}")    // We support multidex here.
        // custom finalizer code ...

    }
    
}
