package poc.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.Plugin
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.GUtil
import com.android.build.api.transform.*
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.QualifiedContent.ContentType
import com.android.build.api.transform.QualifiedContent.Scope
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import org.apache.commons.io.FileUtils
import com.google.common.collect.ImmutableSet

import java.lang.reflect.Method
import java.lang.reflect.Field

import poc.utils.utilities
import poc.utils.Versions


class AndroidPluginData {
    private def variant = null;
    private Project project = null;
    private File pocLogPath = null;

    /**
     * Constructor to create an android data object
     * that represents this project and build variant
     *
     * @param project
     *      The gradle Project that this android data represents
     * @param variant
     *      The build variant that this android data represents
     */
    AndroidPluginData(Project project, def variant) {
        if (project == null || variant == null) {
            throw new RuntimeException("Bad parameter to initialize AndroidPluginData.");
        }

        this.project = project
        this.variant = variant
        this.pocLogPath = new File(this.project.buildDir, "log" + File.separator + "${this.variant.name}")

        if (! hasAndroid()) {
            throw new RuntimeException("You are not running it with an Android project.");
        }

        // ************************************************** DEBUG
        //utilities.printTasksIO( this.project.tasks, true );

        FinalizerTask ft = this.project.tasks.create(
            name: "pocFinalizerFor${this.variant.name.capitalize()}",
            type: FinalizerTask) { FinalizerTask task->
            task.with {
                parentProject = this.project
                parentTask = getDexTransformationTask()
                jniTask = getMergeJniLibFoldersTask()
                variantName = "${this.variant.name}"
                isLibraryMode = false
            }
        }

        ft.outputs.upToDateWhen {return false}
        ft.dependsOn(ft.parentTask)
        ft.dependsOn(ft.jniTask)

        def mergeAssetsTask = getMergeAssetsTask()
        ft.dependsOn(mergeAssetsTask)

        def packagingTask = getPackageApplicationTask()
        packagingTask.dependsOn(ft)
    }

    /**
     * Check if the project executes on top of an Android Gradle plugin
     *
     * @return boolean
     */
    public boolean hasAndroid() {
        return this.project.hasProperty("android")
    }

    /**
     * Retrieves the dex transformation task specific to the android data's
     * build variant. This task is responsible for creating the dex file(s)
     */
    public Task getDexTransformationTask() {
        if (Versions.isPluginVersion3orAbove()) {
            if (this.variant.buildType.debuggable == true) {                    // debug build
                return getGradleTask("transformDexArchiveWithDexMergerFor${this.variant.name.capitalize()}")

            } else {                                                            // release build

                // If the app contains native code, the root dex transformation task is transformClassesWithPreDexFor${variant}, which generates the dex file(s).
                // Otherwise the root dex transformation task is transformClassesWithDexFor${variant}.
                // Same logic applies for Kotlin apps BTW.

                if (hasNativeCodeBuildTask()) {
                    return getGradleTask("transformClassesWithPreDexFor${this.variant.name.capitalize()}", false)
                } else {
                    return getGradleTask("transformClassesWithDexFor${this.variant.name.capitalize()}", false)
                }
            }
        } else {
            return getGradleTask("transformClassesWithDexFor${this.variant.name.capitalize()}", false)
        }
    }

    /**
     * Retrieves the processResources task specific to the android data's
     * build variant. This task is responsible for creating the ap_ file
     */
    public Task getProcessResourcesTask() {
        return getGradleTask("process${this.variant.name.capitalize()}Resources")
    }

    /**
     * Retrieves the mergeAssets task. AGP 2.2+ changes the tasks order, 
     * and this task typically run after Finalizer task executes.
     */
    public Task getMergeAssetsTask() {
        return getGradleTask("merge${this.variant.name.capitalize()}Assets")
    }

    /**
     * Retrieves the MergeJniLibFolders task specific to the android data's
     * build variant. This task takes cares of JNI stuff
     */
    public Task getMergeJniLibFoldersTask() {
        return getGradleTask("merge${this.variant.name.capitalize()}JniLibFolders")
    }

    /**
     * Retrieves the application packaging task specific to the android data's
     * build variant. This task handles packaging up the application and
     * build artifacts into an APK
     */
    public Task getPackageApplicationTask() {
        return getGradleTask("package${this.variant.name.capitalize()}")
    }

    /**
     * Check to see if the project contains native code build task
     */
    public boolean hasNativeCodeBuildTask() {
        return null != getGradleTask("externalNativeBuild${this.variant.name.capitalize()}", false)
    }

    /**
     * Retrieves Gradle task
     *
     * @return
     *      The gradle task, or throws exception if it can't be found
     */
    public Task getGradleTask(String taskName, boolean throwException = true) {
        Task t = getTask(taskName)
        if (t == null && throwException == true) {
            throw new RuntimeException("Error retriving ${taskName} task");
        }

        return t
    }

    /**
     * Grab task directly from the task queue by name
     * @param taskName
     *      Name of the task to grab
     * @return
     *      The FIRST task if it exists on the queue, or null if it does not
     */
    public Task getTask(String taskName, boolean ignoreCase = false) {
        if (this.project.tasks.size() == 0)
            throw new RuntimeException("The project has an empty task set.");   // Which is rare.

        Task t = null

        this.project.tasks.any {
            if (ignoreCase == true && it.getName().equalsIgnoreCase(taskName) ) {
                t = it
                return true                                                     // break from closure
            } else if (it.getName().equals(taskName)) {
                t = it
                return true                                                     // break from closure
            }
        }

        return t
    }
}
