/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class CrowdinExtension(objects: ObjectFactory) {

    /**
     * Configure the project name on Crowdin
     */
    open var projectName: Property<String> = objects.property()
}
