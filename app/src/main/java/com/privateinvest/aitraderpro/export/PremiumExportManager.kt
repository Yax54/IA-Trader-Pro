package com.privateinvest.aitraderpro.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Export premium pleine page.
 *
 * Important : ce moteur ne capture pas seulement le viewport visible.
 * Il génère une image / un PDF long à partir de toutes les sections du rapport,
 * donc le contenu complet est exporté même si l'écran Compose est scrollable.
 */
data class ExportLine(
    val label: String,
    val value: String,
    val level: ExportLevel = ExportLevel.NEUTRAL
)

data class ExportSection(
    val title: String,
    val subtitle: String? = null,
    val lines: List<ExportLine> = emptyList(),
    val notes: List<String> = emptyList()
)

data class ExportReport(
    val title: String,
    val subtitle: String,
    val sections: List<ExportSection>,
    val footer: String = "AI Trader Pro — aide à la décision, validation manuelle obligatoire."
)

enum class ExportLevel { POSITIVE, WARNING, DANGER, NEUTRAL }

enum class ExportFormat { PNG, PDF }

object PremiumExportManager {
    private const val WIDTH = 1240
    private const val MARGIN = 64
    private const val CARD_RADIUS = 26f

    fun exportAndShare(context: Context, report: ExportReport, format: ExportFormat): File {
        val file = when (format) {
            ExportFormat.PNG -> createPng(context, report)
            ExportFormat.PDF -> createPdf(context, report)
        }
        shareFile(context, file, format)
        return file
    }

    fun createPng(context: Context, report: ExportReport): File {
        val height = estimateHeight(report)
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawReport(canvas, report, height)
        val file = File(context.cacheDir, "ai_trader_export_${timestamp()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        bitmap.recycle()
        return file
    }

    fun createPdf(context: Context, report: ExportReport): File {
        val height = estimateHeight(report)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(WIDTH, height, 1).create()
        val page = document.startPage(pageInfo)
        drawReport(page.canvas, report, height)
        document.finishPage(page)
        val file = File(context.cacheDir, "ai_trader_export_${timestamp()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun shareFile(context: Context, file: File, format: ExportFormat) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = if (format == ExportFormat.PNG) "image/png" else "application/pdf"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager l'export AI Trader Pro"))
    }

    private fun drawReport(canvas: Canvas, report: ExportReport, totalHeight: Int) {
        canvas.drawColor(Color.rgb(5, 12, 24))
        val titlePaint = paint(Color.WHITE, 44f, true)
        val subtitlePaint = paint(Color.rgb(203, 213, 225), 25f, false)
        val smallPaint = paint(Color.rgb(148, 163, 184), 22f, false)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(19, 34, 56) }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(56, 189, 248)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        var y = 78f
        canvas.drawText(report.title, MARGIN.toFloat(), y, titlePaint)
        y += 40f
        canvas.drawText(report.subtitle, MARGIN.toFloat(), y, subtitlePaint)
        y += 34f
        canvas.drawText("Généré le ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())}", MARGIN.toFloat(), y, smallPaint)
        y += 44f

        report.sections.forEach { section ->
            val cardTop = y
            val sectionHeight = estimateSectionHeight(section).toFloat()
            canvas.drawRoundRect(
                MARGIN.toFloat(),
                cardTop,
                (WIDTH - MARGIN).toFloat(),
                cardTop + sectionHeight,
                CARD_RADIUS,
                CARD_RADIUS,
                cardPaint
            )
            canvas.drawRoundRect(
                MARGIN.toFloat(),
                cardTop,
                (WIDTH - MARGIN).toFloat(),
                cardTop + sectionHeight,
                CARD_RADIUS,
                CARD_RADIUS,
                borderPaint
            )

            y += 50f
            canvas.drawText(section.title, (MARGIN + 30).toFloat(), y, paint(Color.WHITE, 31f, true))
            section.subtitle?.let {
                y += 34f
                wrapText(canvas, it, MARGIN + 30f, y, WIDTH - (MARGIN * 2) - 60f, paint(Color.rgb(203, 213, 225), 22f, false), 28f)
                y += 34f
            } ?: run { y += 20f }

            section.lines.forEach { line ->
                val lineColor = when (line.level) {
                    ExportLevel.POSITIVE -> Color.rgb(34, 197, 94)
                    ExportLevel.WARNING -> Color.rgb(245, 158, 11)
                    ExportLevel.DANGER -> Color.rgb(225, 29, 72)
                    ExportLevel.NEUTRAL -> Color.rgb(226, 232, 240)
                }
                canvas.drawText(line.label, (MARGIN + 30).toFloat(), y, paint(Color.rgb(148, 163, 184), 23f, false))
                canvas.drawText(line.value, (WIDTH - MARGIN - 430).toFloat(), y, paint(lineColor, 26f, true))
                y += 42f
            }

            section.notes.forEach { note ->
                y = wrapText(canvas, "• $note", MARGIN + 30f, y, WIDTH - (MARGIN * 2) - 60f, paint(Color.rgb(203, 213, 225), 22f, false), 30f)
                y += 8f
            }
            y = cardTop + sectionHeight + 28f
        }

        wrapText(canvas, report.footer, MARGIN.toFloat(), totalHeight - 74f, WIDTH - (MARGIN * 2).toFloat(), smallPaint, 27f)
    }

    private fun estimateHeight(report: ExportReport): Int {
        val sections = report.sections.sumOf { estimateSectionHeight(it) + 28 }
        return max(1700, 260 + sections + 140)
    }

    private fun estimateSectionHeight(section: ExportSection): Int {
        val subtitle = if (section.subtitle.isNullOrBlank()) 0 else 70
        val notesHeight = section.notes.sumOf { max(40, (it.length / 68 + 1) * 34) }
        return 86 + subtitle + section.lines.size * 42 + notesHeight + 52
    }

    private fun paint(color: Int, size: Float, bold: Boolean): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private fun wrapText(canvas: Canvas, text: String, x: Float, yStart: Float, maxWidth: Float, paint: Paint, lineHeight: Float): Float {
        var y = yStart
        val words = text.split(" ")
        var line = ""
        words.forEach { word ->
            val next = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(next) <= maxWidth) {
                line = next
            } else {
                canvas.drawText(line, x, y, paint)
                y += lineHeight
                line = word
            }
        }
        if (line.isNotBlank()) {
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }
        return y
    }

    private fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date())
}
