/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.injection.context

import androidx.fragment.app.FragmentActivity
import dagger.assisted.AssistedFactory
import dev.msfjarvis.aps.data.storage.SAFHelper

@AssistedFactory
interface SAFHelperFactory {
  fun create(activity: FragmentActivity): SAFHelper
}
