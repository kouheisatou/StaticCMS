package io.github.kouheisatou.staticcms.util

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.github.kouheisatou.staticcms.model.*
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
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

    /** ファイル選択ダイアログを表示して画像ファイルを選択 */
    fun selectImageFile(): File? {
        return try {
            val fileChooser =
                JFileChooser().apply {
                    dialogTitle = "Select Image File"
                    fileFilter =
                        FileNameExtensionFilter(
                            "Image files (*.jpg, *.jpeg, *.png, *.gif, *.bmp)",
                            "jpg",
                            "jpeg",
                            "png",
                            "gif",
                            "bmp")
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    isMultiSelectionEnabled = false
                    currentDirectory = File(System.getProperty("user.home"))
                }

            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFile
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error showing file dialog: ${e.message}")
            null
        }
    }

    /**
     * 画像をリサイズして圧縮し、指定されたファイル名で保存
     *
     * @param sourceFile 元画像ファイル
     * @param targetDir 保存先ディレクトリ
     * @param targetFileName 保存ファイル名（id + 拡張子）
     * @param maxWidth 最大幅（デフォルト800px）
     * @param quality 圧縮品質（0.0-1.0、デフォルト0.8）
     * @return 保存されたファイル名（失敗時はnull）
     */
    fun processAndSaveImage(
        sourceFile: File,
        targetDir: File,
        targetFileName: String,
        maxWidth: Int = 800,
        quality: Float = 0.8f
    ): String? {
        return try {
            // 保存先ディレクトリの作成
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // 元画像の読み込み
            val originalImage =
                ImageIO.read(sourceFile) ?: throw Exception("Cannot read image file")

            // リサイズ計算
            val originalWidth = originalImage.width
            val originalHeight = originalImage.height

            val (newWidth, newHeight) =
                if (originalWidth > maxWidth) {
                    val ratio = maxWidth.toFloat() / originalWidth
                    Pair(maxWidth, (originalHeight * ratio).toInt())
                } else {
                    Pair(originalWidth, originalHeight)
                }

            // リサイズ処理
            val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
            val graphics =
                resizedImage.createGraphics().apply {
                    setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                    setRenderingHint(
                        RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                    setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                }

            graphics.drawImage(
                originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH),
                0,
                0,
                null)
            graphics.dispose()

            // ファイル保存
            val targetFile = File(targetDir, targetFileName)
            val formatName = targetFileName.substringAfterLast(".").lowercase()

            // JPEGの場合は品質設定付きで保存
            if (formatName == "jpg" || formatName == "jpeg") {
                val writers = ImageIO.getImageWritersByFormatName("jpeg")
                if (writers.hasNext()) {
                    val writer = writers.next()
                    val writeParam =
                        writer.defaultWriteParam.apply {
                            compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                            compressionQuality = quality
                        }

                    val output = javax.imageio.stream.FileImageOutputStream(targetFile)
                    writer.output = output
                    writer.write(null, javax.imageio.IIOImage(resizedImage, null, null), writeParam)
                    writer.dispose()
                    output.close()
                } else {
                    ImageIO.write(resizedImage, "jpeg", targetFile)
                }
            } else {
                ImageIO.write(resizedImage, formatName, targetFile)
            }

            println("Image processed and saved: ${targetFile.absolutePath}")
            targetFileName
        } catch (e: Exception) {
            println("Error processing image: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Thumbnailカラムの画像選択と処理を行う（非同期対応版）
     *
     * @param rowId 行のID
     * @param targetDir 保存先ディレクトリ
     * @param onProgress プログレス更新コールバック
     * @return 保存されたファイル名（失敗時はnull）
     */
    suspend fun selectAndProcessThumbnailImageAsync(
        rowId: String,
        targetDir: File,
        onProgress: suspend (Float, String) -> Unit = { _, _ -> }
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                onProgress(0.1f, "Opening file dialog...")

                // ファイル選択ダイアログ表示
                val selectedFile = selectImageFile() ?: return@withContext null

                onProgress(0.3f, "Validating image format...")

                // 拡張子を取得
                val extension = selectedFile.extension.lowercase()
                if (extension !in listOf("jpg", "jpeg", "png", "gif", "bmp")) {
                    println("Unsupported image format: $extension")
                    return@withContext null
                }

                onProgress(0.5f, "Preparing image processing...")

                // ファイル名を id.拡張子 の形式で設定
                val targetFileName = "$rowId.$extension"

                onProgress(0.7f, "Processing and saving image...")

                // 画像処理と保存
                val result =
                    processAndSaveImageAsync(selectedFile, targetDir, targetFileName) { progress ->
                        // Map image processing progress from 0.7 to 1.0
                        val mappedProgress = 0.7f + (progress * 0.3f)
                        runBlocking { onProgress(mappedProgress, "Processing image...") }
                    }

                onProgress(1.0f, "Image processing completed")
                result
            } catch (e: Exception) {
                println("Error in async image selection: ${e.message}")
                null
            }
        }
    }

    /**
     * 画像をリサイズして圧縮し、指定されたファイル名で保存（非同期対応版）
     *
     * @param sourceFile 元画像ファイル
     * @param targetDir 保存先ディレクトリ
     * @param targetFileName 保存ファイル名（id + 拡張子）
     * @param maxWidth 最大幅（デフォルト800px）
     * @param quality 圧縮品質（0.0-1.0、デフォルト0.8）
     * @param onProgress プログレス更新コールバック
     * @return 保存されたファイル名（失敗時はnull）
     */
    suspend fun processAndSaveImageAsync(
        sourceFile: File,
        targetDir: File,
        targetFileName: String,
        maxWidth: Int = 800,
        quality: Float = 0.8f,
        onProgress: (Float) -> Unit = {}
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                onProgress(0.1f)

                // 保存先ディレクトリの作成
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                onProgress(0.2f)

                // 元画像の読み込み
                val originalImage =
                    ImageIO.read(sourceFile) ?: throw Exception("Cannot read image file")

                onProgress(0.4f)

                // リサイズ計算
                val originalWidth = originalImage.width
                val originalHeight = originalImage.height

                val (newWidth, newHeight) =
                    if (originalWidth > maxWidth) {
                        val ratio = maxWidth.toFloat() / originalWidth
                        Pair(maxWidth, (originalHeight * ratio).toInt())
                    } else {
                        Pair(originalWidth, originalHeight)
                    }

                onProgress(0.6f)

                // リサイズ処理
                val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
                val graphics =
                    resizedImage.createGraphics().apply {
                        setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                        setRenderingHint(
                            RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                        setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    }

                graphics.drawImage(
                    originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH),
                    0,
                    0,
                    null)
                graphics.dispose()

                onProgress(0.8f)

                // ファイル保存
                val targetFile = File(targetDir, targetFileName)
                val formatName = targetFileName.substringAfterLast(".").lowercase()

                // JPEGの場合は品質設定付きで保存
                if (formatName == "jpg" || formatName == "jpeg") {
                    val writers = ImageIO.getImageWritersByFormatName("jpeg")
                    if (writers.hasNext()) {
                        val writer = writers.next()
                        val writeParam =
                            writer.defaultWriteParam.apply {
                                compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                                compressionQuality = quality
                            }

                        val output = javax.imageio.stream.FileImageOutputStream(targetFile)
                        writer.output = output
                        writer.write(
                            null, javax.imageio.IIOImage(resizedImage, null, null), writeParam)
                        writer.dispose()
                        output.close()
                    } else {
                        ImageIO.write(resizedImage, "jpeg", targetFile)
                    }
                } else {
                    ImageIO.write(resizedImage, formatName, targetFile)
                }

                onProgress(1.0f)

                println("Image processed and saved: ${targetFile.absolutePath}")
                targetFileName
            } catch (e: Exception) {
                println("Error processing image: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    // 既存のselectAndProcessThumbnailImage関数は後方互換性のために残す
    fun selectAndProcessThumbnailImage(rowId: String, targetDir: File): String? {
        return runBlocking { selectAndProcessThumbnailImageAsync(rowId, targetDir) }
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
