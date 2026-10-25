package com.meusgastos.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Comprovantes ficam copiados na pasta privada do app, então não somem se o original for apagado. */
object Receipts {
    private fun dir(context: Context): File =
        File(context.filesDir, "comprovantes").also { it.mkdirs() }

    fun file(context: Context, name: String): File = File(dir(context), name)

    fun exists(context: Context, name: String?): Boolean =
        name != null && file(context, name).exists()

    private fun uriFor(context: Context, name: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file(context, name))

    /** Copia uma foto ou PDF escolhido pelo usuário. Retorna o nome salvo, ou null se não for possível. */
    fun saveCopy(context: Context, source: Uri): String? {
        val mime = context.contentResolver.getType(source)
        val ext = when {
            mime == "application/pdf" -> "pdf"
            mime == "image/png" -> "png"
            mime != null && mime.startsWith("image/") -> "jpg"
            else -> return null
        }
        val name = "comp_${System.currentTimeMillis()}.$ext"
        return try {
            val input = context.contentResolver.openInputStream(source) ?: return null
            input.use { i -> file(context, name).outputStream().use { o -> i.copyTo(o) } }
            name
        } catch (e: Exception) {
            file(context, name).delete()
            null
        }
    }

    /** Prepara o destino de uma foto tirada na hora: nome do arquivo e o endereço para a câmera gravar. */
    fun newPhoto(context: Context): Pair<String, Uri> {
        val name = "comp_${System.currentTimeMillis()}.jpg"
        return name to uriFor(context, name)
    }

    fun delete(context: Context, name: String?) {
        if (name != null) file(context, name).delete()
    }

    /** Abre o comprovante no visualizador do aparelho. Retorna false se não deu para abrir. */
    fun open(context: Context, name: String): Boolean {
        if (!exists(context, name)) return false
        val mime = if (name.endsWith(".pdf")) "application/pdf" else "image/*"
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uriFor(context, name), mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
