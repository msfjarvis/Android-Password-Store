/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.data.storage

import androidx.fragment.app.FragmentActivity
import com.anggrayudi.storage.SimpleStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Wrapper around [SimpleStorage] to allow injecting it through Hilt */
class SAFHelper @AssistedInject constructor(@Assisted activity: FragmentActivity) {
  val storage = SimpleStorage(activity)
}
