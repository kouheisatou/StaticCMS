package io.github.kouheisatou.staticcms.util

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.github.kouheisatou.staticcms.model.*
import java.io.File
import kotlinx.coroutines.*

object FileOperations {
    fun scanContentDirectories(rootDir: File): List<ContentDirectory> {
        val contentDir = File(rootDir, "contents")
        if (!contentDir.exists() || !contentDir.isDirectory) {
            // フォールバック: sample_contentsディレクトリを使用
            val sampleDir = File(rootDir, "sample_contents")
            if (sampleDir.exists() && sampleDir.isDirectory) {
                return scanDirectories(sampleDir)
            }
            return emptyList()
        }
        return scanDirectories(contentDir)
    }

    private fun scanDirectories(dir: File): List<ContentDirectory> {
        return dir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { subDir ->
                val csvFile = subDir.listFiles()?.find { it.extension == "csv" }
                if (csvFile != null) {
                    val type = determineDirectoryType(csvFile)
                    val data = readCsvData(csvFile)
                    ContentDirectory(
                        name = subDir.name,
                        path = subDir.absolutePath,
                        type = type,
                        csvFile = csvFile,
                        data = data,
                    )
                } else {
                    null
                }
            } ?: emptyList()
    }

    private fun determineDirectoryType(csvFile: File): DirectoryType {
        try {
            val headers = csvReader().readAll(csvFile).firstOrNull()
            return if (headers != null && headers.contains("thumbnail")) {
                DirectoryType.ARTICLE
            } else {
                DirectoryType.ENUM
            }
        } catch (e: Exception) {
            return DirectoryType.ENUM
        }
    }

    fun readCsvData(csvFile: File): List<CsvRow> {
        try {
            val rows = csvReader().readAllWithHeader(csvFile)
            return rows.map { row ->
                CsvRow(
                    id = row["id"] ?: "",
                    nameJa = row["nameJa"] ?: "",
                    nameEn = row["nameEn"] ?: "",
                    thumbnail = row["thumbnail"],
                    descJa = row["descJa"],
                    descEn = row["descEn"],
                    additionalFields =
                        row.filterKeys {
                            it !in setOf("id", "nameJa", "nameEn", "thumbnail", "descJa", "descEn")
                        },
                )
            }
        } catch (e: Exception) {
            println("Error reading CSV file: ${e.message}")
            return emptyList()
        }
    }

    fun writeCsvData(
        csvFile: File,
        data: List<CsvRow>,
    ) {
        try {
            val headers = mutableSetOf("id", "nameJa", "nameEn")

            // 最初のデータ行から追加フィールドを取得
            data.firstOrNull()?.let { firstRow ->
                firstRow.thumbnail?.let { headers.add("thumbnail") }
                firstRow.descJa?.let { headers.add("descJa") }
                firstRow.descEn?.let { headers.add("descEn") }
                headers.addAll(firstRow.additionalFields.keys)
            }

            csvWriter().open(csvFile) {
                writeRow(headers.toList())
                data.forEach { row ->
                    val values =
                        headers.map { header ->
                            when (header) {
                                "id" -> row.id
                                "nameJa" -> row.nameJa
                                "nameEn" -> row.nameEn
                                "thumbnail" -> row.thumbnail ?: ""
                                "descJa" -> row.descJa ?: ""
                                "descEn" -> row.descEn ?: ""
                                else -> row.additionalFields[header] ?: ""
                            }
                        }
                    writeRow(values)
                }
            }
        } catch (e: Exception) {
            println("Error writing CSV file: ${e.message}")
        }
    }

    fun writeCsvFile(directory: ContentDirectory) {
        writeCsvData(directory.csvFile, directory.data)
    }

    fun readMarkdownFile(articleDir: File): ArticleContent? {
        val markdownFile = File(articleDir, "article.md")
        val mediaDir = File(articleDir, "media")

        return if (markdownFile.exists()) {
            ArticleContent(
                id = articleDir.name,
                markdownFile = markdownFile,
                mediaDirectory = if (mediaDir.exists()) mediaDir else null,
                content = markdownFile.readText(),
            )
        } else {
            null
        }
    }

    fun writeMarkdownFile(
        articleContent: ArticleContent,
        content: String,
    ) {
        try {
            articleContent.markdownFile.writeText(content)
        } catch (e: Exception) {
            println("Error writing markdown file: ${e.message}")
        }
    }

    // 画像の簡単なリサイズ処理（実際の実装では適切な画像処理ライブラリを使用）
    fun processAndCopyImage(
        sourceFile: File,
        targetDir: File,
        maxWidth: Int = 800,
    ): String? {
        try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetFile = File(targetDir, sourceFile.name)
            sourceFile.copyTo(targetFile, overwrite = true)

            return "./media/${sourceFile.name}"
        } catch (e: Exception) {
            println("Error processing image: ${e.message}")
            return null
        }
    }

    suspend fun simulateClone(
        repositoryUrl: String,
        onProgress: (Float) -> Unit,
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                // クローン処理のシミュレーション（実際のGit操作に置き換え可能）
                val phases =
                    listOf(
                        "Connecting to remote repository..." to 0.1f,
                        "Receiving objects..." to 0.6f,
                        "Resolving deltas..." to 0.9f,
                        "Checking out files..." to 1.0f,
                    )

                for ((phase, targetProgress) in phases) {
                    val startProgress =
                        onProgress.let {
                            // 現在の進捗を取得するため、状態を保持
                            var currentProgress =
                                if (targetProgress == 0.1f) {
                                    0f
                                } else {
                                    phases[phases.indexOf(phase to targetProgress) - 1].second
                                }
                            currentProgress
                        }

                    // 段階的に進捗を更新
                    val steps = 20
                    for (i in 0..steps) {
                        val progress =
                            startProgress + (targetProgress - startProgress) * (i.toFloat() / steps)
                        onProgress(progress)
                        delay(100) // より現実的な速度
                    }
                }

                // 現在はシミュレーションのみ - 実際のクローン保存場所の説明
                println("=== クローン保存場所の説明 ===")
                println("現在: シミュレーションのみ（実際のクローンなし）")
                println("使用中のディレクトリ: ${File("doc").absolutePath}")
                println("実装予定のクローン先: ${getCloneDirectory().absolutePath}")

                // 今回はdoc/sample_contentsディレクトリを返す
                val sampleDir = File("doc/sample_contents")
                if (sampleDir.exists()) {
                    sampleDir.parentFile // docディレクトリを返す
                } else {
                    // フォールバック
                    val currentDir = File(".")
                    currentDir
                }
            } catch (e: Exception) {
                println("Clone error: ${e.message}")
                null
            }
        }
    }

    // 実際のクローン機能を実装する場合の推奨保存場所
    private fun getCloneDirectory(): File {
        val userHome = System.getProperty("user.home")
        val appDataDir = File(userHome, ".staticcms")
        if (!appDataDir.exists()) {
            appDataDir.mkdirs()
        }
        return File(appDataDir, "repositories")
    }

    // 実際のGitクローン実装例（将来の実装用）
    suspend fun actualGitClone(
        repositoryUrl: String,
        onProgress: (Float) -> Unit,
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val cloneDir = getCloneDirectory()
                val repoName = repositoryUrl.substringAfterLast("/").removeSuffix(".git")
                val targetDir = File(cloneDir, repoName)

                // 既存のディレクトリがある場合は削除
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }

                // 実際のgitコマンド実行
                val processBuilder =
                    ProcessBuilder("git", "clone", repositoryUrl, targetDir.absolutePath)
                processBuilder.directory(cloneDir)

                val process = processBuilder.start()

                // プロセス監視とプログレス更新
                // 実際の実装では、git clone の標準出力を解析して進捗を計算

                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    targetDir
                } else {
                    null
                }
            } catch (e: Exception) {
                println("Actual clone error: ${e.message}")
                null
            }
        }
    }
}
