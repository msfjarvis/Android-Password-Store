/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.settings

import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.onClick
import de.Maxr1998.modernpreferences.helpers.pref
import dev.msfjarvis.aps.R
import dev.msfjarvis.aps.ui.git.config.GitConfigActivity
import dev.msfjarvis.aps.ui.git.config.GitServerConfigActivity
import dev.msfjarvis.aps.ui.proxy.ProxySelectorActivity
import dev.msfjarvis.aps.util.git.sshj.SshKey
import dev.msfjarvis.aps.util.settings.GitSettings
import dev.msfjarvis.aps.util.settings.PreferenceKeys
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.appcompat.app.AppCompatActivity
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RepositorySettings(val activity: ComponentActivity) : SettingsProvider {

    private fun <T : Any> launchActivity(clazz: Class<T>) {
        activity.startActivity(Intent(activity, clazz))
    }

    private val sshKeyImportAction = activity.registerForActivityResult(OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        with(activity) {
            runCatching {
                SshKey.import(uri)
                Toast.makeText(this, resources.getString(R.string.ssh_key_success_dialog_title), Toast.LENGTH_LONG).show()
                setResult(AppCompatActivity.RESULT_OK)
                finish()
            }.onFailure { e ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(resources.getString(R.string.ssh_key_error_dialog_title))
                    .setMessage(e.message)
                    .setPositiveButton(resources.getString(R.string.dialog_ok), null)
                    .show()
            }
        }
    }

    /**
     * Opens a file explorer to import the private key
     */
    private fun importSshKey() {
        if (SshKey.exists) {
            MaterialAlertDialogBuilder(activity).run {
                setTitle(R.string.ssh_keygen_existing_title)
                setMessage(R.string.ssh_keygen_existing_message)
                setPositiveButton(R.string.ssh_keygen_existing_replace) { _, _ ->
                    sshKeyImportAction.launch(arrayOf("*/*"))
                }
                setNegativeButton(R.string.ssh_keygen_existing_keep) { _, _ -> }
                show()
            }
        } else {
            sshKeyImportAction.launch(arrayOf("*/*"))
        }
    }

    override fun provideSettings(builder: PreferenceScreen.Builder) {
        builder.apply {
            pref(PreferenceKeys.GIT_SERVER_INFO) {
                titleRes = R.string.pref_edit_git_server_settings
                onClick {
                    launchActivity(GitServerConfigActivity::class.java)
                    true
                }
            }
            pref(PreferenceKeys.PROXY_SETTINGS) {
                titleRes = R.string.pref_edit_proxy_settings
                visible = GitSettings.url?.startsWith("https") == true
                onClick {
                    launchActivity(ProxySelectorActivity::class.java)
                    true
                }
            }
            pref(PreferenceKeys.GIT_CONFIG) {
                titleRes = R.string.pref_edit_git_config
                onClick {
                    launchActivity(GitConfigActivity::class.java)
                    true
                }
            }
            pref(PreferenceKeys.GIT_CONFIG) {
                titleRes = R.string.pref_import_ssh_key_title
                onClick {
                    importSshKey()
                    true
                }
            }
        }
    }
}
