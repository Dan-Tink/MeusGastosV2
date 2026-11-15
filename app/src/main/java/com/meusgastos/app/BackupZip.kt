package com.meusgastos.app

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup em um único arquivo .zip: o JSON com os dados mais os arquivos dos comprovantes.
 * O app lê o .zip direto (e ainda aceita o .json dos backups antigos), sem extrair nada à mão.
 */
object BackupZip {
    private const val JSON_ENTRY = "backup.json"
    private const val DIR = "comprovantes/"
    private const val MAX_JSON = 20 * 1024 * 1024
    private const val MAX_FILE = 50L * 1024 * 1024
    private const val MAX_TOTAL = 500L * 1024 * 1024
    private const val MAX_ENTRIES = 5000

    /** Resultado da leitura: o JSON e a pasta temporária onde os comprovantes ficaram esperando. */
    class Parsed(val json: String, val staging: File?)

    fun write(context: Context, out: OutputStream, json: String, receipts: List<String>) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            receipts.distinct().forEach { name ->
                val file = Receipts.fileIfExists(context, name) ?: return@forEach
                zip.putNextEntry(ZipEntry(DIR + name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Lê um .zip ou um .json antigo. Lança exceção se o arquivo for inválido ou grande demais. */
    fun read(context: Context, input: InputStream): Parsed {
        val buffered = input.buffered()
        buffered.mark(4)
        val head = ByteArray(2)
        val n = buffered.read(head)
        buffered.reset()
        val isZip = n == 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
        if (!isZip) {
            val bytes = buffered.readNBytes(MAX_JSON + 1)
            require(bytes.size <= MAX_JSON) { "arquivo grande demais" }
            return Parsed(String(bytes, Charsets.UTF_8), null)
        }

        val staging = File(context.cacheDir, "restore_staging").also {
            it.deleteRecursively()
            it.mkdirs()
        }
        var json: String? = null
        var total = 0L
        var count = 0
        try {
            ZipInputStream(buffered).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    require(++count <= MAX_ENTRIES) { "arquivo com itens demais" }
                    if (entry.name == JSON_ENTRY) {
                        val bytes = zip.readNBytes(MAX_JSON + 1)
                        require(bytes.size <= MAX_JSON) { "arquivo grande demais" }
                        json = String(bytes, Charsets.UTF_8)
                    } else if (entry.name.startsWith(DIR)) {
                        // Só nomes que o app gera: nada de subpastas nem "../".
                        val name = entry.name.removePrefix(DIR)
                        if (!Receipts.isSafeName(name)) continue
                        val target = File(staging, name)
                        var size = 0L
                        target.outputStream().use { o ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                val r = zip.read(buf)
                                if (r < 0) break
                                size += r
                                total += r
                                require(size <= MAX_FILE && total <= MAX_TOTAL) { "arquivo grande demais" }
                                o.write(buf, 0, r)
                            }
                        }
                    }
                }
            }
            require(json != null) { "backup.json ausente" }
        } catch (e: Exception) {
            staging.deleteRecursively()
            throw e
        }
        return Parsed(json!!, staging)
    }

    /** Depois de restaurar os dados: coloca os comprovantes no lugar e apaga a pasta temporária. */
    fun finish(context: Context, parsed: Parsed, success: Boolean) {
        val staging = parsed.staging ?: return
        try {
            if (success) staging.listFiles()?.forEach { Receipts.importFile(context, it) }
        } finally {
            staging.deleteRecursively()
        }
    }
}
