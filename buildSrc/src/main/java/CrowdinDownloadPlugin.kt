/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

import de.undercouch.gradle.tasks.download.Download
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.unzipTo

@Suppress("UnstableApiUsage")
class CrowdinDownloadPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create<CrowdinExtension>("crowdin")
        val projectName = extension.projectName
        project.tasks.register<Download>("downloadCrowdin") {
            src("https://crowdin.com/backend/download/project/${projectName}.zip")
            dest("build/translations.zip")
            overwrite(true)
        }
        project.tasks.register<Delete>("deleteCrowdin") {
            delete("src/main/res/values-*/strings.xml")
        }
        project.tasks.register<Copy>("extractCrowdin") {
            setDependsOn(setOf("downloadCrowdin", "cleanCrowdin"))
            setMustRunAfter(setOf("downloadCrowdin"))
            doLast {
                unzipTo(File("build/translations"), File("build/translations.zip"))
            }
            doFirst {
                File("build/translations").deleteRecursively()
            }
        }

        project.tasks.register<Copy>("crowdin") {
            setMustRunAfter(setOf("extractCrowdin"))
            from("build/translations/${projectName}/develop/${project.name}/src/main/res")
            into("src/main/res")
            doLast {
                File("build/translations").deleteRecursively()
                File("build/translations.zip").delete()
            }
        }
    }
}
