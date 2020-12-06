/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.settings

import de.Maxr1998.modernpreferences.PreferenceScreen

interface SettingsProvider {
    fun provideSettings(builder: PreferenceScreen.Builder)
}

