package com.example.myapplication

import android.app.DownloadManager
import android.content.Context
import androidx.core.net.toUri
import java.io.File

private const val MODEL_DOWNLOAD_URL =
  "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true"
private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"

object ModelDownloader {

  fun modelFile(context: Context): File =
    File(context.getExternalFilesDir(null), MODEL_FILE_NAME)

  fun isModelDownloaded(context: Context): Boolean = modelFile(context).exists()

  /** Enqueues the model download via the system DownloadManager and returns the download id. */
  fun enqueueDownload(context: Context): Long {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    modelFile(context).delete() // Clear any partial/failed previous download.
    val request =
      DownloadManager.Request(MODEL_DOWNLOAD_URL.toUri())
        .setTitle(MODEL_FILE_NAME)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(context, null, MODEL_FILE_NAME)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
    return downloadManager.enqueue(request)
  }

  /** Returns a value 0..100, or -1 if the download is not in progress / unknown. */
  fun queryProgress(context: Context, downloadId: Long): Int {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
    cursor.use {
      if (!it.moveToFirst()) return -1
      val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
      val status = it.getInt(statusIdx)
      if (status == DownloadManager.STATUS_SUCCESSFUL) return 100
      if (status == DownloadManager.STATUS_FAILED) return -1
      val totalIdx = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
      val downloadedIdx = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
      val total = it.getLong(totalIdx)
      val downloaded = it.getLong(downloadedIdx)
      if (total <= 0) return 0
      return ((downloaded * 100) / total).toInt()
    }
  }

  /** Returns a human-readable failure reason, or null if the download did not fail. */
  fun queryFailureReason(context: Context, downloadId: Long): String? {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
    cursor.use {
      if (!it.moveToFirst()) return "다운로드 정보를 찾을 수 없음"
      val statusIdx = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
      val status = it.getInt(statusIdx)
      if (status != DownloadManager.STATUS_FAILED) return null
      val reasonIdx = it.getColumnIndex(DownloadManager.COLUMN_REASON)
      val reason = it.getInt(reasonIdx)
      return "실패 코드: $reason"
    }
  }
}
