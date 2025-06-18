package io.github.kouheisatou.static_cms.util

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import io.github.kouheisatou.static_cms.model.*
import kotlinx.coroutines.*
import java.io.File
import javax.swing.JFileChooser

object FileOperations {

    fun selectDirectory(): File? {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Select Contents Directory"
        }
        return if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile
        } else null
    }

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
        return dir.listFiles()?.filter { it.isDirectory }?.mapNotNull { subDir ->
            val csvFile = subDir.listFiles()?.find { it.extension == "csv" }
            if (csvFile != null) {
                val type = determineDirectoryType(csvFile)
                val data = readCsvData(csvFile)
                ContentDirectory(
                    name = subDir.name,
                    path = subDir.absolutePath,
                    type = type,
                    csvFile = csvFile,
                    data = data
                )
            } else null
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
                    additionalFields = row.filterKeys { 
                        it !in setOf("id", "nameJa", "nameEn", "thumbnail", "descJa", "descEn")
                    }
                )
            }
        } catch (e: Exception) {
            println("Error reading CSV file: ${e.message}")
            return emptyList()
        }
    }

    fun writeCsvData(csvFile: File, data: List<CsvRow>) {
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
                    val values = headers.map { header ->
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

    fun readMarkdownFile(articleDir: File): ArticleContent? {
        val markdownFile = File(articleDir, "article.md")
        val mediaDir = File(articleDir, "media")
        
        return if (markdownFile.exists()) {
            ArticleContent(
                id = articleDir.name,
                markdownFile = markdownFile,
                mediaDirectory = if (mediaDir.exists()) mediaDir else null,
                content = markdownFile.readText()
            )
        } else null
    }

    fun writeMarkdownFile(articleContent: ArticleContent, content: String) {
        try {
            articleContent.markdownFile.writeText(content)
        } catch (e: Exception) {
            println("Error writing markdown file: ${e.message}")
        }
    }

    // 画像の簡単なリサイズ処理（実際の実装では適切な画像処理ライブラリを使用）
    fun processAndCopyImage(sourceFile: File, targetDir: File, maxWidth: Int = 800): String? {
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

    suspend fun simulateClone(repositoryUrl: String, onProgress: (Float) -> Unit): File? {
        return withContext(Dispatchers.IO) {
            // Git clone をシミュレート
            for (i in 0..100) {
                delay(50)
                onProgress(i / 100f)
            }
            
            // 実際の実装では、ここでgit cloneを実行
            // 今回はsample_contentsディレクトリを返す
            val sampleDir = File("sample_contents")
            if (sampleDir.exists()) sampleDir else null
        }
    }
} 