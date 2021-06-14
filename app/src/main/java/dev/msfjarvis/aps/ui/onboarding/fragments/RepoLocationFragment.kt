/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package dev.msfjarvis.aps.ui.onboarding.fragments

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.callback.StorageAccessCallback
import com.anggrayudi.storage.file.StorageType
import com.anggrayudi.storage.file.getStorageId
import com.github.ajalt.timberkt.d
import com.github.ajalt.timberkt.e
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.msfjarvis.aps.R
import dev.msfjarvis.aps.data.repo.PasswordRepository
import dev.msfjarvis.aps.databinding.FragmentRepoLocationBinding
import dev.msfjarvis.aps.injection.context.SAFHelperFactory
import dev.msfjarvis.aps.util.extensions.finish
import dev.msfjarvis.aps.util.extensions.getString
import dev.msfjarvis.aps.util.extensions.listFilesRecursively
import dev.msfjarvis.aps.util.extensions.performTransactionWithBackStack
import dev.msfjarvis.aps.util.extensions.sharedPrefs
import dev.msfjarvis.aps.util.extensions.unsafeLazy
import dev.msfjarvis.aps.util.extensions.viewBinding
import dev.msfjarvis.aps.util.settings.PasswordSortOrder
import dev.msfjarvis.aps.util.settings.PreferenceKeys
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RepoLocationFragment : Fragment(R.layout.fragment_repo_location) {

  @Inject lateinit var safHelperFactory: SAFHelperFactory
  private lateinit var storage: SimpleStorage
  private val settings by unsafeLazy { requireActivity().applicationContext.sharedPrefs }
  private val binding by viewBinding(FragmentRepoLocationBinding::bind)
  private val sortOrder: PasswordSortOrder
    get() = PasswordSortOrder.getSortOrder(settings)

  private val repositoryInitAction =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == AppCompatActivity.RESULT_OK) {
        initializeRepositoryInfo()
      }
    }

  private val requestStoragePermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        storage.requestStorageAccess(REQUEST_CODE_STORAGE_ACCESS)
      }
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.hidden.setOnClickListener { createRepoInHiddenDir() }
    binding.sdcard.setOnClickListener { createRepoFromExternalDir() }
    storage = safHelperFactory.create(requireActivity()).storage
    if (savedInstanceState != null) {
      storage.onRestoreInstanceState(savedInstanceState)
    }
    storage.storageAccessCallback =
      object : StorageAccessCallback {
        override fun onRootPathNotSelected(
          requestCode: Int,
          rootPath: String,
          uri: Uri,
          selectedStorageType: StorageType,
          expectedStorageType: StorageType
        ) {
          MaterialAlertDialogBuilder(requireContext())
            .setMessage("Please select $rootPath")
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setPositiveButton(android.R.string.ok) { _, _ ->
              val initialRoot =
                if (expectedStorageType.isExpected(selectedStorageType)) selectedStorageType
                else expectedStorageType
              storage.requestStorageAccess(
                REQUEST_CODE_STORAGE_ACCESS,
                initialRoot,
                expectedStorageType
              )
            }
            .show()
        }

        override fun onCanceledByUser(requestCode: Int) {
          Toast.makeText(requireContext(), "Canceled by user", Toast.LENGTH_SHORT).show()
        }

        override fun onRootPathPermissionGranted(requestCode: Int, root: DocumentFile) {
          Toast.makeText(
              requireContext(),
              "Storage access has been granted for ${root.getStorageId(requireContext())}",
              Toast.LENGTH_SHORT
            )
            .show()
        }

        override fun onStoragePermissionDenied(requestCode: Int) {
          requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
      }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    storage.onSaveInstanceState(outState)
    super.onSaveInstanceState(outState)
  }

  /** Initializes an empty repository in the app's private directory */
  private fun createRepoInHiddenDir() {
    settings.edit {
      putBoolean(PreferenceKeys.GIT_EXTERNAL, false)
      remove(PreferenceKeys.GIT_EXTERNAL_REPO)
    }
    initializeRepositoryInfo()
  }

  /** Initializes an empty repository in a selected directory if one does not already exist */
  private fun createRepoFromExternalDir() {
    settings.edit { putBoolean(PreferenceKeys.GIT_EXTERNAL, true) }
    val externalRepo = settings.getString(PreferenceKeys.GIT_EXTERNAL_REPO)
    if (externalRepo == null) {
      storage.requestStorageAccess(REQUEST_CODE_STORAGE_ACCESS)
    } else {
      MaterialAlertDialogBuilder(requireActivity())
        .setTitle(resources.getString(R.string.directory_selected_title))
        .setMessage(resources.getString(R.string.directory_selected_message, externalRepo))
        .setPositiveButton(resources.getString(R.string.use)) { _, _ -> initializeRepositoryInfo() }
        .setNegativeButton(resources.getString(R.string.change)) { _, _ ->
          repositoryInitAction.launch(null)
        }
        .show()
    }
  }

  private fun checkExternalDirectory(): Boolean {
    if (settings.getBoolean(PreferenceKeys.GIT_EXTERNAL, false) &&
        settings.getString(PreferenceKeys.GIT_EXTERNAL_REPO) != null
    ) {
      val externalRepoPath = settings.getString(PreferenceKeys.GIT_EXTERNAL_REPO)
      val dir = externalRepoPath?.let { File(it) }
      if (dir != null && // The directory could be opened
        dir.exists() && // The directory exists
          dir.isDirectory && // The directory, is really a directory
          dir.listFilesRecursively().isNotEmpty() && // The directory contains files
          // The directory contains a non-zero number of password files
          PasswordRepository.getPasswords(
              dir,
              PasswordRepository.getRepositoryDirectory(),
              sortOrder
            )
            .isNotEmpty()
      ) {
        PasswordRepository.closeRepository()
        return true
      }
    }
    return false
  }

  private fun createRepository() {
    val localDir = PasswordRepository.getRepositoryDirectory()
    runCatching {
      check(localDir.exists() || localDir.mkdir()) { "Failed to create directory!" }
      PasswordRepository.createRepository(localDir)
      if (!PasswordRepository.isInitialized) {
        PasswordRepository.initialize()
      }
      parentFragmentManager.performTransactionWithBackStack(KeySelectionFragment.newInstance())
    }
      .onFailure { e ->
        e(e)
        if (!localDir.delete()) {
          d { "Failed to delete local repository: $localDir" }
        }
        finish()
      }
  }

  private fun initializeRepositoryInfo() {
    val externalRepo = settings.getBoolean(PreferenceKeys.GIT_EXTERNAL, false)
    val externalRepoPath = settings.getString(PreferenceKeys.GIT_EXTERNAL_REPO)
    if (externalRepo && externalRepoPath != null) {
      if (checkExternalDirectory()) {
        finish()
        return
      }
    }
    createRepository()
  }

  companion object {

    private const val REQUEST_CODE_STORAGE_ACCESS = 100

    fun newInstance(): RepoLocationFragment = RepoLocationFragment()
  }
}
